-- V7__Update_Payment_Method_Constraint.sql
-- Update payment method constraint to allow PHONEPE and CASH_ON_DELIVERY

-- Drop the old constraint
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_payment_method_check;

-- Create new constraint with updated payment methods
ALTER TABLE orders ADD CONSTRAINT orders_payment_method_check
    CHECK (payment_method IN ('PHONEPE', 'CASH_ON_DELIVERY', 'CREDIT_CARD', 'DEBIT_CARD', 'UPI', 'NET_BANKING', 'COD', 'WALLET'));
