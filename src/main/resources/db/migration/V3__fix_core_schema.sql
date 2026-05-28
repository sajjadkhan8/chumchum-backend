-- V3__fix_core_schema.sql
-- Aligns existing tables with backend-api-spec.md and current entity definitions.
-- All changes are additive (ADD COLUMN IF NOT EXISTS) + safe drops of clearly non-spec columns.

SET search_path TO core;

-- ============================================================
-- users: add name, avatar_url, creator_program_status, deleted_at
-- ============================================================
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS name                  varchar(100),
    ADD COLUMN IF NOT EXISTS avatar_url            varchar(500),
    ADD COLUMN IF NOT EXISTS creator_program_status varchar(40) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS deleted_at            timestamptz;

-- make password_hash nullable for phone-only accounts
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
-- make email nullable for phone-only accounts
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

-- ============================================================
-- creators: add all spec-required profile fields
-- ============================================================
ALTER TABLE creators
    ADD COLUMN IF NOT EXISTS username              varchar(50),
    ADD COLUMN IF NOT EXISTS cover_image_url       varchar(500),
    ADD COLUMN IF NOT EXISTS website               varchar(300),
    ADD COLUMN IF NOT EXISTS niche                 varchar(100),
    ADD COLUMN IF NOT EXISTS availability_status   varchar(100),
    ADD COLUMN IF NOT EXISTS response_time         varchar(50),
    ADD COLUMN IF NOT EXISTS min_price             integer,
    ADD COLUMN IF NOT EXISTS max_price             integer,
    ADD COLUMN IF NOT EXISTS is_verified           boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS is_trending           boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS is_fast_responder     boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS completed_deals       integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS accepts_barter        boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS accepts_hybrid_deals  boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS minimum_budget        integer,
    ADD COLUMN IF NOT EXISTS preferred_industries  text,
    ADD COLUMN IF NOT EXISTS languages             jsonb NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS categories            jsonb NOT NULL DEFAULT '[]'::jsonb;

-- ============================================================
-- brands: rename company_name → name, add logo_url and monthly_budget
-- ============================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'core' AND table_name = 'brands' AND column_name = 'company_name'
    ) THEN
        ALTER TABLE brands RENAME COLUMN company_name TO name;
    END IF;
END$$;

ALTER TABLE brands
    ADD COLUMN IF NOT EXISTS logo_url       varchar(500),
    ADD COLUMN IF NOT EXISTS monthly_budget integer;

-- ============================================================
-- packages: add missing spec fields, fix status/visibility
-- ============================================================
ALTER TABLE packages
    ADD COLUMN IF NOT EXISTS deal_type              varchar(30),
    ADD COLUMN IF NOT EXISTS status                 varchar(30) NOT NULL DEFAULT 'draft',
    ADD COLUMN IF NOT EXISTS visibility             varchar(30) NOT NULL DEFAULT 'public',
    ADD COLUMN IF NOT EXISTS short_description      varchar(300),
    ADD COLUMN IF NOT EXISTS full_description       text,
    ADD COLUMN IF NOT EXISTS barter_description     text,
    ADD COLUMN IF NOT EXISTS barter_category        varchar(100),
    ADD COLUMN IF NOT EXISTS estimated_barter_value integer,
    ADD COLUMN IF NOT EXISTS hybrid_cash_amount     integer,
    ADD COLUMN IF NOT EXISTS hybrid_barter_value    integer,
    ADD COLUMN IF NOT EXISTS creator_expectations   text,
    ADD COLUMN IF NOT EXISTS thumbnail_url          varchar(500),
    ADD COLUMN IF NOT EXISTS is_popular             boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS orders_completed       integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS response_time          varchar(50);

-- copy cover_image into thumbnail_url
UPDATE packages SET thumbnail_url = cover_image WHERE thumbnail_url IS NULL AND cover_image IS NOT NULL;
-- populate deal_type from pricing_type
UPDATE packages SET deal_type = LOWER(pricing_type) WHERE deal_type IS NULL;
-- make type nullable (spec does not require package type sub-tier)
ALTER TABLE packages ALTER COLUMN type DROP NOT NULL;

-- ============================================================
-- orders: full spec-alignment restructure
-- ============================================================
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS order_number  varchar(20),
    ADD COLUMN IF NOT EXISTS deal_type     varchar(30) NOT NULL DEFAULT 'paid',
    ADD COLUMN IF NOT EXISTS amount        integer,
    ADD COLUMN IF NOT EXISTS barter_details text,
    ADD COLUMN IF NOT EXISTS message       text,
    ADD COLUMN IF NOT EXISTS status        varchar(30) NOT NULL DEFAULT 'pending',
    ADD COLUMN IF NOT EXISTS progress      integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS delivery_date date,
    ADD COLUMN IF NOT EXISTS deadline_date date;

-- migrate legacy price → amount
UPDATE orders SET amount = price::integer WHERE amount IS NULL AND price IS NOT NULL;
-- migrate legacy completed boolean → status
UPDATE orders
SET status = CASE WHEN completed THEN 'completed' ELSE 'pending' END
WHERE status = 'pending';

-- drop legacy non-spec columns (payment_intent, completed, price, title, image)
ALTER TABLE orders ALTER COLUMN payment_intent DROP NOT NULL;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_payment_intent_key;
ALTER TABLE orders DROP COLUMN IF EXISTS payment_intent;
ALTER TABLE orders DROP COLUMN IF EXISTS completed;
ALTER TABLE orders DROP COLUMN IF EXISTS price;
ALTER TABLE orders DROP COLUMN IF EXISTS title;
ALTER TABLE orders DROP COLUMN IF EXISTS image;

-- ============================================================
-- messages: rename description→content, add spec fields
-- ============================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'core' AND table_name = 'messages' AND column_name = 'description'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'core' AND table_name = 'messages' AND column_name = 'content'
    ) THEN
        ALTER TABLE messages RENAME COLUMN description TO content;
    END IF;
END$$;

ALTER TABLE messages ALTER COLUMN content DROP NOT NULL;

ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS type                         varchar(30) NOT NULL DEFAULT 'TEXT',
    ADD COLUMN IF NOT EXISTS is_read                      boolean     NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS attachment_url               varchar(500),
    ADD COLUMN IF NOT EXISTS sender_type                  varchar(20),
    ADD COLUMN IF NOT EXISTS offer_deal_type              varchar(30),
    ADD COLUMN IF NOT EXISTS offer_amount                 integer,
    ADD COLUMN IF NOT EXISTS offer_barter_details         text,
    ADD COLUMN IF NOT EXISTS offer_barter_category        varchar(100),
    ADD COLUMN IF NOT EXISTS offer_estimated_barter_value integer,
    ADD COLUMN IF NOT EXISTS offer_creator_expectation    text,
    ADD COLUMN IF NOT EXISTS offer_message                text,
    ADD COLUMN IF NOT EXISTS offer_status                 varchar(30);

-- ============================================================
-- conversations: ensure unread count columns exist
-- ============================================================
ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS unread_count_creator integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS unread_count_brand   integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_message_id      uuid;

-- ============================================================
-- reviews: add order_id, creator_id, brand_id, rating, comment
-- ============================================================
ALTER TABLE reviews
    ADD COLUMN IF NOT EXISTS order_id   uuid REFERENCES orders(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS creator_id uuid REFERENCES creators(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS brand_id   uuid REFERENCES brands(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS rating     integer,
    ADD COLUMN IF NOT EXISTS comment    text;

-- copy star → rating
UPDATE reviews SET rating = star WHERE rating IS NULL;
-- make star nullable (rating is the canonical column now)
ALTER TABLE reviews ALTER COLUMN star DROP NOT NULL;
-- drop old unique constraint; spec requires one review per order
ALTER TABLE reviews DROP CONSTRAINT IF EXISTS uk_review_user_package;

-- ============================================================
-- Indexes
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_orders_creator_status  ON orders (creator_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_brand_status    ON orders (brand_id,   status);
CREATE INDEX IF NOT EXISTS idx_packages_status        ON packages (status);
CREATE INDEX IF NOT EXISTS idx_packages_deal_type     ON packages (deal_type);
CREATE INDEX IF NOT EXISTS idx_packages_creator_status ON packages (creator_id, status);
CREATE INDEX IF NOT EXISTS idx_users_deleted_at       ON users (deleted_at) WHERE deleted_at IS NOT NULL;

