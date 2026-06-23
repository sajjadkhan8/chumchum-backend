package com.zingzing.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Safepay Express Checkout configuration.
 *
 * Required environment variables:
 *   SAFEPAY_API_KEY       - Merchant API key (public, sent in request body)
 *   SAFEPAY_SECRET_KEY    - Secret key (used for Bearer auth to Safepay API)
 *   SAFEPAY_HMAC_KEY      - HMAC key from Safepay dashboard → Developers → Endpoints (webhook verification)
 *
 * Optional:
 *   SAFEPAY_ENVIRONMENT   - "sandbox" (default) or "production"
 *   SAFEPAY_ENABLED       - true (default) or false (disables all Safepay endpoints)
 *   SAFEPAY_INTENT        - Payment intent: "CYBERSOURCE" (default) or "MPGS"
 *   SAFEPAY_ENTRY_MODE    - Payment entry mode for tracker creation: "flex" (default)
 *
 * Switch to production by setting SAFEPAY_ENVIRONMENT=production and using live dashboard keys.
 */
@Component
@ConfigurationProperties(prefix = "safepay")
public class SafepayProperties {

    private boolean enabled = true;

    /** "sandbox" or "production" — controls base URL and environment parameter in checkout URL. */
    private String environment = "sandbox";

    /** Merchant API key — identifies your merchant account (public key, sent in payment session body). */
    private String apiKey = "";

    /**
     * Secret key — used as Bearer token for Safepay REST API authentication.
     * Found in Safepay Dashboard → Developers → API Keys → Secret Key.
     */
    private String secretKey = "";

    /**
     * HMAC key — used to verify incoming webhook signatures (X-SFPY-SIGNATURE header).
     * Found in Safepay Dashboard → Developers → Endpoints → HMAC Key.
     * IMPORTANT: sandbox and production use different HMAC keys.
     */
    private String hmacKey = "";

    /** Payment intent type. CYBERSOURCE is the standard for card payments in Pakistan. */
    private String intent = "CYBERSOURCE";

    /** Safepay entry mode for payment tracker creation. */
    private String entryMode = "flex";

    private String sandboxBaseUrl = "https://sandbox.api.getsafepay.com";
    private String productionBaseUrl = "https://api.getsafepay.com";
    private String refundEndpoint = "";

    /** How many seconds of clock skew are tolerated when validating webhook timestamps. */
    private int webhookToleranceSeconds = 300;

    // ─── Derived helpers ──────────────────────────────────────────────────────

    public String getBaseUrl() {
        return isProduction() ? productionBaseUrl : sandboxBaseUrl;
    }

    public String getCheckoutBaseUrl() {
        return getBaseUrl() + "/checkout";
    }

    public boolean isProduction() {
        return "production".equalsIgnoreCase(environment);
    }

    public boolean isSandbox() {
        return !isProduction();
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getHmacKey() { return hmacKey; }
    public void setHmacKey(String hmacKey) { this.hmacKey = hmacKey; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getEntryMode() { return entryMode; }
    public void setEntryMode(String entryMode) { this.entryMode = entryMode; }

    public String getSandboxBaseUrl() { return sandboxBaseUrl; }
    public void setSandboxBaseUrl(String sandboxBaseUrl) { this.sandboxBaseUrl = sandboxBaseUrl; }

    public String getProductionBaseUrl() { return productionBaseUrl; }
    public void setProductionBaseUrl(String productionBaseUrl) { this.productionBaseUrl = productionBaseUrl; }

    public String getRefundEndpoint() { return refundEndpoint; }
    public void setRefundEndpoint(String refundEndpoint) { this.refundEndpoint = refundEndpoint; }

    public int getWebhookToleranceSeconds() { return webhookToleranceSeconds; }
    public void setWebhookToleranceSeconds(int webhookToleranceSeconds) { this.webhookToleranceSeconds = webhookToleranceSeconds; }
}
