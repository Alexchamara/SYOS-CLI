-- Add full_name column to users table for WEB customers
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);

