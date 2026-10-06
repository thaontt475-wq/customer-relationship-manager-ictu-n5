USE crm_db;

-- Keep legacy role IDs and their relationships until both links are copied.
CREATE TABLE IF NOT EXISTS role_module_scopes (
    role_id BIGINT NOT NULL,
    module_code VARCHAR(40) NOT NULL,
    scope_type VARCHAR(10) NOT NULL,
    PRIMARY KEY (role_id, module_code),
    CONSTRAINT fk_role_module_scopes_role FOREIGN KEY (role_id)
        REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT chk_role_module_scope CHECK (scope_type IN ('SELF', 'TEAM', 'ALL'))
);

SET @has_parent = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'teams' AND COLUMN_NAME = 'parent_id'
);
SET @sql = IF(@has_parent = 0,
    'ALTER TABLE teams ADD COLUMN parent_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_fk = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'teams'
      AND CONSTRAINT_NAME = 'fk_teams_parent'
);
SET @sql = IF(@has_fk = 0,
    'ALTER TABLE teams ADD CONSTRAINT fk_teams_parent FOREIGN KEY (parent_id) REFERENCES teams(id) ON DELETE SET NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO roles(code, name) VALUES
('ADMIN', 'Quản trị hệ thống'),
('DIRECTOR', 'Giám đốc kinh doanh'),
('TEAM_LEAD', 'Trưởng nhóm kinh doanh'),
('SALES_REP', 'Nhân viên kinh doanh'),
('MARKETING', 'Nhân viên Marketing'),
('CUSTOMER_SUCCESS', 'Chăm sóc khách hàng'),
('ACCOUNTANT', 'Kế toán');

START TRANSACTION;
INSERT IGNORE INTO user_roles(user_id, role_id)
SELECT ur.user_id, target.id FROM user_roles ur
JOIN roles legacy ON legacy.id = ur.role_id
JOIN roles target ON target.code = CASE legacy.code
    WHEN 'SALE' THEN 'SALES_REP' WHEN 'MANAGER' THEN 'TEAM_LEAD' END
WHERE legacy.code IN ('SALE', 'MANAGER');

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT target.id, rp.permission_id FROM role_permissions rp
JOIN roles legacy ON legacy.id = rp.role_id
JOIN roles target ON target.code = CASE legacy.code
    WHEN 'SALE' THEN 'SALES_REP' WHEN 'MANAGER' THEN 'TEAM_LEAD' END
WHERE legacy.code IN ('SALE', 'MANAGER');

DELETE ur FROM user_roles ur JOIN roles r ON r.id = ur.role_id
WHERE r.code IN ('SALE', 'MANAGER');
DELETE rp FROM role_permissions rp JOIN roles r ON r.id = rp.role_id
WHERE r.code IN ('SALE', 'MANAGER');
COMMIT;

-- This only removes legacy rows after all user and permission links were copied.
DELETE r FROM roles r WHERE r.code IN ('SALE', 'MANAGER')
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.role_id = r.id)
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id);

INSERT IGNORE INTO permissions(code, name) VALUES
('customer.read', 'Xem khách hàng'), ('customer.create', 'Tạo khách hàng'),
('customer.update', 'Sửa khách hàng'), ('customer.delete', 'Xóa khách hàng'),
('customer.export', 'Xuất khách hàng'),
('contact.read', 'Xem liên hệ'), ('contact.create', 'Tạo liên hệ'),
('contact.update', 'Sửa liên hệ'),
('lead.read', 'Xem lead'), ('lead.create', 'Tạo lead'),
('lead.update', 'Sửa lead'), ('lead.assign', 'Phân bổ lead'),
('opportunity.read', 'Xem cơ hội'), ('opportunity.create', 'Tạo cơ hội'),
('opportunity.update', 'Sửa cơ hội'), ('opportunity.close', 'Đóng cơ hội'),
('opportunity.export', 'Xuất cơ hội'),
('activity.read', 'Xem hoạt động'), ('activity.create', 'Tạo hoạt động'),
('activity.update', 'Sửa hoạt động'), ('activity.export', 'Xuất hoạt động'),
('task.read', 'Xem công việc'), ('task.create', 'Tạo công việc'),
('task.update', 'Sửa công việc'),
('quote.read', 'Xem báo giá'), ('quote.create', 'Tạo báo giá'),
('quote.update', 'Sửa báo giá'), ('quote.approve', 'Duyệt báo giá'),
('quote.export', 'Xuất báo giá'),
('contract.read', 'Xem hợp đồng'), ('contract.create', 'Tạo hợp đồng'),
('contract.update', 'Sửa hợp đồng'),
('kpi.read', 'Xem KPI'), ('kpi.manage', 'Quản lý KPI'),
('report.read', 'Xem báo cáo'), ('dashboard.read', 'Xem dashboard'),
('automation.read', 'Xem tự động hóa'), ('automation.manage', 'Quản lý tự động hóa'),
('notification.read', 'Xem thông báo'),
('role.read', 'Xem vai trò'), ('role.manage', 'Quản lý vai trò'),
('team.read', 'Xem nhóm'), ('team.assign', 'Gán nhóm'),
('product.cost.read', 'Xem giá vốn sản phẩm'),
('product.cost.update', 'Cập nhật giá vốn sản phẩm');

-- Existing permission.code is UNIQUE; INSERT IGNORE keeps its canonical ID.
INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DIRECTOR' AND (
    p.code REGEXP '^(customer|contact|lead|opportunity|activity|task|quote|contract|product|pricebook|pipeline|organization|masterdata|customfield|winloss|competitor|kpi|report|dashboard|automation|notification)\\.'
    OR p.code IN ('user.read', 'audit.read', 'team.read', 'role.read')
);

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'TEAM_LEAD' AND (
    p.code REGEXP '^(customer|contact|lead|opportunity|activity|task)\\.'
    OR p.code IN ('quote.read','quote.create','quote.update','quote.export',
        'contract.read','kpi.read','kpi.manage','report.read','dashboard.read',
        'automation.read','notification.read','team.read',
        'product.read','pricebook.read','pipeline.read','organization.read',
        'masterdata.read','customfield.read','winloss.read','competitor.read')
);

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SALES_REP' AND (
    p.code REGEXP '^(customer|contact|opportunity|activity|task)\\.'
    OR p.code IN ('lead.read','lead.create','lead.update',
        'quote.read','quote.create','quote.update','quote.export','contract.read',
        'kpi.read','report.read','dashboard.read','automation.read','notification.read',
        'product.read','pricebook.read','pipeline.read','organization.read',
        'masterdata.read','customfield.read','winloss.read','competitor.read')
);

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'MARKETING' AND p.code IN (
    'customer.read','customer.create','customer.update','customer.export',
    'contact.read','contact.create','contact.update',
    'lead.read','lead.create','lead.update','lead.assign',
    'opportunity.read','activity.read','activity.create','activity.update',
    'activity.export','report.read','dashboard.read','automation.read',
    'automation.manage','notification.read','product.read','pricebook.read',
    'pipeline.read','organization.read','masterdata.read','customfield.read',
    'winloss.read','competitor.read'
);

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'CUSTOMER_SUCCESS' AND p.code IN (
    'customer.read','customer.create','customer.update','customer.export',
    'contact.read','contact.create','contact.update','opportunity.read',
    'activity.read','activity.create','activity.update','activity.export',
    'task.read','task.create','task.update','quote.read','contract.read',
    'report.read','dashboard.read','automation.read','notification.read',
    'product.read','pricebook.read','pipeline.read','organization.read',
    'masterdata.read','customfield.read','winloss.read','competitor.read'
);

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ACCOUNTANT' AND p.code IN (
    'customer.read','opportunity.read','quote.read','quote.create','quote.update',
    'quote.export','contract.read','contract.create','contract.update',
    'kpi.read','report.read','dashboard.read','product.read','pricebook.read',
    'pipeline.read','organization.read','masterdata.read','customfield.read',
    'winloss.read','competitor.read'
);

-- A role's scope is module-specific; users.data_scope remains only for old clients.
INSERT INTO role_module_scopes(role_id, module_code, scope_type)
SELECT r.id, m.module_code,
    CASE r.code WHEN 'ADMIN' THEN 'ALL' WHEN 'DIRECTOR' THEN 'ALL'
        WHEN 'TEAM_LEAD' THEN 'TEAM' ELSE 'SELF' END
FROM roles r CROSS JOIN (
    SELECT 'customer' AS module_code UNION ALL SELECT 'opportunity'
    UNION ALL SELECT 'activity' UNION ALL SELECT 'quote'
) m
WHERE r.code IN ('ADMIN','DIRECTOR','TEAM_LEAD','SALES_REP',
    'MARKETING','CUSTOMER_SUCCESS','ACCOUNTANT')
ON DUPLICATE KEY UPDATE scope_type = VALUES(scope_type);
