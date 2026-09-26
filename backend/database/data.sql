USE crm_db;

-- CRM-21 / CRM-26 fixtures; rerunning preserves existing roles.
INSERT INTO roles (name) VALUES ('Admin'), ('Sales Rep'), ('Accountant')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- No accounts seeded: no real credentials or plaintext passwords stored.
-- Create local users using PasswordUtil.hashPassword, then assign user_roles.
