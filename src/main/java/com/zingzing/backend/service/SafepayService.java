package com.zingzing.backend.service;

import com.zingzing.backend.config.SafepayProperties;
import com.zingzing.backend.entity.Brand;
import com.zingzing.backend.entity.BrandWallet;
import com.zingzing.backend.entity.SafepayPaymentSession;
import com.zingzing.backend.entity.enums.SafepayPaymentStatus;
import com.zingzing.backend.entity.enums.SafepayPaymentType;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.payment.SafepayApiException;
import com.zingzing.backend.payment.SafepayClient;
import com.zingzing.backend.repository.BrandRepository;
import com.zingzing.backend.repository.BrandWalletRepository;
import com.zingzing.backend.repository.SafepayPaymentSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates Safepay Express Checkout sessions.
 *
 * Responsibilities:
 *  - Initiate a checkout session (create Safepay tracker, build checkout URL)
 *  - Process webhook events (payment.succeeded, payment.failed)
 *  - Apply business effects (credit brand wallet, mark order paid)
 *  - Verify HMAC-SHA512 webhook signatures
 *  - Expire stale sessions on a schedule
 */
@Service
public class SafepayService {

    private static final Logger log = LoggerFactory.getLogger(SafepayService.class);

    /** PKR → paisa multiplier (1 PKR = 100 paisa). */
    private static final int PAISA_MULTIPLIER = 100;

    /** Safepay tracker state indicating a successfully captured payment. */
    private static final String STATE_COMPLETED = "TRACKER_ENDED";

    private final SafepayProperties props;
    private final SafepayClient safepayClient;
    private final SafepayPaymentSessionRepository sessionRepository;
    private final BrandRepository brandRepository;
    private final BrandWalletRepository brandWalletRepository;
    private final PaymentAuditService paymentAuditService;

    @Value("${app.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    public SafepayService(
            SafepayProperties props,
            SafepayClient safepayClient,
            SafepayPaymentSessionRepository sessionRepository,
            BrandRepository brandRepository,
            BrandWalletRepository brandWalletRepository,
            PaymentAuditService paymentAuditService
    ) {
        this.props = props;
        this.safepayClient = safepayClient;
        this.sessionRepository = sessionRepository;
        this.brandRepository = brandRepository;
        this.brandWalletRepository = brandWalletRepository;
        this.paymentAuditService = paymentAuditService;
    }

    // ─── Response records ─────────────────────────────────────────────────────

    public record CheckoutSessionResponse(
            String sessionId,
            String checkoutUrl,
            String trackerToken,
            Instant expiresAt
    ) {}

    public record SessionStatusResponse(
            String sessionId,
            String status,
            int amountPkr,
            String paymentType,
            Instant completedAt,
            String failureReason
    ) {}

    // ─── Initiation ───────────────────────────────────────────────────────────

    /**
     * Creates a Safepay checkout session for a brand wallet top-up.
     *
     * Returns a checkout URL that the frontend should redirect the brand to.
     * The brand completes payment on Safepay's hosted page, then is redirected
     * back to the frontend success/cancel pages. The wallet is credited only
     * after the payment.succeeded webhook is received and HMAC-verified.
     *
     * @param brandId    authenticated brand's UUID
     * @param amountPkr  top-up amount in whole PKR (minimum 1000)
     * @return CheckoutSessionResponse with the Safepay checkout URL
     */
    @Transactional
    public CheckoutSessionResponse initiateWalletTopUp(UUID brandId, int amountPkr) {
        if (!props.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Online payments are currently unavailable");
        }
        if (amountPkr < 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Minimum top-up amount is PKR 1,000");
        }
        if (amountPkr > 10_000_000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Maximum single top-up amount is PKR 10,000,000");
        }

        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand profile not found"));

        // Reserve a session ID upfront so we can embed it in the redirect URL
        UUID sessionId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

        long amountPaisa = (long) amountPkr * PAISA_MULTIPLIER;

        Map<String, Object> metadata = Map.of(
                "session_id", sessionId.toString(),
                "brand_id", brandId.toString(),
                "payment_type", SafepayPaymentType.WALLET_TOPUP.name(),
                "platform", "chamcham"
        );

        String trackerToken;
        String authToken;
        try {
            trackerToken = safepayClient.createPaymentSession(amountPaisa, metadata);
            authToken = safepayClient.createAuthToken();
        } catch (SafepayApiException ex) {
            log.error("Safepay: session initiation failed for brand={}, amount={}", brandId, amountPkr, ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Payment gateway unavailable. Please try again or contact support.");
        }

        // Persist session before redirecting — idempotency and audit safety
        SafepayPaymentSession session = SafepayPaymentSession.builder()
                .id(sessionId)
                .trackerToken(trackerToken)
                .brand(brand)
                .amountPkr(amountPkr)
                .paymentType(SafepayPaymentType.WALLET_TOPUP)
                .status(SafepayPaymentStatus.INITIATED)
                .expiresAt(expiresAt)
                .build();
        sessionRepository.save(session);

        // Build redirect URLs — sessionId is embedded so the result page can poll status
        String successUrl = frontendBaseUrl + "/brand/checkout/success?session=" + sessionId;
        String cancelUrl  = frontendBaseUrl + "/brand/checkout/cancel?session=" + sessionId;

        String checkoutUrl = safepayClient.buildCheckoutUrl(trackerToken, authToken, successUrl, cancelUrl);

        paymentAuditService.log(brandId, brand, "SAFEPAY_TOPUP_INITIATED", "safepay_payment_session",
                sessionId.toString(), "amount=" + amountPkr + " tracker=" + trackerToken);

        log.info("Safepay: checkout session created sessionId={} tracker={} brand={} amount={}",
                sessionId, trackerToken, brandId, amountPkr);

        return new CheckoutSessionResponse(sessionId.toString(), checkoutUrl, trackerToken, expiresAt);
    }

    // ─── Webhook processing ───────────────────────────────────────────────────

    /**
     * Processes a payment.succeeded webhook from Safepay.
     *
     * This is the authoritative confirmation of payment. The wallet is credited
     * here, NOT on the redirect callback, to prevent fraud via URL manipulation.
     *
     * Idempotent: if the session is already COMPLETED, this is a no-op.
     *
     * @param trackerToken     the tracker token from the webhook payload
     * @param safepayPaymentRef Safepay's internal charge/payment reference
     */
    @Transactional
    public void handlePaymentSucceeded(String trackerToken, String safepayPaymentRef) {
        SafepayPaymentSession session = findSessionByTracker(trackerToken);

        if (session.getStatus() == SafepayPaymentStatus.COMPLETED) {
            log.info("Safepay: duplicate payment.succeeded for tracker={}, skipping", trackerToken);
            return;
        }
        if (session.getStatus() == SafepayPaymentStatus.EXPIRED) {
            log.warn("Safepay: payment.succeeded for EXPIRED session tracker={}", trackerToken);
            // Still honour the payment — Safepay confirmed it
        }

        session.setStatus(SafepayPaymentStatus.COMPLETED);
        session.setSafepayPaymentRef(safepayPaymentRef);
        session.setCompletedAt(Instant.now());
        sessionRepository.save(session);

        applyBusinessEffect(session);

        paymentAuditService.log(session.getBrand().getId(), session.getBrand(),
                "SAFEPAY_PAYMENT_SUCCEEDED", "safepay_payment_session",
                session.getId().toString(),
                "tracker=" + trackerToken + " ref=" + safepayPaymentRef + " amount=" + session.getAmountPkr());

        log.info("Safepay: payment succeeded sessionId={} tracker={} amount={}",
                session.getId(), trackerToken, session.getAmountPkr());
    }

    /**
     * Processes a payment.failed webhook from Safepay.
     *
     * Idempotent: if the session is already FAILED, this is a no-op.
     */
    @Transactional
    public void handlePaymentFailed(String trackerToken, String failureReason) {
        SafepayPaymentSession session = findSessionByTracker(trackerToken);

        if (session.getStatus() == SafepayPaymentStatus.FAILED) {
            log.info("Safepay: duplicate payment.failed for tracker={}, skipping", trackerToken);
            return;
        }

        session.setStatus(SafepayPaymentStatus.FAILED);
        session.setFailureReason(failureReason);
        sessionRepository.save(session);

        paymentAuditService.log(session.getBrand().getId(), session.getBrand(),
                "SAFEPAY_PAYMENT_FAILED", "safepay_payment_session",
                session.getId().toString(),
                "tracker=" + trackerToken + " reason=" + failureReason);

        log.warn("Safepay: payment failed sessionId={} tracker={} reason={}",
                session.getId(), trackerToken, failureReason);
    }

    /**
     * Marks a session as CANCELLED when the brand returns via the cancel URL.
     * Only transitions from INITIATED — if the session is already terminal, this is a no-op.
     */
    @Transactional
    public void handleCancelled(UUID sessionId, UUID brandId) {
        sessionRepository.findByIdAndBrandId(sessionId, brandId).ifPresent(session -> {
            if (session.getStatus() == SafepayPaymentStatus.INITIATED) {
                session.setStatus(SafepayPaymentStatus.CANCELLED);
                sessionRepository.save(session);
                log.info("Safepay: session cancelled by user sessionId={}", sessionId);
            }
        });
    }

    // ─── Status polling ───────────────────────────────────────────────────────

    /**
     * Returns the current status of a session owned by the given brand.
     * Used by the frontend to poll for payment confirmation after redirect.
     */
    @Transactional(readOnly = true)
    public SessionStatusResponse getSessionStatus(UUID sessionId, UUID brandId) {
        SafepayPaymentSession session = sessionRepository.findByIdAndBrandId(sessionId, brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment session not found"));

        return toStatusResponse(session);
    }

    // ─── HMAC verification ────────────────────────────────────────────────────

    /**
     * Verifies the X-SFPY-SIGNATURE header on a webhook request.
     *
     * Algorithm: HMAC-SHA512 of the raw request body using the HMAC key
     * from Safepay Dashboard → Developers → Endpoints, hex-encoded.
     *
     * @param rawBody   raw request body bytes (must be the unmodified body)
     * @param signature value of the X-SFPY-SIGNATURE header
     * @return true if the signature matches
     */
    public boolean verifyWebhookSignature(byte[] rawBody, String signature) {
        if (props.getHmacKey() == null || props.getHmacKey().isBlank()) {
            log.error("Safepay: HMAC key is not configured — refusing all webhooks");
            return false;
        }
        if (signature == null || signature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(
                    props.getHmacKey().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            String computed = HexFormat.of().formatHex(mac.doFinal(rawBody));
            // Constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            log.error("Safepay: HMAC verification error", ex);
            return false;
        }
    }

    // ─── Scheduled maintenance ────────────────────────────────────────────────

    /** Expires stale INITIATED sessions every 15 minutes. */
    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void expireOldSessions() {
        int count = sessionRepository.expireOldSessions(Instant.now());
        if (count > 0) {
            log.info("Safepay: expired {} stale payment sessions", count);
        }
    }

    // ─── Business effects ─────────────────────────────────────────────────────

    /**
     * Applies the downstream business effect of a confirmed payment.
     * Currently handles WALLET_TOPUP. ORDER_PAYMENT support is wired but
     * deferred to the order creation integration.
     */
    private void applyBusinessEffect(SafepayPaymentSession session) {
        switch (session.getPaymentType()) {
            case WALLET_TOPUP -> creditBrandWallet(session);
            case ORDER_PAYMENT -> log.info(
                    "Safepay: ORDER_PAYMENT confirmed for session={} — order fulfilment handled by OrderService",
                    session.getId());
        }
    }

    private void creditBrandWallet(SafepayPaymentSession session) {
        Brand brand = session.getBrand();
        BrandWallet wallet = brandWalletRepository.findById(brand.getId())
                .orElseGet(() -> brandWalletRepository.save(
                        BrandWallet.builder().brand(brand).build()));

        wallet.setWalletBalance(wallet.getWalletBalance() + session.getAmountPkr());
        wallet.setNextInvoiceDate(Instant.now().plus(10, ChronoUnit.DAYS));
        brandWalletRepository.save(wallet);

        log.info("Safepay: brand wallet credited brandId={} amount={} PKR newBalance={}",
                brand.getId(), session.getAmountPkr(), wallet.getWalletBalance());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private SafepayPaymentSession findSessionByTracker(String trackerToken) {
        return sessionRepository.findByTrackerToken(trackerToken)
                .orElseThrow(() -> {
                    log.warn("Safepay: received webhook for unknown tracker={}", trackerToken);
                    return new ApiException(HttpStatus.NOT_FOUND,
                            "Payment session not found for tracker: " + trackerToken);
                });
    }

    private SessionStatusResponse toStatusResponse(SafepayPaymentSession session) {
        return new SessionStatusResponse(
                session.getId().toString(),
                session.getStatus().name().toLowerCase(),
                session.getAmountPkr(),
                session.getPaymentType().name().toLowerCase(),
                session.getCompletedAt(),
                session.getFailureReason()
        );
    }
}
