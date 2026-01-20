-- Migration 004: Create task_selected_websites table for multi-website support
-- This enables a task to have multiple websites with specific details (url, anchor, title)

CREATE TABLE IF NOT EXISTS task_selected_websites (
    id SERIAL PRIMARY KEY,
    task_id INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    website_id INTEGER NOT NULL REFERENCES websites(id),
    
    -- Fields filled by Manager before assigning to writer
    target_url TEXT,              -- The specific URL where the content will be published
    anchor_text TEXT,             -- The anchor text for the backlink
    article_title TEXT,           -- The title of the article to be written
    upfront_payment BOOLEAN DEFAULT false,  -- Whether upfront payment is required
    
    -- Additional metadata
    notes TEXT,                   -- Specific notes for this website assignment
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure we don't duplicate website assignments for the same task
    UNIQUE(task_id, website_id)
);

-- Create index for faster lookups
CREATE INDEX idx_task_selected_websites_task_id ON task_selected_websites(task_id);
CREATE INDEX idx_task_selected_websites_website_id ON task_selected_websites(website_id);

-- Add trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_task_selected_websites_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_task_selected_websites_updated_at
    BEFORE UPDATE ON task_selected_websites
    FOR EACH ROW
    EXECUTE FUNCTION update_task_selected_websites_updated_at();
