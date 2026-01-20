const express = require('express');
const router = express.Router();
const writerController = require('../controllers/writerController');
const { authenticate, authorize } = require('../middleware/auth');

// All routes require Writer role
router.use(authenticate, authorize('Writer'));

/**
 * @route   GET /api/writer/tasks
 * @desc    Get my assigned tasks
 * @access  Writer only
 */
router.get('/tasks', writerController.getMyTasks);

/**
 * @route   GET /api/writer/tasks/:id
 * @desc    Get specific task
 * @access  Writer only
 */
router.get('/tasks/:id', writerController.getTaskById);

/**
 * @route   POST /api/writer/tasks/:id/submit-content
 * @desc    Submit content
 * @access  Writer only
 */
router.post('/tasks/:id/submit-content', writerController.submitContent);

/**
 * @route   PATCH /api/writer/tasks/:id/mark-in-progress
 * @desc    Mark task as in progress
 * @access  Writer only
 */
router.patch('/tasks/:id/mark-in-progress', writerController.markInProgress);

/**
 * @route   GET /api/writer/dashboard
 * @desc    Get dashboard statistics
 * @access  Writer only
 */
router.get('/dashboard', writerController.getDashboardStats);

/**
 * @route   GET /api/writer/completed-orders
 * @desc    Get completed orders
 * @access  Writer only
 */
router.get('/completed-orders', writerController.getCompletedOrders);

/**
 * @route   GET /api/writer/completed-orders/:id
 * @desc    Get specific completed order detail
 * @access  Writer only
 */
router.get('/completed-orders/:id', writerController.getCompletedOrderDetail);

module.exports = router;
