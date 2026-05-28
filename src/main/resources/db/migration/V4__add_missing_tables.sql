-- V4__add_missing_tables.sql
-- Adds all spec-required tables that were missing from V1 schema.

SET search_path TO core;

-- ============================================================
-- deliverables (order work items)
-- ============================================================
CREATE TABLE IF NOT EXISTS deliverables (
    id           uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     uuid        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    name         varchar(200),
    status       varchar(30) NOT NULL DEFAULT 'PENDING',
    file_url     varchar(500),
    submitted_at timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_deliverables_order_id ON deliverables(order_id);

-- ============================================================
-- social_accounts (creator social media links)
-- ============================================================
CREATE TABLE IF NOT EXISTS social_accounts (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id      uuid        NOT NULL REFERENCES creators(id) ON DELETE CASCADE,
    platform        varchar(30) NOT NULL,
    username        varchar(100) NOT NULL,
    profile_url     varchar(500),
    followers       integer     NOT NULL DEFAULT 0,
    avg_views       integer,
    engagement_rate numeric(5,2) NOT NULL DEFAULT 0,
    is_verified     boolean     NOT NULL DEFAULT false,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_social_accounts_creator_id ON social_accounts(creator_id);

-- ============================================================
-- content_previews (creator portfolio)
-- ============================================================
CREATE TABLE IF NOT EXISTS content_previews (
    id            uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id    uuid        NOT NULL REFERENCES creators(id) ON DELETE CASCADE,
    type          varchar(20) NOT NULL,
    thumbnail_url varchar(500) NOT NULL,
    media_url     varchar(500) NOT NULL,
    platform      varchar(30) NOT NULL,
    views         integer,
    likes         integer,
    created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_content_previews_creator_id ON content_previews(creator_id);

-- ============================================================
-- package_analytics (per-package metrics)
-- ============================================================
CREATE TABLE IF NOT EXISTS package_analytics (
    package_id             uuid         PRIMARY KEY REFERENCES packages(id) ON DELETE CASCADE,
    views                  integer      NOT NULL DEFAULT 0,
    clicks                 integer      NOT NULL DEFAULT 0,
    inquiries              integer      NOT NULL DEFAULT 0,
    conversion_rate        numeric(5,2) NOT NULL DEFAULT 0,
    completion_rate        numeric(5,2) NOT NULL DEFAULT 0,
    repeat_brands          integer      NOT NULL DEFAULT 0,
    engagement_performance numeric(5,2) NOT NULL DEFAULT 0,
    updated_at             timestamptz  NOT NULL DEFAULT now()
);

-- ============================================================
-- wallets (creator balance)
-- ============================================================
CREATE TABLE IF NOT EXISTS wallets (
    creator_id        uuid        PRIMARY KEY REFERENCES creators(id) ON DELETE CASCADE,
    total_earned      integer     NOT NULL DEFAULT 0,
    available_balance integer     NOT NULL DEFAULT 0,
    pending_balance   integer     NOT NULL DEFAULT 0,
    updated_at        timestamptz NOT NULL DEFAULT now()
);

-- ============================================================
-- transactions (earnings / withdrawal history)
-- ============================================================
CREATE TABLE IF NOT EXISTS transactions (
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id  uuid        NOT NULL REFERENCES creators(id) ON DELETE CASCADE,
    order_id    uuid        REFERENCES orders(id) ON DELETE SET NULL,
    type        varchar(30) NOT NULL,
    amount      integer     NOT NULL,
    description varchar(300) NOT NULL,
    status      varchar(30) NOT NULL DEFAULT 'pending',
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_transactions_creator_type ON transactions(creator_id, type);
CREATE INDEX IF NOT EXISTS idx_transactions_creator_date ON transactions(creator_id, created_at DESC);

-- ============================================================
-- payout_methods (withdrawal destinations)
-- ============================================================
CREATE TABLE IF NOT EXISTS payout_methods (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id      uuid        NOT NULL REFERENCES creators(id) ON DELETE CASCADE,
    type            varchar(30) NOT NULL,
    name            varchar(100) NOT NULL,
    account_details varchar(300) NOT NULL,
    is_default      boolean     NOT NULL DEFAULT false,
    created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_payout_methods_creator_id ON payout_methods(creator_id);

-- ============================================================
-- withdrawal_requests
-- ============================================================
CREATE TABLE IF NOT EXISTS withdrawal_requests (
    id               uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id       uuid        NOT NULL REFERENCES creators(id) ON DELETE CASCADE,
    payout_method_id uuid        NOT NULL REFERENCES payout_methods(id) ON DELETE RESTRICT,
    amount           integer     NOT NULL,
    status           varchar(30) NOT NULL DEFAULT 'pending',
    processed_at     timestamptz,
    created_at       timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_withdrawals_creator_id ON withdrawal_requests(creator_id);

-- ============================================================
-- ambassador_applications
-- ============================================================
CREATE TABLE IF NOT EXISTS ambassador_applications (
    id                      uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id              uuid        NOT NULL UNIQUE REFERENCES creators(id) ON DELETE CASCADE,
    status                  varchar(30) NOT NULL DEFAULT 'draft',
    submitted_at            timestamptz,
    identity_verified       boolean     NOT NULL DEFAULT false,
    engagement_verified     boolean     NOT NULL DEFAULT false,
    content_review_passed   boolean     NOT NULL DEFAULT false,
    background_check_passed boolean     NOT NULL DEFAULT false,
    notes                   text,
    rejection_reason        text,
    approved_at             timestamptz,
    reviewed_by             uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now()
);

-- ============================================================
-- ambassador_scores (computed tier & percentile)
-- ============================================================
CREATE TABLE IF NOT EXISTS ambassador_scores (
    creator_id                  uuid        PRIMARY KEY REFERENCES creators(id) ON DELETE CASCADE,
    total                       integer     NOT NULL DEFAULT 0,
    delivery_score              integer     NOT NULL DEFAULT 0,
    account_age_score           integer     NOT NULL DEFAULT 0,
    rating_score                integer     NOT NULL DEFAULT 0,
    cancellation_score          integer     NOT NULL DEFAULT 0,
    profile_completeness_score  integer     NOT NULL DEFAULT 0,
    consistency_score           integer     NOT NULL DEFAULT 0,
    tier                        varchar(50) NOT NULL DEFAULT 'rising_creator',
    percentile_rank             integer     NOT NULL DEFAULT 0,
    strengths                   jsonb       NOT NULL DEFAULT '[]'::jsonb,
    improvements                jsonb       NOT NULL DEFAULT '[]'::jsonb,
    calculated_at               timestamptz NOT NULL DEFAULT now()
);

-- ============================================================
-- saved_creators (brand bookmarks)
-- ============================================================
CREATE TABLE IF NOT EXISTS saved_creators (
    brand_id   uuid        NOT NULL REFERENCES brands(id) ON DELETE CASCADE,
    creator_id uuid        NOT NULL REFERENCES creators(id) ON DELETE CASCADE,
    saved_at   timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (brand_id, creator_id)
);
CREATE INDEX IF NOT EXISTS idx_saved_creators_brand_id ON saved_creators(brand_id);

-- ============================================================
-- notification_preferences (per-user settings)
-- ============================================================
CREATE TABLE IF NOT EXISTS notification_preferences (
    user_id              uuid    PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    new_orders           boolean NOT NULL DEFAULT true,
    messages             boolean NOT NULL DEFAULT true,
    reviews              boolean NOT NULL DEFAULT true,
    marketing            boolean NOT NULL DEFAULT false,
    weekly_digest        boolean NOT NULL DEFAULT true,
    push_notifications   boolean NOT NULL DEFAULT true,
    email_notifications  boolean NOT NULL DEFAULT true,
    sms_notifications    boolean NOT NULL DEFAULT false
);

-- ============================================================
-- quick_deal_offers (standalone offer, may embed in message)
-- ============================================================
CREATE TABLE IF NOT EXISTS quick_deal_offers (
    id                      uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id              uuid        REFERENCES messages(id) ON DELETE SET NULL,
    conversation_id         uuid        NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    deal_type               varchar(30) NOT NULL,
    amount                  integer,
    barter_details          text,
    barter_category         varchar(100),
    estimated_barter_value  integer,
    creator_expectation     text,
    message                 text        NOT NULL,
    status                  varchar(30) NOT NULL DEFAULT 'pending',
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_quick_deals_conversation ON quick_deal_offers(conversation_id);

