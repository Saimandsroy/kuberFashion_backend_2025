-- V5__Add_Payment_Transactions_Table.sql
-- Create payment_transactions table for tracking PhonePe payments

CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    merchant_order_id VARCHAR(100) NOT NULL UNIQUE,
    phonepe_transaction_id VARCHAR(100),
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    payment_method VARCHAR(50),
    error_code VARCHAR(50),
    error_message VARCHAR(500),
    callback_response TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_payment_transactions_order_id ON payment_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_merchant_order_id ON payment_transactions(merchant_order_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_status ON payment_transactions(status);

-- Trigger for updated_at
CREATE TRIGGER update_payment_transactions_updated_at 
    BEFORE UPDATE ON payment_transactions 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Add address_id column to orders table for linking shipping address
ALTER TABLE orders ADD COLUMN IF NOT EXISTS address_id BIGINT REFERENCES addresses(id);
CREATE INDEX IF NOT EXISTS idx_orders_address_id ON orders(address_id);
