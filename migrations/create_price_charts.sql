-- Price Charts Table Migration
-- Run this SQL to create the price_charts table in your PostgreSQL database

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

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_price_charts_active ON price_charts(is_active);
CREATE INDEX IF NOT EXISTS idx_price_charts_rd ON price_charts(rd_min, rd_max);

-- Insert sample data (optional - matches the existing frontend mock data)
INSERT INTO price_charts (rd_min, rd_max, traffic_min, traffic_max, dr_min, dr_max, da_min, da_max, niche_price_min, niche_price_max, gp_price_min, gp_price_max)
VALUES 
    (0, 100, 0, 500, 10, 20, 10, 20, 5, 10, 5, 10),
    (100, 200, 500, 1000, 20, 30, 20, 30, 5, 12, 10, 20),
    (200, 400, 1000, 2000, 30, 40, 30, 40, 5, 15, 10, 25),
    (400, 1000, 2000, 5000, 40, 50, 40, 50, 10, 20, 10, 30),
    (1000, 100000, 5000, 500000, 50, 100, 50, 100, 10, 30, 15, 40);
