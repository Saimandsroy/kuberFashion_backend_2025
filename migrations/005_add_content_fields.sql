-- Add content fields to task_selected_websites table
ALTER TABLE task_selected_websites 
ADD COLUMN content_link TEXT,
ADD COLUMN content_file TEXT;

-- Index for querying submissions if needed
CREATE INDEX idx_task_selected_websites_content_link ON task_selected_websites(content_link);
