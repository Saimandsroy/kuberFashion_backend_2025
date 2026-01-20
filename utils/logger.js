/**
 * Logger Utility
 * Centralized logging for debugging API requests and errors
 */

const colors = {
    reset: '\x1b[0m',
    bright: '\x1b[1m',
    dim: '\x1b[2m',

    // Foreground colors
    red: '\x1b[31m',
    green: '\x1b[32m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m',
    magenta: '\x1b[35m',
    cyan: '\x1b[36m',
    white: '\x1b[37m',
};

const getTimestamp = () => {
    return new Date().toISOString().replace('T', ' ').substring(0, 23);
};

const logger = {
    /**
     * Log incoming request
     */
    request: (req, context = '') => {
        const { method, originalUrl, body, query, params, headers } = req;
        console.log(`${colors.cyan}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${colors.reset}`);
        console.log(`${colors.bright}${colors.blue}📥 INCOMING REQUEST${colors.reset} ${context ? `[${context}]` : ''}`);
        console.log(`${colors.cyan}⏰ Time:${colors.reset} ${getTimestamp()}`);
        console.log(`${colors.cyan}🔗 Method:${colors.reset} ${colors.yellow}${method}${colors.reset}`);
        console.log(`${colors.cyan}📍 URL:${colors.reset} ${originalUrl}`);
        console.log(`${colors.cyan}👤 User:${colors.reset} ${req.user?.email || 'Anonymous'} (ID: ${req.user?.id || 'N/A'}, Role: ${req.user?.role || 'N/A'})`);

        if (Object.keys(params || {}).length > 0) {
            console.log(`${colors.cyan}📎 Params:${colors.reset}`, params);
        }
        if (Object.keys(query || {}).length > 0) {
            console.log(`${colors.cyan}🔍 Query:${colors.reset}`, query);
        }
        if (body && Object.keys(body).length > 0) {
            // Sanitize sensitive data
            const sanitizedBody = { ...body };
            if (sanitizedBody.password) sanitizedBody.password = '***HIDDEN***';
            console.log(`${colors.cyan}📦 Body:${colors.reset}`, JSON.stringify(sanitizedBody, null, 2));
        }
        console.log(`${colors.cyan}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${colors.reset}`);
    },

    /**
     * Log successful response
     */
    success: (context, data = null, statusCode = 200) => {
        console.log(`${colors.green}✅ SUCCESS${colors.reset} [${context}] - Status: ${statusCode}`);
        if (data && process.env.LOG_DATA === 'true') {
            console.log(`${colors.dim}📤 Response Data:${colors.reset}`, JSON.stringify(data, null, 2));
        }
    },

    /**
     * Log error
     */
    error: (context, error, additionalInfo = {}) => {
        console.log(`${colors.red}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${colors.reset}`);
        console.log(`${colors.bright}${colors.red}❌ ERROR${colors.reset} [${context}]`);
        console.log(`${colors.red}⏰ Time:${colors.reset} ${getTimestamp()}`);
        console.log(`${colors.red}📛 Message:${colors.reset} ${error.message || error}`);

        if (error.code) {
            console.log(`${colors.red}🔢 Code:${colors.reset} ${error.code}`);
        }
        if (error.stack && process.env.NODE_ENV !== 'production') {
            console.log(`${colors.red}📚 Stack:${colors.reset}\n${error.stack}`);
        }
        if (Object.keys(additionalInfo).length > 0) {
            console.log(`${colors.red}ℹ️ Additional Info:${colors.reset}`, additionalInfo);
        }
        console.log(`${colors.red}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${colors.reset}`);
    },

    /**
     * Log warning
     */
    warn: (context, message, data = null) => {
        console.log(`${colors.yellow}⚠️ WARNING${colors.reset} [${context}] - ${message}`);
        if (data) {
            console.log(`${colors.dim}📋 Data:${colors.reset}`, data);
        }
    },

    /**
     * Log info
     */
    info: (context, message, data = null) => {
        console.log(`${colors.blue}ℹ️ INFO${colors.reset} [${context}] - ${message}`);
        if (data) {
            console.log(`${colors.dim}📋 Data:${colors.reset}`, data);
        }
    },

    /**
     * Log database operation
     */
    db: (operation, table, details = null) => {
        console.log(`${colors.magenta}🗃️ DB${colors.reset} [${operation}] - Table: ${table}`);
        if (details) {
            console.log(`${colors.dim}📋 Details:${colors.reset}`, details);
        }
    },

    /**
     * Log authentication event
     */
    auth: (event, email, success = true) => {
        const icon = success ? '🔓' : '🔒';
        const color = success ? colors.green : colors.red;
        console.log(`${color}${icon} AUTH${colors.reset} [${event}] - Email: ${email} - ${success ? 'Success' : 'Failed'}`);
    },
};

module.exports = logger;
