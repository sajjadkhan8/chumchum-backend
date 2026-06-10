-- V17: Budget & Payment fields for brand_offers
ALTER TABLE core.brand_offers
    ADD COLUMN IF NOT EXISTS budget_type            VARCHAR(30),
    ADD COLUMN IF NOT EXISTS payment_structure      VARCHAR(30),
    ADD COLUMN IF NOT EXISTS barter_product_desc    TEXT,
    ADD COLUMN IF NOT EXISTS barter_estimated_value INTEGER,
    ADD COLUMN IF NOT EXISTS travel_costs_covered   BOOLEAN NOT NULL DEFAULT FALSE;

-- Back-fill: treat existing rows as 'fixed' type
UPDATE core.brand_offers SET budget_type = 'fixed' WHERE budget_type IS NULL;

