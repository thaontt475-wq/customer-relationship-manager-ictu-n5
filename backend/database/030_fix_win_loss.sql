SET NAMES utf8mb4;

UPDATE competitors SET 
    note = 'Chi phí đắt đỏ, triển khai phức tạp', 
    weaknesses = 'Chi phí đắt đỏ, triển khai phức tạp', 
    strengths = 'Thương hiệu lâu năm, nhiều module' 
WHERE id = 1;

UPDATE competitors SET 
    note = 'Ít tùy biến, dịch vụ hỗ trợ chậm', 
    weaknesses = 'Ít tùy biến, dịch vụ hỗ trợ chậm', 
    strengths = 'Giá rẻ, đăng ký nhanh' 
WHERE id = 2;

INSERT IGNORE INTO competitors (id, name, strengths, weaknesses, website, is_active, note, active) 
VALUES (3, 'Base CRM', 'Nền tảng quản trị mở rộng, quy trình linh hoạt', 'Chi phí tăng theo người dùng', 'https://base.vn', 1, 'Nền tảng quản trị mở rộng', 1);

INSERT IGNORE INTO competitors (id, name, strengths, weaknesses, website, is_active, note, active) 
VALUES (4, 'MISA AMIS', 'Hệ sinh thái tài chính kế toán đồng bộ', 'Giao diện truyền thống, ít tùy biến theo dự án lớn', 'https://amis.misa.vn', 1, 'Hệ sinh thái kế toán đồng bộ', 1);

UPDATE win_loss_reasons SET type = 'WON' WHERE type = 'WIN';
UPDATE win_loss_reasons SET type = 'LOST' WHERE type = 'LOSS';
