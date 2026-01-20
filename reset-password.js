/**
 * Reset password for saimands LM writer account
 */
require('dotenv').config();
const { Pool } = require('pg');
const bcrypt = require('bcryptjs');

const pool = new Pool({
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 5432,
    database: process.env.DB_NAME || 'workflow_management',
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
});

async function resetPassword() {
    try {
        console.log('🔧 Resetting password for saimands LM...\n');

        // Get user details first
        const userResult = await pool.query(`
            SELECT id, username, email, role FROM users WHERE id = 8
        `);

        if (userResult.rows.length === 0) {
            console.log('❌ User not found!');
            await pool.end();
            return;
        }

        const user = userResult.rows[0];
        console.log(`Found user: ${user.username} (${user.email}) - Role: ${user.role}`);

        // Hash the new password
        const newPassword = 'password123';
        const hashedPassword = await bcrypt.hash(newPassword, 10);

        // Update password
        await pool.query(`
            UPDATE users SET password_hash = $1 WHERE id = $2
        `, [hashedPassword, 8]);

        console.log(`✅ Password reset successfully!`);
        console.log(`\n📝 Login credentials:`);
        console.log(`   Email: ${user.email}`);
        console.log(`   Password: ${newPassword}`);

        await pool.end();
    } catch (error) {
        console.error('❌ Failed:', error.message);
        await pool.end();
    }
}

resetPassword();
