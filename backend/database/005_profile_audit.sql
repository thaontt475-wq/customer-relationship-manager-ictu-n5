USE crm_db;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'crm_db'
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'phone'
);

SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE users ADD COLUMN phone VARCHAR(30) NULL',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'crm_db'
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'avatar_url'
);

SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500) NULL',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'crm_db'
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'avatar_thumbnail_url'
);

SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE users ADD COLUMN avatar_thumbnail_url VARCHAR(500) NULL',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NULL,
    action VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
);


INSERT IGNORE INTO permissions(code, name)
VALUES
('audit.read', 'Xem audit log');

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON p.code = 'audit.read'
WHERE r.code = 'ADMIN';