-- CRM-35: User Profile Email Signature
USE crm_db;

ALTER TABLE users ADD COLUMN IF NOT EXISTS signature TEXT NULL AFTER phone;
