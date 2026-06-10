-- Add enhanced references/guidelines fields to brand_offers
ALTER TABLE core.brand_offers ADD COLUMN key_message TEXT;
ALTER TABLE core.brand_offers ADD COLUMN dos_and_donts TEXT;
ALTER TABLE core.brand_offers ADD COLUMN hashtags_mentions TEXT;
ALTER TABLE core.brand_offers ADD COLUMN usage_rights TEXT;
ALTER TABLE core.brand_offers ADD COLUMN terms_and_conditions TEXT;
ALTER TABLE core.brand_offers ADD COLUMN expected_outcomes TEXT;

