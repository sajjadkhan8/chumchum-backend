package com.chamcham.backend.service;

import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.PayoutMethod;
import com.chamcham.backend.entity.Wallet;
import com.chamcham.backend.entity.WithdrawalRequest;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.entity.enums.WithdrawalStatus;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.PayoutMethodRepository;
import com.chamcham.backend.repository.WalletRepository;
import com.chamcham.backend.repository.WithdrawalRequestRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WithdrawalService {

    private final WithdrawalRequestRepository withdrawalRepo;
    private final WalletRepository walletRepository;
    private final PayoutMethodRepository payoutMethodRepository;
    private final CreatorRepository creatorRepository;

    public WithdrawalService(WithdrawalRequestRepository withdrawalRepo, WalletRepository walletRepository,
                             PayoutMethodRepository payoutMethodRepository, CreatorRepository creatorRepository) {
        this.withdrawalRepo = withdrawalRepo;
        this.walletRepository = walletRepository;
        this.payoutMethodRepository = payoutMethodRepository;
        this.creatorRepository = creatorRepository;
    }

    @Transactional
    public WithdrawalRequest requestWithdrawal(UUID userId, UserRole role, UUID payoutMethodId, int amount) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can withdraw");
        if (amount <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Amount must be positive");

        Creator creator = creatorRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));
        Wallet wallet = walletRepository.findByCreatorId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No wallet found"));

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
        return withdrawalRepo.save(wr);
    }

    public Page<WithdrawalRequest> list(UUID userId, UserRole role, int page, int size) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can view withdrawals");
        return withdrawalRepo.findByCreatorIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }
}

