-- Safepay Express Checkout payment session tracking
-- Each row represents one checkout session initiated by a brand.
-- Lifecycle: INITIATED → COMPLETED (webhook) | FAILED (webhook) | CANCELLED (redirect) | EXPIRED (no activity)

set search_path to core;

CREATE TABLE safepay_payment_sessions (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Safepay tracker token (e.g. "track_4f7d7e2d-ee05-44e3-81b7-6f6f2cca9727")
    tracker_token        VARCHAR(120) NOT NULL,
    brand_id             UUID         NOT NULL REFERENCES brands(id),
    -- NULL for wallet top-ups; set for direct order payments
    order_id             UUID         REFERENCES orders(id),
    -- Amount in whole PKR (our internal representation; converted to paisa when calling Safepay)
    amount_pkr           INTEGER      NOT NULL,
    -- WALLET_TOPUP or ORDER_PAYMENT
    payment_type         VARCHAR(30)  NOT NULL,
    -- INITIATED → COMPLETED | FAILED | CANCELLED | EXPIRED
    status               VARCHAR(30)  NOT NULL DEFAULT 'INITIATED',
    -- Safepay's internal payment/charge reference from webhook payload
    safepay_payment_ref  VARCHAR(200),
    failure_reason       VARCHAR(500),
    -- JSON blob for additional context (e.g. Safepay tracker state snapshot)
    metadata_json        TEXT,
    -- Sessions expire after 1 hour (matches Safepay auth token TTL)
    expires_at           TIMESTAMPTZ  NOT NULL,
    completed_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_safepay_tracker UNIQUE (tracker_token)
);

CREATE INDEX idx_safepay_sessions_brand  ON safepay_payment_sessions(brand_id);
CREATE INDEX idx_safepay_sessions_status ON safepay_payment_sessions(status);
CREATE INDEX idx_safepay_sessions_order  ON safepay_payment_sessions(order_id)
    WHERE order_id IS NOT NULL;
