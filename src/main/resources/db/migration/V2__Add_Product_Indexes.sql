-- =====================================================
-- Product Management Optimization Migration
-- Version: 2
-- Description: Add indexes for better query performance
-- =====================================================

-- Index for active products filtering (most common query)
CREATE INDEX IF NOT EXISTS idx_products_active 
ON products(active);

-- Index for category-based filtering
CREATE INDEX IF NOT EXISTS idx_products_category_active 
ON products(category_id, active);

-- Index for featured products
CREATE INDEX IF NOT EXISTS idx_products_featured_active 
ON products(featured, active) 
WHERE active = true;

-- Index for stock availability
CREATE INDEX IF NOT EXISTS idx_products_instock_active 
ON products(in_stock, active) 
WHERE active = true;

-- Index for price range queries
CREATE INDEX IF NOT EXISTS idx_products_price 
ON products(price);

-- Index for rating-based sorting
CREATE INDEX IF NOT EXISTS idx_products_rating 
ON products(rating DESC);

-- Index for newest products sorting
CREATE INDEX IF NOT EXISTS idx_products_created_at 
ON products(created_at DESC);

-- Composite index for category + price range queries
CREATE INDEX IF NOT EXISTS idx_products_category_price 
ON products(category_id, price, active);

-- Index for search by name (PostgreSQL specific - case insensitive)
CREATE INDEX IF NOT EXISTS idx_products_name_lower 
ON products(LOWER(name));

-- Index for product images join
CREATE INDEX IF NOT EXISTS idx_product_images_product_id 
ON product_images(product_id);

-- Index for product sizes join
CREATE INDEX IF NOT EXISTS idx_product_sizes_product_id 
ON product_sizes(product_id);

-- Index for product colors join
CREATE INDEX IF NOT EXISTS idx_product_colors_product_id 
ON product_colors(product_id);

-- =====================================================
-- Performance Statistics Update
-- =====================================================
-- Analyze tables to update statistics for query planner
ANALYZE products;
ANALYZE product_images;
ANALYZE product_sizes;
ANALYZE product_colors;
ANALYZE categories;

-- =====================================================
-- Comments for documentation
-- =====================================================
COMMENT ON INDEX idx_products_active IS 'Optimizes queries filtering by active status';
COMMENT ON INDEX idx_products_category_active IS 'Optimizes category-based product listings';
COMMENT ON INDEX idx_products_featured_active IS 'Optimizes featured products queries';
COMMENT ON INDEX idx_products_price IS 'Optimizes price range filtering';
COMMENT ON INDEX idx_products_rating IS 'Optimizes top-rated products queries';
COMMENT ON INDEX idx_products_created_at IS 'Optimizes newest products queries';
