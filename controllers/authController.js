const User = require('../models/User');
const jwt = require('jsonwebtoken');
const logger = require('../utils/logger');

/**
 * Authentication Controller
 * Handles login, logout, and token management
 */

/**
 * @route   POST /api/auth/login
 * @desc    Login user and return JWT token
 * @access  Public
 */
const login = async (req, res, next) => {
    try {
        const { email, password } = req.body;

        logger.info('Auth', `Login attempt for email: ${email}`);

        // Validation
        if (!email || !password) {
            logger.warn('Auth', 'Login failed - missing email or password');
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Email and password are required'
            });
        }

        // Find user by email
        logger.db('SELECT', 'users', { email });
        const user = await User.findByEmail(email);

        if (!user) {
            logger.auth('Login', email, false);
            return res.status(401).json({
                error: 'Authentication Failed',
                message: 'Invalid email or password'
            });
        }

        // Check if user is active
        if (!user.is_active) {
            logger.warn('Auth', `Login failed - account disabled for: ${email}`);
            return res.status(403).json({
                error: 'Account Disabled',
                message: 'Your account has been disabled. Please contact admin.'
            });
        }

        // Verify password
        const isPasswordValid = await User.verifyPassword(password, user.password_hash);

        if (!isPasswordValid) {
            logger.auth('Login', email, false);
            return res.status(401).json({
                error: 'Authentication Failed',
                message: 'Invalid email or password'
            });
        }

        // Generate JWT token
        const token = jwt.sign(
            {
                id: user.id,
                email: user.email,
                role: user.role
            },
            process.env.JWT_SECRET,
            {
                expiresIn: process.env.JWT_EXPIRES_IN || '24h'
            }
        );

        logger.auth('Login', email, true);
        logger.success('Auth', { userId: user.id, role: user.role });

        // Return user data and token
        res.json({
            message: 'Login successful',
            token,
            user: {
                id: user.id,
                username: user.username,
                email: user.email,
                role: user.role,
                wallet_balance: user.wallet_balance
            }
        });

    } catch (error) {
        logger.error('Auth:Login', error, { email: req.body?.email });
        next(error);
    }
};

/**
 * @route   POST /api/auth/logout
 * @desc    Logout user (client-side token removal)
 * @access  Private
 */
const logout = async (req, res) => {
    // Since we're using JWT, logout is handled client-side by removing the token
    // This endpoint is just a placeholder for potential future enhancements
    res.json({
        message: 'Logout successful',
        note: 'Please remove the token from client storage'
    });
};

/**
 * @route   GET /api/auth/me
 * @desc    Get current user profile
 * @access  Private
 */
const getCurrentUser = async (req, res, next) => {
    try {
        const user = await User.findById(req.user.id);

        if (!user) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'User not found'
            });
        }

        res.json({
            user: {
                id: user.id,
                username: user.username,
                email: user.email,
                role: user.role,
                wallet_balance: user.wallet_balance,
                is_active: user.is_active,
                created_at: user.created_at
            }
        });

    } catch (error) {
        next(error);
    }
};

/**
 * @route   POST /api/auth/register
 * @desc    Register new blogger user
 * @access  Public
 */
const register = async (req, res, next) => {
    try {
        const { name, email, password, confirm_password, whatsapp, skype } = req.body;

        logger.info('Auth', `Registration attempt for email: ${email}`);

        // Validation
        if (!name || name.trim().length < 2) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Name is required and must be at least 2 characters'
            });
        }

        if (!email || !email.includes('@')) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Valid email address is required'
            });
        }

        if (!password || password.length < 8) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Password must be at least 8 characters'
            });
        }

        if (password !== confirm_password) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Passwords do not match'
            });
        }

        if (!whatsapp || whatsapp.trim().length === 0) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'WhatsApp number is required'
            });
        }

        if (!skype || skype.trim().length === 0) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Skype ID is required'
            });
        }

        // Check if email already exists
        const existingUser = await User.findByEmail(email);
        if (existingUser) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Email already registered'
            });
        }

        // Hash password
        const bcrypt = require('bcrypt');
        const hashedPassword = await bcrypt.hash(password, 10);

        // Import database query
        const { query } = require('../config/database');

        // Create new user with role 'vendor' (blogger)
        const result = await query(
            `INSERT INTO users (name, email, password, whatsapp, skype, role, status, created_at, updated_at)
             VALUES ($1, $2, $3, $4, $5, 'vendor', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
             RETURNING id, name, email, role, created_at`,
            [name.trim(), email.toLowerCase().trim(), hashedPassword, whatsapp.trim(), skype.trim()]
        );

        const newUser = result.rows[0];

        // Create wallet for new user
        try {
            await query(
                `INSERT INTO wallets (user_id, balance, created_at, updated_at)
                 VALUES ($1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)`,
                [newUser.id]
            );
        } catch (walletError) {
            // Wallet might already exist, ignore this error
            logger.warn('Auth', `Wallet creation skipped for user ${newUser.id}: ${walletError.message}`);
        }

        logger.success('Auth', { message: 'User registered successfully', userId: newUser.id });

        res.status(201).json({
            message: 'Registration successful! You can now sign in.',
            user: {
                id: newUser.id,
                name: newUser.name,
                email: newUser.email,
                role: newUser.role
            }
        });

    } catch (error) {
        logger.error('Auth:Register', error, { email: req.body?.email });
        next(error);
    }
};

module.exports = {
    login,
    logout,
    getCurrentUser,
    register
};
