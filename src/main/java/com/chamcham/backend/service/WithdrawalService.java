package com.chamcham.backend.service;

import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.PayoutMethod;
import com.chamcham.backend.entity.Wallet;
import com.chamcham.backend.entity.WithdrawalRequest;
import com.chamcham.backend.entity.CreatorPayoutPreference;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.entity.enums.WithdrawalStatus;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.CreatorPayoutPreferenceRepository;
import com.chamcham.backend.repository.PayoutMethodRepository;
import com.chamcham.backend.repository.WalletRepository;
import com.chamcham.backend.repository.WithdrawalRequestRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WithdrawalService {

    private static final Logger log = LoggerFactory.getLogger(WithdrawalService.class);

    private final WithdrawalRequestRepository withdrawalRepo;
    private final WalletRepository walletRepository;
    private final PayoutMethodRepository payoutMethodRepository;
    private final CreatorRepository creatorRepository;
    private final CreatorPayoutPreferenceRepository creatorPayoutPreferenceRepository;
    private final PaymentAuditService paymentAuditService;

    public WithdrawalService(WithdrawalRequestRepository withdrawalRepo, WalletRepository walletRepository,
                             PayoutMethodRepository payoutMethodRepository,
                             CreatorRepository creatorRepository,
                             CreatorPayoutPreferenceRepository creatorPayoutPreferenceRepository,
                             PaymentAuditService paymentAuditService) {
        this.withdrawalRepo = withdrawalRepo;
        this.walletRepository = walletRepository;
        this.payoutMethodRepository = payoutMethodRepository;
        this.creatorRepository = creatorRepository;
        this.creatorPayoutPreferenceRepository = creatorPayoutPreferenceRepository;
        this.paymentAuditService = paymentAuditService;
    }

    @Transactional
    public WithdrawalRequest requestWithdrawal(UUID userId, UserRole role, UUID payoutMethodId, int amount) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can withdraw");
        if (amount <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        if (amount > 5_000_000) throw new ApiException(HttpStatus.BAD_REQUEST, "Amount exceeds single withdrawal limit of PKR 5,000,000");

        Creator creator = creatorRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));
        Wallet wallet = walletRepository.findByCreatorId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No wallet found"));

        int minimum = creatorPayoutPreferenceRepository.findById(userId)
                .map(CreatorPayoutPreference::getMinimumPayoutAmount)
                .orElse(1000);

        // All amounts are in whole PKR (not paisa). Minimum floor is PKR 1,000.
        // creator_payout_preferences.minimum_payout_amount defaults to PKR 5,000.
        if (amount < Math.max(1000, minimum)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Amount is below your minimum payout threshold");
        }

        if (wallet.getAvailableBalance() < amount) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient available balance");
        }

        PayoutMethod pm = payoutMethodRepository.findById(payoutMethodId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payout method not found"));
        if (!pm.getCreator().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This payout method does not belong to you");
        }

        wallet.setAvailableBalance(wallet.getAvailableBalance() - amount);
        wallet.setPendingBalance(wallet.getPendingBalance() + amount);
        walletRepository.save(wallet);

        WithdrawalRequest wr = WithdrawalRequest.builder()
                .creator(creator).payoutMethod(pm).amount(amount).status(WithdrawalStatus.PENDING)
                .build();
        WithdrawalRequest saved = withdrawalRepo.save(wr);
        paymentAuditService.log(userId, null, "CREATOR_WITHDRAWAL_REQUESTED", "withdrawal_request", saved.getId().toString(),
                "amount=" + amount);
        return saved;
    }

    public Page<WithdrawalRequest> list(UUID userId, UserRole role, int page, int size) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can view withdrawals");
        return withdrawalRepo.findByCreatorIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional
    public WithdrawalRequest processWithdrawal(UUID withdrawalId, WithdrawalStatus newStatus) {
        WithdrawalRequest wr = withdrawalRepo.findById(withdrawalId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Withdrawal request not found"));

        if (wr.getStatus() != WithdrawalStatus.PENDING && wr.getStatus() != WithdrawalStatus.PROCESSING) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot update a withdrawal that is already " + wr.getStatus().name().toLowerCase());
        }
        if (newStatus == WithdrawalStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot revert to PENDING");
        }

        Wallet wallet = walletRepository.findByCreatorId(wr.getCreator().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Wallet not found for creator"));

        if (newStatus == WithdrawalStatus.COMPLETED) {
            // Deduct from pending balance — funds have now left the platform
            wallet.setPendingBalance(Math.max(0, wallet.getPendingBalance() - wr.getAmount()));
            walletRepository.save(wallet);
        } else if (newStatus == WithdrawalStatus.FAILED) {
            // Return amount to available balance
            wallet.setPendingBalance(Math.max(0, wallet.getPendingBalance() - wr.getAmount()));
            wallet.setAvailableBalance(wallet.getAvailableBalance() + wr.getAmount());
            walletRepository.save(wallet);
            log.error("WITHDRAWAL FAILED: withdrawalId={}, creatorId={}, amount={}",
                    withdrawalId, wr.getCreator().getId(), wr.getAmount());
        }

        wr.setStatus(newStatus);
        WithdrawalRequest saved = withdrawalRepo.save(wr);
        if (newStatus == WithdrawalStatus.COMPLETED) {
            log.info("Withdrawal completed: withdrawalId={}, creatorId={}, amount={}",
                    withdrawalId, wr.getCreator().getId(), wr.getAmount());
        }
        paymentAuditService.log(wr.getCreator().getId(), null, "WITHDRAWAL_STATUS_UPDATED",
                "withdrawal_request", saved.getId().toString(), "status=" + newStatus.name().toLowerCase());
        return saved;
    }

    public Page<WithdrawalRequest> listForAdmin(String search, WithdrawalStatus status, int page, int size) {
        WithdrawalStatus statusFilter = status;
        return withdrawalRepo.searchForAdmin(
                search == null || search.isBlank() ? null : search.trim(),
                statusFilter,
                PageRequest.of(Math.max(page, 0), Math.min(size, 100)));
    }
}

