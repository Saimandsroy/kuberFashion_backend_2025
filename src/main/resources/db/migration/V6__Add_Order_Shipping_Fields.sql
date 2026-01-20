-- V6__Add_Order_Shipping_Fields.sql
-- Add detailed shipping address fields to orders table

ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_first_name VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_last_name VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_address_line1 VARCHAR(200);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_address_line2 VARCHAR(200);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_city VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_state VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_postal_code VARCHAR(20);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_country VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_phone VARCHAR(15);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS total_items INTEGER DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_transaction_id VARCHAR(100);
