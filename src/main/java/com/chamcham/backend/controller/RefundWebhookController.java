package com.chamcham.backend.controller;

import com.chamcham.backend.entity.enums.TransactionStatus;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.payment.RefundProviderWebhookEvent;
import com.chamcham.backend.payment.RefundProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks/refunds")
public class RefundWebhookController {

    private final ApplicationEventPublisher eventPublisher;
    private final RefundProvider refundProvider;

    public RefundWebhookController(ApplicationEventPublisher eventPublisher, RefundProvider refundProvider) {
        this.eventPublisher = eventPublisher;
        this.refundProvider = refundProvider;
    }

    public record RefundWebhookRequest(String providerRefundId, String status, String failureReason) {}

    @PostMapping("/{provider}")
    public ResponseEntity<Map<String, Object>> receive(
            @PathVariable String provider,
            @RequestHeader(value = "X-Refund-Signature", required = false) String signature,
            @RequestBody RefundWebhookRequest request
    ) {
        if (!refundProvider.providerName().equals(provider) || !refundProvider.verifyWebhookSignature(signature)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refund webhook signature");
        }
        if (request.providerRefundId() == null || request.providerRefundId().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "providerRefundId is required");
        }
        TransactionStatus status;
        try {
            status = TransactionStatus.valueOf(request.status().trim().toUpperCase());
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid refund status");
        }
        eventPublisher.publishEvent(new RefundProviderWebhookEvent(provider, request.providerRefundId(), status, request.failureReason()));
        return ResponseEntity.ok(Map.of("success", true));
    }
}
