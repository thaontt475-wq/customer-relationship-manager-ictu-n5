SET NAMES utf8mb4;

-- Cập nhật ghi chú cho đối thủ cạnh tranh nếu đã tồn tại
UPDATE competitors SET 
    note = 'Chi phí đắt đỏ, triển khai phức tạp - Thương hiệu lâu năm, nhiều module'
WHERE id = 1;

UPDATE competitors SET 
    note = 'Ít tùy biến, dịch vụ hỗ trợ chậm - Giá rẻ, đăng ký nhanh'
WHERE id = 2;

-- Thêm đối thủ mẫu tương thích với cấu trúc canonical của bảng competitors (id, name, note, active)
INSERT IGNORE INTO competitors (id, name, note, active) 
VALUES 
    (3, 'Base CRM', 'Nền tảng quản trị mở rộng, quy trình linh hoạt. Chi phí tăng theo người dùng', 1),
    (4, 'MISA AMIS', 'Hệ sinh thái tài chính kế toán đồng bộ. Giao diện truyền thống, ít tùy biến theo dự án lớn', 1);

-- Đồng bộ các cột legacy weaknesses, strengths nếu schema có hỗ trợ
SET @has_weak = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competitors' AND COLUMN_NAME = 'weaknesses');
SET @fix_sql = IF(@has_weak > 0, 'UPDATE competitors SET weaknesses = note WHERE id IN (1, 2, 3, 4)', 'SELECT 1');
PREPARE stmt_fix FROM @fix_sql; EXECUTE stmt_fix; DEALLOCATE PREPARE stmt_fix;

UPDATE win_loss_reasons SET type = 'WON' WHERE type = 'WIN';
UPDATE win_loss_reasons SET type = 'LOST' WHERE type = 'LOSS';
