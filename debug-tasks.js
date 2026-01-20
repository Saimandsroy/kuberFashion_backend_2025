/**
 * Debug script to check what the writer sees vs all writer's tasks
 */
require('dotenv').config();
const { Pool } = require('pg');

const pool = new Pool({
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 5432,
    database: process.env.DB_NAME || 'workflow_management',
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
});

async function debug() {
    try {
        console.log('🔍 Checking tasks assigned to writer1 (ID: 4)...\n');

        // Get all tasks for writer1
        const writer1Result = await pool.query(`
            SELECT t.id, t.current_status, t.client_name, t.assigned_writer_id
            FROM tasks t
            WHERE t.assigned_writer_id = 4
            ORDER BY t.id
        `);

        console.log('📋 All tasks for writer1 (ID: 4):');
        console.log('-'.repeat(80));
        writer1Result.rows.forEach(task => {
            const statusIcon = ['ASSIGNED_TO_WRITER', 'WRITING_IN_PROGRESS'].includes(task.current_status) ? '✅' : '❌';
            console.log(`   ${statusIcon} Task #${task.id} | Status: ${task.current_status} | Client: ${task.client_name || 'N/A'}`);
        });

        const activeCount = writer1Result.rows.filter(t =>
            ['ASSIGNED_TO_WRITER', 'WRITING_IN_PROGRESS'].includes(t.current_status)
        ).length;
        console.log(`\n   ✅ = Visible in Order Notifications, ❌ = Filtered out`);
        console.log(`   Total visible to writer1: ${activeCount}`);

        console.log('\n📋 Pending Manager Approval (Tasks manager needs to assign):');
        console.log('-'.repeat(80));
        const pendingResult = await pool.query(`
            SELECT t.id, t.current_status, t.client_name, t.assigned_team_id
            FROM tasks t
            WHERE t.current_status = 'PENDING_MANAGER_APPROVAL_1'
            ORDER BY t.id
        `);
        pendingResult.rows.forEach(task => {
            console.log(`   Task #${task.id} | Status: ${task.current_status} | Client: ${task.client_name || 'N/A'} | Team ID: ${task.assigned_team_id || 'None'}`);
        });

        console.log('\n📋 Recently assigned tasks (last 5):');
        console.log('-'.repeat(80));
        const recentResult = await pool.query(`
            SELECT t.id, t.current_status, t.client_name, t.assigned_writer_id, t.updated_at,
                u.username as writer_name
            FROM tasks t
            LEFT JOIN users u ON t.assigned_writer_id = u.id
            WHERE t.assigned_writer_id IS NOT NULL
            ORDER BY t.updated_at DESC
            LIMIT 5
        `);
        recentResult.rows.forEach(task => {
            console.log(`   Task #${task.id} | Status: ${task.current_status} | Writer: ${task.writer_name} (ID: ${task.assigned_writer_id}) | Updated: ${task.updated_at}`);
        });

        await pool.end();
    } catch (error) {
        console.error('❌ Debug failed:', error.message);
        await pool.end();
    }
}

debug();
