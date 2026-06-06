package com.chamcham.backend.service;

import com.chamcham.backend.entity.*;
import com.chamcham.backend.entity.enums.*;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.payment.RefundProvider;
import com.chamcham.backend.payment.RefundProviderWebhookEvent;
import com.chamcham.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefundService {

    private final RefundProvider refundProvider;
    private final PaymentRefundRepository refundRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final OrderRepository orderRepository;
    private final AdminAuditLogRepository auditRepository;

    public RefundService(RefundProvider refundProvider,
                         PaymentRefundRepository refundRepository,
                         TransactionRepository transactionRepository,
                         WalletRepository walletRepository,
                         OrderRepository orderRepository,
                         AdminAuditLogRepository auditRepository) {
        this.refundProvider = refundProvider;
        this.refundRepository = refundRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.orderRepository = orderRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public PaymentRefund requestRefund(DisputeCase dispute, User admin, Integer requestedAmount, String reason) {
        validateRefundAllowed(dispute, requestedAmount, reason);
        Order order = dispute.getOrder();
        int refundAmount = requestedAmount == null ? order.getAmount() : requestedAmount;
        RefundProvider.RefundSubmission submission = refundProvider.submit(
                new RefundProvider.RefundRequest(null, order.getId(), refundAmount, reason.trim())
        );
        PaymentRefund refund = refundRepository.save(PaymentRefund.builder()
                .dispute(dispute)
                .order(order)
                .executedByAdmin(admin)
                .amount(refundAmount)
                .reason(reason.trim())
                .status(TransactionStatus.PENDING)
                .provider(refundProvider.providerName())
                .providerRefundId(submission.providerRefundId())
                .build());
        dispute.setRefund(refund);
        audit(admin, "DISPUTE_REFUND_REQUESTED", dispute.getId().toString(),
                "provider=" + refund.getProvider() + ", providerRefundId=" + refund.getProviderRefundId() + ", amount=" + refundAmount);
        return refund;
    }

    @EventListener
    @Transactional
    public void handleProviderWebhook(RefundProviderWebhookEvent event) {
        PaymentRefund refund = refundRepository.findByProviderRefundId(event.providerRefundId()).orElse(null);
        if (refund == null || !refund.getProvider().equals(event.provider()) || refund.getStatus() != TransactionStatus.PENDING) return;
        if (event.status() == TransactionStatus.FAILED) {
            markFailed(refund, event.failureReason() == null ? "Provider rejected refund" : event.failureReason());
            return;
        }
        if (event.status() != TransactionStatus.COMPLETED) return;
        try {
            confirmRefund(refund);
        } catch (ApiException ex) {
            markFailed(refund, ex.getMessage());
        }
    }

    private void confirmRefund(PaymentRefund refund) {
        Order order = refund.getOrder();
        int clawbackAmount = 0;
        Transaction earning = transactionRepository.findByOrderIdAndType(order.getId(), TransactionType.EARNING).orElse(null);
        if (earning != null && earning.getStatus() == TransactionStatus.COMPLETED) {
            clawbackAmount = Math.min(refund.getAmount(), earning.getAmount());
            Wallet wallet = walletRepository.findByCreatorIdForUpdate(order.getCreator().getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Creator wallet is unavailable for earnings clawback"));
            if (wallet.getAvailableBalance() < clawbackAmount) {
                throw new ApiException(HttpStatus.CONFLICT, "Creator available balance is lower than the required clawback");
            }
            wallet.setAvailableBalance(wallet.getAvailableBalance() - clawbackAmount);
            wallet.setTotalEarned(Math.max(0, wallet.getTotalEarned() - clawbackAmount));
            walletRepository.save(wallet);
            transactionRepository.save(Transaction.builder()
                    .creator(order.getCreator())
                    .order(order)
                    .type(TransactionType.REFUND)
                    .amount(-clawbackAmount)
                    .description("Provider-confirmed refund clawback for order " + orderLabel(order))
                    .status(TransactionStatus.COMPLETED)
                    .build());
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        refund.setCreatorClawbackAmount(clawbackAmount);
        refund.setStatus(TransactionStatus.COMPLETED);
        refund.setConfirmedAt(Instant.now());
        refundRepository.save(refund);
        audit(refund.getExecutedByAdmin(), "DISPUTE_REFUND_CONFIRMED", refund.getDispute().getId().toString(),
                "provider=" + refund.getProvider() + ", providerRefundId=" + refund.getProviderRefundId()
                        + ", amount=" + refund.getAmount() + ", creatorClawback=" + clawbackAmount);
    }

    private void markFailed(PaymentRefund refund, String reason) {
        refund.setStatus(TransactionStatus.FAILED);
        refund.setFailureReason(reason);
        refund.setConfirmedAt(Instant.now());
        refundRepository.save(refund);
        audit(refund.getExecutedByAdmin(), "DISPUTE_REFUND_FAILED", refund.getDispute().getId().toString(),
                "provider=" + refund.getProvider() + ", providerRefundId=" + refund.getProviderRefundId() + ", reason=" + reason);
    }

    private void validateRefundAllowed(DisputeCase dispute, Integer requestedAmount, String reason) {
        if (dispute.getStatus() != DisputeStatus.RESOLVED) throw new ApiException(HttpStatus.BAD_REQUEST, "Resolve the dispute before requesting a refund");
        if (dispute.getResolution() != DisputeResolution.CANCEL_ORDER
                && dispute.getResolution() != DisputeResolution.BRAND_FAVORED
                && dispute.getResolution() != DisputeResolution.MUTUAL_AGREEMENT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This dispute resolution does not permit a refund");
        }
        if (refundRepository.existsByDisputeId(dispute.getId())) throw new ApiException(HttpStatus.CONFLICT, "A refund has already been requested for this dispute");
        int orderAmount = dispute.getOrder().getAmount() == null ? 0 : dispute.getOrder().getAmount();
        int amount = requestedAmount == null ? orderAmount : requestedAmount;
        if (orderAmount <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "This order has no refundable paid amount");
        if (amount <= 0 || amount > orderAmount) throw new ApiException(HttpStatus.BAD_REQUEST, "Refund amount must be between 1 and " + orderAmount);
        if (reason == null || reason.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "Refund reason is required");
    }

    private void audit(User admin, String action, String disputeId, String details) {
        auditRepository.save(AdminAuditLog.builder().admin(admin).action(action).targetType("dispute").targetId(disputeId).details(details).build());
    }

    private String orderLabel(Order order) {
        return order.getOrderNumber() == null ? order.getId().toString() : order.getOrderNumber();
    }
}
