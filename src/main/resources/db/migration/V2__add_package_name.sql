-- Add name column to packages table to allow creators to distinguish between packages with identical details
ALTER TABLE core.packages
ADD COLUMN name varchar(100) NOT NULL DEFAULT 'Unnamed Package';

-- Update existing packages to have meaningful names (title if available)
UPDATE core.packages SET name = title WHERE name = 'Unnamed Package';

-- Remove the default constraint after populating the column
ALTER TABLE core.packages
ALTER COLUMN name DROP DEFAULT;

