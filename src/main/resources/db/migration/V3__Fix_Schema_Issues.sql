-- =====================================================
-- Schema Fixes for E-Commerce Platform
-- Version: 3
-- Description: Fix email column type and ensure proper constraints
-- =====================================================

-- Fix 1: Change email column from bytea to VARCHAR if needed
-- This fixes the "function lower(bytea) does not exist" error
DO $$
BEGIN
    -- Check if email column is bytea type
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' 
        AND column_name = 'email' 
        AND data_type = 'bytea'
    ) THEN
        -- Convert bytea to VARCHAR
        ALTER TABLE users ALTER COLUMN email TYPE VARCHAR(100) USING email::text;
        RAISE NOTICE 'Email column converted from bytea to VARCHAR(100)';
    ELSE
        RAISE NOTICE 'Email column is already VARCHAR type';
    END IF;
END $$;

-- Fix 2: Ensure email column has proper constraints
ALTER TABLE users 
    ALTER COLUMN email SET NOT NULL;

-- Ensure unique constraint exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'users_email_key'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT users_email_key UNIQUE (email);
        RAISE NOTICE 'Added unique constraint on email';
    END IF;
END $$;

-- Fix 3: Create index for case-insensitive email search
CREATE INDEX IF NOT EXISTS idx_users_email_lower 
ON users(LOWER(email));

-- Fix 4: Ensure wishlist_items table exists with proper structure
CREATE TABLE IF NOT EXISTS wishlist_items (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT unique_user_product UNIQUE (user_id, product_id)
);

-- Fix 5: Create indexes for wishlist performance
CREATE INDEX IF NOT EXISTS idx_wishlist_user_id ON wishlist_items(user_id);
CREATE INDEX IF NOT EXISTS idx_wishlist_product_id ON wishlist_items(product_id);

-- Fix 6: Ensure product visibility flags are properly set
UPDATE products 
SET active = true 
WHERE active IS NULL;

UPDATE products 
SET in_stock = true 
WHERE in_stock IS NULL;

-- Fix 7: Add index for product active status queries
CREATE INDEX IF NOT EXISTS idx_products_active_category 
ON products(active, category_id) 
WHERE active = true;

-- Fix 8: Create view for admin-user product synchronization
CREATE OR REPLACE VIEW v_active_products AS
SELECT 
    p.id,
    p.name,
    p.slug,
    p.price,
    p.original_price,
    p.discount,
    p.image,
    p.rating,
    p.reviews,
    p.description,
    p.in_stock,
    p.featured,
    p.stock_quantity,
    p.created_at,
    c.id as category_id,
    c.slug as category_slug,
    c.name as category_name
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE p.active = true;

-- Fix 9: Add comments for documentation
COMMENT ON TABLE wishlist_items IS 'User wishlist items with product references';
COMMENT ON INDEX idx_users_email_lower IS 'Enables case-insensitive email search';
COMMENT ON VIEW v_active_products IS 'Active products with category info for user dashboard';

-- Fix 10: Grant necessary permissions (adjust as needed for your setup)
-- GRANT SELECT ON v_active_products TO your_app_user;

-- =====================================================
-- Verification Queries (Run these to verify fixes)
-- =====================================================
-- Check email column type:
-- SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'email';

-- Check wishlist table structure:
-- SELECT * FROM information_schema.tables WHERE table_name = 'wishlist_items';

-- Verify active products:
-- SELECT COUNT(*) FROM v_active_products;

-- =====================================================
-- Performance Statistics Update
-- =====================================================
ANALYZE users;
ANALYZE wishlist_items;
ANALYZE products;
ANALYZE categories;
