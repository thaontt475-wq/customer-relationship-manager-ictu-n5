-- ==============================================================================
-- 02_seed_users_roles.sql: VAI TRÒ, PHÂN QUYỀN, CƠ CẤU NHÓM & TÀI KHOẢN NGƯỜI DÙNG
-- Hệ thống: NovaCRM | Dành cho môi trường QA / Demo
-- Mật khẩu chung cho tất cả tài khoản demo: Password123@
-- Hash BCrypt: $2a$12$QVtdE0bBS7/VyRzWFfSYEeUkBdjUB44dqj/hMuc0xbfWeZlrKSvuK
-- ==============================================================================

SET NAMES utf8mb4;

-- 1. Chuẩn hóa Vai trò (roles)
INSERT INTO roles (id, code, name) VALUES
(1, 'ADMIN', 'Quản trị viên'),
(4, 'DIRECTOR', 'Giám đốc kinh doanh'),
(5, 'TEAM_LEAD', 'Trưởng nhóm kinh doanh'),
(6, 'SALES_REP', 'Nhân viên kinh doanh'),
(7, 'MARKETING', 'Nhân viên Marketing'),
(8, 'CUSTOMER_SUCCESS', 'Chăm sóc khách hàng'),
(9, 'ACCOUNTANT', 'Kế toán')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2. Chuẩn hóa Danh mục Quyền hạn (permissions)
INSERT IGNORE INTO permissions (code, name) VALUES
('customer.read', 'Xem khách hàng'), ('customer.create', 'Tạo khách hàng'),
('customer.update', 'Sửa khách hàng'), ('customer.delete', 'Xóa khách hàng'),
('customer.export', 'Xuất khách hàng'), ('customer.merge', 'Gộp khách hàng'),
('contact.read', 'Xem liên hệ'), ('contact.create', 'Tạo liên hệ'),
('contact.update', 'Sửa liên hệ'), ('contact.delete', 'Xóa liên hệ'),
('lead.read', 'Xem lead'), ('lead.create', 'Tạo lead'),
('lead.update', 'Sửa lead'), ('lead.delete', 'Xóa lead'),
('lead.assign', 'Phân bổ lead'), ('lead.convert', 'Chuyển đổi lead'),
('lead.score', 'Chấm điểm lead'), ('lead.scoring.manage', 'Cấu hình scoring'),
('lead.assignment_rules', 'Cấu hình quy tắc phân bổ'), ('lead.accept', 'Tiếp nhận lead'),
('lead.reject', 'Từ chối lead'), ('lead.sla_monitor', 'Giám sát SLA lead'),
('opportunity.read', 'Xem cơ hội'), ('opportunity.create', 'Tạo cơ hội'),
('opportunity.update', 'Sửa cơ hội'), ('opportunity.close', 'Đóng cơ hội'),
('opportunity.export', 'Xuất cơ hội'),
('quote.read', 'Xem báo giá'), ('quote.create', 'Tạo báo giá'),
('quote.update', 'Sửa báo giá'), ('quote.approve', 'Duyệt báo giá'),
('quote.export', 'Xuất báo giá'),
('contract.read', 'Xem hợp đồng'), ('contract.create', 'Tạo hợp đồng'),
('contract.update', 'Sửa hợp đồng'), ('contract.delete', 'Xóa hợp đồng'),
('activity.read', 'Xem hoạt động'), ('activity.create', 'Tạo hoạt động'),
('activity.update', 'Sửa hoạt động'), ('activity.export', 'Xuất hoạt động'),
('support.read', 'Xem yêu cầu hỗ trợ'), ('support.create', 'Tạo yêu cầu hỗ trợ'),
('support.update', 'Cập nhật hỗ trợ'),
('campaign.read', 'Xem chiến dịch'), ('campaign.create', 'Tạo chiến dịch'),
('campaign.update', 'Cập nhật chiến dịch'),
('user.read', 'Xem người dùng'), ('user.create', 'Tạo người dùng'),
('user.update', 'Sửa người dùng'), ('user.manage', 'Quản lý người dùng'),
('team.read', 'Xem cơ cấu nhóm'), ('team.manage', 'Quản lý cơ cấu nhóm'),
('product.read', 'Xem sản phẩm'), ('product.create', 'Tạo sản phẩm'),
('product.update', 'Sửa sản phẩm'), ('product.cost.read', 'Xem giá vốn'),
('product.cost.update', 'Sửa giá vốn'),
('masterdata.read', 'Xem danh mục chung'), ('masterdata.manage', 'Quản lý danh mục chung'),
('customfield.read', 'Xem trường tùy chỉnh'), ('customfield.manage', 'Quản lý trường tùy chỉnh'),
('pipeline.read', 'Xem pipeline'), ('pipeline.manage', 'Quản lý pipeline'),
('audit.read', 'Xem nhật ký audit log');

-- 3. Gán quyền hạn chi tiết cho từng Vai trò (role_permissions)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 1, p.id FROM permissions p;

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 4, p.id FROM permissions p
WHERE p.code NOT LIKE 'user.manage' AND p.code NOT LIKE 'customfield.manage';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 5, p.id FROM permissions p
WHERE p.code LIKE 'customer.%' OR p.code LIKE 'contact.%' OR p.code LIKE 'opportunity.%'
   OR p.code LIKE 'lead.%' OR p.code LIKE 'activity.%' OR p.code LIKE 'quote.%'
   OR p.code LIKE 'contract.read' OR p.code LIKE 'product.read' OR p.code LIKE 'pipeline.read'
   OR p.code IN ('team.read', 'masterdata.read', 'customfield.read');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 6, p.id FROM permissions p
WHERE p.code IN ('customer.read', 'customer.create', 'customer.update', 'customer.export',
                 'contact.read', 'contact.create', 'contact.update',
                 'opportunity.read', 'opportunity.create', 'opportunity.update', 'opportunity.close',
                 'lead.read', 'lead.create', 'lead.update', 'lead.accept', 'lead.reject', 'lead.convert',
                 'quote.read', 'quote.create', 'quote.update', 'quote.export',
                 'contract.read', 'activity.read', 'activity.create', 'activity.update', 'activity.export',
                 'product.read', 'pipeline.read', 'masterdata.read', 'customfield.read');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 7, p.id FROM permissions p
WHERE p.code LIKE 'lead.%' OR p.code LIKE 'campaign.%'
   OR p.code IN ('customer.read', 'customer.create', 'customer.update', 'customer.export',
                 'contact.read', 'contact.create', 'contact.update',
                 'activity.read', 'activity.create', 'activity.update',
                 'product.read', 'masterdata.read', 'customfield.read');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 8, p.id FROM permissions p
WHERE p.code LIKE 'support.%' OR p.code LIKE 'customer.read' OR p.code LIKE 'contact.read'
   OR p.code LIKE 'activity.%' OR p.code LIKE 'contract.read';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 9, p.id FROM permissions p
WHERE p.code LIKE 'quote.%' OR p.code LIKE 'contract.%' OR p.code LIKE 'customer.read';

-- 4. Phạm vi truy cập dữ liệu (role_module_scopes: ALL / TEAM / SELF)
INSERT INTO role_module_scopes (role_id, module_code, scope_type)
VALUES
(1, 'customer', 'ALL'), (1, 'opportunity', 'ALL'), (1, 'lead', 'ALL'), (1, 'activity', 'ALL'), (1, 'quote', 'ALL'), (1, 'contract', 'ALL'),
(4, 'customer', 'ALL'), (4, 'opportunity', 'ALL'), (4, 'lead', 'ALL'), (4, 'activity', 'ALL'), (4, 'quote', 'ALL'), (4, 'contract', 'ALL'),
(5, 'customer', 'TEAM'), (5, 'opportunity', 'TEAM'), (5, 'lead', 'TEAM'), (5, 'activity', 'TEAM'), (5, 'quote', 'TEAM'), (5, 'contract', 'TEAM'),
(6, 'customer', 'SELF'), (6, 'opportunity', 'SELF'), (6, 'lead', 'SELF'), (6, 'activity', 'SELF'), (6, 'quote', 'SELF'), (6, 'contract', 'SELF'),
(7, 'customer', 'ALL'), (7, 'opportunity', 'ALL'), (7, 'lead', 'SELF'), (7, 'activity', 'ALL'), (7, 'quote', 'ALL'), (7, 'contract', 'ALL'),
(8, 'customer', 'ALL'), (8, 'opportunity', 'ALL'), (8, 'lead', 'ALL'), (8, 'activity', 'ALL'), (8, 'quote', 'ALL'), (8, 'contract', 'ALL'),
(9, 'customer', 'ALL'), (9, 'opportunity', 'ALL'), (9, 'lead', 'ALL'), (9, 'activity', 'ALL'), (9, 'quote', 'ALL'), (9, 'contract', 'ALL')
ON DUPLICATE KEY UPDATE scope_type = VALUES(scope_type);

-- 5. Tài khoản người dùng mẫu phục vụ Demo & Test phân quyền
-- Mật khẩu chung: Password123@ (BCrypt hash)
INSERT INTO users (id, full_name, email, password_hash, status, failed_login_attempts, locked_until, data_scope, team_id, phone, email_signature)
VALUES
(1, 'Quản Trị Viên Hệ Thống', 'demo.admin@company.com', '$2a$12$QVtdE0bBS7/VyRzWFfSYEeUkBdjUB44dqj/hMuc0xbfWeZlrKSvuK', 'ACTIVE', 0, NULL, 'ALL', 1, '0901000000', 'NovaCRM System Administrator'),
(10, 'Nguyễn Quản Trị (Admin)', 'admin@company.com', '$2a$12$QVtdE0bBS7/VyRzWFfSYEeUkBdjUB44dqj/hMuc0xbfWeZlrKSvuK', 'ACTIVE', 0, NULL, 'ALL', 1, '0901000001', 'NovaCRM System Administrator'),
(11, 'Trần Quang Minh (Giám đốc)', 'director@company.com', '$2a$12$QVtdE0bBS7/VyRzWFfSYEeUkBdjUB44dqj/hMuc0xbfWeZlrKSvuK', 'ACTIVE', 0, NULL, 'ALL', 1, '0901000002', 'Trần Quang Minh - Giám đốc Kinh doanh Toàn quốc'),
(21, 'Lê Hoàng Long (Trưởng nhóm Bắc)', 'teamlead.bac@company.com', '$2a$12$QVtdE0bBS7/VyRzWFfSYEeUkBdjUB44dqj/hMuc0xbfWeZlrKSvuK', 'ACTIVE', 0, NULL, 'TEAM', 1, '0901000021', 'Lê Hoàng Long - Trưởng phòng Kinh doanh Miền Bắc'),
(22, 'Võ Thị Mai (Trưởng nhóm Nam)', 'teamlead.nam@company.com', '$2a$12$QVtdE0bBS7/VyRzWFfSYEeUkBdjUB44dqj/hMuc0xbfWeZlrKSvuK', 'ACTIVE', 0, NULL, 'TEAM', 3, '0901000022', 'Võ Thị Mai - Trưởng phòng Kinh doanh Miền Nam'),
(101, 'Nguyễn Văn Hùng (Sales Bắc 1)', 'sales.bac1@company.com', '$2a$12$QVtdE0bBS7/VyRzWFfSYEeUkBdjUB44dqj/hMuc0xbfWeZlrKSvuK', 'ACTIVE', 0, NULL, 'SELF', 1, '0912101101', 'Nguyễn Văn Hùng - Chuyên viên Kinh doanh Khách hàng DN'),
(102, 'Trần Thu Hà (Sales Bắc 2)', 'sales.bac2@company.com', '$2a$12$QVtdE0bBS7/VyRzWFfSYEeUkBdjUB44dqj/hMuc0xbfWeZlrKSvuK', 'ACTIVE', 0, NULL, 'SELF', 1, '0912102102', 'Trần Thu Hà - Chuyên viên Kinh doanh Miền Bắc'),
(103, 'Lê Hoàng Cường (Sales Trung)', 'sales.trung@company.com', '$2a$12$QVtdE0bBS7/VyRzWFfSYEeUkBdjUB44dqj/hMuc0xbfWeZlrKSvuK', 'ACTIVE', 0, NULL, 'SELF', 2, '0912103103', 'Lê Hoàng Cường - Đại diện Kinh doanh Chi nhánh Đà Nẵng'),
(104, 'Phạm Thu Dung (Sales Nam)', 'sales.nam@company.com', '$2a$12$QVtdE0bBS7/VyRzWFfSYEeUkBdjUB44dqj/hMuc0xbfWeZlrKSvuK', 'ACTIVE', 0, NULL, 'SELF', 3, '0912104104', 'Phạm Thu Dung - Chuyên viên Kinh doanh Miền Nam'),
(3020, 'Hoàng Yến Nhi (Marketing)', 'marketing@company.com', '$2a$12$QVtdE0bBS7/VyRzWFfSYEeUkBdjUB44dqj/hMuc0xbfWeZlrKSvuK', 'ACTIVE', 0, NULL, 'ALL', 1, '0901003020', 'Hoàng Yến Nhi - Trưởng nhóm Lead Generation & Marketing'),
(4010, 'Đỗ Minh Tuấn (CSKH)', 'cskh@company.com', '$2a$12$QVtdE0bBS7/VyRzWFfSYEeUkBdjUB44dqj/hMuc0xbfWeZlrKSvuK', 'ACTIVE', 0, NULL, 'ALL', 1, '0901004010', 'Đỗ Minh Tuấn - Chăm sóc Khách hàng & Hỗ trợ Sau bán'),
(5010, 'Bùi Thúy Hạnh (Kế toán)', 'accountant@company.com', '$2a$12$QVtdE0bBS7/VyRzWFfSYEeUkBdjUB44dqj/hMuc0xbfWeZlrKSvuK', 'ACTIVE', 0, NULL, 'ALL', 1, '0901005010', 'Bùi Thúy Hạnh - Kế toán Quản lý Hợp đồng & Doanh thu'),
(9001, 'Tài khoản Đã Khóa (Test S1-10)', 'locked.user@company.com', '$2a$12$QVtdE0bBS7/VyRzWFfSYEeUkBdjUB44dqj/hMuc0xbfWeZlrKSvuK', 'LOCKED', 5, '2099-12-31 23:59:59', 'SELF', 1, '0909999999', 'Tài khoản nhân sự đã nghỉ việc')
ON DUPLICATE KEY UPDATE 
    full_name = VALUES(full_name),
    password_hash = VALUES(password_hash),
    status = VALUES(status),
    failed_login_attempts = VALUES(failed_login_attempts),
    locked_until = VALUES(locked_until),
    data_scope = VALUES(data_scope),
    team_id = VALUES(team_id),
    phone = VALUES(phone),
    email_signature = VALUES(email_signature);

-- 6. Cơ cấu Nhóm kinh doanh (teams)
INSERT INTO teams (id, name, active, parent_id, leader_id, region_id) VALUES
(1, 'Kinh doanh miền Bắc', 1, NULL, 21, 3),
(2, 'Kinh doanh miền Trung', 1, NULL, 103, 4),
(3, 'Kinh doanh miền Nam', 1, NULL, 22, 5),
(4, 'Khách hàng Doanh nghiệp lớn (Enterprise)', 1, 1, 21, 3)
ON DUPLICATE KEY UPDATE name = VALUES(name), parent_id = VALUES(parent_id), leader_id = VALUES(leader_id), region_id = VALUES(region_id), active = 1;

-- 7. Gán Vai trò cho Người dùng (user_roles)
DELETE FROM user_roles WHERE user_id IN (1, 10, 11, 21, 22, 101, 102, 103, 104, 3020, 4010, 5010, 9001);

INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1),       -- demo.admin@company.com: ADMIN
(10, 1),      -- admin@company.com: ADMIN
(11, 4),      -- director@company.com: DIRECTOR
(21, 5),      -- teamlead.bac@company.com: TEAM_LEAD
(22, 5),      -- teamlead.nam@company.com: TEAM_LEAD
(101, 6),     -- sales.bac1@company.com: SALES_REP
(102, 6),     -- sales.bac2@company.com: SALES_REP
(103, 6),     -- sales.trung@company.com: SALES_REP
(104, 6),     -- sales.nam@company.com: SALES_REP
(3020, 7),    -- marketing@company.com: MARKETING
(4010, 8),    -- cskh@company.com: CUSTOMER_SUCCESS
(5010, 9),    -- accountant@company.com: ACCOUNTANT
(9001, 6);    -- locked.user@company.com: SALES_REP
