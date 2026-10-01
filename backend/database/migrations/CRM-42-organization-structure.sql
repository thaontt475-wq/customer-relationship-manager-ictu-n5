-- CRM-42: extend the existing teams table with organization hierarchy metadata.
-- This migration is intentionally one-shot for databases created before CRM-42.

USE crm_db;

ALTER TABLE teams
    ADD COLUMN parent_id BIGINT NULL,
    ADD COLUMN leader_user_id BIGINT NULL,
    ADD COLUMN region VARCHAR(20) NOT NULL DEFAULT 'NATIONAL',
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD INDEX idx_teams_parent_id (parent_id),
    ADD UNIQUE INDEX uq_teams_leader_user_id (leader_user_id),
    ADD INDEX idx_teams_region (region),
    ADD INDEX idx_teams_active (active);

ALTER TABLE teams
    ADD CONSTRAINT fk_teams_parent
        FOREIGN KEY (parent_id) REFERENCES teams(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_teams_leader
        FOREIGN KEY (leader_user_id) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_teams_region
        CHECK (region IN ('NORTH', 'CENTRAL', 'SOUTH', 'NATIONAL', 'OVERSEAS'));
