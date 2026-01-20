const Transaction = require('../models/Transaction');
const { processWithdrawalApproval } = require('../utils/walletService');

/**
 * Accountant Controller
 * Handles payment processing and financial records
 */

/**
 * @route   GET /api/accountant/payments
 * @desc    Get all payment requests (withdrawals)
 * @access  Accountant only
 */
const getPayments = async (req, res, next) => {
    try {
        const { status } = req.query;

        const filters = {};
        if (status && status !== 'all') filters.status = status;

        const payments = await Transaction.findAll(filters);

        res.json({
            count: payments.length,
            payments
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   PATCH /api/accountant/payments/:id/pay
 * @desc    Mark a payment as paid
 * @access  Accountant only
 */
const processPayment = async (req, res, next) => {
    try {
        const { id } = req.params;

        // Using the existing wallet service function which handles:
        // 1. Verification of transaction status
        // 2. Deduction from wallet (if not already done)
        // 3. Updating transaction status to 'Paid'
        const payment = await processWithdrawalApproval(id, req.user.id);

        res.json({
            message: 'Payment processed successfully',
            payment
        });
    } catch (error) {
        next(error);
    }
};

module.exports = {
    getPayments,
    processPayment
};
