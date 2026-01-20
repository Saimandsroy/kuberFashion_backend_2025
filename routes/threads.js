const express = require('express');
const router = express.Router();
const threadController = require('../controllers/threadController');
const { authenticate } = require('../middleware/auth');

/**
 * Thread Routes
 * All routes require authentication
 */

// Get thread stats for current user
router.get('/stats', authenticate, threadController.getThreadStats);

// Get all threads for user
router.get('/', authenticate, threadController.getThreads);

// Get thread by ID with messages
router.get('/:id', authenticate, threadController.getThreadById);

// Create new thread
router.post('/', authenticate, threadController.createThread);

// Add message to thread
router.post('/:id/messages', authenticate, threadController.addMessage);

// Update thread status
router.patch('/:id/status', authenticate, threadController.updateThreadStatus);

// Update thread details
router.put('/:id', authenticate, threadController.updateThread);

// Delete thread
router.delete('/:id', authenticate, threadController.deleteThread);

module.exports = router;
