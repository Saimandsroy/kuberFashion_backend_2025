/**
 * Migration Runner Script
 * Run: node run-migration.js
 */
require('dotenv').config();
const { Pool } = require('pg');

// Create direct connection
const pool = new Pool({
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 5432,
    database: process.env.DB_NAME || 'workflow_management',
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
});

const migrationSQL = `
CREATE TABLE IF NOT EXISTS price_charts (
    id SERIAL PRIMARY KEY,
    rd_min INTEGER NOT NULL DEFAULT 0,
    rd_max INTEGER NOT NULL DEFAULT 0,
    traffic_min INTEGER NOT NULL DEFAULT 0,
    traffic_max INTEGER NOT NULL DEFAULT 0,
    dr_min INTEGER NOT NULL DEFAULT 0,
    dr_max INTEGER NOT NULL DEFAULT 0,
    da_min INTEGER NOT NULL DEFAULT 0,
    da_max INTEGER NOT NULL DEFAULT 0,
    niche_price_min DECIMAL(10, 2) NOT NULL DEFAULT 0,
    niche_price_max DECIMAL(10, 2) NOT NULL DEFAULT 0,
    gp_price_min DECIMAL(10, 2) NOT NULL DEFAULT 0,
    gp_price_max DECIMAL(10, 2) NOT NULL DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
`;

async function runMigration() {
    try {
        console.log('🔧 Running price_charts migration...');

        // Create table
        await pool.query(migrationSQL);
        console.log('✅ Table created successfully');

        // Check if data exists
        const checkResult = await pool.query('SELECT COUNT(*) FROM price_charts');
        if (parseInt(checkResult.rows[0].count) === 0) {
            // Insert sample data
            await pool.query(`
                INSERT INTO price_charts (rd_min, rd_max, traffic_min, traffic_max, dr_min, dr_max, da_min, da_max, niche_price_min, niche_price_max, gp_price_min, gp_price_max)
                VALUES 
                    (0, 100, 0, 500, 10, 20, 10, 20, 5, 10, 5, 10),
                    (100, 200, 500, 1000, 20, 30, 20, 30, 5, 12, 10, 20),
                    (200, 400, 1000, 2000, 30, 40, 30, 40, 5, 15, 10, 25),
                    (400, 1000, 2000, 5000, 40, 50, 40, 50, 10, 20, 10, 30),
                    (1000, 100000, 5000, 500000, 50, 100, 50, 100, 10, 30, 15, 40)
            `);
            console.log('✅ Sample data inserted');
        } else {
            console.log('ℹ️  Sample data already exists, skipping');
        }

        // Verify
        const result = await pool.query('SELECT COUNT(*) FROM price_charts');
        console.log(`📊 Total price charts: ${result.rows[0].count}`);

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
