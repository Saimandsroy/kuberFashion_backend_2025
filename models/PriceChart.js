const { query } = require('../config/database');

/**
 * PriceChart Model
 * Handles pricing tier management for link building services
 * Uses production column names: rd_start_range, rd_end_range, etc.
 */
class PriceChart {
    /**
     * Create price_charts table if it doesn't exist (with correct production column names)
     */
    static async createTableIfNotExists() {
        await query(`
            CREATE TABLE IF NOT EXISTS price_charts (
                id SERIAL PRIMARY KEY,
                rd_start_range double precision NOT NULL DEFAULT 0,
                rd_end_range double precision NOT NULL DEFAULT 0,
                traffic_start_range double precision NOT NULL DEFAULT 0,
                traffic_end_range double precision NOT NULL DEFAULT 0,
                dr_start_range double precision NOT NULL DEFAULT 0,
                dr_end_range double precision NOT NULL DEFAULT 0,
                da_start_range double precision NOT NULL DEFAULT 0,
                da_end_range double precision NOT NULL DEFAULT 0,
                niche_start_range double precision NOT NULL DEFAULT 0,
                niche_end_range double precision NOT NULL DEFAULT 0,
                gp_start_range double precision NOT NULL DEFAULT 0,
                gp_end_range double precision NOT NULL DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        `);

        // Check if table is empty and seed default data from production screenshot
        const countResult = await query('SELECT COUNT(*) FROM price_charts');
        if (parseInt(countResult.rows[0].count) === 0) {
            const seedData = [
                { rd_start: 0, rd_end: 100, traffic_start: 0, traffic_end: 500, dr_start: 10, dr_end: 20, da_start: 10, da_end: 20, niche_start: 5, niche_end: 10, gp_start: 5, gp_end: 10 },
                { rd_start: 100, rd_end: 200, traffic_start: 500, traffic_end: 1000, dr_start: 20, dr_end: 30, da_start: 20, da_end: 30, niche_start: 5, niche_end: 12, gp_start: 10, gp_end: 20 },
                { rd_start: 200, rd_end: 400, traffic_start: 1000, traffic_end: 2000, dr_start: 30, dr_end: 40, da_start: 30, da_end: 40, niche_start: 5, niche_end: 15, gp_start: 10, gp_end: 25 },
                { rd_start: 400, rd_end: 1000, traffic_start: 2000, traffic_end: 5000, dr_start: 40, dr_end: 50, da_start: 40, da_end: 50, niche_start: 10, niche_end: 20, gp_start: 10, gp_end: 30 },
                { rd_start: 1000, rd_end: 100000, traffic_start: 5000, traffic_end: 500000, dr_start: 50, dr_end: 50000, da_start: 50, da_end: 50000, niche_start: 10, niche_end: 30, gp_start: 15, gp_end: 40 },
                { rd_start: 3, rd_end: 9, traffic_start: 9, traffic_end: 12, dr_start: 12, dr_end: 18, da_start: 18, da_end: 21, niche_start: 21, niche_end: 27, gp_start: 27, gp_end: 30 }
            ];

            for (const row of seedData) {
                await query(
                    `INSERT INTO price_charts (rd_start_range, rd_end_range, traffic_start_range, traffic_end_range, dr_start_range, dr_end_range, da_start_range, da_end_range, niche_start_range, niche_end_range, gp_start_range, gp_end_range)
                     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)`,
                    [row.rd_start, row.rd_end, row.traffic_start, row.traffic_end, row.dr_start, row.dr_end, row.da_start, row.da_end, row.niche_start, row.niche_end, row.gp_start, row.gp_end]
                );
            }
        }
    }

    /**
     * Get all price chart entries
     */
    static async findAll(filters = {}) {
        try {
            let sql = 'SELECT * FROM price_charts WHERE 1=1';
            const params = [];

            sql += ' ORDER BY rd_start_range ASC, traffic_start_range ASC';

            const result = await query(sql, params);
            return result.rows;
        } catch (error) {
            // Table doesn't exist - create it and seed data
            if (error.code === '42P01') {
                await this.createTableIfNotExists();
                return this.findAll(filters);
            }
            throw error;
        }
    }

    /**
     * Find price chart by ID
     */
    static async findById(id) {
        const result = await query(
            'SELECT * FROM price_charts WHERE id = $1',
            [id]
        );
        return result.rows[0];
    }

    /**
     * Create new price chart entry
     */
    static async create(data) {
        const {
            rd_start_range, rd_end_range, traffic_start_range, traffic_end_range,
            dr_start_range, dr_end_range, da_start_range, da_end_range,
            niche_start_range, niche_end_range,
            gp_start_range, gp_end_range
        } = data;

        const result = await query(
            `INSERT INTO price_charts 
             (rd_start_range, rd_end_range, traffic_start_range, traffic_end_range, dr_start_range, dr_end_range, da_start_range, da_end_range, 
              niche_start_range, niche_end_range, gp_start_range, gp_end_range)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
             RETURNING *`,
            [rd_start_range, rd_end_range, traffic_start_range, traffic_end_range, dr_start_range, dr_end_range, da_start_range, da_end_range,
                niche_start_range, niche_end_range, gp_start_range, gp_end_range]
        );

        return result.rows[0];
    }

    /**
     * Update price chart entry
     */
    static async update(id, updates) {
        const allowedFields = [
            'rd_start_range', 'rd_end_range', 'traffic_start_range', 'traffic_end_range',
            'dr_start_range', 'dr_end_range', 'da_start_range', 'da_end_range',
            'niche_start_range', 'niche_end_range',
            'gp_start_range', 'gp_end_range'
        ];

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
            `UPDATE price_charts 
             SET ${setClause.join(', ')}, updated_at = CURRENT_TIMESTAMP 
             WHERE id = $${paramIndex} 
             RETURNING *`,
            params
        );

        return result.rows[0];
    }

    /**
     * Delete price chart entry
     */
    static async delete(id) {
        const result = await query(
            'DELETE FROM price_charts WHERE id = $1 RETURNING id',
            [id]
        );
        return result.rows[0];
    }

    /**
     * Get price for given metrics
     */
    static async getPriceForMetrics(rd, traffic, dr, da) {
        const result = await query(
            `SELECT * FROM price_charts 
             WHERE rd_start_range <= $1 AND rd_end_range >= $1
             AND traffic_start_range <= $2 AND traffic_end_range >= $2
             AND dr_start_range <= $3 AND dr_end_range >= $3
             AND da_start_range <= $4 AND da_end_range >= $4
             LIMIT 1`,
            [rd, traffic, dr, da]
        );
        return result.rows[0];
    }
}

module.exports = PriceChart;
