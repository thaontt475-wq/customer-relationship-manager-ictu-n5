-- ==============================================================================
-- 03_seed_customers_contacts.sql: KHÁCH HÀNG DOANH NGHIỆP & NGƯỜI LIÊN HỆ
-- Hệ thống: NovaCRM | Dành cho môi trường QA / Demo
-- Kịch bản kiểm thử:
--   - Phân quyền dữ liệu SELF / TEAM / ALL
--   - Phân cấp Công ty Mẹ - Con (Parent-Child Hierarchy)
--   - Cờ cảnh báo Rủi ro Churn (Support Risk Flag)
--   - Cảnh báo Chăm sóc định kỳ (Periodic Customer Care > 30 ngày)
--   - Phát hiện và Gộp Khách hàng trùng lặp (Customer Deduplication & Merge)
--   - Đầy đủ 4 vai trò mua hàng: DECISION_MAKER, INFLUENCER, END_USER, BLOCKER
-- ==============================================================================

SET NAMES utf8mb4;

-- 1. Hồ sơ Khách hàng Doanh nghiệp (customers)
INSERT INTO customers (
    id, name, tax_code, status, email, phone, website, address,
    industry_id, company_size_id, owner_user_id, parent_customer_id,
    is_deleted, support_risk_active, support_risk_episode, region
) VALUES
-- Khách 1: Khách hàng lớn đã ký hợp đồng - Thuộc Sales Bắc 1 (Team 1)
(1, 'Tập đoàn Công nghệ Alpha Holdings', '0101234567', 'KHACH_HANG', 'contact@alphaholdings.vn', '02438889999', 'https://alphaholdings.vn', 'Tòa nhà Alpha Tower, Cầu Giấy, Hà Nội', 100, 124, 101, NULL, 0, 0, 0, 'MB'),

-- Khách 2: Công ty con của Alpha Holdings (Test S3-05 Công ty mẹ/con) - Thuộc Sales Bắc 1 (Team 1)
(2, 'Công ty CP Phần mềm Alpha Solutions', '0108901234', 'KHACH_HANG', 'info@alphasolutions.vn', '02438889988', 'https://alphasolutions.vn', 'Tầng 5, Alpha Tower, Cầu Giấy, Hà Nội', 100, 122, 101, 1, 0, 0, 0, 'MB'),

-- Khách 3: Khách hàng đang có CỜ RỦI RO do ticket tồn đọng (Test S3-08) - Thuộc Sales Bắc 1 (Team 1)
(3, 'Công ty CP Bán lẻ SmartMart Việt Nam', '0102345678', 'DANG_GIAO_DICH', 'support@smartmart.vn', '02437778888', 'https://smartmart.vn', 'Số 88 Trần Duy Hưng, Cầu Giấy, Hà Nội', 101, 122, 101, NULL, 0, 1, 1, 'MB'),

-- Khách 4: Khách hàng tiềm năng Logistics - Thuộc Sales Bắc 1 (Team 1)
(4, 'Công ty TNHH Logistics Vận tải Á Châu', '0103456789', 'TIEM_NANG', 'contact@achau-logistics.vn', '02436667777', 'https://achau-logistics.vn', 'Khu Công nghiệp Nam Từ Liêm, Hà Nội', 105, 121, 101, NULL, 0, 0, 0, 'MB'),

-- Khách 5: Khách hàng trùng lặp tên & website với Khách 4 (Test S3-04 Gộp Khách hàng) - Thuộc Sales Bắc 2 (Team 1)
(5, 'Công ty TNHH Vận tải Á Châu Toàn Cầu', '0103456799', 'TIEM_NANG', 'sales@achau-logistics.vn', '02436667788', 'https://achau-logistics.vn', 'KCN Nam Từ Liêm, Hà Nội', 105, 121, 102, NULL, 0, 0, 0, 'MB'),

-- Khách 6: Khách hàng Dược phẩm - Thuộc Sales Bắc 2 (Team 1) -> Sales 1 không thấy, Team Lead 1 thấy
(6, 'Công ty Dược phẩm Quốc tế Đông Á', '0104567890', 'DANG_GIAO_DICH', 'office@donga-pharma.vn', '02435556666', 'https://donga-pharma.vn', 'Lô 12 KCN Sài Đồng, Long Biên, Hà Nội', 106, 123, 102, NULL, 0, 0, 0, 'MB'),

-- Khách 7: Khách hàng Miền Nam (Test S3-09 Chăm sóc định kỳ: Hợp đồng 180tr, 45 ngày chưa tương tác) - Thuộc Sales Nam (Team 3)
(7, 'Công ty CP Sản xuất Cơ khí Miền Nam', '0306789012', 'KHACH_HANG', 'info@cokhimiennam.com.vn', '02838881111', 'https://cokhimiennam.com.vn', 'Khu Công nghiệp Sóng Thần 2, Dĩ An, Bình Dương', 102, 124, 104, NULL, 0, 0, 0, 'MN'),

-- Khách 8: Khách hàng Bất động sản Miền Nam - Thuộc Sales Nam (Team 3)
(8, 'Tập đoàn Bất động sản Phú Mỹ Hưng Thịnh', '0307890123', 'DANG_GIAO_DICH', 'contact@phumyhungthinh.vn', '02839992222', 'https://phumyhungthinh.vn', 'Số 100 Nguyễn Lương Bằng, Quận 7, TP.HCM', 104, 123, 104, NULL, 0, 0, 0, 'MN')
ON DUPLICATE KEY UPDATE 
    name = VALUES(name), status = VALUES(status), email = VALUES(email), phone = VALUES(phone),
    website = VALUES(website), address = VALUES(address), industry_id = VALUES(industry_id),
    company_size_id = VALUES(company_size_id), owner_user_id = VALUES(owner_user_id),
    parent_customer_id = VALUES(parent_customer_id), is_deleted = 0,
    support_risk_active = VALUES(support_risk_active), support_risk_episode = VALUES(support_risk_episode),
    region = VALUES(region);

-- 2. Danh bạ Người liên hệ theo Vai trò mua hàng (contacts)
INSERT INTO contacts (id, customer_id, full_name, email, phone, title, buying_role, is_primary, is_deleted) VALUES
-- Khách 1 (Alpha Holdings): Đầy đủ 4 vai trò mua hàng
(1, 1, 'Nguyễn Thanh Hải', 'hai.nt@alphaholdings.vn', '0912345678', 'Tổng Giám đốc (CEO)', 'DECISION_MAKER', 1, 0),
(2, 1, 'Lê Thị Bích Ngọc', 'ngoc.lt@alphaholdings.vn', '0912345679', 'Giám đốc Công nghệ (CTO)', 'INFLUENCER', 0, 0),
(3, 1, 'Trần Văn Quân', 'quan.tv@alphaholdings.vn', '0912345680', 'Trưởng phòng Vận hành', 'END_USER', 0, 0),
(4, 1, 'Phạm Đức Thắng', 'thang.pd@alphaholdings.vn', '0912345681', 'Kế toán trưởng', 'BLOCKER', 0, 0),

-- Khách 2 (Alpha Solutions)
(5, 2, 'Vũ Đình Phong', 'phong.vd@alphasolutions.vn', '0912345682', 'Giám đốc Khối Phần mềm', 'DECISION_MAKER', 1, 0),

-- Khách 3 (SmartMart)
(6, 3, 'Hoàng Văn Tuấn', 'tuan.hv@smartmart.vn', '0987654321', 'Giám đốc Vận hành (COO)', 'DECISION_MAKER', 1, 0),
(7, 3, 'Nguyễn Mai Anh', 'anh.nm@smartmart.vn', '0987654322', 'Trưởng phòng Dịch vụ Khách hàng', 'END_USER', 0, 0),

-- Khách 4 (Logistics Á Châu)
(8, 4, 'Ngô Trí Dũng', 'dung.nt@achau-logistics.vn', '0976543210', 'Phó Giám đốc Thường trực', 'DECISION_MAKER', 1, 0),

-- Khách 5 (Trùng Á Châu - Test Gộp liên hệ)
(9, 5, 'Nguyễn Văn Hưng', 'hung.nv@achau-logistics.vn', '0976543211', 'Trưởng phòng Điều vận Logistics', 'INFLUENCER', 1, 0),

-- Khách 6 (Dược Đông Á)
(10, 6, 'Trương Mỹ Linh', 'linh.tm@donga-pharma.vn', '0965432109', 'Giám đốc Mua sắm & Đấu thầu', 'DECISION_MAKER', 1, 0),

-- Khách 7 (Cơ khí Miền Nam)
(11, 7, 'Lâm Quốc Cường', 'cuong.lq@cokhimiennam.com.vn', '0945678901', 'Phó Tổng Giám đốc Kỹ thuật', 'DECISION_MAKER', 1, 0),

-- Khách 8 (Phú Mỹ Hưng Thịnh)
(12, 8, 'Dương Hoài Nam', 'nam.dh@phumyhungthinh.vn', '0934567891', 'Giám đốc Quản lý Dự án BĐS', 'DECISION_MAKER', 1, 0)
ON DUPLICATE KEY UPDATE 
    customer_id = VALUES(customer_id), full_name = VALUES(full_name), email = VALUES(email),
    phone = VALUES(phone), title = VALUES(title), buying_role = VALUES(buying_role),
    is_primary = VALUES(is_primary), is_deleted = 0;
