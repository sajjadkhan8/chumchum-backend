package com.chamcham.backend.service;

import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.Transaction;
import com.chamcham.backend.entity.Wallet;
import com.chamcham.backend.entity.enums.TransactionType;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.TransactionRepository;
import com.chamcham.backend.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EarningsService {

    private final CreatorRepository creatorRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public EarningsService(CreatorRepository creatorRepository, WalletRepository walletRepository,
                           TransactionRepository transactionRepository) {
        this.creatorRepository = creatorRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    public record EarningsSummary(int totalEarned, int availableBalance, int pendingBalance,
                                  int totalWithdrawn, int platformFees) {}

    public EarningsSummary getSummary(UUID userId, UserRole role) {
        requireCreator(role);
        Creator creator = findCreator(userId);
        Wallet wallet = walletRepository.findByCreatorId(creator.getId())
                .orElse(Wallet.builder().creator(creator).build());

        long withdrawn = transactionRepository.sumCompletedByCreatorAndType(creator.getId(), TransactionType.WITHDRAWAL);
        long fees = transactionRepository.sumCompletedByCreatorAndType(creator.getId(), TransactionType.PLATFORM_FEE);

        return new EarningsSummary(
                wallet.getTotalEarned(),
                wallet.getAvailableBalance(),
                wallet.getPendingBalance(),
                (int) Math.abs(withdrawn),
                (int) Math.abs(fees)
        );
    }

    public Page<Transaction> getTransactions(UUID userId, UserRole role, int page, int size) {
        requireCreator(role);
        Creator creator = findCreator(userId);
        return transactionRepository.findByCreatorIdOrderByCreatedAtDesc(
                creator.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private void requireCreator(UserRole role) {
        if (!role.isCreator()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can access earnings");
        }
    }

    private Creator findCreator(UUID userId) {
        return creatorRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator profile not found"));
    }
}

