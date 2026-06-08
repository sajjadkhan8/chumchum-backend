-- Add notification preference columns to creator_payout_preferences table
ALTER TABLE creator_payout_preferences
ADD COLUMN earnings_notifications_enabled boolean NOT NULL DEFAULT true,
ADD COLUMN weekly_digest_enabled boolean NOT NULL DEFAULT false;

