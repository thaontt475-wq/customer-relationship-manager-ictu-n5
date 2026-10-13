USE crm_db;

-- Sửa lỗi mã hóa tiếng Việt (Mojibake) cho bảng roles
UPDATE roles SET name = 'Quản trị viên' WHERE code = 'ADMIN';
UPDATE roles SET name = 'Giám đốc kinh doanh' WHERE code = 'DIRECTOR';
UPDATE roles SET name = 'Trưởng nhóm kinh doanh' WHERE code = 'TEAM_LEAD';
UPDATE roles SET name = 'Nhân viên kinh doanh' WHERE code = 'SALES_REP' OR code = 'SALE';
UPDATE roles SET name = 'Nhân viên Marketing' WHERE code = 'MARKETING';
UPDATE roles SET name = 'Chăm sóc khách hàng' WHERE code = 'CUSTOMER_SUCCESS';
UPDATE roles SET name = 'Kế toán' WHERE code = 'ACCOUNTANT';

-- Sửa lỗi mã hóa tiếng Việt cho bảng teams
UPDATE teams SET name = 'Kinh doanh miền Bắc' WHERE id = 1 OR name LIKE '%B%c%';
UPDATE teams SET name = 'Kinh doanh miền Trung' WHERE id = 2 OR name LIKE '%Trung%';
UPDATE teams SET name = 'Kinh doanh miền Nam' WHERE id = 3 OR name LIKE '%Nam%';
