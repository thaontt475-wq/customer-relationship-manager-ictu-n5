-- ==============================================================================
-- 07_verify_seed.sql: BÁO CÁO KIỂM TRA TÍNH TOÀN VẸN & SỐ LƯỢNG SEED DATA
-- Hệ thống: NovaCRM | Dành cho môi trường QA / Demo
-- ==============================================================================

SET NAMES utf8mb4;

SELECT '==================================================' AS '---';
SELECT '         NOVACRM SEED DATA VERIFICATION           ' AS '---';
SELECT '==================================================' AS '---';

-- 1. Bảng số lượng bản ghi chính
SELECT 'TÀI KHOẢN & PHÂN QUYỀN' AS Category, 'users' AS TableName, COUNT(*) AS TotalRows,
       SUM(CASE WHEN password_hash LIKE '$2a$%' OR password_hash LIKE '$2b$%' THEN 1 ELSE 0 END) AS ValidBcryptPasswords
FROM users WHERE id >= 10
UNION ALL
SELECT 'TÀI KHOẢN & PHÂN QUYỀN', 'roles', COUNT(*), NULL FROM roles
UNION ALL
SELECT 'TÀI KHOẢN & PHÂN QUYỀN', 'permissions', COUNT(*), NULL FROM permissions
UNION ALL
SELECT 'TÀI KHOẢN & PHÂN QUYỀN', 'teams', COUNT(*), NULL FROM teams
UNION ALL
SELECT 'KHÁCH HÀNG & LIÊN HỆ', 'customers', COUNT(*), NULL FROM customers WHERE is_deleted = 0
UNION ALL
SELECT 'KHÁCH HÀNG & LIÊN HỆ', 'contacts', COUNT(*), NULL FROM contacts WHERE is_deleted = 0
UNION ALL
SELECT 'KHÁCH HÀNG & LIÊN HỆ', 'custom_fields', COUNT(*), NULL FROM custom_fields
UNION ALL
SELECT 'KINH DOANH & CPQ', 'products', COUNT(*), NULL FROM products WHERE active = 1
UNION ALL
SELECT 'KINH DOANH & CPQ', 'opportunities', COUNT(*), NULL FROM opportunities WHERE is_deleted = 0
UNION ALL
SELECT 'KINH DOANH & CPQ', 'quotes', COUNT(*), NULL FROM quotes
UNION ALL
SELECT 'KINH DOANH & CPQ', 'contracts', COUNT(*), NULL FROM contracts
UNION ALL
SELECT 'MARKETING & LEADS', 'campaigns', COUNT(*), NULL FROM campaigns
UNION ALL
SELECT 'MARKETING & LEADS', 'lead_capture_forms', COUNT(*), NULL FROM lead_capture_forms
UNION ALL
SELECT 'MARKETING & LEADS', 'leads', COUNT(*), NULL FROM leads WHERE is_deleted = 0
UNION ALL
SELECT 'MARKETING & LEADS', 'lead_assignment_rules', COUNT(*), NULL FROM lead_assignment_rules
UNION ALL
SELECT 'CHĂM SÓC & TƯƠNG TÁC', 'activities', COUNT(*), NULL FROM activities WHERE is_deleted = 0
UNION ALL
SELECT 'CHĂM SÓC & TƯƠNG TÁC', 'support_requests', COUNT(*), NULL FROM support_requests WHERE is_deleted = 0
UNION ALL
SELECT 'CHĂM SÓC & TƯƠNG TÁC', 'customer_attachments', COUNT(*), NULL FROM customer_attachments WHERE is_deleted = 0
UNION ALL
SELECT 'CHĂM SÓC & TƯƠNG TÁC', 'notifications', COUNT(*), NULL FROM notifications;

-- 2. Kiểm tra các kịch bản kiểm thử nghiệp vụ đặc thù
SELECT 'KỊCH BẢN ĐẶC THÙ' AS 'Kiem_Tra',
       'Cờ rủi ro Churn (S3-08)' AS 'Kich_Ban',
       COUNT(*) AS 'So_Luong_Phu_Hop',
       'Yêu cầu: Có khách hàng có cờ support_risk_active=1' AS 'Mo_Ta'
FROM customers WHERE support_risk_active = 1
UNION ALL
SELECT 'KỊCH BẢN ĐẶC THÙ',
       'Phân cấp Mẹ - Con (S3-05)',
       COUNT(*),
       'Yêu cầu: Có khách hàng có parent_customer_id'
FROM customers WHERE parent_customer_id IS NOT NULL
UNION ALL
SELECT 'KỊCH BẢN ĐẶC THÙ',
       'Cặp Khách trùng lặp (S3-04)',
       COUNT(*),
       'Yêu cầu: 2 khách có cùng website để test gộp'
FROM customers WHERE website = 'https://achau-logistics.vn'
UNION ALL
SELECT 'KỊCH BẢN ĐẶC THÙ',
       'Cặp Lead trùng lặp (S4-04)',
       COUNT(*),
       'Yêu cầu: 2 leads có cùng email/phone'
FROM leads WHERE email = 'khai.tq@deltasmart.vn'
UNION ALL
SELECT 'KỊCH BẢN ĐẶC THÙ',
       'Lead quá hạn SLA (S4-07)',
       COUNT(*),
       'Yêu cầu: Lead có sla_result=OVERDUE'
FROM leads WHERE sla_result = 'OVERDUE'
UNION ALL
SELECT 'KỊCH BẢN ĐẶC THÙ',
       'Lead đã chuyển đổi 3-trong-1 (S4-08)',
       COUNT(*),
       'Yêu cầu: Lead status=CONVERTED có customer/contact/opportunity'
FROM leads WHERE status = 'CONVERTED';

-- 3. Kiểm tra tính toàn vẹn tham chiếu (Không có bản ghi mồ côi)
SELECT 'KIỂM TRA LIÊN KẾT (ORPHAN CHECK)' AS CheckType,
       'contacts -> customers' AS Relation,
       COUNT(*) AS OrphanCount
FROM contacts c LEFT JOIN customers cu ON c.customer_id = cu.id
WHERE cu.id IS NULL
UNION ALL
SELECT 'KIỂM TRA LIÊN KẾT (ORPHAN CHECK)',
       'opportunities -> customers',
       COUNT(*)
FROM opportunities o LEFT JOIN customers cu ON o.customer_id = cu.id
WHERE cu.id IS NULL
UNION ALL
SELECT 'KIỂM TRA LIÊN KẾT (ORPHAN CHECK)',
       'activities -> users',
       COUNT(*)
FROM activities a LEFT JOIN users u ON a.owner_user_id = u.id
WHERE u.id IS NULL
UNION ALL
SELECT 'KIỂM TRA LIÊN KẾT (ORPHAN CHECK)',
       'support_requests -> customers',
       COUNT(*)
FROM support_requests sr LEFT JOIN customers cu ON sr.customer_id = cu.id
WHERE cu.id IS NULL;

-- 4. Danh sách tài khoản mẫu để đăng nhập Demo
SELECT u.id, u.email AS login_email, u.full_name, r.name AS role_name, t.name AS team_name, u.data_scope, 'Password123@' AS demo_password
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
LEFT JOIN teams t ON u.team_id = t.id
WHERE u.id IN (1, 10, 11, 21, 22, 101, 102, 103, 104, 3020, 4010, 5010, 9001)
ORDER BY u.id;
