-- Tags are removed from offer wizard to avoid confusion with hashtags/mentions.
ALTER TABLE core.brand_offers DROP COLUMN IF EXISTS tags;

