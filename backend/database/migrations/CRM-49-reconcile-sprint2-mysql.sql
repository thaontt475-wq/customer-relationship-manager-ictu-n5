-- S2-04/S2-06/S2-07/S2-08/S2-09/S2-10: additive MySQL 8 reconciliation.

-- Back up crm_db before executing. Existing migrations remain unchanged.

-- Re-runnable: checks each missing column, creates missing tables, INSERT IGNORE seeds.

USE crm_db;

-- CRM-37 / S2-04 - Immutable business change history
USE crm_db;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    object_type VARCHAR(50) NOT NULL,
    object_id BIGINT NOT NULL,
    before_value JSON NOT NULL,
    after_value JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_logs_actor_time (actor_user_id, created_at),
    INDEX idx_audit_logs_object_time (object_type, object_id, created_at),
    INDEX idx_audit_logs_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- CRM-44: Common Master Data Categories (Danh mục dùng chung)
-- Categories: INDUSTRY (Ngành nghề), COMPANY_SIZE (Quy mô), LEAD_SOURCE (Nguồn Lead), ACTIVITY_TYPE (Loại hoạt động)
USE crm_db;

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500) NULL,
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_category_type_code (type, code),
    INDEX idx_category_type_order (type, display_order ASC),
    INDEX idx_category_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optional reference columns on customers, leads, activities
SET @crm_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='customers' AND column_name='industry_id'), 'SELECT 1', 'ALTER TABLE customers ADD COLUMN industry_id BIGINT NULL');
PREPARE crm_stmt FROM @crm_ddl; EXECUTE crm_stmt; DEALLOCATE PREPARE crm_stmt;
SET @crm_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='customers' AND column_name='company_size_id'), 'SELECT 1', 'ALTER TABLE customers ADD COLUMN company_size_id BIGINT NULL');
PREPARE crm_stmt FROM @crm_ddl; EXECUTE crm_stmt; DEALLOCATE PREPARE crm_stmt;

CREATE TABLE IF NOT EXISTS leads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner_user_id BIGINT NOT NULL,
    lead_source_id BIGINT NULL,
    industry_id BIGINT NULL,
    company_size_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_leads_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @crm_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='activities' AND column_name='activity_type_id'), 'SELECT 1', 'ALTER TABLE activities ADD COLUMN activity_type_id BIGINT NULL');
PREPARE crm_stmt FROM @crm_ddl; EXECUTE crm_stmt; DEALLOCATE PREPARE crm_stmt;

-- Seed default master data with display_order
INSERT IGNORE INTO categories (type, code, name, description, display_order, is_active) VALUES
-- 1. Ngành nghề (Industries)
('INDUSTRY', 'IT', 'Công nghệ thông tin & Viễn thông', 'Phần mềm, phần cứng, dịch vụ IT, viễn thông', 1, TRUE),
('INDUSTRY', 'RETAIL', 'Bán lẻ & Thương mại điện tử', 'Hàng tiêu dùng nhanh, siêu thị, sàn TMĐT', 2, TRUE),
('INDUSTRY', 'MANUFACTURING', 'Sản xuất & Chế tạo', 'Cơ khí, điện tử, dệt may, gia công', 3, TRUE),
('INDUSTRY', 'FINANCE', 'Tài chính - Ngân hàng - Bảo hiểm', 'Ngân hàng, chứng khoán, fintech, bảo hiểm', 4, TRUE),
('INDUSTRY', 'REAL_ESTATE', 'Bất động sản & Xây dựng', 'Kinh doanh BĐS, tư vấn thiết kế, thi công', 5, TRUE),
('INDUSTRY', 'EDUCATION', 'Giáo dục & Đào tạo', 'Trường học, trung tâm ngoại ngữ, edtech', 6, TRUE),
('INDUSTRY', 'HEALTHCARE', 'Y tế & Dược phẩm', 'Bệnh viện, phòng khám, công ty dược', 7, TRUE),
('INDUSTRY', 'OTHER', 'Ngành nghề khác', 'Các lĩnh vực ngành nghề khác', 99, TRUE),

-- 2. Quy mô doanh nghiệp (Company Sizes)
('COMPANY_SIZE', 'MICRO', 'Dưới 10 nhân sự (Siêu nhỏ)', 'Doanh nghiệp siêu nhỏ, hộ kinh doanh', 1, TRUE),
('COMPANY_SIZE', 'SMALL', '10 - 50 nhân sự (Nhỏ)', 'Doanh nghiệp nhỏ', 2, TRUE),
('COMPANY_SIZE', 'MEDIUM', '51 - 200 nhân sự (Vừa)', 'Doanh nghiệp quy mô vừa', 3, TRUE),
('COMPANY_SIZE', 'LARGE', '201 - 500 nhân sự (Lớn)', 'Doanh nghiệp quy mô lớn', 4, TRUE),
('COMPANY_SIZE', 'ENTERPRISE', 'Trên 500 nhân sự (Tập đoàn)', 'Tập đoàn đa quốc gia, tổng công ty', 5, TRUE),

-- 3. Nguồn Lead (Lead Sources)
('LEAD_SOURCE', 'WEBSITE', 'Website / Tự nhiên (Organic)', 'Khách hàng truy cập website đăng ký form', 1, TRUE),
('LEAD_SOURCE', 'FACEBOOK', 'Facebook Ads / Fanpage', 'Quảng cáo Facebook, bài viết cộng đồng', 2, TRUE),
('LEAD_SOURCE', 'GOOGLE', 'Google Ads / Tìm kiếm', 'Quảng cáo tìm kiếm, Google Display Network', 3, TRUE),
('LEAD_SOURCE', 'REFERRAL', 'Giới thiệu (Referral)', 'Khách hàng cũ hoặc đối tác giới thiệu', 4, TRUE),
('LEAD_SOURCE', 'EVENT', 'Sự kiện / Hội thảo', 'Tham gia triển lãm, workshop, hội nghị', 5, TRUE),
('LEAD_SOURCE', 'HOTLINE', 'Hotline / Đến trực tiếp', 'Khách gọi điện thoại trực tiếp văn phòng', 6, TRUE),
('LEAD_SOURCE', 'EMAIL', 'Email Marketing', 'Chiến dịch gửi thư điện tử', 7, TRUE),
('LEAD_SOURCE', 'OTHER', 'Nguồn khác', 'Các kênh giới thiệu khác', 99, TRUE),

-- 4. Loại hoạt động (Activity Types)
('ACTIVITY_TYPE', 'CALL', 'Cuộc gọi điện (Call)', 'Gọi tư vấn, chăm sóc, chốt đơn', 1, TRUE),
('ACTIVITY_TYPE', 'MEETING', 'Gặp mặt trực tiếp (Meeting)', 'Gặp trực tiếp khách hàng hoặc đối tác', 2, TRUE),
('ACTIVITY_TYPE', 'DEMO', 'Demo sản phẩm / Trực tuyến', 'Thuyết trình sản phẩm qua Zoom/Google Meet', 3, TRUE),
('ACTIVITY_TYPE', 'EMAIL', 'Gửi Email', 'Gửi báo giá, tài liệu, xác nhận thông tin', 4, TRUE),
('ACTIVITY_TYPE', 'TASK', 'Nhiệm vụ / Việc cần làm (Task)', 'Công việc nội bộ chuẩn bị cho khách hàng', 5, TRUE),
('ACTIVITY_TYPE', 'LUNCH', 'Gặp ăn trưa / Tiếp khách', 'Gặp gỡ ăn trưa, giao lưu đối tác', 6, TRUE);


-- CRM-46 / S2-08: Custom fields. Safe to run repeatedly.
CREATE TABLE IF NOT EXISTS custom_field_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    field_label VARCHAR(255) NOT NULL,
    field_type VARCHAR(20) NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    show_on_form BOOLEAN NOT NULL DEFAULT TRUE,
    usable_in_filter BOOLEAN NOT NULL DEFAULT TRUE,
    exportable BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_custom_field_entity_name (entity_type, field_name),
    INDEX idx_custom_field_entity_order (entity_type, sort_order),
    INDEX idx_custom_field_active (is_active),
    CONSTRAINT chk_custom_field_type CHECK (field_type IN ('TEXT', 'NUMBER', 'DATE', 'SELECT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS custom_field_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    custom_field_id BIGINT NOT NULL,
    option_value VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_custom_field_option (custom_field_id, option_value),
    INDEX idx_custom_field_option_order (custom_field_id, sort_order),
    CONSTRAINT fk_custom_field_option_definition
        FOREIGN KEY (custom_field_id) REFERENCES custom_field_definitions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS custom_field_values (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    custom_field_id BIGINT NOT NULL,
    record_id BIGINT NOT NULL,
    field_value TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_custom_field_record (custom_field_id, record_id),
    INDEX idx_custom_field_value_record (record_id),
    CONSTRAINT fk_custom_field_value_definition
        FOREIGN KEY (custom_field_id) REFERENCES custom_field_definitions(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- CRM-47: Pipeline Stages and Transition Rules Management
USE crm_db;

-- Pipeline stages table
CREATE TABLE IF NOT EXISTS pipeline_stages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pipeline_id BIGINT NOT NULL DEFAULT 1,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    stage_order INT NOT NULL DEFAULT 1,
    win_probability INT NOT NULL DEFAULT 0,
    requirements TEXT NULL,
    is_won BOOLEAN NOT NULL DEFAULT FALSE,
    is_lost BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pipeline_stage_code (pipeline_id, code),
    INDEX idx_pipeline_stage_order (pipeline_id, stage_order ASC),
    INDEX idx_pipeline_stage_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Enhance opportunities table with stage_id, amount, contact_name, lost_reason
SET @crm_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='opportunities' AND column_name='stage_id'), 'SELECT 1', 'ALTER TABLE opportunities ADD COLUMN stage_id BIGINT NULL');
PREPARE crm_stmt FROM @crm_ddl; EXECUTE crm_stmt; DEALLOCATE PREPARE crm_stmt;
SET @crm_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='opportunities' AND column_name='amount'), 'SELECT 1', 'ALTER TABLE opportunities ADD COLUMN amount DECIMAL(15, 2) NULL DEFAULT 0.00');
PREPARE crm_stmt FROM @crm_ddl; EXECUTE crm_stmt; DEALLOCATE PREPARE crm_stmt;
SET @crm_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='opportunities' AND column_name='contact_name'), 'SELECT 1', 'ALTER TABLE opportunities ADD COLUMN contact_name VARCHAR(255) NULL');
PREPARE crm_stmt FROM @crm_ddl; EXECUTE crm_stmt; DEALLOCATE PREPARE crm_stmt;
SET @crm_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='opportunities' AND column_name='lost_reason'), 'SELECT 1', 'ALTER TABLE opportunities ADD COLUMN lost_reason VARCHAR(500) NULL');
PREPARE crm_stmt FROM @crm_ddl; EXECUTE crm_stmt; DEALLOCATE PREPARE crm_stmt;
SET @crm_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='opportunities' AND column_name='probability'), 'SELECT 1', 'ALTER TABLE opportunities ADD COLUMN probability INT NULL');
PREPARE crm_stmt FROM @crm_ddl; EXECUTE crm_stmt; DEALLOCATE PREPARE crm_stmt;

-- Seed default sales pipeline stages (Order: 1 to 6, Win probability: 10% to 100%)
INSERT IGNORE INTO pipeline_stages (pipeline_id, code, name, stage_order, win_probability, requirements, is_won, is_lost, is_active) VALUES
(1, 'PROSPECTING', 'Tìm kiếm & Tiếp cận', 1, 10, 'Xác định khách hàng tiềm năng và người liên hệ chính', FALSE, FALSE, TRUE),
(1, 'QUALIFICATION', 'Đánh giá & Xác định nhu cầu', 2, 25, 'Xác định rõ ngân sách, thẩm quyền, nhu cầu và tiến độ (BANT)', FALSE, FALSE, TRUE),
(1, 'PROPOSAL', 'Gửi đề xuất & Báo giá', 3, 50, 'Bắt buộc nhập giá trị cơ hội (amount) và gửi báo giá chính thức', FALSE, FALSE, TRUE),
(1, 'NEGOTIATION', 'Thương lượng & Đàm phán', 4, 75, 'Thống nhất điều khoản hợp đồng và chính sách giá/chiết khấu', FALSE, FALSE, TRUE),
(1, 'CLOSED_WON', 'Chốt thành công (Won)', 5, 100, 'Ký hợp đồng hoặc xác nhận đơn hàng thành công; giá trị phải lớn hơn 0', TRUE, FALSE, TRUE),
(1, 'CLOSED_LOST', 'Thất bại (Lost)', 6, 0, 'Bắt buộc ghi rõ lý do thất bại (lost_reason)', FALSE, TRUE, TRUE);


-- CRM-48 / S2-10: Reference data for win/loss reasons and competitors.
-- No Opportunity close-flow columns are introduced by this story.
CREATE TABLE IF NOT EXISTS win_loss_reasons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reason_type VARCHAR(10) NOT NULL,
    reason_text VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_win_loss_reason_type_text (reason_type, reason_text),
    INDEX idx_win_loss_reason_order (reason_type, is_active, display_order),
    CONSTRAINT chk_win_loss_reason_type CHECK (reason_type IN ('WIN', 'LOSS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS competitors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    strengths VARCHAR(1000) NULL,
    weaknesses VARCHAR(1000) NULL,
    website VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_competitor_name (name),
    INDEX idx_competitor_active_order (is_active, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Reconcile organization metadata without inventing team leaders.
SET @crm_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='teams' AND column_name='parent_id'), 'SELECT 1', 'ALTER TABLE teams ADD COLUMN parent_id BIGINT NULL');
PREPARE crm_stmt FROM @crm_ddl; EXECUTE crm_stmt; DEALLOCATE PREPARE crm_stmt;
SET @crm_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='teams' AND column_name='leader_user_id'), 'SELECT 1', 'ALTER TABLE teams ADD COLUMN leader_user_id BIGINT NULL');
PREPARE crm_stmt FROM @crm_ddl; EXECUTE crm_stmt; DEALLOCATE PREPARE crm_stmt;
SET @crm_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='teams' AND column_name='region'), 'SELECT 1', 'ALTER TABLE teams ADD COLUMN region VARCHAR(20) NOT NULL DEFAULT ''NATIONAL''');
PREPARE crm_stmt FROM @crm_ddl; EXECUTE crm_stmt; DEALLOCATE PREPARE crm_stmt;
SET @crm_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='teams' AND column_name='active'), 'SELECT 1', 'ALTER TABLE teams ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE');
PREPARE crm_stmt FROM @crm_ddl; EXECUTE crm_stmt; DEALLOCATE PREPARE crm_stmt;
SET @crm_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='teams' AND column_name='created_at'), 'SELECT 1', 'ALTER TABLE teams ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP');
PREPARE crm_stmt FROM @crm_ddl; EXECUTE crm_stmt; DEALLOCATE PREPARE crm_stmt;
SET @crm_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='teams' AND column_name='updated_at'), 'SELECT 1', 'ALTER TABLE teams ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
PREPARE crm_stmt FROM @crm_ddl; EXECUTE crm_stmt; DEALLOCATE PREPARE crm_stmt;
INSERT IGNORE INTO roles (name) VALUES ('Director');