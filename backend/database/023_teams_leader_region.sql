USE crm_db;

-- 1. Bổ sung cột leader_id vào bảng teams nếu chưa có
SET @has_leader = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teams' AND COLUMN_NAME = 'leader_id'
);
SET @sql = IF(@has_leader = 0,
    'ALTER TABLE teams ADD COLUMN leader_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Khóa ngoại leader_id tham chiếu users(id)
SET @has_fk_leader = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'teams'
      AND CONSTRAINT_NAME = 'fk_teams_leader'
);
SET @sql = IF(@has_fk_leader = 0,
    'ALTER TABLE teams ADD CONSTRAINT fk_teams_leader FOREIGN KEY (leader_id) REFERENCES users(id) ON DELETE SET NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Bổ sung cột region_id vào bảng teams nếu chưa có
SET @has_region = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teams' AND COLUMN_NAME = 'region_id'
);
SET @sql = IF(@has_region = 0,
    'ALTER TABLE teams ADD COLUMN region_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Khóa ngoại region_id tham chiếu master_data(id)
SET @has_fk_region = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'teams'
      AND CONSTRAINT_NAME = 'fk_teams_region'
);
SET @sql = IF(@has_fk_region = 0,
    'ALTER TABLE teams ADD CONSTRAINT fk_teams_region FOREIGN KEY (region_id) REFERENCES master_data(id) ON DELETE SET NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Seed danh mục Khu vực địa lý vào master_data (type = 'region')
INSERT IGNORE INTO master_data (type, code, name, active, display_order) VALUES
('region', 'MB', 'Miền Bắc', 1, 1),
('region', 'MT', 'Miền Trung', 1, 2),
('region', 'MN', 'Miền Nam', 1, 3),
('region', 'TN', 'Tây Nguyên', 1, 4),
('region', 'MDB', 'Miền Đông Nam Bộ', 1, 5),
('region', 'MTW', 'Đồng bằng Sông Cửu Long (Miền Tây)', 1, 6);

-- 4. Bổ sung quyền quản lý nhóm kinh doanh
INSERT IGNORE INTO permissions(code, name) VALUES
('team.manage', 'Quản lý nhóm kinh doanh');

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code = 'team.manage';

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DIRECTOR' AND p.code = 'team.manage';

-- 5. Cập nhật gán khu vực mặc định cho các nhóm hiện có
UPDATE teams t
JOIN master_data md ON md.type = 'region' AND md.code = 'MB'
SET t.region_id = md.id
WHERE t.name LIKE '%Bắc%' AND t.region_id IS NULL;

UPDATE teams t
JOIN master_data md ON md.type = 'region' AND md.code = 'MT'
SET t.region_id = md.id
WHERE t.name LIKE '%Trung%' AND t.region_id IS NULL;

UPDATE teams t
JOIN master_data md ON md.type = 'region' AND md.code = 'MN'
SET t.region_id = md.id
WHERE t.name LIKE '%Nam%' AND t.region_id IS NULL;
