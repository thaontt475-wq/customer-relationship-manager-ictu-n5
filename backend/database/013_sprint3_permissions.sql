-- Run AFTER 002_permissions.sql, 007_sprint1_roles_scope.sql and 012 schema.
USE crm_db;
INSERT IGNORE INTO permissions(code,name) VALUES
 ('customer.read','Xem khách hàng'),('customer.create','Tạo khách hàng'),
 ('customer.update','Sửa khách hàng'),('customer.delete','Xóa khách hàng'),
 ('opportunity.read','Xem cơ hội'),('activity.read','Xem hoạt động'),('activity.create','Tạo hoạt động');
-- Do not grant all roles here; administrator/team leader configure role_module_scopes.
INSERT IGNORE INTO role_permissions(role_id,permission_id)
 SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.code='ADMIN'
 AND p.code IN ('customer.read','customer.create','customer.update','customer.delete','opportunity.read','activity.read','activity.create');
-- Existing role_module_scopes must define module_code customer, opportunity, activity.
