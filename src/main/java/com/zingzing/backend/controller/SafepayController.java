package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.service.SafepayService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints for Safepay Express Checkout.
 *
 * Flow:
 *  1. POST /initiate-topup → returns {checkoutUrl} → frontend redirects brand
 *  2. Brand pays on Safepay hosted page
 *  3. Safepay fires webhook → SafepayWebhookController credits wallet
 *  4. Safepay redirects brand to /brand/checkout/success?session={id}
 *  5. Frontend calls GET /session/{id} to confirm payment
 *  6. Brand clicks cancel → frontend calls POST /session/{id}/cancel
 */
@RestController
@RequestMapping("/api/v1/payments/safepay")
public class SafepayController {

    private static final Logger log = LoggerFactory.getLogger(SafepayController.class);

    private final SafepayService safepayService;

    public SafepayController(SafepayService safepayService) {
        this.safepayService = safepayService;
    }

    /** Request body for initiating a wallet top-up. */
    public record InitiateTopUpRequest(
            @NotNull Integer amount
    ) {}

    /**
     * Initiates a Safepay checkout session for a wallet top-up.
     *
     * Returns a checkout URL that the frontend must redirect the brand to.
     * Only brands may call this endpoint.
     */
    @PostMapping("/initiate-topup")
    public ResponseEntity<Map<String, Object>> initiateTopUp(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody InitiateTopUpRequest request
    ) {
        requireBrandRole(authUser);
        SafepayService.CheckoutSessionResponse session =
                safepayService.initiateWalletTopUp(authUser.userId(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "data", session));
    }

    /**
     * Polls the status of a Safepay payment session.
     *
     * Frontend calls this after redirect from Safepay to confirm wallet credit.
     * Returned status values: initiated | completed | failed | cancelled | expired
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionStatus(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String tracker
    ) {
        requireBrandRole(authUser);
        SafepayService.SessionStatusResponse status =
                safepayService.getSessionStatus(sessionId, authUser.userId(), tracker);
        return ResponseEntity.ok(Map.of("success", true, "data", status));
    }

    /**
     * Records a cancellation when the brand navigates back via the cancel URL.
     *
     * This is best-effort — the session may already be FAILED from a webhook.
     * Idempotent: calling multiple times has no additional effect.
     */
    @PostMapping("/session/{sessionId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelSession(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID sessionId
    ) {
        requireBrandRole(authUser);
        safepayService.handleCancelled(sessionId, authUser.userId());
        return ResponseEntity.ok(Map.of("success", true, "message", "Payment session cancelled"));
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private void requireBrandRole(AuthenticatedUser authUser) {
        if (!authUser.role().isBrand()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can initiate payments");
        }
    }
}
