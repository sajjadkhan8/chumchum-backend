-- Allow creators to review brands (bidirectional reviews).
-- Previously the reviews table had a single unique constraint on order_id,
-- allowing only one review per order total. We change this to one review per
-- (order_id, reviewer_type) so both parties can review the same order.

set search_path to core;

-- 1. Add reviewer_type column. Existing rows are all brand reviews.
ALTER TABLE reviews
    ADD COLUMN IF NOT EXISTS reviewer_type VARCHAR(10) NOT NULL DEFAULT 'BRAND';

-- 2. Drop the old single-review-per-order constraint.
ALTER TABLE reviews DROP CONSTRAINT IF EXISTS uk_review_order;

-- 3. New constraint: one review per order per reviewer type.
ALTER TABLE reviews
    ADD CONSTRAINT uk_review_order_type UNIQUE (order_id, reviewer_type);

-- 4. Add aggregate brand rating columns (mirrors creator rating/total_reviews).
ALTER TABLE brands
    ADD COLUMN IF NOT EXISTS brand_rating NUMERIC(3,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS brand_total_reviews INT NOT NULL DEFAULT 0;
