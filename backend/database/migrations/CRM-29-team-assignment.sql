-- Run ONCE on a database created before CRM-29.
-- Fresh databases use schema.sql instead; do not run both.
USE crm_db;

CREATE TABLE IF NOT EXISTS teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE users
    ADD COLUMN team_id BIGINT NULL AFTER status,
    ADD INDEX idx_users_team_id (team_id),
    ADD CONSTRAINT fk_users_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL;
