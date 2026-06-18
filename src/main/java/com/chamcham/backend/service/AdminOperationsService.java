package com.chamcham.backend.service;

import com.chamcham.backend.entity.AdminAuditLog;
import com.chamcham.backend.entity.DisputeCase;
import com.chamcham.backend.entity.Order;
import com.chamcham.backend.entity.PaymentAuditLog;
import com.chamcham.backend.entity.Transaction;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.WithdrawalRequest;
import com.chamcham.backend.entity.enums.DisputeResolution;
import com.chamcham.backend.entity.enums.DisputeStatus;
import com.chamcham.backend.entity.enums.TransactionStatus;
import com.chamcham.backend.entity.enums.TransactionType;
import com.chamcham.backend.entity.enums.WithdrawalStatus;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.AdminAuditLogRepository;
import com.chamcham.backend.repository.DisputeCaseRepository;
import com.chamcham.backend.repository.OrderRepository;
import com.chamcham.backend.repository.PaymentAuditLogRepository;
import com.chamcham.backend.repository.TransactionRepository;
import com.chamcham.backend.repository.UserRepository;
import com.chamcham.backend.repository.WithdrawalRequestRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminOperationsService {

    private final DisputeCaseRepository disputeRepository;
    private final AdminAuditLogRepository auditRepository;
    private final PaymentAuditLogRepository paymentAuditLogRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final RefundService refundService;

    public AdminOperationsService(DisputeCaseRepository disputeRepository,
                                  AdminAuditLogRepository auditRepository,
                                  PaymentAuditLogRepository paymentAuditLogRepository,
                                  OrderRepository orderRepository,
                                  UserRepository userRepository,
                                  TransactionRepository transactionRepository,
                                  WithdrawalRequestRepository withdrawalRepository,
                                  RefundService refundService) {
        this.disputeRepository = disputeRepository;
        this.auditRepository = auditRepository;
        this.paymentAuditLogRepository = paymentAuditLogRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.refundService = refundService;
    }

    public Page<DisputeCase> listDisputes(String search, DisputeStatus status, int page, int limit) {
        return disputeRepository.searchForAdmin(blankToNull(search), status, PageRequest.of(safePage(page), safeLimit(limit)));
    }

    @Transactional
    public DisputeCase createDispute(UUID adminId, UUID orderId, String title, String description, String priority) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        User admin = requireAdmin(adminId);
        DisputeCase dispute = disputeRepository.save(DisputeCase.builder()
                .order(order)
                .title(title.trim())
                .description(description.trim())
                .priority(normalizePriority(priority))
                .assignedAdmin(admin)
                .build());
        log(adminId, "DISPUTE_CREATED", "dispute", dispute.getId().toString(), "Order " + orderId + ": " + title.trim());
        initializeDisputeRelationships(dispute);
        return dispute;
    }

    @Transactional
    public DisputeCase updateDispute(UUID adminId,
                                     UUID id,
                                     DisputeStatus status,
                                     String priority,
                                     DisputeResolution resolution,
                                     String resolutionNotes,
                                     boolean assignToMe) {
        DisputeCase dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dispute not found"));
        User admin = requireAdmin(adminId);
        if (status != null) dispute.setStatus(status);
        if (priority != null) dispute.setPriority(normalizePriority(priority));
        if (assignToMe) dispute.setAssignedAdmin(admin);
        if (resolution != null) dispute.setResolution(resolution);
        if (resolutionNotes != null) dispute.setResolutionNotes(resolutionNotes.trim());
        if (status == DisputeStatus.RESOLVED || status == DisputeStatus.CLOSED) {
            dispute.setResolvedAt(Instant.now());
        } else if (status != null) {
            dispute.setResolvedAt(null);
        }
        DisputeCase saved = disputeRepository.save(dispute);
        log(adminId, "DISPUTE_UPDATED", "dispute", id.toString(),
                "status=" + saved.getStatus().name().toLowerCase() + ", resolution=" + saved.getResolution().name().toLowerCase());
        initializeDisputeRelationships(saved);
        return saved;
    }

    public Page<AdminAuditLog> listAuditLogs(String search, String action, int page, int limit) {
        return auditRepository.searchForAdmin(blankToNull(search), blankToNull(action), PageRequest.of(safePage(page), safeLimit(limit)));
    }

    public Page<PaymentAuditLog> listPaymentAuditLogs(String search, String action, UUID brandId, int page, int limit) {
        return paymentAuditLogRepository.searchForAdmin(
                blankToNull(search),
                blankToNull(action),
                brandId,
                PageRequest.of(safePage(page), safeLimit(limit))
        );
    }

    @Transactional
    public DisputeCase executeRefund(UUID adminId, UUID disputeId, Integer requestedAmount, String reason) {
        DisputeCase dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dispute not found"));
        User admin = requireAdmin(adminId);
        refundService.requestRefund(dispute, admin, requestedAmount, reason);
        initializeDisputeRelationships(dispute);
        return dispute;
    }

    @Transactional
    public void log(UUID adminId, String action, String targetType, String targetId, String details) {
        User admin = requireAdmin(adminId);
        auditRepository.save(AdminAuditLog.builder()
                .admin(admin)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build());
    }

    public Map<String, Object> toDisputeMap(DisputeCase dispute) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", dispute.getId());
        row.put("orderId", dispute.getOrder().getId());
        row.put("orderNumber", dispute.getOrder().getOrderNumber());
        row.put("packageTitle", dispute.getOrder().getServicePackage().getTitle());
        row.put("creatorName", dispute.getOrder().getCreator().getName());
        row.put("brandName", dispute.getOrder().getBrand().getDisplayName());
        row.put("orderAmount", dispute.getOrder().getAmount());
        row.put("orderStatus", dispute.getOrder().getStatus().name().toLowerCase());
        row.put("dealType", dispute.getOrder().getDealType().name().toLowerCase());
        row.put("title", dispute.getTitle());
        row.put("description", dispute.getDescription());
        row.put("status", dispute.getStatus().name().toLowerCase());
        row.put("priority", dispute.getPriority());
        row.put("assignedAdminId", dispute.getAssignedAdmin() == null ? null : dispute.getAssignedAdmin().getId());
        row.put("assignedAdminName", dispute.getAssignedAdmin() == null ? null : dispute.getAssignedAdmin().getName());
        row.put("resolution", dispute.getResolution().name().toLowerCase());
        row.put("resolutionNotes", dispute.getResolutionNotes());
        row.put("resolvedAt", dispute.getResolvedAt());
        row.put("refundExecuted", dispute.getRefund() != null && dispute.getRefund().getStatus().name().equals("COMPLETED"));
        row.put("refundStatus", dispute.getRefund() == null ? null : dispute.getRefund().getStatus().name().toLowerCase());
        row.put("refundProvider", dispute.getRefund() == null ? null : dispute.getRefund().getProvider());
        row.put("providerRefundId", dispute.getRefund() == null ? null : dispute.getRefund().getProviderRefundId());
        row.put("refundFailureReason", dispute.getRefund() == null ? null : dispute.getRefund().getFailureReason());
        row.put("refundAmount", dispute.getRefund() == null ? null : dispute.getRefund().getAmount());
        row.put("creatorClawbackAmount", dispute.getRefund() == null ? null : dispute.getRefund().getCreatorClawbackAmount());
        row.put("refundReason", dispute.getRefund() == null ? null : dispute.getRefund().getReason());
        row.put("refundExecutedAt", dispute.getRefund() == null ? null : dispute.getRefund().getConfirmedAt());
        row.put("createdAt", dispute.getCreatedAt());
        row.put("updatedAt", dispute.getUpdatedAt());
        return row;
    }

    public Map<String, Object> toAuditMap(AdminAuditLog audit) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", audit.getId());
        row.put("adminId", audit.getAdmin().getId());
        row.put("adminName", audit.getAdmin().getName());
        row.put("action", audit.getAction());
        row.put("targetType", audit.getTargetType());
        row.put("targetId", audit.getTargetId());
        row.put("details", audit.getDetails());
        row.put("createdAt", audit.getCreatedAt());
        return row;
    }

    public Map<String, Object> toPaymentAuditMap(PaymentAuditLog audit) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", audit.getId());
        row.put("actorId", audit.getActor().getId());
        row.put("actorName", audit.getActor().getName());
        row.put("brandId", audit.getBrand() == null ? null : audit.getBrand().getId());
        row.put("brandName", audit.getBrand() == null ? null : audit.getBrand().getDisplayName());
        row.put("action", audit.getAction());
        row.put("targetType", audit.getTargetType());
        row.put("targetId", audit.getTargetId());
        row.put("details", audit.getDetails());
        row.put("createdAt", audit.getCreatedAt());
        return row;
    }

    public Page<Transaction> listTransactions(String search, String type, String status, int page, int limit) {
        TransactionType typeFilter = null;
        if (type != null && !type.isBlank() && !type.equalsIgnoreCase("all")) {
            try { typeFilter = TransactionType.valueOf(type.trim().toUpperCase()); }
            catch (IllegalArgumentException ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid transaction type: " + type); }
        }
        TransactionStatus statusFilter = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            try { statusFilter = TransactionStatus.valueOf(status.trim().toUpperCase()); }
            catch (IllegalArgumentException ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid transaction status: " + status); }
        }
        return transactionRepository.searchForAdmin(blankToNull(search), typeFilter, statusFilter, PageRequest.of(safePage(page), safeLimit(limit)));
    }

    public Page<WithdrawalRequest> listWithdrawals(String search, String status, int page, int limit) {
        WithdrawalStatus statusFilter = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            try { statusFilter = WithdrawalStatus.valueOf(status.trim().toUpperCase()); }
            catch (IllegalArgumentException ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid withdrawal status: " + status); }
        }
        return withdrawalRepository.searchForAdmin(blankToNull(search), statusFilter, PageRequest.of(safePage(page), safeLimit(limit)));
    }

    @Transactional
    public WithdrawalRequest processWithdrawal(UUID adminId, UUID id, String status) {
        requireAdmin(adminId);
        WithdrawalRequest withdrawal = withdrawalRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Withdrawal request not found"));
        WithdrawalStatus newStatus;
        try { newStatus = WithdrawalStatus.valueOf(status.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid withdrawal status: " + status); }
        if (withdrawal.getStatus() == WithdrawalStatus.COMPLETED || withdrawal.getStatus() == WithdrawalStatus.FAILED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot update a finalized withdrawal");
        }
        withdrawal.setStatus(newStatus);
        if (newStatus == WithdrawalStatus.COMPLETED || newStatus == WithdrawalStatus.FAILED) {
            withdrawal.setProcessedAt(Instant.now());
        }
        WithdrawalRequest saved = withdrawalRepository.save(withdrawal);
        log(adminId, "WITHDRAWAL_STATUS_CHANGED", "withdrawal_request", id.toString(),
                "status=" + newStatus.name().toLowerCase() + ", creator=" + withdrawal.getCreator().getName());
        return saved;
    }

    public Map<String, Object> getPaymentsStats() {
        long pendingWithdrawals = withdrawalRepository.countByStatus(WithdrawalStatus.PENDING);
        long pendingWithdrawalsAmount = withdrawalRepository.sumAmountByStatus(WithdrawalStatus.PENDING);
        long completedWithdrawals = withdrawalRepository.countByStatus(WithdrawalStatus.COMPLETED);
        long completedWithdrawalsAmount = withdrawalRepository.sumAmountByStatus(WithdrawalStatus.COMPLETED);
        long totalTransactions = transactionRepository.count();
        long pendingTransactions = transactionRepository.countByStatus(TransactionStatus.PENDING);
        long totalEarnings = transactionRepository.sumByTypeAndStatus(TransactionType.EARNING, TransactionStatus.COMPLETED);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalTransactions", totalTransactions);
        stats.put("pendingTransactions", pendingTransactions);
        stats.put("totalEarnings", totalEarnings);
        stats.put("pendingWithdrawals", pendingWithdrawals);
        stats.put("pendingWithdrawalsAmount", pendingWithdrawalsAmount);
        stats.put("completedWithdrawals", completedWithdrawals);
        stats.put("completedWithdrawalsAmount", completedWithdrawalsAmount);
        return stats;
    }

    public Map<String, Object> toTransactionMap(Transaction t) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", t.getId());
        row.put("creatorId", t.getCreator().getId());
        row.put("creatorName", t.getCreator().getName());
        row.put("orderId", t.getOrder() == null ? null : t.getOrder().getId());
        row.put("type", t.getType().name().toLowerCase());
        row.put("amount", t.getAmount());
        row.put("description", t.getDescription());
        row.put("status", t.getStatus().name().toLowerCase());
        row.put("createdAt", t.getCreatedAt());
        return row;
    }

    public Map<String, Object> toWithdrawalMap(WithdrawalRequest w) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", w.getId());
        row.put("creatorId", w.getCreator().getId());
        row.put("creatorName", w.getCreator().getName());
        row.put("payoutMethodId", w.getPayoutMethod().getId());
        row.put("payoutMethodName", w.getPayoutMethod().getName());
        row.put("payoutMethodType", w.getPayoutMethod().getType().name().toLowerCase());
        row.put("amount", w.getAmount());
        row.put("status", w.getStatus().name().toLowerCase());
        row.put("processedAt", w.getProcessedAt());
        row.put("createdAt", w.getCreatedAt());
        return row;
    }

    private User requireAdmin(UUID adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Admin not found"));
        if (!admin.getRole().isAdmin()) throw new ApiException(HttpStatus.FORBIDDEN, "Admin access required");
        return admin;
    }

    private String normalizePriority(String priority) {
        String normalized = priority == null ? "normal" : priority.trim().toLowerCase();
        if (!normalized.equals("low") && !normalized.equals("normal") && !normalized.equals("high") && !normalized.equals("urgent")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid priority: " + priority);
        }
        return normalized;
    }

    private void initializeDisputeRelationships(DisputeCase dispute) {
        dispute.getOrder().getServicePackage().getTitle();
        dispute.getOrder().getCreator().getName();
        dispute.getOrder().getBrand().getDisplayName();
        if (dispute.getAssignedAdmin() != null) dispute.getAssignedAdmin().getName();
        if (dispute.getRefund() != null) dispute.getRefund().getAmount();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int safePage(int page) {
        return Math.max(0, page);
    }

    private int safeLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }
}
