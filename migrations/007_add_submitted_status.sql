-- Migration 007: Add missing SUBMITTED_TO_MANAGER status to task_status ENUM
-- This status is required for the writer submission workflow

-- Add the missing SUBMITTED_TO_MANAGER status if it doesn't exist
-- PostgreSQL allows adding values to ENUM types
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'SUBMITTED_TO_MANAGER' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'task_status')
    ) THEN
        ALTER TYPE task_status ADD VALUE 'SUBMITTED_TO_MANAGER' AFTER 'WRITING_IN_PROGRESS';
    END IF;
END$$;
