-- Sprint 4–6 gap fixes
-- 1. Quick deal offers: add delivery_days and platform so synthetic packages use real values
ALTER TABLE quick_deal_offers
    ADD COLUMN delivery_days integer NOT NULL DEFAULT 7,
    ADD COLUMN platform      varchar(30) NOT NULL DEFAULT 'INSTAGRAM';

-- 2. Orders: add barter_product_received flag for barter delivery confirmation
ALTER TABLE orders
    ADD COLUMN barter_product_received boolean NOT NULL DEFAULT false;
