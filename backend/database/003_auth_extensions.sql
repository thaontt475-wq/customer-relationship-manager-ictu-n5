USE crm_db;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'crm_db'
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'session_version'
);

SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE users ADD COLUMN session_version INT NOT NULL DEFAULT 0',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_password_reset_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'crm_db'
      AND TABLE_NAME = 'password_reset_tokens'
      AND INDEX_NAME = 'idx_password_reset_user'
);

SET @sql = IF(
    @index_exists = 0,
    'CREATE INDEX idx_password_reset_user ON password_reset_tokens(user_id)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'crm_db'
      AND TABLE_NAME = 'password_reset_tokens'
      AND INDEX_NAME = 'idx_password_reset_expires'
);

SET @sql = IF(
    @index_exists = 0,
    'CREATE INDEX idx_password_reset_expires ON password_reset_tokens(expires_at)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;