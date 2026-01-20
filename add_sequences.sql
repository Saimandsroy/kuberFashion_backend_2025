-- Add sequences for auto-incrementing IDs
-- Run this AFTER importing pg_schema.sql and pg_data.sql

DO $$
DECLARE
    tbl RECORD;
    max_id BIGINT;
    seq_name TEXT;
BEGIN
    FOR tbl IN 
        SELECT table_name 
        FROM information_schema.columns 
        WHERE column_name = 'id' 
        AND table_schema = 'public'
        AND data_type IN ('bigint', 'integer')
    LOOP
        seq_name := tbl.table_name || '_id_seq';
        
        -- Get the max ID from the table
        EXECUTE format('SELECT COALESCE(MAX(id), 0) FROM %I', tbl.table_name) INTO max_id;
        
        -- Create sequence if not exists
        EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %I START WITH %s', seq_name, max_id + 1);
        
        -- Set default to use the sequence
        EXECUTE format('ALTER TABLE %I ALTER COLUMN id SET DEFAULT nextval(''%I'')', tbl.table_name, seq_name);
        
        RAISE NOTICE 'Created sequence % starting at % for table %', seq_name, max_id + 1, tbl.table_name;
    END LOOP;
END $$;

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO PUBLIC;
