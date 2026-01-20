// Fix ALL approved withdrawals for ALL bloggers
// This updates credit entries that weren't updated during approval
require('dotenv').config();
const { Pool } = require('pg');

const pool = new Pool({
    host: process.env.DB_HOST,
    port: process.env.DB_PORT || 5432,
    database: process.env.DB_NAME,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD
});

async function fixAllWithdrawals() {
    const client = await pool.connect();
    try {
        console.log('🔍 Finding ALL approved withdrawals that need fixing...\n');

        // First, let's see how many credit entries need to be updated across ALL approved withdrawals
        const checkResult = await client.query(`
            SELECT COUNT(*) as count
            FROM wallet_histories wh
            WHERE wh.type = 'credit'
              AND wh.approved_date IS NULL
              AND wh.order_detail_id IN (
                  SELECT wh_debit.order_detail_id 
                  FROM wallet_histories wh_debit
                  JOIN withdraw_requests wr ON wh_debit.withdraw_request_id = wr.id
                  WHERE wr.status = 1  -- Approved withdrawals only
                    AND wh_debit.order_detail_id IS NOT NULL
              )
        `);

        console.log(`📊 Found ${checkResult.rows[0].count} credit entries that need updating\n`);

        if (parseInt(checkResult.rows[0].count) > 0) {
            // Update ALL credit entries for ALL approved withdrawals
            const updateResult = await client.query(`
                UPDATE wallet_histories 
                SET approved_date = CURRENT_TIMESTAMP, 
                    remarks = COALESCE(remarks, '') || ' [Fixed]',
                    updated_at = CURRENT_TIMESTAMP
                WHERE type = 'credit'
                  AND approved_date IS NULL
                  AND order_detail_id IN (
                      SELECT wh_debit.order_detail_id 
                      FROM wallet_histories wh_debit
                      JOIN withdraw_requests wr ON wh_debit.withdraw_request_id = wr.id
                      WHERE wr.status = 1  -- Approved withdrawals only
                        AND wh_debit.order_detail_id IS NOT NULL
                  )
            `);

            console.log(`✅ Updated ${updateResult.rowCount} credit entries for ALL bloggers\n`);
        }

        // Show summary of remaining unapproved credits per blogger (top 10)
        const summaryResult = await client.query(`
            SELECT 
                u.name, 
                u.email, 
                COUNT(*) as pending_credits,
                ROUND(CAST(SUM(wh.price) AS NUMERIC), 2) as wallet_balance
            FROM wallet_histories wh
            JOIN wallets w ON wh.wallet_id = w.id
            JOIN users u ON w.user_id = u.id
            WHERE wh.type = 'credit'
              AND wh.approved_date IS NULL
            GROUP BY u.id, u.name, u.email
            ORDER BY wallet_balance DESC
            LIMIT 10
        `);

        console.log('📊 Top 10 bloggers with wallet balances (pending credits):');
        console.log('━'.repeat(70));
        if (summaryResult.rows.length === 0) {
            console.log('   No bloggers with pending credits found');
        } else {
            summaryResult.rows.forEach((row, i) => {
                console.log(`   ${i + 1}. ${row.name.substring(0, 20).padEnd(20)} | ${row.email.substring(0, 30).padEnd(30)} | $${row.wallet_balance}`);
            });
        }

        // Count total bloggers with wallet balance
        const totalResult = await client.query(`
            SELECT COUNT(DISTINCT w.user_id) as total_bloggers
            FROM wallet_histories wh
            JOIN wallets w ON wh.wallet_id = w.id
            WHERE wh.type = 'credit'
              AND wh.approved_date IS NULL
        `);

        console.log(`\n📊 Total bloggers with wallet balance: ${totalResult.rows[0].total_bloggers}`);
        console.log('\n✅ Fix script completed for ALL bloggers!');
    } catch (error) {
        console.error('❌ Error:', error.message);
        throw error;
    } finally {
        client.release();
        await pool.end();
    }
}

fixAllWithdrawals().catch(console.error);
