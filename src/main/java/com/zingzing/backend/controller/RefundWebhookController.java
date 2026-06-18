package com.zingzing.backend.controller;

import com.zingzing.backend.entity.enums.TransactionStatus;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.payment.RefundProviderWebhookEvent;
import com.zingzing.backend.payment.RefundProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks/refunds")
public class RefundWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RefundWebhookController.class);

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
        log.info("Refund webhook received: provider={}, refundId={}, status={}",
                provider, request.providerRefundId(), request.status());

        if (!refundProvider.providerName().equals(provider) || !refundProvider.verifyWebhookSignature(signature)) {
            log.warn("Refund webhook rejected: provider={}, signaturePresent={}", provider, signature != null);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refund webhook signature");
        }
        if (request.providerRefundId() == null || request.providerRefundId().isBlank()) {
            log.warn("Refund webhook missing providerRefundId: provider={}", provider);
            throw new ApiException(HttpStatus.BAD_REQUEST, "providerRefundId is required");
        }
        TransactionStatus status;
        try {
            status = TransactionStatus.valueOf(request.status().trim().toUpperCase());
        } catch (Exception ex) {
            log.warn("Refund webhook invalid status value: provider={}, status={}", provider, request.status());
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid refund status");
        }

        log.debug("Refund webhook accepted, publishing event: provider={}, refundId={}", provider, request.providerRefundId());
        eventPublisher.publishEvent(new RefundProviderWebhookEvent(provider, request.providerRefundId(), status, request.failureReason()));
        return ResponseEntity.ok(Map.of("success", true));
    }
}
