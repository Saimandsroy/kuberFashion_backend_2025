-- Workflow Management Database Schema
-- PostgreSQL 12+

-- Create ENUM types
CREATE TYPE user_role AS ENUM ('Admin', 'Manager', 'Team', 'Writer', 'Blogger');

CREATE TYPE website_status AS ENUM ('Active', 'Inactive');

CREATE TYPE task_status AS ENUM (
  'DRAFT',
  'PENDING_MANAGER_APPROVAL_1',
  'ASSIGNED_TO_WRITER',
  'WRITING_IN_PROGRESS',
  'PENDING_MANAGER_APPROVAL_2',
  'ASSIGNED_TO_BLOGGER',
  'PUBLISHED_PENDING_VERIFICATION',
  'PENDING_FINAL_CHECK',
  'COMPLETED',
  'REJECTED',
  'CREDITED'
);

CREATE TYPE transaction_status AS ENUM ('Requested', 'Processing', 'Paid', 'Rejected');

-- =====================================================
-- Users Table
-- =====================================================
CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  username VARCHAR(100) UNIQUE NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role user_role NOT NULL,
  wallet_balance DECIMAL(10, 2) DEFAULT 0.00,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_email ON users(email);

-- =====================================================
-- Websites/Inventory Table
-- =====================================================
CREATE TABLE websites (
  id SERIAL PRIMARY KEY,
  domain_url VARCHAR(500) UNIQUE NOT NULL,
  category VARCHAR(100),
  da_pa_score INTEGER CHECK (da_pa_score >= 0 AND da_pa_score <= 100),
  status website_status DEFAULT 'Active',
  added_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_websites_status ON websites(status);
CREATE INDEX idx_websites_category ON websites(category);

-- =====================================================
-- Tasks/Posts Table (Core Entity)
-- =====================================================
CREATE TABLE tasks (
  id SERIAL PRIMARY KEY,
  website_id INTEGER REFERENCES websites(id) ON DELETE SET NULL,
  suggested_topic_url TEXT,
  content_body TEXT,
  content_instructions TEXT,
  live_published_url TEXT,
  
  -- User assignments
  created_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
  assigned_writer_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
  assigned_blogger_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
  
  -- Workflow status
  current_status task_status DEFAULT 'DRAFT',
  
  -- Metadata
  notes TEXT,
  rejection_reason TEXT,
  payment_amount DECIMAL(10, 2) DEFAULT 0.00,
  
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tasks_status ON tasks(current_status);
CREATE INDEX idx_tasks_created_by ON tasks(created_by);
CREATE INDEX idx_tasks_assigned_writer ON tasks(assigned_writer_id);
CREATE INDEX idx_tasks_assigned_blogger ON tasks(assigned_blogger_id);

-- =====================================================
-- Transactions/Withdrawals Table
-- =====================================================
CREATE TABLE transactions (
  id SERIAL PRIMARY KEY,
  user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
  amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
  status transaction_status DEFAULT 'Requested',
  request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  processed_date TIMESTAMP,
  processed_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
  notes TEXT,
  rejection_reason TEXT
);

CREATE INDEX idx_transactions_user ON transactions(user_id);
CREATE INDEX idx_transactions_status ON transactions(status);

-- =====================================================
-- System Config Table
-- =====================================================
CREATE TABLE system_config (
  id SERIAL PRIMARY KEY,
  config_key VARCHAR(100) UNIQUE NOT NULL,
  config_value TEXT NOT NULL,
  description TEXT,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default config
INSERT INTO system_config (config_key, config_value, description) VALUES
  ('default_post_payment', '50.00', 'Default payment amount per completed post'),
  ('default_currency', 'USD', 'Currency for all transactions'),
  ('min_withdrawal_amount', '100.00', 'Minimum amount for withdrawal request');

-- =====================================================
-- Triggers for updated_at
-- =====================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_websites_updated_at BEFORE UPDATE ON websites
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tasks_updated_at BEFORE UPDATE ON tasks
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_system_config_updated_at BEFORE UPDATE ON system_config
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- Create default admin user (password: admin123)
-- Hash generated with bcrypt for 'admin123'
-- =====================================================
INSERT INTO users (username, email, password_hash, role, wallet_balance) VALUES
  ('admin', 'admin@workflow.com', '$2a$10$/vCDlS4j8jWU3VpQscKk9OqeT612ddbR.IZd2FE1j8zGbpxzdj6Am', 'Admin', 0.00);

COMMENT ON TABLE users IS 'Stores all system users with role-based access';
COMMENT ON TABLE websites IS 'Inventory of websites available for posting';
COMMENT ON TABLE tasks IS 'Core workflow entity tracking the entire lifecycle';
COMMENT ON TABLE transactions IS 'Blogger withdrawal requests and payment history';
COMMENT ON TABLE system_config IS 'System-wide configuration settings';
