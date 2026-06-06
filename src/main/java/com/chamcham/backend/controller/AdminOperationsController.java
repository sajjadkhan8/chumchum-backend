package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.entity.enums.DisputeResolution;
import com.chamcham.backend.entity.enums.DisputeStatus;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.service.AdminOperationsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminOperationsController {

    private final AdminOperationsService operationsService;

    public AdminOperationsController(AdminOperationsService operationsService) {
        this.operationsService = operationsService;
    }

    public record CreateDisputeRequest(
            UUID orderId,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 5000) String description,
            @Size(max = 20) String priority
    ) {}

    public record UpdateDisputeRequest(
            String status,
            String priority,
            String resolution,
            @Size(max = 5000) String resolutionNotes,
            Boolean assignToMe
    ) {}

    public record ExecuteRefundRequest(
            Integer amount,
            @NotBlank @Size(max = 500) String reason
    ) {}

    @GetMapping("/disputes")
    public ResponseEntity<Map<String, Object>> disputes(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        requireAdmin(authUser);
        var result = operationsService.listDisputes(search, parseStatus(status), page, limit);
        return ok(Map.of(
                "disputes", result.getContent().stream().map(operationsService::toDisputeMap).toList(),
                "total", result.getTotalElements(),
                "page", result.getNumber(),
                "limit", result.getSize()
        ));
    }

    @PostMapping("/disputes")
    public ResponseEntity<Map<String, Object>> createDispute(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody CreateDisputeRequest request
    ) {
        requireAdmin(authUser);
        if (request.orderId() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "orderId is required");
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "data", operationsService.toDisputeMap(operationsService.createDispute(
                        authUser.userId(), request.orderId(), request.title(), request.description(), request.priority()
                ))
        ));
    }

    @PatchMapping("/disputes/{id}")
    public ResponseEntity<Map<String, Object>> updateDispute(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDisputeRequest request
    ) {
        requireAdmin(authUser);
        return ok(operationsService.toDisputeMap(operationsService.updateDispute(
                authUser.userId(),
                id,
                parseStatus(request.status()),
                request.priority(),
                parseResolution(request.resolution()),
                request.resolutionNotes(),
                Boolean.TRUE.equals(request.assignToMe())
        )));
    }

    @PostMapping("/disputes/{id}/refund")
    public ResponseEntity<Map<String, Object>> executeRefund(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID id,
            @Valid @RequestBody ExecuteRefundRequest request
    ) {
        requireAdmin(authUser);
        return ok(operationsService.toDisputeMap(
                operationsService.executeRefund(authUser.userId(), id, request.amount(), request.reason())
        ));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Map<String, Object>> auditLogs(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        requireAdmin(authUser);
        var result = operationsService.listAuditLogs(search, action, page, limit);
        return ok(Map.of(
                "logs", result.getContent().stream().map(operationsService::toAuditMap).toList(),
                "total", result.getTotalElements(),
                "page", result.getNumber(),
                "limit", result.getSize()
        ));
    }

    @GetMapping("/payment-audit-logs")
    public ResponseEntity<Map<String, Object>> paymentAuditLogs(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        requireAdmin(authUser);
        var result = operationsService.listPaymentAuditLogs(search, action, brandId, page, limit);
        return ok(Map.of(
                "logs", result.getContent().stream().map(operationsService::toPaymentAuditMap).toList(),
                "total", result.getTotalElements(),
                "page", result.getNumber(),
                "limit", result.getSize()
        ));
    }

    private DisputeStatus parseStatus(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("all")) return null;
        try {
            return DisputeStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid dispute status: " + value);
        }
    }

    private DisputeResolution parseResolution(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return DisputeResolution.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid dispute resolution: " + value);
        }
    }

    private void requireAdmin(AuthenticatedUser authUser) {
        if (authUser == null || !authUser.role().isAdmin()) throw new ApiException(HttpStatus.FORBIDDEN, "Admin access required");
    }

    private ResponseEntity<Map<String, Object>> ok(Object data) {
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }
}
