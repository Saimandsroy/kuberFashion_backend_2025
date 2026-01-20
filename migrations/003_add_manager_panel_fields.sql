-- Migration 003: Add missing Manager Panel fields to tasks table
-- Based on deep dive analysis of production Manager Create Order form

-- Add manual order ID (for "Order ID" field in form)
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS manual_order_id VARCHAR(50);

-- Add client website (for "Client website" field)
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS client_website VARCHAR(500);

-- Add FC flag (Foreign Currency - for "FC" Yes/No field)
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS fc BOOLEAN DEFAULT false;

-- Add order package (for "Order package" dropdown)
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS order_package VARCHAR(100);

-- Add category (for "Category" dropdown)
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS category VARCHAR(100);

-- Add FC pricing columns to websites table
ALTER TABLE websites ADD COLUMN IF NOT EXISTS fc_gp_price DECIMAL(10, 2) DEFAULT 0.00;
ALTER TABLE websites ADD COLUMN IF NOT EXISTS fc_niche_price DECIMAL(10, 2) DEFAULT 0.00;

-- Create indexes for new fields
CREATE INDEX IF NOT EXISTS idx_tasks_category ON tasks(category);
CREATE INDEX IF NOT EXISTS idx_tasks_order_package ON tasks(order_package);

COMMENT ON COLUMN tasks.manual_order_id IS 'Manager-entered Order ID for client tracking';
COMMENT ON COLUMN tasks.client_website IS 'Client website URL for the order';
COMMENT ON COLUMN tasks.fc IS 'Foreign Currency flag - affects pricing';
COMMENT ON COLUMN tasks.order_package IS 'Package type selected for the order';
COMMENT ON COLUMN tasks.category IS 'Category/niche for the order';
