-- Migration 002: Add missing fields and tables based on production gap analysis
-- PostgreSQL 12+

-- =====================================================
-- ALTER USERS TABLE - Add missing fields
-- =====================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS login_count INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS payment_details JSONB;

-- =====================================================
-- ALTER WEBSITES TABLE - Add missing production fields
-- =====================================================
ALTER TABLE websites ADD COLUMN IF NOT EXISTS dr INTEGER CHECK (dr >= 0 AND dr <= 100);
ALTER TABLE websites ADD COLUMN IF NOT EXISTS da INTEGER CHECK (da >= 0 AND da <= 100);
ALTER TABLE websites ADD COLUMN IF NOT EXISTS traffic INTEGER DEFAULT 0;
ALTER TABLE websites ADD COLUMN IF NOT EXISTS rd INTEGER DEFAULT 0;
ALTER TABLE websites ADD COLUMN IF NOT EXISTS niche_price DECIMAL(10, 2) DEFAULT 0.00;
ALTER TABLE websites ADD COLUMN IF NOT EXISTS gp_price DECIMAL(10, 2) DEFAULT 0.00;
ALTER TABLE websites ADD COLUMN IF NOT EXISTS blogger_id INTEGER REFERENCES users(id) ON DELETE SET NULL;

-- Rename da_pa_score to da if it exists (for backward compatibility)
-- Note: Run this only if da_pa_score column exists and da doesn't
-- ALTER TABLE websites RENAME COLUMN da_pa_score TO da;

-- =====================================================
-- ALTER TASKS TABLE - Add missing production fields
-- =====================================================
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS manager_id INTEGER REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS client_name VARCHAR(255);
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS order_type VARCHAR(100) DEFAULT 'Guest Post';
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS no_of_links INTEGER DEFAULT 1;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS tat_deadline TIMESTAMP;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS niche_price DECIMAL(10, 2) DEFAULT 0.00;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS gp_price DECIMAL(10, 2) DEFAULT 0.00;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS assigned_team_id INTEGER REFERENCES users(id) ON DELETE SET NULL;

-- Add index for manager_id
CREATE INDEX IF NOT EXISTS idx_tasks_manager ON tasks(manager_id);
CREATE INDEX IF NOT EXISTS idx_tasks_assigned_team ON tasks(assigned_team_id);

-- =====================================================
-- ALTER TRANSACTIONS TABLE - Add missing production fields
-- =====================================================
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS payment_method VARCHAR(100);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS invoice_no VARCHAR(100);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS bank_details JSONB;

-- =====================================================
-- CREATE THREADS TABLE - For communication/tickets system
-- =====================================================
CREATE TABLE IF NOT EXISTS threads (
  id SERIAL PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  subject VARCHAR(255),
  priority VARCHAR(20) DEFAULT 'Medium' CHECK (priority IN ('Low', 'Medium', 'High', 'Critical')),
  status VARCHAR(50) DEFAULT 'Open' CHECK (status IN ('Open', 'In Progress', 'Resolved', 'Closed')),
  created_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
  assigned_to INTEGER REFERENCES users(id) ON DELETE SET NULL,
  task_id INTEGER REFERENCES tasks(id) ON DELETE SET NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_threads_created_by ON threads(created_by);
CREATE INDEX IF NOT EXISTS idx_threads_assigned_to ON threads(assigned_to);
CREATE INDEX IF NOT EXISTS idx_threads_status ON threads(status);
CREATE INDEX IF NOT EXISTS idx_threads_task ON threads(task_id);

-- =====================================================
-- CREATE THREAD MESSAGES TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS thread_messages (
  id SERIAL PRIMARY KEY,
  thread_id INTEGER REFERENCES threads(id) ON DELETE CASCADE,
  user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
  message TEXT NOT NULL,
  attachments JSONB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_thread_messages_thread ON thread_messages(thread_id);
CREATE INDEX IF NOT EXISTS idx_thread_messages_user ON thread_messages(user_id);

-- =====================================================
-- CREATE USER PERMISSIONS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS user_permissions (
  id SERIAL PRIMARY KEY,
  user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
  permission_key VARCHAR(100) NOT NULL,
  permission_value BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, permission_key)
);

CREATE INDEX IF NOT EXISTS idx_user_permissions_user ON user_permissions(user_id);

-- =====================================================
-- CREATE NOTIFICATIONS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS notifications (
  id SERIAL PRIMARY KEY,
  user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
  title VARCHAR(255) NOT NULL,
  message TEXT,
  type VARCHAR(50),
  is_read BOOLEAN DEFAULT false,
  action_url VARCHAR(500),
  related_task_id INTEGER REFERENCES tasks(id) ON DELETE SET NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(is_read);

-- =====================================================
-- CREATE PRICE CHARTS TABLE (if not exists)
-- =====================================================
CREATE TABLE IF NOT EXISTS price_charts (
  id SERIAL PRIMARY KEY,
  rd_min INTEGER DEFAULT 0,
  rd_max INTEGER DEFAULT 0,
  traffic_min INTEGER DEFAULT 0,
  traffic_max INTEGER DEFAULT 0,
  dr_min INTEGER DEFAULT 0,
  dr_max INTEGER DEFAULT 0,
  da_min INTEGER DEFAULT 0,
  da_max INTEGER DEFAULT 0,
  niche_price_min DECIMAL(10, 2) DEFAULT 0.00,
  niche_price_max DECIMAL(10, 2) DEFAULT 0.00,
  gp_price_min DECIMAL(10, 2) DEFAULT 0.00,
  gp_price_max DECIMAL(10, 2) DEFAULT 0.00,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- ADD TRIGGERS FOR NEW TABLES
-- =====================================================
CREATE TRIGGER update_threads_updated_at BEFORE UPDATE ON threads
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_price_charts_updated_at BEFORE UPDATE ON price_charts
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- UPDATE task_status ENUM to include new statuses if needed
-- =====================================================
-- Note: PostgreSQL doesn't easily allow adding values to ENUM
-- If needed, you can run this to add new statuses:
-- ALTER TYPE task_status ADD VALUE IF NOT EXISTS 'PENDING_TEAM_SELECTION';
-- ALTER TYPE task_status ADD VALUE IF NOT EXISTS 'TEAM_SUBMITTED';

COMMENT ON TABLE threads IS 'Communication threads/tickets between users';
COMMENT ON TABLE thread_messages IS 'Messages within threads';
COMMENT ON TABLE user_permissions IS 'Granular user permissions';
COMMENT ON TABLE notifications IS 'User notifications';
COMMENT ON TABLE price_charts IS 'Pricing configuration based on website metrics';
