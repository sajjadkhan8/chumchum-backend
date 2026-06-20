package com.zingzing.backend.payment;

import com.zingzing.backend.config.SafepayProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Low-level HTTP client for the Safepay REST API.
 *
 * API reference: https://apidocs.getsafepay.com/
 *
 * Authentication: Bearer {secretKey} on every request.
 * The merchant_api_key (public) is sent in the request body where required.
 *
 * Amount convention: Safepay expects amounts in lowest denomination (paisa for PKR).
 * Conversion is handled by the caller: amountPkr * 100 = amountPaisa.
 */
@Component
public class SafepayClient {

    private static final Logger log = LoggerFactory.getLogger(SafepayClient.class);

    private final SafepayProperties props;
    private final RestClient restClient;

    public SafepayClient(SafepayProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ─── Request / Response records ───────────────────────────────────────────

    /** Request body for POST /order/payments/v3/ */
    public record CreateSessionRequest(
            @JsonProperty("merchant_api_key") String merchantApiKey,
            String intent,
            String mode,
            String currency,
            /** Amount in paisa (PKR * 100). */
            long amount,
            @JsonProperty("entry_mode") String entryMode,
            @JsonProperty("include_fees") boolean includeFees,
            Map<String, Object> metadata
    ) {}

    /** Minimal tracker info extracted from the Safepay session response. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TrackerInfo(
            @JsonProperty("token") String token,
            @JsonProperty("state") String state
    ) {}

    /** Minimal payment status used when verifying via /reporter. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentStatus(
            @JsonProperty("token") String token,
            @JsonProperty("state") String state,
            @JsonProperty("is_success") Boolean isSuccess
    ) {}

    public record RefundResult(String refundId, String providerPaymentId, String rawResponse) {}

    // ─── API operations ───────────────────────────────────────────────────────

    /**
     * Creates a Safepay payment session (tracker).
     *
     * POST /order/payments/v3/
     *
     * @param amountPaisa amount in paisa (PKR × 100)
     * @param metadata    arbitrary key-value pairs stored against the tracker
     * @return tracker token (e.g. "track_4f7d7e2d-...")
     * @throws SafepayApiException on any API or HTTP error
     */
    public String createPaymentSession(long amountPaisa, Map<String, Object> metadata) {
        log.debug("Safepay: creating payment session, amountPaisa={}", amountPaisa);

        CreateSessionRequest body = new CreateSessionRequest(
                props.getApiKey(),
                props.getIntent(),
                "payment",
                "PKR",
                amountPaisa,
                "hosted",
                false,
                metadata
        );

        try {
            JsonNode response = restClient.post()
                    .uri("/order/payments/v3/")
                    .header("Authorization", "Bearer " + props.getSecretKey())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String token = extractTrackerToken(response);
            log.info("Safepay: payment session created, tracker={}", token);
            return token;

        } catch (RestClientException ex) {
            log.error("Safepay: failed to create payment session", ex);
            throw new SafepayApiException("Could not initiate payment session with Safepay: " + ex.getMessage(), ex);
        }
    }

    /**
     * Obtains a short-lived (1-hour) authentication token for the hosted checkout URL.
     *
     * POST /client/passport/v1/token
     *
     * @return authentication token string (tbt parameter in checkout URL)
     * @throws SafepayApiException on any API or HTTP error
     */
    public String createAuthToken() {
        log.debug("Safepay: requesting auth token");

        try {
            JsonNode response = restClient.post()
                    .uri("/client/passport/v1/token")
                    .header("Authorization", "Bearer " + props.getSecretKey())
                    .retrieve()
                    .body(JsonNode.class);

            String token = extractStringData(response, "auth token");
            log.debug("Safepay: auth token acquired");
            return token;

        } catch (RestClientException ex) {
            log.error("Safepay: failed to create auth token", ex);
            throw new SafepayApiException("Could not obtain Safepay authentication token: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fetches the current state of a payment tracker.
     *
     * GET /reporter/api/v1/payments/{trackerToken}
     *
     * Use this to confirm payment completion when the webhook has not yet arrived.
     * For production flows, prefer webhook-based confirmation.
     *
     * @param trackerToken the tracker token returned by createPaymentSession
     * @return PaymentStatus with state field ("TRACKER_ENDED" = completed)
     */
    public PaymentStatus fetchPaymentStatus(String trackerToken) {
        log.debug("Safepay: fetching payment status for tracker={}", trackerToken);

        try {
            JsonNode response = restClient.get()
                    .uri("/reporter/api/v1/payments/{token}", trackerToken)
                    .header("Authorization", "Bearer " + props.getSecretKey())
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode data = unwrapData(response);
            JsonNode tracker = data.has("tracker") ? data.get("tracker") : data;

            String state = tracker.has("state") ? tracker.get("state").asText() : "UNKNOWN";
            boolean isSuccess = "TRACKER_ENDED".equals(state);

            log.debug("Safepay: tracker={} state={}", trackerToken, state);
            return new PaymentStatus(trackerToken, state, isSuccess);

        } catch (RestClientException ex) {
            log.warn("Safepay: could not fetch payment status for tracker={}", trackerToken, ex);
            throw new SafepayApiException("Could not fetch payment status from Safepay: " + ex.getMessage(), ex);
        }
    }

    public RefundResult createRefund(String trackerToken, String safepayPaymentRef, int amountPkr, String reason, Map<String, Object> metadata) {
        if (props.getRefundEndpoint() == null || props.getRefundEndpoint().isBlank()) {
            throw new SafepayApiException("Safepay refund endpoint is not configured. Set SAFEPAY_REFUND_ENDPOINT before enabling Safepay refunds.");
        }
        if (props.getSecretKey() == null || props.getSecretKey().isBlank()) {
            throw new SafepayApiException("Safepay secret key is not configured");
        }
        Map<String, Object> body = Map.of(
                "merchant_api_key", props.getApiKey(),
                "tracker", trackerToken == null ? "" : trackerToken,
                "payment_reference", safepayPaymentRef == null ? "" : safepayPaymentRef,
                "amount", (long) amountPkr * 100L,
                "currency", "PKR",
                "reason", reason,
                "metadata", metadata
        );
        try {
            JsonNode response = restClient.post()
                    .uri(props.getRefundEndpoint())
                    .header("Authorization", "Bearer " + props.getSecretKey())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String refundId = firstText(response, "id", "refund_id", "token", "data.id", "data.refund_id", "data.token");
            if (refundId == null || refundId.isBlank()) {
                throw new SafepayApiException("Safepay refund response missing refund id");
            }
            return new RefundResult(refundId, safepayPaymentRef == null || safepayPaymentRef.isBlank() ? trackerToken : safepayPaymentRef,
                    response == null ? "{}" : response.toString());
        } catch (RestClientException ex) {
            log.error("Safepay: failed to create refund tracker={} ref={}", trackerToken, safepayPaymentRef, ex);
            throw new SafepayApiException("Could not submit refund to Safepay: " + ex.getMessage(), ex);
        }
    }

    /**
     * Builds the hosted checkout URL to which the brand should be redirected.
     *
     * URL format: {checkoutBaseUrl}?env={env}&tracker={token}&tbt={authToken}&source=hosted
     *             &redirect_url={successUrl}&cancel_url={cancelUrl}
     *
     * Safepay appends ?tracker={trackerToken} to the redirect_url on success.
     */
    public String buildCheckoutUrl(String trackerToken, String authToken,
                                    String redirectUrl, String cancelUrl) {
        String env = props.isProduction() ? "production" : "sandbox";
        return props.getCheckoutBaseUrl()
                + "?env=" + encode(env)
                + "&tracker=" + encode(trackerToken)
                + "&tbt=" + encode(authToken)
                + "&source=hosted"
                + "&redirect_url=" + encode(redirectUrl)
                + "&cancel_url=" + encode(cancelUrl);
    }

    // ─── JSON helpers ─────────────────────────────────────────────────────────

    private String extractTrackerToken(JsonNode response) {
        JsonNode data = unwrapData(response);
        if (data == null) {
            throw new SafepayApiException("Safepay session response missing 'data' field");
        }
        JsonNode tracker = data.has("tracker") ? data.get("tracker") : data;
        if (tracker == null || !tracker.has("token")) {
            throw new SafepayApiException("Safepay session response missing tracker.token");
        }
        return tracker.get("token").asText();
    }

    private String extractStringData(JsonNode response, String fieldDescription) {
        JsonNode data = unwrapData(response);
        if (data == null) {
            throw new SafepayApiException("Safepay response missing 'data' field for " + fieldDescription);
        }
        // data may be a string directly or an object with a token field
        if (data.isTextual()) {
            return data.asText();
        }
        if (data.has("token")) {
            return data.get("token").asText();
        }
        throw new SafepayApiException("Unexpected Safepay response format for " + fieldDescription);
    }

    private JsonNode unwrapData(JsonNode response) {
        if (response == null) return null;
        return response.has("data") ? response.get("data") : response;
    }

    private String firstText(JsonNode response, String... paths) {
        for (String path : paths) {
            JsonNode cursor = response;
            for (String part : path.split("\\.")) {
                if (cursor == null || cursor.isNull()) break;
                cursor = cursor.get(part);
            }
            if (cursor != null && cursor.isValueNode() && !cursor.asText().isBlank()) return cursor.asText();
        }
        return null;
    }

    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
