-- S4-07 Lead SLA Response and Rejection Management
-- Compatible with S4-02 and S4-06 schemas

CREATE TABLE IF NOT EXISTS leads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(150) NOT NULL,
    company_name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    phone VARCHAR(50) NULL,
    title VARCHAR(100) NULL DEFAULT 'Khách hàng tiềm năng',
    source_id BIGINT NULL,
    source VARCHAR(50) NULL DEFAULT 'WEB_FORM',
    region VARCHAR(100) NULL,
    region_code VARCHAR(50) NULL,
    industry VARCHAR(100) NULL,
    employee_count INT NULL DEFAULT 0,
    score INT NULL DEFAULT 0,
    notes VARCHAR(2000) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_RESPONSE',
    owner_user_id BIGINT NULL,
    assigned_to VARCHAR(50) NULL,
    assigned_to_user_id BIGINT NULL,
    assigned_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    sla_total_minutes INT NOT NULL DEFAULT 120,
    sla_deadline DATETIME NULL,
    accepted_at DATETIME NULL,
    rejected_at DATETIME NULL,
    reject_reason VARCHAR(50) NULL,
    reject_details TEXT NULL,
    sla_result VARCHAR(20) NULL,
    response_time_text VARCHAR(255) NULL,
    contacting_stage VARCHAR(255) NULL,
    unassigned_reason VARCHAR(255) NULL,
    is_locked TINYINT(1) NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_leads_assigned (assigned_to_user_id, status),
    INDEX idx_leads_status (status),
    INDEX idx_leads_owner (owner_user_id),
    INDEX idx_leads_phone (phone),
    INDEX idx_leads_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- If table leads already existed from base, ensure SLA columns exist
SET @db = DATABASE();

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'assigned_to_user_id');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN assigned_to_user_id BIGINT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'assigned_at');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN assigned_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'sla_total_minutes');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN sla_total_minutes INT NOT NULL DEFAULT 120', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'sla_deadline');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN sla_deadline DATETIME NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'accepted_at');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN accepted_at DATETIME NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'rejected_at');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN rejected_at DATETIME NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'reject_reason');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN reject_reason VARCHAR(50) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'reject_details');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN reject_details TEXT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'sla_result');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN sla_result VARCHAR(20) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'response_time_text');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN response_time_text VARCHAR(255) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'contacting_stage');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN contacting_stage VARCHAR(255) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'employee_count');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN employee_count INT NULL DEFAULT 0', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'score');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN score INT NULL DEFAULT 0', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'is_locked');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN is_locked TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'assigned_to');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN assigned_to VARCHAR(50) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'title');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN title VARCHAR(100) NULL DEFAULT ''Khách hàng tiềm năng''', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'region');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN region VARCHAR(100) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'region_code');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN region_code VARCHAR(50) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'industry');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN industry VARCHAR(100) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'source');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN source VARCHAR(50) NULL DEFAULT ''WEB_FORM''', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'unassigned_reason');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN unassigned_reason VARCHAR(255) NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'owner_user_id');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN owner_user_id BIGINT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'created_by');
SET @sql = IF(@col = 0, 'ALTER TABLE leads ADD COLUMN created_by BIGINT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS lead_assignment_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lead_id BIGINT NULL,
    lead_name VARCHAR(150) NOT NULL,
    lead_phone VARCHAR(50) NULL,
    company_name VARCHAR(255) NULL,
    rep_id VARCHAR(50) NOT NULL,
    rep_name VARCHAR(150) NOT NULL,
    rep_dept VARCHAR(150) NULL,
    method VARCHAR(255) NOT NULL,
    action_type VARCHAR(50) NOT NULL DEFAULT 'ASSIGN',
    note TEXT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_lah_lead (lead_id),
    INDEX idx_lah_assigned_at (assigned_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'lead_assignment_history' AND COLUMN_NAME = 'action_type');
SET @sql = IF(@col = 0, 'ALTER TABLE lead_assignment_history ADD COLUMN action_type VARCHAR(50) NOT NULL DEFAULT "ASSIGN"', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Permissions for S4-07 Lead SLA
INSERT IGNORE INTO permissions(code, name) VALUES
('lead.read', 'Xem lead'),
('lead.accept', 'Tiếp nhận lead được phân'),
('lead.reject', 'Từ chối lead được phân'),
('lead.sla_monitor', 'Giám sát cam kết SLA lead'),
('lead.manage', 'Quản lý Lead');

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'DIRECTOR', 'MANAGER', 'TEAM_LEAD', 'SALES_REP') AND p.code LIKE 'lead.%';

INSERT IGNORE INTO role_module_scopes(role_id, module_code, scope_type)
SELECT r.id, 'lead', CASE WHEN r.code IN ('ADMIN', 'DIRECTOR') THEN 'ALL' WHEN r.code = 'TEAM_LEAD' THEN 'TEAM' ELSE 'SELF' END
FROM roles r WHERE r.code IN ('ADMIN', 'DIRECTOR', 'TEAM_LEAD', 'SALES_REP', 'MARKETING');

-- Seed SLA Demonstration Leads if table is empty
SET @sales_rep_id = (SELECT id FROM users WHERE email = 'qa.s4.sales_rep@example.invalid' LIMIT 1);
SET @sales_rep_id = IFNULL(@sales_rep_id, 3);

INSERT INTO leads (
    id, full_name, company_name, phone, email, title, region, industry, employee_count, score,
    status, owner_user_id, assigned_to, assigned_to_user_id, assigned_at, sla_total_minutes, sla_deadline,
    accepted_at, rejected_at, reject_reason, reject_details, sla_result, response_time_text, contacting_stage,
    is_locked, created_by, created_at
)
SELECT 1, 'Trần Anh Quân', 'Tập đoàn Xây dựng Delta Vina', '0912888999', 'quan.ta@deltavina.com', 'Tổng Giám đốc (CEO)',
       'Miền Bắc (Hà Nội)', 'Bất động sản & Xây dựng', 220, 88, 'PENDING_RESPONSE', @sales_rep_id, CONCAT('USR-', @sales_rep_id), @sales_rep_id,
       DATE_SUB(NOW(), INTERVAL 25 MINUTE), 120, DATE_ADD(DATE_SUB(NOW(), INTERVAL 25 MINUTE), INTERVAL 120 MINUTE),
       NULL, NULL, NULL, NULL, 'ON_TIME', NULL, NULL, 0, 1, DATE_SUB(NOW(), INTERVAL 30 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM leads WHERE id = 1);

INSERT INTO leads (
    id, full_name, company_name, phone, email, title, region, industry, employee_count, score,
    status, owner_user_id, assigned_to, assigned_to_user_id, assigned_at, sla_total_minutes, sla_deadline,
    accepted_at, rejected_at, reject_reason, reject_details, sla_result, response_time_text, contacting_stage,
    is_locked, created_by, created_at
)
SELECT 2, 'Hoàng Mai Linh', 'Chuỗi Cửa hàng Trà Sữa TocoToco', '0987333444', 'linh.hm@tocotoco.vn', 'Trưởng phòng Thu mua & Cung ứng',
       'Miền Bắc (Hà Nội)', 'Bán lẻ & Chuỗi F&B', 95, 74, 'PENDING_RESPONSE', @sales_rep_id, CONCAT('USR-', @sales_rep_id), @sales_rep_id,
       DATE_SUB(NOW(), INTERVAL 102 MINUTE), 120, DATE_ADD(DATE_SUB(NOW(), INTERVAL 102 MINUTE), INTERVAL 120 MINUTE),
       NULL, NULL, NULL, NULL, 'ON_TIME', NULL, NULL, 0, 1, DATE_SUB(NOW(), INTERVAL 110 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM leads WHERE id = 2);

INSERT INTO leads (
    id, full_name, company_name, phone, email, title, region, industry, employee_count, score,
    status, owner_user_id, assigned_to, assigned_to_user_id, assigned_at, sla_total_minutes, sla_deadline,
    accepted_at, rejected_at, reject_reason, reject_details, sla_result, response_time_text, contacting_stage,
    is_locked, created_by, created_at
)
SELECT 3, 'Đặng Quốc Việt', 'Công ty Cổ phần VinaSoftware Global', '0903555666', 'viet.dq@vinasoftware.com', 'Giám đốc Công nghệ (CTO)',
       'Miền Nam (TP.HCM)', 'CNTT & Viễn thông', 160, 92, 'PENDING_RESPONSE', @sales_rep_id, CONCAT('USR-', @sales_rep_id), @sales_rep_id,
       DATE_SUB(NOW(), INTERVAL 145 MINUTE), 120, DATE_ADD(DATE_SUB(NOW(), INTERVAL 145 MINUTE), INTERVAL 120 MINUTE),
       NULL, NULL, NULL, NULL, 'OVERDUE', NULL, NULL, 0, 1, DATE_SUB(NOW(), INTERVAL 150 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM leads WHERE id = 3);

INSERT INTO leads (
    id, full_name, company_name, phone, email, title, region, industry, employee_count, score,
    status, owner_user_id, assigned_to, assigned_to_user_id, assigned_at, sla_total_minutes, sla_deadline,
    accepted_at, rejected_at, reject_reason, reject_details, sla_result, response_time_text, contacting_stage,
    is_locked, created_by, created_at
)
SELECT 4, 'Lê Bích Ngọc', 'Tập đoàn Dược phẩm Hoa Linh Pharma', '0915667788', 'ngoc.lb@duochoalinh.vn', 'Phó Giám đốc Kỹ thuật',
       'Miền Bắc (Hải Phòng)', 'Dược phẩm & Y tế', 130, 82, 'CONTACTING', @sales_rep_id, CONCAT('USR-', @sales_rep_id), @sales_rep_id,
       DATE_SUB(NOW(), INTERVAL 180 MINUTE), 120, DATE_ADD(DATE_SUB(NOW(), INTERVAL 180 MINUTE), INTERVAL 120 MINUTE),
       DATE_SUB(NOW(), INTERVAL 162 MINUTE), NULL, NULL, NULL, 'ON_TIME', 'Tiếp nhận đúng hạn (sau 18 phút)', 'Đã gọi điện thoại lần 1 - Khách hẹn gửi hồ sơ giải pháp', 0, 1, DATE_SUB(NOW(), INTERVAL 190 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM leads WHERE id = 4);

INSERT INTO leads (
    id, full_name, company_name, phone, email, title, region, industry, employee_count, score,
    status, owner_user_id, assigned_to, assigned_to_user_id, assigned_at, sla_total_minutes, sla_deadline,
    accepted_at, rejected_at, reject_reason, reject_details, sla_result, response_time_text, contacting_stage,
    is_locked, created_by, created_at
)
SELECT 5, 'Vũ Đình Trọng', 'Logistics Á Châu Toàn Cầu', '0944112233', 'trong.vd@achaucorp.com', 'Trưởng phòng Tiếp vận',
       'Miền Nam (Bình Dương)', 'Sản xuất & Vận tải', 85, 68, 'CONTACTING', @sales_rep_id, CONCAT('USR-', @sales_rep_id), @sales_rep_id,
       DATE_SUB(NOW(), INTERVAL 300 MINUTE), 120, DATE_ADD(DATE_SUB(NOW(), INTERVAL 300 MINUTE), INTERVAL 120 MINUTE),
       DATE_SUB(NOW(), INTERVAL 168 MINUTE), NULL, NULL, NULL, 'OVERDUE', 'Tiếp nhận trễ hạn (sau 132 phút - trễ 12 phút)', 'Đang xếp lịch họp Demo trực tuyến qua MS Teams', 0, 1, DATE_SUB(NOW(), INTERVAL 310 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM leads WHERE id = 5);

INSERT INTO leads (
    id, full_name, company_name, phone, email, title, region, industry, employee_count, score,
    status, owner_user_id, assigned_to, assigned_to_user_id, assigned_at, sla_total_minutes, sla_deadline,
    accepted_at, rejected_at, reject_reason, reject_details, sla_result, response_time_text, contacting_stage,
    is_locked, created_by, created_at
)
SELECT 6, 'Nguyễn Thanh Sơn', 'Chuỗi Bách Hóa Miền Trung', '0977001122', 'son.nt@bachhoamientrung.vn', 'Cửa hàng trưởng',
       'Miền Trung (Đà Nẵng)', 'Bán lẻ', 20, 45, 'REJECTED', @sales_rep_id, NULL, NULL,
       DATE_SUB(NOW(), INTERVAL 360 MINUTE), 120, DATE_ADD(DATE_SUB(NOW(), INTERVAL 360 MINUTE), INTERVAL 120 MINUTE),
       NULL, DATE_SUB(NOW(), INTERVAL 348 MINUTE), 'INVALID_PHONE', 'Đã gọi 3 cuộc chuông đổ không người nhấc máy, kiểm tra lại số Zalo không tồn tại.', NULL, NULL, NULL, 0, 1, DATE_SUB(NOW(), INTERVAL 370 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM leads WHERE id = 6);

-- Safe creation and expansion of notifications table for Lead SLA alerts (preserves existing Customer notifications)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    customer_id BIGINT NULL,
    lead_id BIGINT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    event_key VARCHAR(150) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME NULL,
    UNIQUE KEY uq_s3_08_notification_event(event_key),
    KEY ix_s3_08_notification_recipient(user_id, read_at, id),
    KEY ix_s3_08_notification_customer(customer_id),
    KEY ix_notifications_lead(lead_id),
    FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @has_notif = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications');
SET @sql = IF(@has_notif > 0, 'ALTER TABLE notifications MODIFY COLUMN customer_id BIGINT NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_lead_col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'lead_id');
SET @sql = IF(@has_notif > 0 AND @has_lead_col = 0, 'ALTER TABLE notifications ADD COLUMN lead_id BIGINT NULL, ADD INDEX ix_notifications_lead (lead_id)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_fk_lead = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = @db AND TABLE_NAME = 'notifications' AND CONSTRAINT_NAME = 'fk_notifications_lead');
SET @sql = IF(@has_notif > 0 AND @has_fk_lead = 0, 'ALTER TABLE notifications ADD CONSTRAINT fk_notifications_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE SET NULL', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Ensure QA SALES_REP team connects to QA TEAM_LEAD if both exist
UPDATE teams SET parent_id = (SELECT id FROM (SELECT id FROM teams WHERE name = 'QA TEAM_LEAD' LIMIT 1) t2),
                 leader_id = (SELECT id FROM (SELECT id FROM users WHERE email = 'qa.s4.team_lead@example.invalid' LIMIT 1) u2)
WHERE name = 'QA SALES_REP' AND (parent_id IS NULL OR leader_id IS NULL);

