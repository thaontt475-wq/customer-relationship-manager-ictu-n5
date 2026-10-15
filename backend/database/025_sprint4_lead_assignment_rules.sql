USE crm_db;

-- 1. Bổ sung các cột phục vụ phân bổ Lead vào bảng leads nếu chưa có
CREATE TABLE IF NOT EXISTS leads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(150) NOT NULL,
    company_name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    phone VARCHAR(50) NULL,
    source VARCHAR(50) NULL DEFAULT 'WEB_FORM',
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    notes TEXT NULL,
    form_id VARCHAR(100) NULL,
    ip_address VARCHAR(50) NULL,
    assigned_to BIGINT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_leads_status (status),
    INDEX idx_leads_assigned (assigned_to),
    INDEX idx_leads_is_deleted (is_deleted)
);

-- Bổ sung region
SET @has_region = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'region');
SET @sql = IF(@has_region = 0, 'ALTER TABLE leads ADD COLUMN region VARCHAR(100) NULL DEFAULT "Miền Bắc (Hà Nội)"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Bổ sung region_code
SET @has_region_code = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'region_code');
SET @sql = IF(@has_region_code = 0, 'ALTER TABLE leads ADD COLUMN region_code VARCHAR(50) NULL DEFAULT "NORTH"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Bổ sung industry
SET @has_industry = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'industry');
SET @sql = IF(@has_industry = 0, 'ALTER TABLE leads ADD COLUMN industry VARCHAR(100) NULL DEFAULT "ALL"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Bổ sung title (chức danh khách hàng)
SET @has_title = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'title');
SET @sql = IF(@has_title = 0, 'ALTER TABLE leads ADD COLUMN title VARCHAR(100) NULL DEFAULT "Khách hàng tiềm năng"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Bổ sung unassigned_reason
SET @has_reason = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'unassigned_reason');
SET @sql = IF(@has_reason = 0, 'ALTER TABLE leads ADD COLUMN unassigned_reason VARCHAR(255) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Bảng cấu hình Quy tắc Phân bổ Lead (lead_assignment_rules)
CREATE TABLE IF NOT EXISTS lead_assignment_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(50) NULL,
    name VARCHAR(255) NOT NULL,
    priority_order INT NOT NULL DEFAULT 1,
    region VARCHAR(50) NOT NULL DEFAULT 'NATIONWIDE',
    industry VARCHAR(100) NOT NULL DEFAULT 'ALL',
    mechanism VARCHAR(50) NOT NULL DEFAULT 'ROUND_ROBIN',
    reps TEXT NULL,
    description TEXT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    total_assigned INT NOT NULL DEFAULT 0,
    last_assigned_index INT NOT NULL DEFAULT 0,
    last_assigned_user_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rules_priority (priority_order, is_active)
);

-- 3. Bảng Nhật ký Phân bổ Lead (lead_assignment_history)
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
    note TEXT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_lah_lead (lead_id),
    INDEX idx_lah_assigned_at (assigned_at)
);

-- 4. Bổ sung Quyền cho Module Phân bổ Lead vào bảng permissions
INSERT IGNORE INTO permissions (code, name) VALUES
    ('lead.assignment_rules', 'Cấu hình quy tắc phân bổ Lead'),
    ('lead.auto_assign', 'Kích hoạt phân bổ Lead tự động'),
    ('lead.manage', 'Quản lý Lead');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'DIRECTOR', 'MANAGER') AND p.code LIKE 'lead.%';

-- 5. Seed dữ liệu mặc định cho Quy tắc Phân bổ Lead (First-Match-Wins)
INSERT IGNORE INTO lead_assignment_rules (id, rule_code, name, priority_order, region, industry, mechanism, reps, description, is_active, total_assigned, last_assigned_index) VALUES
(1, 'RULE-01', 'Phân bổ Khách hàng Bất động sản & Xây dựng (Miền Bắc)', 1, 'NORTH', 'Bất động sản', 'ROUND_ROBIN', '["USR-101", "USR-102"]', 'Xoay vòng đều giữa chuyên viên am hiểu thị trường địa ốc Miền Bắc.', 1, 38, 0),
(2, 'RULE-02', 'Phân bổ Lead Công nghệ thông tin & Viễn thông', 2, 'NATIONWIDE', 'CNTT', 'ROUND_ROBIN', '["USR-105", "USR-106"]', 'Lead phần mềm, API tích hợp và ERP giao cho đội ngũ chuyên môn IT.', 1, 42, 1),
(3, 'RULE-03', 'Điều phối Thị trường Miền Nam (Round-Robin)', 3, 'SOUTH', 'ALL', 'ROUND_ROBIN', '["USR-104", "USR-105"]', 'Tất cả khách hàng tiềm năng tại TP.HCM và các tỉnh Nam Bộ.', 1, 35, 0),
(4, 'RULE-04', 'Điều phối Thị trường Miền Trung (Đà Nẵng & lân cận)', 4, 'CENTRAL', 'ALL', 'DEDICATED', '["USR-103"]', 'Chỉ định trực tiếp Trưởng nhóm chi nhánh miền Trung quản lý.', 1, 19, 0),
(5, 'RULE-05', 'Khách hàng Doanh nghiệp Toàn quốc - Cân bằng tải', 5, 'NATIONWIDE', 'Doanh nghiệp', 'LOAD_BALANCED', '["USR-101", "USR-104", "USR-106"]', 'Tự động phân cho nhân viên đang có số Lead thấp nhất để giảm quá tải.', 1, 26, 0);

-- 6. Seed dữ liệu mẫu cho Lịch sử phân bổ
INSERT IGNORE INTO lead_assignment_history (id, lead_id, lead_name, lead_phone, company_name, rep_id, rep_name, rep_dept, method, note, assigned_at) VALUES
(1, 1, 'Nguyễn Minh Quang', '0987123456', 'Công ty CP Công nghệ DigiTech VN', 'USR-105', 'Vũ Hải Đăng', 'Phòng Kinh doanh Miền Nam', 'Tự động: Quy tắc #2 (CNTT & Viễn thông)', 'Khớp điều kiện ngành CNTT và khu vực toàn quốc qua Round-Robin.', '2026-10-14 09:15:00'),
(2, 2, 'Nguyễn Thị Phương Thảo', '0987654321', 'Tập đoàn Địa ốc Hoàng Gia Land', 'USR-102', 'Trần Thu Hà', 'Phòng Kinh doanh Miền Bắc', 'Tự động: Quy tắc #1 (Bất động sản Miền Bắc)', 'Khớp điều kiện Bất động sản Miền Bắc qua Round-Robin.', '2026-10-14 08:45:00');

-- 7. Seed tài khoản người dùng tương ứng cho các Chuyên viên kinh doanh
INSERT IGNORE INTO users (id, full_name, email, password_hash, status, session_version, data_scope, team_id, phone) VALUES
(101, 'Nguyễn Văn Hùng', 'hung.nv@company.com', '$2a$10$Xpt0Qh4R1tD2J0d4oHkP.OYt6Yc1f2K4r3l5s6t7u8v9w0x1y2z3a', 'ACTIVE', 1, 'TEAM', 1, '0912101101'),
(102, 'Trần Thu Hà', 'ha.tt@company.com', '$2a$10$Xpt0Qh4R1tD2J0d4oHkP.OYt6Yc1f2K4r3l5s6t7u8v9w0x1y2z3a', 'ACTIVE', 1, 'TEAM', 1, '0912102102'),
(103, 'Lê Hoàng Cường', 'cuong.lh@company.com', '$2a$10$Xpt0Qh4R1tD2J0d4oHkP.OYt6Yc1f2K4r3l5s6t7u8v9w0x1y2z3a', 'ACTIVE', 1, 'TEAM', 2, '0912103103'),
(104, 'Phạm Thu Dung', 'dung.pt@company.com', '$2a$10$Xpt0Qh4R1tD2J0d4oHkP.OYt6Yc1f2K4r3l5s6t7u8v9w0x1y2z3a', 'ACTIVE', 1, 'TEAM', 3, '0912104104'),
(105, 'Vũ Hải Đăng', 'dang.vh@company.com', '$2a$10$Xpt0Qh4R1tD2J0d4oHkP.OYt6Yc1f2K4r3l5s6t7u8v9w0x1y2z3a', 'ACTIVE', 1, 'TEAM', 3, '0912105105'),
(106, 'Đỗ Minh Tuấn', 'tuan.dm@company.com', '$2a$10$Xpt0Qh4R1tD2J0d4oHkP.OYt6Yc1f2K4r3l5s6t7u8v9w0x1y2z3a', 'ACTIVE', 1, 'ALL', 1, '0912106106');

-- 8. Seed dữ liệu Lead thực tế trong hàng chờ phân bổ (Pending Assignment Queue)
INSERT IGNORE INTO leads (id, full_name, title, company_name, phone, email, region, region_code, industry, status, assigned_to, unassigned_reason, source) VALUES
(101, 'Trịnh Xuân Bách', 'Tổng Giám đốc', 'Hệ thống Anh ngữ EduSmart', '0912111222', 'bach.tx@giaoducthongminh.edu.vn', 'Miền Bắc (Hà Nội)', 'NORTH', 'Giáo dục & Đào tạo', 'PENDING_ASSIGNMENT', NULL, 'Chưa có quy tắc chỉ định riêng cho ngành Giáo dục', 'WEBSITE'),
(102, 'Ngô Phương Thảo', 'Trưởng phòng Thu mua', 'HTX Nông nghiệp Công nghệ cao Xanh', '0988776655', 'thao.np@nongnghiepxanh.vn', 'Miền Trung (Đà Nẵng)', 'CENTRAL', 'Sản xuất & Chế biến', 'PENDING_ASSIGNMENT', NULL, 'Chờ phê duyệt phân bổ đặc thù Miền Trung', 'WEB_FORM'),
(103, 'Lý Minh Triết', 'Giám đốc Kinh doanh', 'Dược phẩm Sông Tiền Cần Thơ', '0934567890', 'triet.lm@duochaugiang.com', 'Miền Nam (Cần Thơ)', 'SOUTH', 'Dược phẩm & Y tế', 'PENDING_ASSIGNMENT', NULL, 'Khách hàng yêu cầu chuyên gia Dược phẩm miền Nam', 'HOTLINE'),
(104, 'Bùi Thái Dương', 'Chủ tịch HĐQT', 'Vận tải Biển Hải Phòng Logistics', '0977665544', 'duong.bt@vantaibien.vn', 'Miền Bắc (Hải Phòng)', 'NORTH', 'Sản xuất & Vận tải', 'PENDING_ASSIGNMENT', NULL, 'Lead VIP cần Trưởng phòng chỉ định nhân sự chuyên trách', 'PARTNER');


