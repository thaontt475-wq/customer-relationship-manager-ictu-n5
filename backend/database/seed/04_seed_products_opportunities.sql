-- ==============================================================================
-- 04_seed_products_opportunities.sql: SẢN PHẨM, CƠ HỘI KINH DOANH, BÁO GIÁ & HỢP ĐỒNG
-- Hệ thống: NovaCRM | Dành cho môi trường QA / Demo
-- Bao gồm:
--   - Danh mục sản phẩm (One-time, Subscription SaaS, Dịch vụ triển khai)
--   - Cơ hội bán hàng trải dài qua các giai đoạn pipeline (OPEN, WON, LOST)
--   - Báo giá CPQ & chi tiết từng dòng báo giá (Quote Items)
--   - Hợp đồng kinh tế gắn liền với cơ hội và khách hàng (Contracts)
-- ==============================================================================

SET NAMES utf8mb4;

-- 1. Danh mục Sản phẩm & Giá chuẩn (products)
INSERT INTO products (id, code, name, type, unit, list_price, floor_price, cost_price, active) VALUES
(10, 'CRM-ENT-01', 'NovaCRM Gói Doanh Nghiệp (Enterprise)', 'ONE_TIME', 'Bản quyền vĩnh viễn', 24000000.00, 18000000.00, 10000000.00, 1),
(11, 'CRM-STD-02', 'NovaCRM Gói Tiêu Chuẩn (Cloud SaaS)', 'SUBSCRIPTION', 'Gói/Người dùng/Tháng', 250000.00, 190000.00, 100000.00, 1),
(12, 'SRV-IMP-03', 'Dịch vụ Triển khai On-Premise & Chuyển giao', 'ONE_TIME', 'Dự án', 50000000.00, 40000000.00, 25000000.00, 1),
(13, 'SRV-BI-04', 'Gói Tùy biến Báo cáo & Dashboard BI', 'ONE_TIME', 'Gói tính năng', 15000000.00, 12000000.00, 7000000.00, 1),
(14, 'SRV-SUPP-VIP', 'Dịch vụ Hỗ trợ & Bảo trì Kỹ thuật VIP 24/7', 'SUBSCRIPTION', 'Gói/Năm', 36000000.00, 30000000.00, 15000000.00, 1),
(15, 'LEG-SRV-05', 'Gói Bảo trì Hạ tầng Legacy 2024 (Ngừng bán)', 'ONE_TIME', 'Gói', 8000000.00, 6000000.00, 3000000.00, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type), unit = VALUES(unit),
    list_price = VALUES(list_price), floor_price = VALUES(floor_price), cost_price = VALUES(cost_price), active = VALUES(active);

-- 2. Cơ hội kinh doanh (opportunities)
INSERT INTO opportunities (id, name, customer_id, contact_name, amount, stage_id, probability, expected_close_date, status, owner_user_id, is_deleted) VALUES
(1, 'Triển khai Hệ thống NovaCRM Enterprise Toàn diện', 1, 'Nguyễn Thanh Hải', 150000000.00, 6, 100, '2026-08-15', 'WON', 101, 0),
(2, 'Gói Tùy biến CRM cho Công ty con Alpha Solutions', 2, 'Vũ Đình Phong', 60000000.00, 6, 100, '2026-09-01', 'WON', 101, 0),
(3, 'Mở rộng 50 User Cloud SaaS cho Chuỗi SmartMart', 3, 'Hoàng Văn Tuấn', 45000000.00, 4, 70, '2026-11-15', 'OPEN', 101, 0),
(4, 'Tư vấn Giải pháp Quản trị Vận tải & Logistics Á Châu', 4, 'Ngô Trí Dũng', 35000000.00, 2, 25, '2026-12-01', 'OPEN', 101, 0),
(5, 'Hệ thống Quản lý Phân phối Dược phẩm Đông Á', 6, 'Trương Mỹ Linh', 80000000.00, 3, 50, '2026-11-30', 'OPEN', 102, 0),
(6, 'Giải pháp CRM Quản lý Đơn hàng Cơ khí Miền Nam', 7, 'Lâm Quốc Cường', 180000000.00, 6, 100, '2026-07-20', 'WON', 104, 0),
(7, 'Phần mềm Chăm sóc Khách hàng Mua Căn hộ Phú Mỹ Hưng', 8, 'Dương Hoài Nam', 120000000.00, 5, 85, '2026-11-10', 'OPEN', 104, 0)
ON DUPLICATE KEY UPDATE 
    name = VALUES(name), customer_id = VALUES(customer_id), contact_name = VALUES(contact_name),
    amount = VALUES(amount), stage_id = VALUES(stage_id), probability = VALUES(probability),
    expected_close_date = VALUES(expected_close_date), status = VALUES(status), owner_user_id = VALUES(owner_user_id), is_deleted = 0;

-- 3. Báo giá (quotes)
INSERT INTO quotes (id, quote_number, title, customer_id, opportunity_id, discount_percent, subtotal, total_amount, status, requires_approval, owner_user_id, is_deleted) VALUES
(1, 'BG-2026-001', 'Báo giá Bản quyền & Triển khai NovaCRM Enterprise - Alpha Holdings', 1, 1, 0.0000, 150000000.00, 150000000.00, 'APPROVED', 0, 101, 0),
(2, 'BG-2026-002', 'Báo giá Gói Thuê bao Cloud SaaS 50 Users - SmartMart', 3, 3, 10.0000, 50000000.00, 45000000.00, 'SENT', 0, 101, 0),
(3, 'BG-2026-003', 'Báo giá Triển khai On-Premise Cơ khí Miền Nam', 7, 6, 0.0000, 180000000.00, 180000000.00, 'APPROVED', 0, 104, 0)
ON DUPLICATE KEY UPDATE 
    quote_number = VALUES(quote_number), title = VALUES(title), customer_id = VALUES(customer_id),
    opportunity_id = VALUES(opportunity_id), discount_percent = VALUES(discount_percent), subtotal = VALUES(subtotal),
    total_amount = VALUES(total_amount), status = VALUES(status), requires_approval = VALUES(requires_approval),
    owner_user_id = VALUES(owner_user_id), is_deleted = 0;

-- 4. Chi tiết Báo giá (quote_items)
INSERT INTO quote_items (id, quote_id, product_id, quantity, unit_price, discount_percent, amount) VALUES
(1, 1, 10, 1, 100000000.00, 0.0000, 100000000.00),
(2, 1, 12, 1, 50000000.00, 0.0000, 50000000.00),
(3, 2, 11, 50, 250000.00, 10.0000, 30000000.00),
(4, 2, 13, 1, 15000000.00, 0.0000, 15000000.00),
(5, 3, 10, 1, 120000000.00, 0.0000, 120000000.00),
(6, 3, 12, 1, 60000000.00, 0.0000, 60000000.00)
ON DUPLICATE KEY UPDATE 
    quote_id = VALUES(quote_id), product_id = VALUES(product_id), quantity = VALUES(quantity),
    unit_price = VALUES(unit_price), discount_percent = VALUES(discount_percent), amount = VALUES(amount);

-- 5. Hợp đồng kinh tế (contracts & customer_contracts)
INSERT INTO contracts (id, contract_number, title, customer_id, opportunity_id, quote_id, total_amount, signed_date, start_date, end_date, payment_terms, status, notes, owner_user_id, is_deleted) VALUES
(1, 'HD-2026-001', 'Hợp đồng Cung cấp & Triển khai Hệ thống NovaCRM Enterprise', 1, 1, 1, 150000000.00, '2026-08-15', '2026-09-01', '2027-08-31', 'Thanh toán 3 đợt: 40% - 40% - 20%', 'ACTIVE', 'Hợp đồng chiến lược khối Enterprise', 101, 0),
(2, 'HD-2026-002', 'Hợp đồng Triển khai Gói Mở rộng Alpha Solutions', 2, 2, NULL, 60000000.00, '2026-09-01', '2026-09-15', '2027-09-14', 'Thanh toán 2 đợt: 50% - 50%', 'ACTIVE', 'Dự án con của tập đoàn Alpha', 101, 0),
(3, 'HD-2026-003', 'Hợp đồng Cung cấp Bản quyền Phần mềm Quản trị Quan hệ Khách hàng', 7, 6, 3, 180000000.00, '2026-07-20', '2026-08-01', '2027-07-31', 'Thanh toán trọn gói chuyển khoản ngân hàng', 'ACTIVE', 'Khách hàng trọng điểm khu vực miền Nam', 104, 0)
ON DUPLICATE KEY UPDATE 
    contract_number = VALUES(contract_number), title = VALUES(title), customer_id = VALUES(customer_id),
    opportunity_id = VALUES(opportunity_id), quote_id = VALUES(quote_id), total_amount = VALUES(total_amount),
    signed_date = VALUES(signed_date), start_date = VALUES(start_date), end_date = VALUES(end_date),
    status = VALUES(status), owner_user_id = VALUES(owner_user_id), is_deleted = 0;

INSERT INTO customer_contracts (id, customer_id, total_amount, status, signed_at) VALUES
(1, 1, 150000000.00, 'ACTIVE', '2026-08-15 10:00:00'),
(2, 2, 60000000.00, 'ACTIVE', '2026-09-01 14:30:00'),
(3, 7, 180000000.00, 'ACTIVE', '2026-07-20 09:15:00')
ON DUPLICATE KEY UPDATE total_amount = VALUES(total_amount), status = VALUES(status), signed_at = VALUES(signed_at);
