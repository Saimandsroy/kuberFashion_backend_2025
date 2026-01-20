const Thread = require('../models/Thread');

/**
 * Thread Controller
 * Handles communication threads/tickets between users
 */

/**
 * @route   GET /api/threads
 * @desc    Get all threads for user
 * @access  All authenticated users
 */
const getThreads = async (req, res, next) => {
    try {
        const { status, task_id } = req.query;

        const filters = { user_id: req.user.id };
        if (status) filters.status = status;
        if (task_id) filters.task_id = task_id;

        const threads = await Thread.findAll(filters);

        res.json({
            count: threads.length,
            threads
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   GET /api/threads/:id
 * @desc    Get thread by ID with messages
 * @access  All authenticated users
 */
const getThreadById = async (req, res, next) => {
    try {
        const { id } = req.params;
        const thread = await Thread.findById(id);

        if (!thread) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Thread not found'
            });
        }

        // Check if user has access to this thread
        if (thread.created_by !== req.user.id && thread.assigned_to !== req.user.id) {
            // Allow managers and admins to view all threads
            if (req.user.role !== 'Manager' && req.user.role !== 'Admin') {
                return res.status(403).json({
                    error: 'Forbidden',
                    message: 'You do not have access to this thread'
                });
            }
        }

        res.json({ thread });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   POST /api/threads
 * @desc    Create new thread
 * @access  All authenticated users
 */
const createThread = async (req, res, next) => {
    try {
        const { title, subject, priority, assigned_to, task_id } = req.body;

        // Validation
        if (!title) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Title is required'
            });
        }

        const thread = await Thread.create({
            title,
            subject,
            priority,
            created_by: req.user.id,
            assigned_to,
            task_id
        });

        res.status(201).json({
            message: 'Thread created successfully',
            thread
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   POST /api/threads/:id/messages
 * @desc    Add message to thread
 * @access  All authenticated users with access
 */
const addMessage = async (req, res, next) => {
    try {
        const { id } = req.params;
        const { message, attachments } = req.body;

        // Validation
        if (!message || message.trim().length === 0) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Message is required'
            });
        }

        // Check thread exists
        const thread = await Thread.findById(id);
        if (!thread) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Thread not found'
            });
        }

        // Check access
        if (thread.created_by !== req.user.id && thread.assigned_to !== req.user.id) {
            if (req.user.role !== 'Manager' && req.user.role !== 'Admin') {
                return res.status(403).json({
                    error: 'Forbidden',
                    message: 'You do not have access to this thread'
                });
            }
        }

        const newMessage = await Thread.addMessage(id, req.user.id, message, attachments);

        res.status(201).json({
            message: 'Message added successfully',
            threadMessage: newMessage
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   PATCH /api/threads/:id/status
 * @desc    Update thread status
 * @access  Thread creator or assigned user or Manager/Admin
 */
const updateThreadStatus = async (req, res, next) => {
    try {
        const { id } = req.params;
        const { status } = req.body;

        const validStatuses = ['Open', 'In Progress', 'Resolved', 'Closed'];
        if (!status || !validStatuses.includes(status)) {
            return res.status(400).json({
                error: 'Validation Error',
                message: `Status must be one of: ${validStatuses.join(', ')}`
            });
        }

        const thread = await Thread.findById(id);
        if (!thread) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Thread not found'
            });
        }

        // Check access
        if (thread.created_by !== req.user.id && thread.assigned_to !== req.user.id) {
            if (req.user.role !== 'Manager' && req.user.role !== 'Admin') {
                return res.status(403).json({
                    error: 'Forbidden',
                    message: 'You cannot update this thread'
                });
            }
        }

        const updatedThread = await Thread.updateStatus(id, status);

        res.json({
            message: 'Thread status updated',
            thread: updatedThread
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   PUT /api/threads/:id
 * @desc    Update thread details
 * @access  Thread creator or Manager/Admin
 */
const updateThread = async (req, res, next) => {
    try {
        const { id } = req.params;
        const updates = req.body;

        const thread = await Thread.findById(id);
        if (!thread) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Thread not found'
            });
        }

        // Check access
        if (thread.created_by !== req.user.id) {
            if (req.user.role !== 'Manager' && req.user.role !== 'Admin') {
                return res.status(403).json({
                    error: 'Forbidden',
                    message: 'You cannot update this thread'
                });
            }
        }

        const updatedThread = await Thread.update(id, updates);

        res.json({
            message: 'Thread updated successfully',
            thread: updatedThread
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   DELETE /api/threads/:id
 * @desc    Delete thread
 * @access  Thread creator or Admin
 */
const deleteThread = async (req, res, next) => {
    try {
        const { id } = req.params;

        const thread = await Thread.findById(id);
        if (!thread) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Thread not found'
            });
        }

        // Only creator or admin can delete
        if (thread.created_by !== req.user.id && req.user.role !== 'Admin') {
            return res.status(403).json({
                error: 'Forbidden',
                message: 'You cannot delete this thread'
            });
        }

        await Thread.delete(id);

        res.json({
            message: 'Thread deleted successfully'
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   GET /api/threads/stats
 * @desc    Get thread stats for current user
 * @access  All authenticated users
 */
const getThreadStats = async (req, res, next) => {
    try {
        const stats = await Thread.getCountForUser(req.user.id);

        res.json({ stats });
    } catch (error) {
        next(error);
    }
};

module.exports = {
    getThreads,
    getThreadById,
    createThread,
    addMessage,
    updateThreadStatus,
    updateThread,
    deleteThread,
    getThreadStats
};
