require('dotenv').config();
const fs = require('fs');
const path = require('path');
const { pool } = require('../config/database');

/**
 * Simple migration runner
 * Executes SQL files in the migrations directory
 */
async function runMigrations() {
    try {
        console.log('🔧 Starting database migration...\n');

        const migrationFile = path.join(__dirname, '001_create_tables.sql');
        const sql = fs.readFileSync(migrationFile, 'utf8');

        console.log(`Executing: ${path.basename(migrationFile)}`);
        await pool.query(sql);

        console.log('✅ Migration completed successfully!\n');
        console.log('📊 Database schema created with:');
        console.log('   - Users table (with Admin role)');
        console.log('   - Websites table');
        console.log('   - Tasks table (workflow core)');
        console.log('   - Transactions table');
        console.log('   - System Config table\n');
        console.log('🔐 Default admin user created:');
        console.log('   Email: admin@workflow.com');
        console.log('   Password: admin123');
        console.log('   ⚠️  CHANGE THIS PASSWORD IN PRODUCTION!\n');

        process.exit(0);
    } catch (error) {
        console.error('❌ Migration failed:', error);
        process.exit(1);
    }
}

runMigrations();
