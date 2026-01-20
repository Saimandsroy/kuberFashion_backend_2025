const express = require('express');
const router = express.Router();
const accountantController = require('../controllers/accountantController');
const { authenticate, authorize } = require('../middleware/auth');

// All routes require Accountant role
router.use(authenticate, authorize('Accountant'));

// ==================== PAYMENT MANAGEMENT ====================
router.get('/payments', accountantController.getPayments);
router.patch('/payments/:id/pay', accountantController.processPayment);

module.exports = router;
