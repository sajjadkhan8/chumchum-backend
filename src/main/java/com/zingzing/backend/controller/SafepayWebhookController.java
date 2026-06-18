package com.zingzing.backend.controller;

import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.service.SafepayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * Receives and processes Safepay payment lifecycle webhooks.
 *
 * Security model:
 *  - Endpoint is public (no JWT required) — Safepay fires it server-to-server.
 *  - Every request must pass HMAC-SHA512 signature verification using
 *    the X-SFPY-SIGNATURE header before any business logic runs.
 *  - Return HTTP 200 on success (even for failed payments) so Safepay
 *    does not retry unnecessarily.
 *  - Return HTTP 401 on bad signature so Safepay retries on genuine delivery
 *    failures but not on fraudulent requests.
 *
 * Handled events:
 *  - payment.succeeded  → credit brand wallet (wallet top-up) or confirm order
 *  - payment.failed     → mark session failed
 *
 * Safepay webhook payload structure:
 * {
 *   "type": "payment.succeeded",
 *   "version": 1,
 *   "data": {
 *     "tracker": "track_xxx",
 *     "intent": "CYBERSOURCE",
 *     "state": "TRACKER_ENDED",
 *     "amount": 1000000,
 *     "net": 980000,
 *     "fee": 20000,
 *     "currency": "PKR",
 *     "customer_email": "user@example.com",
 *     "metadata": { "session_id": "...", "brand_id": "..." },
 *     "charged_at": "2024-01-15T10:30:00Z"
 *   }
 * }
 */
@RestController
@RequestMapping("/api/v1/webhooks/safepay")
public class SafepayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SafepayWebhookController.class);

    private static final String EVENT_PAYMENT_SUCCEEDED = "payment.succeeded";
    private static final String EVENT_PAYMENT_FAILED    = "payment.failed";

    private final SafepayService safepayService;
    private final ObjectMapper objectMapper;

    public SafepayWebhookController(SafepayService safepayService, ObjectMapper objectMapper) {
        this.safepayService = safepayService;
        this.objectMapper = objectMapper;
    }

    /**
     * Receives all Safepay webhook events.
     *
     * The raw body byte array is required for HMAC verification —
     * Spring reads the body into bytes before parsing JSON.
     *
     * @param signature value of the X-SFPY-SIGNATURE header
     * @param rawBody   raw request body (used for HMAC verification)
     */
    @PostMapping(consumes = "application/json")
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(value = "X-SFPY-SIGNATURE", required = false) String signature,
            @RequestBody byte[] rawBody
    ) {
        // ── 1. Verify HMAC signature ─────────────────────────────────────────
        if (!safepayService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Safepay webhook: HMAC verification failed — rejecting request");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }

        // ── 2. Parse event ───────────────────────────────────────────────────
        JsonNode payload;
        try {
            payload = objectMapper.readTree(rawBody);
        } catch (IOException ex) {
            log.error("Safepay webhook: could not parse JSON body", ex);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Malformed webhook payload");
        }

        String eventType = payload.has("type") ? payload.get("type").asText() : "";
        JsonNode data = payload.has("data") ? payload.get("data") : payload;

        log.info("Safepay webhook: received event type={}", eventType);

        // ── 3. Route to handler ──────────────────────────────────────────────
        return switch (eventType) {
            case EVENT_PAYMENT_SUCCEEDED -> handlePaymentSucceeded(data);
            case EVENT_PAYMENT_FAILED    -> handlePaymentFailed(data);
            default -> {
                // Unknown event type — acknowledge without error so Safepay doesn't retry
                log.debug("Safepay webhook: unhandled event type={}, acknowledging", eventType);
                yield ResponseEntity.ok(Map.of("success", true, "message", "Event acknowledged"));
            }
        };
    }

    // ─── Event handlers ───────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> handlePaymentSucceeded(JsonNode data) {
        String trackerToken = extractTracker(data);
        String paymentRef = data.has("charged_at")
                ? trackerToken + "_" + data.get("charged_at").asText()
                : trackerToken;

        log.info("Safepay webhook: payment.succeeded tracker={}", trackerToken);
        safepayService.handlePaymentSucceeded(trackerToken, paymentRef);

        return ResponseEntity.ok(Map.of("success", true));
    }

    private ResponseEntity<Map<String, Object>> handlePaymentFailed(JsonNode data) {
        String trackerToken = extractTracker(data);
        String failureReason = data.has("error")
                ? data.get("error").asText("payment declined")
                : (data.has("category") ? data.get("category").asText("unknown") : "payment declined");

        log.info("Safepay webhook: payment.failed tracker={} reason={}", trackerToken, failureReason);
        safepayService.handlePaymentFailed(trackerToken, failureReason);

        return ResponseEntity.ok(Map.of("success", true));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String extractTracker(JsonNode data) {
        if (data == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Webhook payload missing data field");
        }
        JsonNode tracker = data.has("tracker") ? data.get("tracker") : null;
        String token = null;

        if (tracker != null) {
            // data.tracker may be a string or an object with a token field
            token = tracker.isTextual() ? tracker.asText()
                    : (tracker.has("token") ? tracker.get("token").asText() : null);
        }

        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Webhook payload missing tracker token");
        }
        return token;
    }
}
