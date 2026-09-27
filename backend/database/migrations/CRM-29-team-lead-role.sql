-- CRM-29 - ensure Team Lead role exists
USE crm_db;

INSERT INTO roles (name)
VALUES ('Team Lead')
ON DUPLICATE KEY UPDATE name = VALUES(name);