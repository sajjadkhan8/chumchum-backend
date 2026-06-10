-- Drop legacy offer targeting/requirements columns decommissioned from wizard flow
ALTER TABLE core.brand_offers DROP COLUMN IF EXISTS requirements;
ALTER TABLE core.brand_offers DROP COLUMN IF EXISTS min_followers;
ALTER TABLE core.brand_offers DROP COLUMN IF EXISTS min_engagement_rate;
ALTER TABLE core.brand_offers DROP COLUMN IF EXISTS preferred_delivery_days;
ALTER TABLE core.brand_offers DROP COLUMN IF EXISTS slots;

