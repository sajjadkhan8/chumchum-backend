package com.zingzing.backend.service;

import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.Transaction;
import com.zingzing.backend.entity.Wallet;
import com.zingzing.backend.entity.enums.TransactionStatus;
import com.zingzing.backend.entity.enums.TransactionType;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.CreatorRepository;
import com.zingzing.backend.repository.OrderRepository;
import com.zingzing.backend.repository.TransactionRepository;
import com.zingzing.backend.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EarningsService {

    private final CreatorRepository creatorRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final double feeRate;

    public EarningsService(CreatorRepository creatorRepository, WalletRepository walletRepository,
                           TransactionRepository transactionRepository,
                           OrderRepository orderRepository,
                           @Value("${platform.fee-rate:0.10}") double feeRate) {
        this.creatorRepository = creatorRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.orderRepository = orderRepository;
        this.feeRate = feeRate;
    }

    public record EarningsSummary(int totalEarned, int availableBalance, int pendingBalance,
                                  int totalWithdrawn, int platformFees,
                                  int awaitingApprovalGross, int awaitingApprovalNet,
                                  int awaitingApprovalFees, int awaitingApprovalCount) {}

    public EarningsSummary getSummary(UUID userId, UserRole role) {
        requireCreator(role);
        Creator creator = findCreator(userId);
        Wallet wallet = walletRepository.findByCreatorId(creator.getId())
                .orElse(Wallet.builder().creator(creator).build());

        long withdrawn = transactionRepository.sumByCreatorAndTypeAndStatus(creator.getId(), TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED);
        long fees = transactionRepository.sumByCreatorAndTypeAndStatus(creator.getId(), TransactionType.PLATFORM_FEE, TransactionStatus.COMPLETED);
        List<Integer> awaitingApprovalAmounts = orderRepository.findAwaitingApprovalPaidAmountsByCreatorId(creator.getId());
        int awaitingApprovalGross = awaitingApprovalAmounts.stream().mapToInt(Integer::intValue).sum();
        int awaitingApprovalFees = awaitingApprovalAmounts.stream()
                .mapToInt(amount -> (int) Math.round(amount * feeRate))
                .sum();
        int awaitingApprovalNet = Math.max(0, awaitingApprovalGross - awaitingApprovalFees);

        return new EarningsSummary(
                wallet.getTotalEarned(),
                wallet.getAvailableBalance(),
                wallet.getPendingBalance(),
                (int) Math.abs(withdrawn),
                (int) Math.abs(fees),
                awaitingApprovalGross,
                awaitingApprovalNet,
                awaitingApprovalFees,
                awaitingApprovalAmounts.size()
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
