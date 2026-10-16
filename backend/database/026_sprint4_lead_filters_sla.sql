-- ===================================================================
-- S4-09 / CRM-123: Danh sách, tìm kiếm và lọc Lead & Quản lý SLA, Bộ lọc lưu sẵn
-- ===================================================================

-- 1. Đảm bảo bảng leads có đầy đủ các cột phục vụ Tìm kiếm nâng cao & Cảnh báo SLA
CREATE TABLE IF NOT EXISTS leads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NULL,
    full_name VARCHAR(150) NOT NULL,
    title VARCHAR(100) NULL DEFAULT 'Khách hàng tiềm năng',
    company_name VARCHAR(255) NULL,
    industry VARCHAR(100) NULL DEFAULT 'ALL',
    tax_code VARCHAR(50) NULL,
    email VARCHAR(255) NULL,
    phone VARCHAR(50) NULL,
    source VARCHAR(50) NULL DEFAULT 'WEB_FORM',
    source_name VARCHAR(150) NULL,
    rating VARCHAR(20) NULL DEFAULT 'WARM',
    tier VARCHAR(20) NULL DEFAULT 'WARM',
    score INT NOT NULL DEFAULT 50,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    assigned_to BIGINT NULL,
    estimated_value DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    sla_deadline DATETIME NULL,
    sla_minutes_total INT NOT NULL DEFAULT 120,
    last_contacted_at DATETIME NULL,
    notes TEXT NULL,
    form_id VARCHAR(100) NULL,
    ip_address VARCHAR(50) NULL,
    region VARCHAR(100) NULL DEFAULT 'Miền Bắc (Hà Nội)',
    region_code VARCHAR(50) NULL DEFAULT 'NORTH',
    unassigned_reason VARCHAR(255) NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_leads_status (status),
    INDEX idx_leads_source (source),
    INDEX idx_leads_assigned (assigned_to),
    INDEX idx_leads_is_deleted (is_deleted),
    INDEX idx_leads_created (created_at)
);

-- Bổ sung các cột nếu bảng leads đã tồn tại trước đó từ các sprint khác
SET @has_code = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'code');
SET @sql = IF(@has_code = 0, 'ALTER TABLE leads ADD COLUMN code VARCHAR(50) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_source_name = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'source_name');
SET @sql = IF(@has_source_name = 0, 'ALTER TABLE leads ADD COLUMN source_name VARCHAR(150) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_rating = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'rating');
SET @sql = IF(@has_rating = 0, 'ALTER TABLE leads ADD COLUMN rating VARCHAR(20) NULL DEFAULT "WARM"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_tier = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'tier');
SET @sql = IF(@has_tier = 0, 'ALTER TABLE leads ADD COLUMN tier VARCHAR(20) NULL DEFAULT "WARM"', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_score = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'score');
SET @sql = IF(@has_score = 0, 'ALTER TABLE leads ADD COLUMN score INT NOT NULL DEFAULT 50', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_est_val = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'estimated_value');
SET @sql = IF(@has_est_val = 0, 'ALTER TABLE leads ADD COLUMN estimated_value DECIMAL(15, 2) NOT NULL DEFAULT 0.00', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_sla_deadline = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'sla_deadline');
SET @sql = IF(@has_sla_deadline = 0, 'ALTER TABLE leads ADD COLUMN sla_deadline DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_sla_total = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'sla_minutes_total');
SET @sql = IF(@has_sla_total = 0, 'ALTER TABLE leads ADD COLUMN sla_minutes_total INT NOT NULL DEFAULT 120', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_last_contact = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'last_contacted_at');
SET @sql = IF(@has_last_contact = 0, 'ALTER TABLE leads ADD COLUMN last_contacted_at DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_tax_code = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'tax_code');
SET @sql = IF(@has_tax_code = 0, 'ALTER TABLE leads ADD COLUMN tax_code VARCHAR(50) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_is_deleted = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'is_deleted');
SET @sql = IF(@has_is_deleted = 0, 'ALTER TABLE leads ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_form_id = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'form_id');
SET @sql = IF(@has_form_id = 0, 'ALTER TABLE leads ADD COLUMN form_id VARCHAR(100) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_ip_address = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leads' AND COLUMN_NAME = 'ip_address');
SET @sql = IF(@has_ip_address = 0, 'ALTER TABLE leads ADD COLUMN ip_address VARCHAR(50) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Bảng quản lý Bộ lọc lưu sẵn cho Lead (saved_lead_filters)
CREATE TABLE IF NOT EXISTS saved_lead_filters (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    filter_criteria TEXT NOT NULL,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_slf_user (user_id)
);

-- 3. Quyền hạn cho Module Lead (Permissions)
INSERT IGNORE INTO permissions (code, name) VALUES
    ('lead.read', 'Xem danh sách Lead'),
    ('lead.create', 'Thêm mới Lead'),
    ('lead.update', 'Chỉnh sửa Lead'),
    ('lead.delete', 'Xóa Lead'),
    ('lead.filter_manage', 'Quản lý Bộ lọc lưu sẵn');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'DIRECTOR', 'MANAGER', 'SALES_REP', 'SALES_EXECUTIVE') AND p.code LIKE 'lead.%';

-- Gán quyền phạm vi dữ liệu (role_module_scopes) cho Module lead
INSERT IGNORE INTO role_module_scopes (role_id, module_code, scope_type)
SELECT r.id, 'lead', 
    CASE 
        WHEN r.code IN ('ADMIN', 'DIRECTOR') THEN 'ALL'
        WHEN r.code IN ('MANAGER', 'TEAM_LEAD') THEN 'TEAM'
        ELSE 'SELF'
    END
FROM roles r;

-- 4. Cập nhật và bổ sung dữ liệu Lead mẫu phong phú với đầy đủ nhiệt độ HOT/WARM/COLD và cờ SLA quá hạn
UPDATE leads SET 
    code = CONCAT('LEAD-2026-', LPAD(id, 3, '0')),
    tier = IF(tier IS NULL OR tier = 'ALL', 'WARM', tier),
    rating = IF(rating IS NULL OR rating = 'ALL', 'WARM', rating),
    sla_minutes_total = 120
WHERE code IS NULL OR code = '';

-- Chèn các Lead mẫu thử nghiệm chuẩn theo AC1, AC2 của S4-09
INSERT INTO leads (id, code, full_name, title, company_name, industry, tax_code, phone, email, source, source_name, rating, tier, score, status, assigned_to, estimated_value, sla_deadline, sla_minutes_total, notes)
VALUES
(201, 'LEAD-2026-201', 'Trần Quang Khải', 'Tổng Giám đốc (CEO)', 'Công ty Cổ phần Công nghệ Delta Smart', 'CNTT & Viễn thông', '0109988112', '0912111222', 'khai.tq@deltasmart.vn', 'WEBSITE', 'Website Form', 'HOT', 'HOT', 95, 'NEW', 1, 350000000.00, DATE_SUB(NOW(), INTERVAL 3 HOUR), 120, 'Khách hàng đăng ký gói Enterprise 150 người dùng qua form website, cần liên hệ gấp trước 10h sáng.'),
(202, 'LEAD-2026-202', 'Đoàn Minh Trí', 'Giám đốc Mua sắm & Cung ứng', 'Chuỗi Cửa hàng Tiện lợi VinMart+', 'Bán lẻ & Chuỗi F&B', '0108777666', '0988333444', 'tri.dm@vincommerce.vn', 'EVENT', 'Hội thảo Chuyển đổi số Bán lẻ', 'WARM', 'WARM', 76, 'NEW', 101, 220000000.00, DATE_SUB(NOW(), INTERVAL 90 MINUTE), 120, 'Gặp tại hội thảo, quan tâm phân hệ Quản lý Hợp đồng và Chăm sóc Khách hàng thân thiết.'),
(203, 'LEAD-2026-203', 'Nguyễn Hoàng Nam', 'Giám đốc Công nghệ (CTO)', 'Công ty Cổ phần Công nghệ Cao Việt Tech', 'CNTT & Viễn thông', '0108923451', '0912345678', 'nam.nh@viettech.com.vn', 'CAMPAIGN', 'Chiến dịch CRM 2026', 'HOT', 'HOT', 92, 'IN_PROGRESS', 1, 250000000.00, DATE_ADD(NOW(), INTERVAL 45 MINUTE), 120, 'Đã gọi điện thoại lần 1, khách hàng yêu cầu gửi tài liệu demo kỹ thuật và bảng báo giá.'),
(204, 'LEAD-2026-204', 'Vũ Thị Bích Ngọc', 'Trưởng phòng Kế hoạch Đầu tư', 'Tập đoàn Dược phẩm Quốc tế Mediphar', 'Sản xuất & Chế tạo', '0107892341', '0988776655', 'ngoc.vb@mediphar.vn', 'WEBSITE', 'Website Form', 'HOT', 'HOT', 88, 'NEW', NULL, 420000000.00, DATE_ADD(NOW(), INTERVAL 100 MINUTE), 120, 'Nhu cầu tích hợp hệ thống CRM với phần mềm quản lý sản xuất thuốc GMP.'),
(205, 'LEAD-2026-205', 'Phạm Quốc Dũng', 'Phó Tổng Giám đốc Kinh doanh', 'Công ty Cổ phần Địa ốc & Xây dựng Đất Việt', 'Bất động sản & Xây dựng', '0309876543', '0933555777', 'dung.pq@datvietland.vn', 'REFERRAL', 'Giới thiệu / Đối tác', 'HOT', 'HOT', 86, 'QUALIFIED', 1, 350000000.00, NULL, 120, 'Đã thẩm định nhu cầu đầy đủ, đạt tiêu chuẩn chuyển đổi sang Khách hàng & Cơ hội.'),
(206, 'LEAD-2026-206', 'Đinh Tuấn Anh', 'Giám đốc Chuyển đổi số', 'Chuỗi Bán lẻ Thời trang An Phước', 'Bán lẻ & Chuỗi F&B', '0106543219', '0904123987', 'anh.dt@anphuocfashion.com', 'EXCEL', 'Nhập Excel', 'WARM', 'WARM', 79, 'IN_PROGRESS', 102, 180000000.00, DATE_ADD(NOW(), INTERVAL 80 MINUTE), 120, 'Chuỗi 25 showroom cần CRM tích hợp điểm thưởng Zalo Mini App và đồng bộ POS.'),
(207, 'LEAD-2026-207', 'Lê Thanh Bình', 'Chủ tịch HĐQT', 'Công ty Cổ phần Vận tải Biển Đông Dương', 'Vận tải & Logistics', '0105432198', '0977889900', 'binh.lt@dongduonglog.com', 'PARTNER', 'Đối tác Công nghệ', 'COLD', 'COLD', 45, 'NEW', NULL, 90000000.00, DATE_SUB(NOW(), INTERVAL 5 HOUR), 120, 'Khách hàng quan tâm theo dõi định vị phương tiện, ngân sách ban đầu hạn chế.'),
(208, 'LEAD-2026-208', 'Nguyễn Thị Thu Thủy', 'Trưởng phòng Hành chính Nhân sự', 'Trường Quốc tế Song ngữ Wellspring', 'Giáo dục & Đào tạo', '0104321987', '0915667788', 'thuy.ntt@wellspring.edu.vn', 'HOTLINE', 'Tổng đài 1900', 'WARM', 'WARM', 68, 'LOST', 103, 120000000.00, NULL, 120, 'Trường đã chọn giải pháp ERP học đường khác do thời gian triển khai nhanh hơn.')
ON DUPLICATE KEY UPDATE 
    code = VALUES(code),
    full_name = VALUES(full_name),
    company_name = VALUES(company_name),
    rating = VALUES(rating),
    tier = VALUES(tier),
    score = VALUES(score),
    status = VALUES(status),
    assigned_to = VALUES(assigned_to),
    estimated_value = VALUES(estimated_value),
    sla_deadline = VALUES(sla_deadline),
    notes = VALUES(notes);

-- 5. Seed Bộ lọc mẫu cá nhân cho user_id = 1 (CRM Administrator)
INSERT INTO saved_lead_filters (id, user_id, name, filter_criteria, is_default)
VALUES
(1, 1, '🔥 Lead VIP Cần Chăm Sóc Gấp', '{"keyword":"","status":"NEW","tier":"HOT","source":"ALL","owner":"ALL","dateRange":"ALL","slaOverdueOnly":false,"sort":"SLA_URGENT"}', 0),
(2, 1, '🌐 Lead Nguồn Website Tháng Này', '{"keyword":"","status":"ALL","tier":"ALL","source":"WEBSITE","owner":"ALL","dateRange":"THIS_MONTH","slaOverdueOnly":false,"sort":"NEWEST"}', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), filter_criteria = VALUES(filter_criteria);
