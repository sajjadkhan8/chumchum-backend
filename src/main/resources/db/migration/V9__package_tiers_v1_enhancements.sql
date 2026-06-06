-- V9: Package Tiers V1 Enhancements
-- Adds enhanced tier support for package pricing variants (Lite/Standard/Premium, etc.)
-- Ensures PKR-only currency for V1

BEGIN;

-- Add new columns to package_tiers table
ALTER TABLE core.package_tiers
ADD COLUMN IF NOT EXISTS description TEXT,
ADD COLUMN IF NOT EXISTS position INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS is_primary BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Change deliverables from string to jsonb array for consistency
ALTER TABLE core.package_tiers
DROP COLUMN IF EXISTS deliverables;

ALTER TABLE core.package_tiers
ADD COLUMN IF NOT EXISTS deliverables jsonb DEFAULT '[]'::jsonb;

-- Update price field to integer (removing BigDecimal precision)
ALTER TABLE core.package_tiers
ALTER COLUMN price TYPE INTEGER;

-- Ensure package_tiers.package_id is not null
ALTER TABLE core.package_tiers
ALTER COLUMN package_id SET NOT NULL;

-- Add unique constraint on service_package.currency to enforce PKR only in V1 scope
-- (For multi-currency support later, this can be relaxed)
ALTER TABLE core.packages
ALTER COLUMN currency SET DEFAULT 'PKR';

-- Create index on package_tiers for better query performance
CREATE INDEX IF NOT EXISTS idx_package_tiers_package_id_position
ON core.package_tiers(package_id, position);

-- Update existing packages to ensure currency is PKR (V1 requirement)
UPDATE core.packages
SET currency = 'PKR'
WHERE currency IS NULL OR currency != 'PKR';

-- Backfill position for existing tiers
WITH tier_positions AS (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY package_id ORDER BY created_at) - 1 AS new_position
  FROM core.package_tiers
)
UPDATE core.package_tiers
SET position = tp.new_position
FROM tier_positions tp
WHERE core.package_tiers.id = tp.id
AND core.package_tiers.position IS NULL;

-- Ensure first tier in each package is marked as primary
WITH first_tiers AS (
  SELECT id FROM core.package_tiers
  WHERE position = 0 OR (position IS NULL AND created_at = (SELECT MIN(created_at) FROM core.package_tiers pt2 WHERE pt2.package_id = core.package_tiers.package_id))
)
UPDATE core.package_tiers
SET is_primary = true
WHERE id IN (SELECT id FROM first_tiers);

COMMIT;

