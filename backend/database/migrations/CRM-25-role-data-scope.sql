-- Run ONCE on a database created before CRM-25.
-- Fresh databases use schema.sql instead; do not run both.
USE crm_db;
ALTER TABLE users
    ADD COLUMN data_scope VARCHAR(10) NOT NULL DEFAULT 'SELF' AFTER status;
