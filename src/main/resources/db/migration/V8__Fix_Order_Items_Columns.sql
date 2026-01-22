-- =====================================================
-- V8: Fix order_items table column names
-- Description: Rename columns to match JPA entity mapping
-- =====================================================

-- Rename 'price' column to 'unit_price'
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'order_items' 
        AND column_name = 'price'
    ) THEN
        ALTER TABLE order_items RENAME COLUMN price TO unit_price;
        RAISE NOTICE 'Renamed price column to unit_price';
    ELSE
        RAISE NOTICE 'Column price does not exist or already renamed';
    END IF;
END $$;

-- Rename 'size' column to 'selected_size'
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'order_items' 
        AND column_name = 'size'
    ) THEN
        ALTER TABLE order_items RENAME COLUMN size TO selected_size;
        RAISE NOTICE 'Renamed size column to selected_size';
    ELSE
        RAISE NOTICE 'Column size does not exist or already renamed';
    END IF;
END $$;

-- Rename 'color' column to 'selected_color'
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'order_items' 
        AND column_name = 'color'
    ) THEN
        ALTER TABLE order_items RENAME COLUMN color TO selected_color;
        RAISE NOTICE 'Renamed color column to selected_color';
    ELSE
        RAISE NOTICE 'Column color does not exist or already renamed';
    END IF;
END $$;

-- Add 'total_price' column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'order_items' 
        AND column_name = 'total_price'
    ) THEN
        ALTER TABLE order_items ADD COLUMN total_price DECIMAL(10,2) NOT NULL DEFAULT 0;
        RAISE NOTICE 'Added total_price column';
    ELSE
        RAISE NOTICE 'Column total_price already exists';
    END IF;
END $$;

-- Add 'updated_at' column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'order_items' 
        AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE order_items ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
        RAISE NOTICE 'Added updated_at column';
    ELSE
        RAISE NOTICE 'Column updated_at already exists';
    END IF;
END $$;

-- Update existing records to calculate total_price from unit_price * quantity
UPDATE order_items SET total_price = unit_price * quantity WHERE total_price = 0;

-- Remove the default constraint from total_price now that existing records are updated
ALTER TABLE order_items ALTER COLUMN total_price DROP DEFAULT;

-- =====================================================
-- Verification
-- =====================================================
-- SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'order_items';
