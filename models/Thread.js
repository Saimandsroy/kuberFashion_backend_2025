const { query } = require('../config/database');

/**
 * Thread Model
 * Handles communication threads/tickets between users
 */
class Thread {
    /**
     * Get all threads with filters
     */
    static async findAll(filters = {}) {
        let sql = `
            SELECT 
                t.*,
                u_created.username as created_by_name,
                u_assigned.username as assigned_to_name,
                (SELECT COUNT(*) FROM thread_messages WHERE thread_id = t.id) as message_count
            FROM threads t
            LEFT JOIN users u_created ON t.created_by = u_created.id
            LEFT JOIN users u_assigned ON t.assigned_to = u_assigned.id
            WHERE 1=1
        `;

        const params = [];
        let paramIndex = 1;

        if (filters.created_by) {
            sql += ` AND t.created_by = $${paramIndex}`;
            params.push(filters.created_by);
            paramIndex++;
        }

        if (filters.assigned_to) {
            sql += ` AND t.assigned_to = $${paramIndex}`;
            params.push(filters.assigned_to);
            paramIndex++;
        }

        if (filters.status) {
            sql += ` AND t.status = $${paramIndex}`;
            params.push(filters.status);
            paramIndex++;
        }

        if (filters.task_id) {
            sql += ` AND t.task_id = $${paramIndex}`;
            params.push(filters.task_id);
            paramIndex++;
        }

        // Filter for user's threads (either created or assigned)
        if (filters.user_id) {
            sql += ` AND (t.created_by = $${paramIndex} OR t.assigned_to = $${paramIndex})`;
            params.push(filters.user_id);
            paramIndex++;
        }

        sql += ' ORDER BY t.updated_at DESC';

        const result = await query(sql, params);
        return result.rows;
    }

    /**
     * Find thread by ID with messages
     */
    static async findById(id) {
        const threadResult = await query(
            `SELECT 
                t.*,
                u_created.username as created_by_name,
                u_assigned.username as assigned_to_name
            FROM threads t
            LEFT JOIN users u_created ON t.created_by = u_created.id
            LEFT JOIN users u_assigned ON t.assigned_to = u_assigned.id
            WHERE t.id = $1`,
            [id]
        );

        if (!threadResult.rows[0]) {
            return null;
        }

        // Get messages for this thread
        const messagesResult = await query(
            `SELECT 
                m.*,
                u.username as user_name
            FROM thread_messages m
            LEFT JOIN users u ON m.user_id = u.id
            WHERE m.thread_id = $1
            ORDER BY m.created_at ASC`,
            [id]
        );

        return {
            ...threadResult.rows[0],
            messages: messagesResult.rows
        };
    }

    /**
     * Create new thread
     */
    static async create(threadData) {
        const { title, subject, priority, created_by, assigned_to, task_id } = threadData;

        const result = await query(
            `INSERT INTO threads (title, subject, priority, created_by, assigned_to, task_id) 
             VALUES ($1, $2, $3, $4, $5, $6) 
             RETURNING *`,
            [title, subject || null, priority || 'Medium', created_by, assigned_to || null, task_id || null]
        );

        return result.rows[0];
    }

    /**
     * Add message to thread
     */
    static async addMessage(threadId, userId, message, attachments = null) {
        // Add the message
        const messageResult = await query(
            `INSERT INTO thread_messages (thread_id, user_id, message, attachments) 
             VALUES ($1, $2, $3, $4) 
             RETURNING *`,
            [threadId, userId, message, attachments ? JSON.stringify(attachments) : null]
        );

        // Update thread's updated_at
        await query(
            'UPDATE threads SET updated_at = CURRENT_TIMESTAMP WHERE id = $1',
            [threadId]
        );

        return messageResult.rows[0];
    }

    /**
     * Update thread status
     */
    static async updateStatus(id, status) {
        const result = await query(
            `UPDATE threads 
             SET status = $1, updated_at = CURRENT_TIMESTAMP 
             WHERE id = $2 
             RETURNING *`,
            [status, id]
        );
        return result.rows[0];
    }

    /**
     * Update thread
     */
    static async update(id, updates) {
        const allowedFields = ['title', 'subject', 'priority', 'status', 'assigned_to'];
        const setClause = [];
        const params = [];
        let paramIndex = 1;

        for (const [key, value] of Object.entries(updates)) {
            if (allowedFields.includes(key)) {
                setClause.push(`${key} = $${paramIndex}`);
                params.push(value);
                paramIndex++;
            }
        }

        if (setClause.length === 0) {
            throw new Error('No valid fields to update');
        }

        params.push(id);

        const result = await query(
            `UPDATE threads SET ${setClause.join(', ')}, updated_at = CURRENT_TIMESTAMP 
             WHERE id = $${paramIndex} 
             RETURNING *`,
            params
        );

        return result.rows[0];
    }

    /**
     * Delete thread
     */
    static async delete(id) {
        const result = await query(
            'DELETE FROM threads WHERE id = $1 RETURNING id',
            [id]
        );
        return result.rows[0];
    }

    /**
     * Get thread count for user
     */
    static async getCountForUser(userId) {
        const result = await query(
            `SELECT 
                COUNT(*) FILTER (WHERE status = 'Open') as open_count,
                COUNT(*) FILTER (WHERE status = 'In Progress') as in_progress_count,
                COUNT(*) FILTER (WHERE status = 'Resolved') as resolved_count,
                COUNT(*) as total_count
            FROM threads 
            WHERE created_by = $1 OR assigned_to = $1`,
            [userId]
        );
        return result.rows[0];
    }
}

module.exports = Thread;
