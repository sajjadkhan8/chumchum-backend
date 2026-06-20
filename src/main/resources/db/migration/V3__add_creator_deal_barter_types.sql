-- Add deal_types and barter_types JSONB columns to core.creators
ALTER TABLE core.creators
    ADD COLUMN IF NOT EXISTS deal_types   jsonb,
    ADD COLUMN IF NOT EXISTS barter_types jsonb;
