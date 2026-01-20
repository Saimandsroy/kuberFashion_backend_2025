-- IMMEDIATE FIX: Add missing kuber_coupons column
-- Execute this directly on your Supabase database

-- Step 1: Add the column with default value (safe for existing data)
ALTER TABLE users ADD COLUMN kuber_coupons INTEGER DEFAULT 0 NOT NULL;

-- Step 2: Verify the column was added
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'users' AND column_name = 'kuber_coupons';

-- Step 3: Update existing users to have 0 coupons (redundant due to DEFAULT, but explicit)
UPDATE users SET kuber_coupons = 0 WHERE kuber_coupons IS NULL;

-- Step 4: Verify data integrity
SELECT COUNT(*) as total_users, 
       COUNT(kuber_coupons) as users_with_coupons,
       AVG(kuber_coupons) as avg_coupons
FROM users;
