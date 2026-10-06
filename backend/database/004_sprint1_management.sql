USE crm_db;

CREATE TABLE IF NOT EXISTS teams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'crm_db'
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'data_scope'
);

SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE users ADD COLUMN data_scope VARCHAR(10) NOT NULL DEFAULT ''SELF''',
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
      AND COLUMN_NAME = 'team_id'
);

SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE users ADD COLUMN team_id BIGINT NULL',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS account_lock_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NULL,
    performed_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id)
        REFERENCES users(id),

    FOREIGN KEY (performed_by)
        REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS data_transfer_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    performed_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (from_user_id)
        REFERENCES users(id),

    FOREIGN KEY (to_user_id)
        REFERENCES users(id),

    FOREIGN KEY (performed_by)
        REFERENCES users(id)
);

INSERT IGNORE INTO permissions(code, name)
VALUES
('permission.read', 'Xem phân quyền'),
('permission.manage', 'Quản lý phân quyền'),

('user.read', 'Xem người dùng'),
('user.create', 'Tạo người dùng'),
('user.update', 'Cập nhật người dùng'),
('user.delete', 'Xóa người dùng'),

('team.read', 'Xem nhóm kinh doanh'),
('team.assign', 'Gán nhóm kinh doanh'),

('user.lock', 'Khóa hoặc mở khóa người dùng'),
('user.transfer', 'Bàn giao dữ liệu người dùng');

INSERT IGNORE INTO role_permissions(
    role_id,
    permission_id
)
SELECT
    r.id,
    p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN';

INSERT IGNORE INTO teams(name, active)
VALUES
('Kinh doanh miền Bắc', TRUE),
('Kinh doanh miền Trung', TRUE),
('Kinh doanh miền Nam', TRUE);