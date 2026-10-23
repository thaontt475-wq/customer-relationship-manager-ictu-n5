-- ==============================================================================
-- 06_seed_activities.sql: HOẠT ĐỘNG TƯƠNG TÁC, SUPPORT TICKETS & DÒNG THỜI GIAN
-- Hệ thống: NovaCRM | Dành cho môi trường QA / Demo
-- Kịch bản kiểm thử:
--   - Dòng thời gian tương tác (Timeline): Cuộc gọi, Gặp gỡ, Email, Nhiệm vụ
--   - Hỗ trợ khách hàng (Support Requests / Tickets) cho S3-08 (Cờ rủi ro Churn)
--   - Tương tác quá hạn chăm sóc > 30 ngày (Periodic Customer Care S3-09)
--   - Tệp đính kèm khách hàng (Customer Attachments)
--   - Thông báo hệ thống (Notifications) cảnh báo rủi ro & phân bổ
--   - Lịch sử chuyển đổi Lead (Lead Conversion History)
-- ==============================================================================

SET NAMES utf8mb4;

-- 1. Yêu cầu hỗ trợ kỹ thuật (support_requests)
-- Khách 3 (SmartMart) có 2 ticket chưa giải quyết -> Kích hoạt cờ rủi ro support_risk_active = 1
INSERT INTO support_requests (
    id, customer_id, title, description, priority, assignee_user_id, status, created_by, updated_by, created_at, updated_at, is_deleted
) VALUES
-- Khách 3: Ticket khẩn cấp nghẽn đồng bộ POS
(1, 3, 'Lỗi đồng bộ dữ liệu giao dịch POS vào giờ cao điểm', 'Hệ thống POS tại 12 cửa hàng bị ngắt kết nối API định kỳ lúc 18h-20h, giao dịch phải ghi nhận offline.', 'URGENT', 105, 'NEW', 101, 105, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 0),

-- Khách 3: Ticket hiệu năng báo cáo tồn kho
(2, 3, 'Chậm tải báo cáo đối soát công nợ chi nhánh', 'Xuất báo cáo Excel tồn kho trên 50.000 SKU bị timeout 504. Yêu cầu tối ưu query hoặc export bất đồng bộ.', 'HIGH', 105, 'PROCESSING', 101, 105, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 0),

-- Khách 1 (Alpha Holdings): Ticket đã giải quyết tốt
(3, 1, 'Hỗ trợ cấp chứng thư SSL bổ sung cho subdomain portal', 'Cần cài đặt SSL Wildcard cho cổng thông tin nội bộ portal.alphaholdings.vn.', 'LOW', 105, 'RESOLVED', 101, 105, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY), 0)
ON DUPLICATE KEY UPDATE
    title = VALUES(title), description = VALUES(description), priority = VALUES(priority),
    assignee_user_id = VALUES(assignee_user_id), status = VALUES(status), updated_at = VALUES(updated_at);

-- 2. Hoạt động tương tác khách hàng & Lead (activities)
INSERT INTO activities (
    id, subject, type, description, status, due_date, customer_id, opportunity_id, lead_id, owner_user_id, is_deleted, created_at
) VALUES
-- Khách 1 (Alpha Holdings): Dòng thời gian tương tác gần đây
(1, 'Họp kick-off triển khai NovaCRM Enterprise', 'MEETING', 'Họp trực tiếp tại văn phòng Alpha Holdings với CEO và CTO. Thống nhất milestone giai đoạn 1.', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 5 DAY), 1, 1, NULL, 101, 0, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2, 'Cuộc gọi kiểm tra tiến độ tích hợp LDAP/SSO', 'CALL', 'Trao đổi qua điện thoại với Giám đốc Công nghệ Lê Thị Bích Ngọc về cấu hình ADFS.', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 2 DAY), 1, 1, NULL, 101, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 'Lên lịch đào tạo người dùng cuối đợt 1', 'TASK', 'Đào tạo 40 nhân sự phòng Vận hành và Chăm sóc khách hàng.', 'PENDING', DATE_ADD(NOW(), INTERVAL 3 DAY), 1, 1, NULL, 101, 0, NOW()),

-- Khách 3 (SmartMart): Cuộc họp xử lý sự cố hỗ trợ
(4, 'Họp khẩn cấp với Ban điều hành SmartMart về sự cố POS', 'MEETING', 'Sales Lead và CSKH Lead cùng họp cam kết SLA khắc phục lỗi đồng bộ API POS.', 'PENDING', DATE_ADD(NOW(), INTERVAL 1 DAY), 3, NULL, NULL, 101, 0, NOW()),

-- Khách 6 (Đông Á Pharma): Demo trực tuyến giải pháp phân phối
(5, 'Demo trực tuyến tính năng Quản lý Đại lý phân phối', 'MEETING', 'Trình diễn luồng đặt hàng B2B qua mobile web cho Trưởng phòng Kinh doanh.', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 3 DAY), 6, 4, NULL, 102, 0, DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- Khách 7 (Cơ khí Miền Nam): Tương tác cũ cách đây 45 ngày (Phục vụ Test S3-09 Cảnh báo Chăm sóc định kỳ)
(6, 'Gặp mặt định kỳ quý 2 đánh giá hợp đồng bảo trì', 'MEETING', 'Trao đổi về tình hình vận hành xưởng dập khuôn. Khách hàng hài lòng, chưa có phát sinh mới.', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 45 DAY), 7, 5, NULL, 104, 0, DATE_SUB(NOW(), INTERVAL 45 DAY)),

-- Lead 101 (Trần Quang Khải): Hoạt động tư vấn sơ bộ
(7, 'Gọi điện tư vấn sau khi nhận form đăng ký Cloud ERP', 'CALL', 'Khách hàng có nhu cầu triển khai cho 80 người dùng trong tháng tới. Đã hẹn gửi báo giá sơ bộ.', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, 101, 101, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- Lead 102 (Đoàn Minh Trí): Nhiệm vụ phân bổ SLA
(8, 'Liên hệ trong vòng 2 giờ theo cam kết SLA', 'TASK', 'Khách hàng quan tâm gói HRM. Cần gọi xác nhận nhu cầu cụ thể.', 'PENDING', DATE_ADD(NOW(), INTERVAL 1 HOUR), NULL, NULL, 102, 101, 0, NOW())
ON DUPLICATE KEY UPDATE
    subject = VALUES(subject), type = VALUES(type), description = VALUES(description),
    status = VALUES(status), due_date = VALUES(due_date), updated_at = NOW();

-- 3. Tệp tài liệu đính kèm (customer_attachments)
INSERT INTO customer_attachments (
    id, customer_id, file_name, file_url, uploaded_at, is_deleted
) VALUES
(1, 1, 'Ho_so_nang_luc_Alpha_Holdings.pdf', '/attachments/customers/1/Ho_so_nang_luc_Alpha_Holdings.pdf', DATE_SUB(NOW(), INTERVAL 10 DAY), 0),
(2, 1, 'Thoa_thuan_bao_mat_NDA_signed.pdf', '/attachments/customers/1/Thoa_thuan_bao_mat_NDA_signed.pdf', DATE_SUB(NOW(), INTERVAL 10 DAY), 0),
(3, 7, 'Bien_ban_nghiem_thu_Co_Khi_MN_2026.pdf', '/attachments/customers/7/Bien_ban_nghiem_thu_Co_Khi_MN_2026.pdf', DATE_SUB(NOW(), INTERVAL 40 DAY), 0)
ON DUPLICATE KEY UPDATE
    file_name = VALUES(file_name), file_url = VALUES(file_url), is_deleted = 0;

-- 4. Thông báo hệ thống (notifications)
INSERT INTO notifications (
    id, user_id, customer_id, type, title, message, event_key, created_at, read_at
) VALUES
-- Cảnh báo rủi ro support ticket cho Sales phụ trách (User 101)
(1, 101, 3, 'SUPPORT_RISK_ALERT', 'Cảnh báo rủi ro Churn: Khách hàng SmartMart có ticket khẩn cấp', 'SmartMart phát sinh ticket URGENT #1 chưa xử lý quá 24h. Vui lòng liên hệ hỗ trợ ngay.', 'RISK_TICKET_3_1', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),

-- Cảnh báo khách hàng chưa tương tác > 30 ngày cho Sales Miền Nam (User 104)
(2, 104, 7, 'CARE_SCHEDULE_ALERT', 'Nhắc nhở chăm sóc định kỳ: Cơ khí Miền Nam', 'Khách hàng có hợp đồng đang hiệu lực nhưng chưa có tương tác trong 45 ngày.', 'CARE_DUE_7_45', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL)
ON DUPLICATE KEY UPDATE
    title = VALUES(title), message = VALUES(message), read_at = VALUES(read_at);

-- 5. Lịch sử chuyển đổi Lead sang Khách hàng (lead_conversion_history)
INSERT INTO lead_conversion_history (
    id, lead_id, customer_id, contact_id, opportunity_id, converted_by, converted_at, customer_mode, conversion_payload, lead_snapshot
) VALUES
(1, 110, 1, 1, 1, 101, '2026-08-15 09:30:00', 'NEW_CUSTOMER',
 '{"source":"WEBSITE","notes":"Đã chuyển đổi thành công sang Khách hàng Alpha Holdings #1, Liên hệ #1 và Cơ hội #1"}',
 '{"id":110,"name":"Nguyễn Thanh Hải","company":"Tập đoàn Công nghệ Alpha Holdings","email":"hai.nt@alphaholdings.vn","phone":"0912345678","score":98,"status":"CONVERTED"}')
ON DUPLICATE KEY UPDATE
    customer_id = VALUES(customer_id), contact_id = VALUES(contact_id), opportunity_id = VALUES(opportunity_id),
    lead_snapshot = VALUES(lead_snapshot);
