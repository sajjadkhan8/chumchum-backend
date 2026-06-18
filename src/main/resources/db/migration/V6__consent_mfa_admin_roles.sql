-- CRIT-7: Store user consent to Terms of Service with timestamp and version
ALTER TABLE core.users ADD COLUMN IF NOT EXISTS terms_accepted_at TIMESTAMPTZ;
ALTER TABLE core.users ADD COLUMN IF NOT EXISTS terms_version VARCHAR(20);

-- HIGH-8: TOTP MFA fields for admin accounts
ALTER TABLE core.users ADD COLUMN IF NOT EXISTS totp_secret VARCHAR(64);
ALTER TABLE core.users ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- MED-15: Expand role constraint to support least-privilege admin tiers
ALTER TABLE core.users DROP CONSTRAINT IF EXISTS ck_users_role;
ALTER TABLE core.users ADD CONSTRAINT ck_users_role
    CHECK (role IN ('CREATOR', 'BRAND', 'PLATFORM_ADMIN', 'SUPPORT', 'FINANCE_OPS'));
