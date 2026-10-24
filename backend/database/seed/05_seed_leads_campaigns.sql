-- ==============================================================================
-- 05_seed_leads_campaigns.sql: CHIẾN DỊCH, LEAD & QUY TẮC TỰ ĐỘNG
-- Hệ thống: NovaCRM | Dành cho môi trường QA / Demo
-- Kịch bản kiểm thử:
--   - Đầy đủ phân loại nhiệt độ Lead: HOT (>=80), WARM (50-79), COLD (<50)
--   - Đầy đủ trạng thái Lead: NEW, PENDING_ASSIGNMENT, CONTACTING, QUALIFIED, CONVERTED, REJECTED
--   - Cam kết phản hồi SLA: Đúng hạn (ON_TIME), Quá hạn (OVERDUE), Cận hạn
--   - Hàng chờ phân bổ tự động (Pending Assignment Queue - S4-06)
--   - Phát hiện & Gộp Lead trùng lặp theo Email/SĐT (Lead Dedup & Merge - S4-04)
--   - Chuyển đổi Lead 3-trong-1 sang Khách hàng, Liên hệ, Cơ hội (Lead Conversion - S4-08)
--   - Quy tắc phân bổ tự động: ROUND_ROBIN, LOAD_BALANCED, DEDICATED (S4-06)
--   - Cấu hình chấm điểm tự động (Lead Scoring Engine - S4-05)
--   - Bộ lọc lưu sẵn cho Lead và Khách hàng (Saved Views / Filters - S3-07, S4-09)
-- ==============================================================================

SET NAMES utf8mb4;

-- 1. Chiến dịch Marketing (campaigns)
INSERT INTO campaigns (id, code, name, type, status, start_date, end_date, budget, actual_cost, expected_revenue, description, owner_id, is_deleted) VALUES
(1, 'CAMP-2026-01', 'Chiến dịch Chuyển đổi số Doanh nghiệp Q4/2026', 'DIGITAL', 'IN_PROGRESS', '2026-10-01', '2026-12-31', 50000000.00, 28000000.00, 350000000.00, 'Thu hút khách hàng khối CNTT và Bán lẻ quan tâm tối ưu bán hàng đa kênh', 3020, 0),
(2, 'CAMP-2026-02', 'Hội thảo Tech Summit Vietnam 2026', 'EVENT', 'COMPLETED', '2026-09-15', '2026-09-20', 80000000.00, 75000000.00, 500000000.00, 'Tài trợ gian hàng và hội thảo chuyên đề CRM cho doanh nghiệp sản xuất và logistics', 3020, 0),
(3, 'CAMP-2026-03', 'Email Marketing Giới thiệu Phiên bản NovaCRM 2.0', 'EMAIL', 'PLANNING', '2026-11-01', '2026-11-30', 15000000.00, 0.00, 120000000.00, 'Chiến dịch nuôi dưỡng khách hàng tiềm năng cũ và cung cấp ưu đãi dùng thử 30 ngày', 3020, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type), status = VALUES(status), budget = VALUES(budget),
    actual_cost = VALUES(actual_cost), expected_revenue = VALUES(expected_revenue), owner_id = VALUES(owner_id), is_deleted = 0;

-- 2. Biểu mẫu thu thập Lead trực tuyến (lead_capture_forms)
INSERT INTO lead_capture_forms (token, owner_user_id, form_name, active) VALUES
('wf-landing-2026-token-0001', 3020, 'Biểu mẫu Đăng ký Tư vấn Chuyển đổi số trên Landing Page', 1),
('wf-demo-2026-token-0002', 3020, 'Biểu mẫu Đăng ký Trải nghiệm Live Demo NovaCRM', 1)
ON DUPLICATE KEY UPDATE form_name = VALUES(form_name), owner_user_id = 3020, active = 1;

-- 3. Quản lý Khách hàng tiềm năng (leads)
INSERT INTO leads (
    id, code, full_name, title, company_name, industry, tax_code, phone, email,
    source, source_name, rating, tier, score, status, assigned_to, assigned_to_user_id,
    estimated_value, sla_deadline, sla_minutes_total, sla_result, notes, region, region_code, is_locked, is_deleted
) VALUES
-- 1. Lead Nóng, QUÁ HẠN SLA 3 giờ cần cảnh báo đỏ (S4-07, S4-09)
(101, 'LEAD-2026-101', 'Trần Quang Khải', 'Tổng Giám đốc (CEO)', 'Công ty Cổ phần Công nghệ Delta Smart', 'CNTT & Viễn thông', '0109988112', '0912111222', 'khai.tq@deltasmart.vn', 'WEBSITE', 'Website Form', 'HOT', 'HOT', 95, 'NEW', 'USR-101', 101, 350000000.00, DATE_SUB(NOW(), INTERVAL 3 HOUR), 120, 'OVERDUE', 'Đăng ký gói Enterprise 150 người dùng qua form website, cần gọi điện ngay.', 'Miền Bắc (Hà Nội)', 'NORTH', 0, 0),

-- 2. Lead Ấm, QUÁ HẠN SLA 90 phút (S4-07)
(102, 'LEAD-2026-102', 'Đoàn Minh Trí', 'Giám đốc Mua sắm & Cung ứng', 'Chuỗi Cửa hàng Tiện lợi VinCommerce', 'Bán lẻ & Chuỗi F&B', '0108777666', '0988333444', 'tri.dm@vincommerce.vn', 'EVENT', 'Hội thảo Chuyển đổi số', 'WARM', 'WARM', 76, 'NEW', 'USR-101', 101, 220000000.00, DATE_SUB(NOW(), INTERVAL 90 MINUTE), 120, 'OVERDUE', 'Gặp tại hội thảo, quan tâm phân hệ Quản lý Hợp đồng.', 'Miền Bắc (Hà Nội)', 'NORTH', 0, 0),

-- 3. Lead Nóng đang chăm sóc (ĐÚNG HẠN SLA)
(103, 'LEAD-2026-103', 'Nguyễn Hoàng Nam', 'Giám đốc Công nghệ (CTO)', 'Công ty Cổ phần Công nghệ Cao Việt Tech', 'CNTT & Viễn thông', '0108923451', '0912345678', 'nam.nh@viettech.com.vn', 'WEB_FORM', 'Chiến dịch Q4', 'HOT', 'HOT', 92, 'CONTACTING', 'USR-101', 101, 250000000.00, DATE_ADD(NOW(), INTERVAL 45 MINUTE), 120, 'ON_TIME', 'Đã gọi điện thoại lần 1, khách yêu cầu gửi tài liệu demo kỹ thuật.', 'Miền Bắc (Hà Nội)', 'NORTH', 0, 0),

-- 4. Lead trong hàng chờ phân bổ tự động (PENDING_ASSIGNMENT - S4-06)
(104, 'LEAD-2026-104', 'Vũ Thị Bích Ngọc', 'Trưởng phòng Kế hoạch Đầu tư', 'Tập đoàn Dược phẩm Quốc tế Mediphar', 'Dược phẩm & Y tế', '0107892341', '0988776655', 'ngoc.vb@mediphar.vn', 'WEBSITE', 'Website Form', 'HOT', 'HOT', 88, 'PENDING_ASSIGNMENT', NULL, NULL, 420000000.00, DATE_ADD(NOW(), INTERVAL 100 MINUTE), 120, 'ON_TIME', 'Chờ thuật toán tự động phân bổ cho chuyên viên Dược phẩm.', 'Miền Bắc (Hà Nội)', 'NORTH', 0, 0),

-- 5. Lead Đủ tiêu chuẩn (Sẵn sàng chuyển đổi 3-trong-1 - S4-08)
(105, 'LEAD-2026-105', 'Phạm Quốc Dũng', 'Phó Tổng Giám đốc Kinh doanh', 'Công ty Cổ phần Địa ốc & Xây dựng Đất Việt', 'Bất động sản & Xây dựng', '0309876543', '0933555777', 'dung.pq@datvietland.vn', 'REFERRAL', 'Giới thiệu / Đối tác', 'HOT', 'HOT', 86, 'QUALIFIED', 'USR-101', 101, 350000000.00, NULL, 120, 'ON_TIME', 'Đã thẩm định nhu cầu đầy đủ, sẵn sàng chuyển đổi sang Khách hàng & Cơ hội.', 'Miền Nam (TP.HCM)', 'SOUTH', 0, 0),

-- 6. Lead Ấm đang liên hệ giao cho Sales Bắc 2 (Team 1)
(106, 'LEAD-2026-106', 'Đinh Tuấn Anh', 'Giám đốc Chuyển đổi số', 'Chuỗi Bán lẻ Thời trang An Phước', 'Bán lẻ & Chuỗi F&B', '0106543219', '0904123987', 'anh.dt@anphuocfashion.com', 'EXCEL', 'Nhập Excel', 'WARM', 'WARM', 79, 'CONTACTING', 'USR-102', 102, 180000000.00, DATE_ADD(NOW(), INTERVAL 80 MINUTE), 120, 'ON_TIME', 'Chuỗi 25 showroom cần CRM tích hợp điểm thưởng Zalo Mini App.', 'Miền Bắc (Hà Nội)', 'NORTH', 0, 0),

-- 7. Lead Lạnh (Điểm thấp - S4-05)
(107, 'LEAD-2026-107', 'Lê Thanh Bình', 'Chủ tịch HĐQT', 'Công ty Cổ phần Vận tải Biển Đông Dương', 'Vận tải & Kho bãi (Logistics)', '0105432198', '0977889900', 'binh.lt@dongduonglog.com', 'PARTNER', 'Đối tác Công nghệ', 'COLD', 'COLD', 45, 'NEW', NULL, NULL, 90000000.00, DATE_SUB(NOW(), INTERVAL 5 HOUR), 120, 'OVERDUE', 'Khách hàng quan tâm theo dõi phương tiện, ngân sách ban đầu còn hạn chế.', 'Miền Bắc (Hải Phòng)', 'NORTH', 0, 0),

-- 8. Lead Bị từ chối (Test S4-07 Lý do từ chối & log)
(108, 'LEAD-2026-108', 'Nguyễn Thị Thu Thủy', 'Trưởng phòng Hành chính Nhân sự', 'Trường Quốc tế Song ngữ Wellspring', 'Giáo dục & Đào tạo', '0104321987', '0915667788', 'thuy.ntt@wellspring.edu.vn', 'COLD_CALL', 'Telesales', 'WARM', 'WARM', 68, 'REJECTED', 'USR-103', 103, 120000000.00, NULL, 120, 'OVERDUE', 'Đã gọi 3 cuộc không người nghe máy; số máy không hoạt động.', 'Miền Bắc (Hà Nội)', 'NORTH', 0, 0),

-- 9. Lead Trùng email và số điện thoại với Lead 101 để test Gộp Lead (Test S4-04)
(109, 'LEAD-2026-109', 'Trần Quang Khải (Đăng ký lại)', 'CEO', 'Delta Smart Corporation', 'CNTT & Viễn thông', '0109988112', '0912111222', 'khai.tq@deltasmart.vn', 'WEB_FORM', 'Landing Page Q4', 'WARM', 'WARM', 75, 'NEW', 'USR-102', 102, 200000000.00, DATE_ADD(NOW(), INTERVAL 60 MINUTE), 120, 'ON_TIME', 'Bản ghi gửi lần 2 cùng email và số điện thoại.', 'Miền Bắc (Hà Nội)', 'NORTH', 0, 0),

-- 10. Lead ĐÃ CHUYỂN ĐỔI THÀNH CÔNG (Test S4-08 Đã chuyển đổi, bị khóa không sửa)
(110, 'LEAD-2026-110', 'Nguyễn Thanh Hải (Đã chuyển)', 'Tổng Giám đốc', 'Tập đoàn Công nghệ Alpha Holdings', 'CNTT & Viễn thông', '0101234567', '0912345678', 'hai.nt@alphaholdings.vn', 'WEBSITE', 'Website Form', 'HOT', 'HOT', 98, 'CONVERTED', 'USR-101', 101, 150000000.00, NULL, 120, 'ON_TIME', 'Đã chuyển đổi thành công sang Khách hàng #1, Liên hệ #1 và Cơ hội #1.', 'Miền Bắc (Hà Nội)', 'NORTH', 1, 0)
ON DUPLICATE KEY UPDATE 
    code = VALUES(code), full_name = VALUES(full_name), title = VALUES(title),
    company_name = VALUES(company_name), industry = VALUES(industry), tax_code = VALUES(tax_code),
    phone = VALUES(phone), email = VALUES(email), source = VALUES(source), source_name = VALUES(source_name),
    rating = VALUES(rating), tier = VALUES(tier), score = VALUES(score), status = VALUES(status),
    assigned_to = VALUES(assigned_to), assigned_to_user_id = VALUES(assigned_to_user_id),
    estimated_value = VALUES(estimated_value), sla_deadline = VALUES(sla_deadline),
    sla_result = VALUES(sla_result), notes = VALUES(notes), region = VALUES(region),
    region_code = VALUES(region_code), is_locked = VALUES(is_locked), is_deleted = 0;

-- Cập nhật thông tin chi tiết chuyển đổi cho Lead 110
UPDATE leads SET 
    converted_at = '2026-08-15 09:30:00',
    converted_by = 101,
    converted_customer_id = 1,
    converted_contact_id = 1,
    converted_opportunity_id = 1,
    is_locked = 1
WHERE id = 110;

-- Cập nhật lý do từ chối cho Lead 108
UPDATE leads SET 
    rejected_at = DATE_SUB(NOW(), INTERVAL 1 DAY),
    reject_reason = 'INVALID_PHONE',
    reject_details = 'Đã gọi 3 cuộc không người nghe máy; kiểm tra số Zalo không tồn tại.'
WHERE id = 108;

-- 4. Liên kết Chiến dịch - Lead (campaign_leads)
INSERT IGNORE INTO campaign_leads (campaign_id, lead_id, status, notes) VALUES
(1, 101, 'CLICKED', 'Khách nhấn link từ quảng cáo Google Ads'),
(1, 103, 'RESPONDED', 'Khách điền biểu mẫu tư vấn giải pháp'),
(1, 109, 'SENT', 'Gửi email xác nhận đăng ký lại'),
(2, 102, 'RESPONDED', 'Khách ghé thăm gian hàng Tech Summit và quét QR'),
(2, 107, 'ATTENDED', 'Tham dự phiên hội thảo chuyên đề ERP/CRM');

-- 5. Quy tắc phân bổ Lead tự động (lead_assignment_rules - S4-06)
INSERT INTO lead_assignment_rules (id, rule_code, name, priority_order, region, industry, mechanism, reps, description, is_active, total_assigned, last_assigned_index) VALUES
(1, 'RULE-01', 'Phân bổ Khách hàng Bất động sản Miền Bắc', 1, 'NORTH', 'Bất động sản', 'ROUND_ROBIN', '["USR-101", "USR-102"]', 'Xoay vòng đều giữa chuyên viên am hiểu thị trường địa ốc Miền Bắc.', 1, 38, 0),
(2, 'RULE-02', 'Phân bổ Lead Công nghệ thông tin & Viễn thông', 2, 'NATIONWIDE', 'CNTT', 'ROUND_ROBIN', '["USR-101", "USR-104"]', 'Lead phần mềm, API tích hợp và ERP giao cho đội ngũ am hiểu IT.', 1, 42, 1),
(3, 'RULE-03', 'Điều phối Thị trường Miền Nam (Round-Robin)', 3, 'SOUTH', 'ALL', 'ROUND_ROBIN', '["USR-104"]', 'Tất cả khách hàng tiềm năng tại TP.HCM và các tỉnh Nam Bộ.', 1, 35, 0),
(4, 'RULE-04', 'Điều phối Thị trường Miền Trung (Đà Nẵng & lân cận)', 4, 'CENTRAL', 'ALL', 'DEDICATED', '["USR-103"]', 'Chỉ định trực tiếp đại diện chi nhánh miền Trung phụ trách.', 1, 19, 0),
(5, 'RULE-05', 'Khách hàng Doanh nghiệp Toàn quốc - Cân bằng tải', 5, 'NATIONWIDE', 'ALL', 'LOAD_BALANCED', '["USR-101", "USR-102", "USR-104"]', 'Tự động phân cho nhân viên đang có số Lead thấp nhất để giảm quá tải.', 1, 26, 0)
ON DUPLICATE KEY UPDATE 
    rule_code = VALUES(rule_code), name = VALUES(name), priority_order = VALUES(priority_order),
    region = VALUES(region), industry = VALUES(industry), mechanism = VALUES(mechanism),
    reps = VALUES(reps), description = VALUES(description), is_active = 1;

-- 6. Cấu hình chấm điểm Lead (lead_scoring_config - S4-05)
INSERT INTO lead_scoring_config (id, version, hot, warm, aggregation, min_score, max_score, rules_json, updated_by, updated_at) VALUES
(1, 1, 80, 50, 'SUM_ALL', 0, 100, '[
    {"id": "RULE-IND-01", "group": "INDUSTRY", "name": "Ngành CNTT & Viễn thông", "field": "industry", "operator": "CONTAINS", "value": "CNTT", "points": 25, "description": "Doanh nghiệp công nghệ", "isActive": true},
    {"id": "RULE-IND-02", "group": "INDUSTRY", "name": "Ngành Bán lẻ & Chuỗi F&B", "field": "industry", "operator": "CONTAINS", "value": "Bán lẻ", "points": 20, "description": "Doanh nghiệp bán lẻ", "isActive": true},
    {"id": "RULE-SIZE-01", "group": "COMPANY_SIZE", "name": "Quy mô lớn trên 100 nhân sự", "field": "employeeCount", "operator": "GREATER_THAN", "value": "100", "points": 30, "description": "Khách hàng Enterprise", "isActive": true},
    {"id": "RULE-SRC-01", "group": "SOURCE", "name": "Nguồn Website Form", "field": "source", "operator": "CONTAINS", "value": "WEB", "points": 20, "description": "Lead chủ động đăng ký", "isActive": true},
    {"id": "RULE-TITLE-01", "group": "CUSTOM", "name": "Lãnh đạo cấp cao CEO/CTO", "field": "title", "operator": "CONTAINS", "value": "Giám đốc", "points": 25, "description": "Người ra quyết định", "isActive": true}
]', 1, NOW())
ON DUPLICATE KEY UPDATE 
    version = VALUES(version), hot = 80, warm = 50, aggregation = 'SUM_ALL', min_score = 0, max_score = 100,
    rules_json = VALUES(rules_json), updated_by = 1, updated_at = NOW();

-- 7. Bộ lọc lưu sẵn cho Lead và Khách hàng (saved_lead_filters & saved_customer_filters)
INSERT INTO saved_lead_filters (id, user_id, name, filter_criteria, is_default) VALUES
(1, 10, '🔥 Lead VIP Cần Chăm Sóc Gấp', '{"keyword":"","status":"NEW","tier":"HOT","source":"ALL","owner":"ALL","dateRange":"ALL","slaOverdueOnly":false,"sort":"SLA_URGENT"}', 1),
(2, 10, '🌐 Lead Nguồn Website Tháng Này', '{"keyword":"","status":"ALL","tier":"ALL","source":"WEBSITE","owner":"ALL","dateRange":"THIS_MONTH","slaOverdueOnly":false,"sort":"NEWEST"}', 0),
(3, 101, '🎯 Lead Nóng Được Giao Của Tôi', '{"keyword":"","status":"CONTACTING","tier":"HOT","source":"ALL","owner":"SELF","dateRange":"ALL","slaOverdueOnly":false,"sort":"SCORE_DESC"}', 1),
(4, 101, '⚠️ Lead Quá Hạn Phản Hồi SLA', '{"keyword":"","status":"NEW","tier":"ALL","source":"ALL","owner":"SELF","dateRange":"ALL","slaOverdueOnly":true,"sort":"SLA_URGENT"}', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), filter_criteria = VALUES(filter_criteria), is_default = VALUES(is_default);

INSERT INTO saved_customer_filters (id, user_id, name, criteria) VALUES
(1, 10, 'Khách hàng Doanh nghiệp lớn Miền Bắc', '{"region":"MB","company_size":"ENTERPRISE","status":"KHACH_HANG"}'),
(2, 10, 'Khách hàng có cờ cảnh báo Rủi ro', '{"support_risk":true}'),
(3, 101, 'Danh mục khách hàng phụ trách của tôi', '{"owner":"SELF"}')
ON DUPLICATE KEY UPDATE name = VALUES(name), criteria = VALUES(criteria);
