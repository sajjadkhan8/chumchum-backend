-- Sprint 7–8 gap fixes

-- 7.7: Admin moderation — add suspend/ban fields to users
ALTER TABLE core.users
    ADD COLUMN IF NOT EXISTS ban_reason      VARCHAR(500),
    ADD COLUMN IF NOT EXISTS suspended_until TIMESTAMPTZ;
