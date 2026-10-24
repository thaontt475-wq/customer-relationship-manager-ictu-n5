-- ==============================================================================
-- 01_seed_master_data.sql: DANH MỤC DÙNG CHUNG, PIPELINE & CẤU HÌNH CƠ BẢN
-- Hệ thống: NovaCRM | Dành cho môi trường QA / Demo
-- An toàn: Hoàn toàn lặp lại (idempotent), không ghi đè dữ liệu tuỳ biến khác
-- ==============================================================================

SET NAMES utf8mb4;

-- 1. Khu vực địa lý (region)
INSERT INTO master_data (id, type, code, name, active, display_order) VALUES
(3, 'region', 'MB', 'Miền Bắc', 1, 1),
(4, 'region', 'MT', 'Miền Trung', 1, 2),
(5, 'region', 'MN', 'Miền Nam', 1, 3),
(6, 'region', 'TN', 'Tây Nguyên', 1, 4),
(7, 'region', 'MDB', 'Miền Đông Nam Bộ', 1, 5),
(8, 'region', 'MTW', 'Đồng bằng Sông Cửu Long (Miền Tây)', 1, 6)
ON DUPLICATE KEY UPDATE name = VALUES(name), active = 1;

-- 2. Ngành nghề kinh doanh (industry)
INSERT INTO master_data (id, type, code, name, active, display_order) VALUES
(100, 'industry', 'CNTT', 'Công nghệ thông tin & Viễn thông', 1, 1),
(101, 'industry', 'RETAIL', 'Bán lẻ & Thương mại điện tử', 1, 2),
(102, 'industry', 'MANUFACTURING', 'Sản xuất & Chế tạo cơ khí', 1, 3),
(103, 'industry', 'FINANCE', 'Tài chính / Ngân hàng / Bảo hiểm', 1, 4),
(104, 'industry', 'REAL_ESTATE', 'Bất động sản & Xây dựng', 1, 5),
(105, 'industry', 'LOGISTICS', 'Vận tải & Kho bãi (Logistics)', 1, 6),
(106, 'industry', 'HEALTHCARE', 'Y tế & Dược phẩm', 1, 7),
(107, 'industry', 'EDUCATION', 'Giáo dục & Đào tạo', 1, 8),
(108, 'industry', 'FNB', 'Thực phẩm & Đồ uống (F&B)', 1, 9),
(109, 'industry', 'SERVICES', 'Dịch vụ & Du lịch', 1, 10),
(110, 'industry', 'OTHER', 'Ngành nghề khác', 1, 11)
ON DUPLICATE KEY UPDATE name = VALUES(name), active = 1;

-- 3. Quy mô doanh nghiệp (company-size)
INSERT INTO master_data (id, type, code, name, active, display_order) VALUES
(120, 'company-size', 'MICRO', 'Dưới 10 nhân sự (Siêu nhỏ)', 1, 1),
(121, 'company-size', 'SMALL', '10 - 50 nhân sự (Nhỏ)', 1, 2),
(122, 'company-size', 'MEDIUM', '50 - 200 nhân sự (Vừa)', 1, 3),
(123, 'company-size', 'LARGE', '200 - 500 nhân sự (Lớn)', 1, 4),
(124, 'company-size', 'ENTERPRISE', 'Trên 500 nhân sự (Tập đoàn)', 1, 5)
ON DUPLICATE KEY UPDATE name = VALUES(name), active = 1;

-- 4. Nguồn Lead (lead-source)
INSERT INTO master_data (id, type, code, name, active, display_order) VALUES
(130, 'lead-source', 'WEBSITE', 'Website / Đăng ký trực tuyến', 1, 1),
(131, 'lead-source', 'WEB_FORM', 'Biểu mẫu nhúng Landing Page', 1, 2),
(132, 'lead-source', 'REFERRAL', 'Giới thiệu từ đối tác / khách hàng', 1, 3),
(133, 'lead-source', 'COLD_CALL', 'Telesales / Tiếp cận trực tiếp', 1, 4),
(134, 'lead-source', 'SOCIAL_MEDIA', 'Mạng xã hội (Facebook, LinkedIn)', 1, 5),
(135, 'lead-source', 'EVENT', 'Hội thảo / Sự kiện / Triển lãm', 1, 6),
(136, 'lead-source', 'PARTNER', 'Kênh đối tác phân phối', 1, 7),
(137, 'lead-source', 'OTHER', 'Nguồn khác', 1, 8)
ON DUPLICATE KEY UPDATE name = VALUES(name), active = 1;

-- 5. Loại hoạt động chăm sóc (activity-type)
INSERT INTO master_data (id, type, code, name, active, display_order) VALUES
(140, 'activity-type', 'CALL', 'Cuộc gọi điện thoại', 1, 1),
(141, 'activity-type', 'MEETING', 'Gặp mặt trực tiếp', 1, 2),
(142, 'activity-type', 'EMAIL', 'Gửi thư điện tử (Email)', 1, 3),
(143, 'activity-type', 'DEMO', 'Demo giới thiệu sản phẩm', 1, 4),
(144, 'activity-type', 'NOTE', 'Ghi chú nội bộ', 1, 5)
ON DUPLICATE KEY UPDATE name = VALUES(name), active = 1;

-- 6. Giai đoạn Pipeline cơ hội (pipeline_stages)
INSERT INTO pipeline_stages (id, pipeline_id, name, order_no, probability, exit_condition, condition_required, active) VALUES
(1, 1, 'Tiếp cận', 1, 10, 'Phải liên hệ được đầu mối chính', 1, 1),
(2, 1, 'Xác định nhu cầu', 2, 25, 'Hoàn thành bản ghi nhận yêu cầu', 1, 1),
(3, 1, 'Đề xuất giải pháp', 3, 50, 'Đã gửi tài liệu giải pháp hoặc demo', 1, 1),
(4, 1, 'Báo giá', 4, 70, 'Đã gửi báo giá chính thức có hiệu lực', 1, 1),
(5, 1, 'Đàm phán & Thương thảo', 5, 85, 'Đã chốt điều khoản hợp đồng & giá cuối', 1, 1),
(6, 1, 'Chốt thành công (Won)', 6, 100, 'Hợp đồng đã ký kết đầy đủ hai bên', 1, 1),
(7, 1, 'Đóng thất bại (Lost)', 7, 0, 'Phải ghi rõ lý do thua và đối thủ (nếu có)', 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), order_no = VALUES(order_no), probability = VALUES(probability),
    exit_condition = VALUES(exit_condition), condition_required = VALUES(condition_required), active = 1;

-- 7. Lý do Thắng/Thua cơ hội (win_loss_reasons)
INSERT INTO win_loss_reasons (id, type, name, active) VALUES
(1, 'WON', 'Giá cả cạnh tranh và chính sách thanh toán linh hoạt', 1),
(2, 'WON', 'Tính năng đáp ứng sát với quy trình nghiệp vụ thực tế', 1),
(3, 'WON', 'Dịch vụ chăm sóc và hỗ trợ kỹ thuật tận tâm 24/7', 1),
(4, 'WON', 'Thương hiệu uy tín và năng lực triển khai đã được chứng minh', 1),
(5, 'LOST', 'Giá vượt quá mức ngân sách được duyệt', 1),
(6, 'LOST', 'Thiếu một số tính năng tùy biến đặc thù theo yêu cầu', 1),
(7, 'LOST', 'Đối thủ cạnh tranh chào giá và chiết khấu cao hơn', 1),
(8, 'LOST', 'Khách hàng hoãn hoặc hủy dự án do thay đổi kế hoạch nội bộ', 1)
ON DUPLICATE KEY UPDATE type = VALUES(type), name = VALUES(name), active = 1;

-- 8. Đối thủ cạnh tranh (competitors)
INSERT INTO competitors (id, name, note, active) VALUES
(1, 'Base CRM', 'Nền tảng quản trị mở rộng, quy trình linh hoạt nhưng chi phí tăng cao theo user', 1),
(2, 'MISA AMIS CRM', 'Hệ sinh thái tài chính kế toán đồng bộ nhưng ít tùy biến sâu cho quy trình riêng', 1),
(3, 'Salesforce CRM', 'Chuẩn mực quốc tế chuyên sâu cho tập đoàn lớn nhưng chi phí bản quyền rất cao', 1),
(4, 'Bravo Software', 'Giải pháp ERP/CRM may đo cho khối sản xuất cơ khí và nhà máy', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), note = VALUES(note), active = 1;

-- 9. Trường tùy chỉnh (custom_fields)
INSERT INTO custom_fields (id, entity_type, field_key, label, field_type, options_json, required_field, active) VALUES
(1, 'CUSTOMER', 'cf_budget_vnd', 'Ngân sách CNTT hàng năm (VND)', 'NUMBER', NULL, 0, 1),
(2, 'CUSTOMER', 'cf_erp_system', 'Hệ thống ERP đang sử dụng', 'TEXT', NULL, 0, 1),
(3, 'CUSTOMER', 'cf_deployment_model', 'Mô hình triển khai mong muốn', 'SELECT', '["Cloud SaaS", "On-Premise", "Hybrid"]', 0, 1),
(4, 'OPPORTUNITY', 'cf_decision_timeframe', 'Khung thời gian ra quyết định', 'SELECT', '["Trong 1 tháng", "1-3 tháng", "Trên 3 tháng"]', 0, 1)
ON DUPLICATE KEY UPDATE label = VALUES(label), field_type = VALUES(field_type), options_json = VALUES(options_json), required_field = VALUES(required_field), active = 1;
