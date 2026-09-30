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
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS industry_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS company_size_id BIGINT NULL;

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

ALTER TABLE activities
    ADD COLUMN IF NOT EXISTS activity_type_id BIGINT NULL;

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
