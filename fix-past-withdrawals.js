// Fix past approved withdrawals - updates credit entries that weren't updated during approval
require('dotenv').config();
const { Pool } = require('pg');

const pool = new Pool({
    host: process.env.DB_HOST,
    port: process.env.DB_PORT || 5432,
    database: process.env.DB_NAME,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD
});

async function fixPastWithdrawals() {
    const client = await pool.connect();
    try {
        console.log('🔍 Finding approved withdrawals from today that need fixing...');

        // First, let's see how many credit entries need to be updated
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

        console.log(`📊 Found ${checkResult.rows[0].count} credit entries that need updating`);

        if (checkResult.rows[0].count > 0) {
            // Update the credit entries
            const updateResult = await client.query(`
                UPDATE wallet_histories 
                SET approved_date = CURRENT_TIMESTAMP, 
                    remarks = COALESCE(remarks, '') || ' [Retroactive Fix]',
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

            console.log(`✅ Updated ${updateResult.rowCount} credit entries`);
        }

        // Also check for any credits that should have been updated for artdailynews
        const artdailyCheck = await client.query(`
            SELECT u.email, COUNT(*) as count, SUM(wh.price) as total
            FROM wallet_histories wh
            JOIN wallets w ON wh.wallet_id = w.id
            JOIN users u ON w.user_id = u.id
            WHERE wh.type = 'credit'
              AND wh.approved_date IS NULL
            GROUP BY u.id, u.email
            HAVING u.email IN ('artdailynews@yahoo.com', 'onlymailchecker@gmail.com')
        `);

        console.log('\n📊 Remaining unapproved credits for test users:');
        if (artdailyCheck.rows.length === 0) {
            console.log('   All credits have been properly marked as approved! ✅');
        } else {
            artdailyCheck.rows.forEach(row => {
                console.log(`   ${row.email}: ${row.count} entries, total $${row.total}`);
            });
        }

        console.log('\n✅ Fix script completed!');
    } catch (error) {
        console.error('❌ Error:', error.message);
        throw error;
    } finally {
        client.release();
        await pool.end();
    }
}

fixPastWithdrawals().catch(console.error);
