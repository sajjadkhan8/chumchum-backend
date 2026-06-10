-- Add Control tab fields to brand_offers table
ALTER TABLE core.brand_offers ADD COLUMN creator_type VARCHAR(50);
ALTER TABLE core.brand_offers ADD COLUMN follower_range VARCHAR(50);
ALTER TABLE core.brand_offers ADD COLUMN creator_gender_preference VARCHAR(20);
ALTER TABLE core.brand_offers ADD COLUMN min_age INT;
ALTER TABLE core.brand_offers ADD COLUMN max_age INT;
ALTER TABLE core.brand_offers ADD COLUMN application_type VARCHAR(50);
ALTER TABLE core.brand_offers ADD COLUMN max_applicants INT;
ALTER TABLE core.brand_offers ADD COLUMN proposal_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE core.brand_offers ADD COLUMN portfolio_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE core.brand_offers ADD COLUMN custom_screening_questions TEXT;
ALTER TABLE core.brand_offers ADD COLUMN content_submission_deadline DATE;
ALTER TABLE core.brand_offers ADD COLUMN go_live_date DATE;
ALTER TABLE core.brand_offers ADD COLUMN campaign_duration INT;

