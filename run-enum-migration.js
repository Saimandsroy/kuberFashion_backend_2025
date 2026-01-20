/**
 * Run specific migration to add SUBMITTED_TO_MANAGER status
 */
require('dotenv').config();
const { Pool } = require('pg');
const fs = require('fs');
const path = require('path');

const pool = new Pool({
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 5432,
    database: process.env.DB_NAME || 'workflow_management',
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
});

async function runMigration() {
    try {
        console.log('🔧 Adding SUBMITTED_TO_MANAGER status to task_status enum...');

        // Check if status already exists
        const checkResult = await pool.query(`
            SELECT 1 FROM pg_enum 
            WHERE enumlabel = 'SUBMITTED_TO_MANAGER' 
            AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'task_status')
        `);

        if (checkResult.rows.length > 0) {
            console.log('ℹ️  SUBMITTED_TO_MANAGER status already exists, skipping...');
        } else {
            // Add the enum value
            await pool.query(`ALTER TYPE task_status ADD VALUE 'SUBMITTED_TO_MANAGER' AFTER 'WRITING_IN_PROGRESS'`);
            console.log('✅ SUBMITTED_TO_MANAGER status added successfully');
        }

        // Verify enum values
        const enumResult = await pool.query(`
            SELECT enumlabel FROM pg_enum 
            WHERE enumtypid = (SELECT oid FROM pg_type WHERE typname = 'task_status')
            ORDER BY enumsortorder
        `);
        console.log('📊 Current task_status enum values:');
        enumResult.rows.forEach(row => console.log(`   - ${row.enumlabel}`));

        console.log('🎉 Migration completed successfully!');
        await pool.end();
        process.exit(0);
    } catch (error) {
        console.error('❌ Migration failed:', error.message);
        await pool.end();
        process.exit(1);
    }
}

runMigration();
