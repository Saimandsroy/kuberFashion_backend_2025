/**
 * Centralized error handling middleware
 */
const logger = require('../utils/logger');

const errorHandler = (err, req, res, next) => {
    // Log detailed error information
    logger.error(`${req.method} ${req.originalUrl}`, err, {
        userId: req.user?.id,
        userEmail: req.user?.email,
        userRole: req.user?.role,
        body: req.body,
        params: req.params,
        query: req.query
    });

    // Default error
    let statusCode = err.statusCode || 500;
    let message = err.message || 'Internal Server Error';

    // Handle specific error types
    if (err.name === 'ValidationError') {
        statusCode = 400;
        message = 'Validation Error';
    }

    if (err.name === 'UnauthorizedError' || err.name === 'JsonWebTokenError') {
        statusCode = 401;
        message = 'Unauthorized';
    }

    if (err.code === '23505') { // PostgreSQL unique violation
        statusCode = 409;
        message = 'Resource already exists';
    }

    if (err.code === '23503') { // PostgreSQL foreign key violation
        statusCode = 400;
        message = 'Invalid reference to related resource';
    }

    if (err.code === 'ECONNREFUSED') {
        statusCode = 503;
        message = 'Database connection refused. Is PostgreSQL running?';
    }

    res.status(statusCode).json({
        error: {
            message,
            ...(process.env.NODE_ENV === 'development' && {
                stack: err.stack,
                details: err
            })
        }
    });
};

module.exports = errorHandler;

