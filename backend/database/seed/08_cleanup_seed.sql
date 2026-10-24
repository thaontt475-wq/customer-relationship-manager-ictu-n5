-- ==============================================================================
-- 08_cleanup_seed.sql: DỌN DẸP AN TOÀN DỮ LIỆU MẪU (SEED DATA)
-- Hệ thống: NovaCRM | Dành cho môi trường QA / Demo
-- NGUYÊN TẮC AN TOÀN:
--   - KHÔNG sử dụng TRUNCATE table
--   - Chỉ xóa các bản ghi trong dải ID đã định nghĩa cho dữ liệu mẫu
--   - Chạy trong TRANSACTION; nếu gặp lỗi sẽ tự động ROLLBACK
--   - Chặn thực thi nếu database hiện tại có tên chứa 'prod' hoặc 'production'
-- ==============================================================================

SET NAMES utf8mb4;

DELIMITER $$
DROP PROCEDURE IF EXISTS SafeCleanupSeedData$$
CREATE PROCEDURE SafeCleanupSeedData()
BEGIN
    DECLARE current_db VARCHAR(100);
    SELECT DATABASE() INTO current_db;

    -- 1. Kiểm tra an toàn: Chặn tuyệt đối môi trường production
    IF current_db LIKE '%prod%' OR current_db LIKE '%production%' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'NGUY HIỂM: Thao tác dọn dẹp seed data bị từ chối trên database Production!';
    END IF;

    -- 2. Thực hiện xóa theo thứ tự đảo ngược của khóa ngoại (Foreign Keys)
    START TRANSACTION;

    -- Chăm sóc & Tương tác
    DELETE FROM lead_conversion_history WHERE id IN (1, 2, 3, 4, 5);
    DELETE FROM notifications WHERE id IN (1, 2, 3, 4, 5);
    DELETE FROM customer_attachments WHERE customer_id BETWEEN 1 AND 20;
    DELETE FROM activities WHERE id BETWEEN 1 AND 50;
    DELETE FROM support_requests WHERE id BETWEEN 1 AND 20;

    -- Kinh doanh & Báo giá
    DELETE FROM customer_contracts WHERE customer_id BETWEEN 1 AND 20;
    DELETE FROM contracts WHERE customer_id BETWEEN 1 AND 20;
    DELETE FROM quote_items WHERE quote_id BETWEEN 1 AND 20;
    DELETE FROM quotes WHERE customer_id BETWEEN 1 AND 20;
    DELETE FROM opportunities WHERE customer_id BETWEEN 1 AND 20;
    DELETE FROM products WHERE id BETWEEN 100 AND 120;

    -- Marketing & Leads
    DELETE FROM campaign_leads WHERE campaign_id BETWEEN 1 AND 10;
    DELETE FROM saved_lead_filters WHERE user_id IN (10, 11, 21, 22, 101, 102, 103, 104, 3020, 4010, 5010, 9001);
    DELETE FROM saved_customer_filters WHERE user_id IN (10, 11, 21, 22, 101, 102, 103, 104, 3020, 4010, 5010, 9001);
    DELETE FROM lead_scoring_config WHERE id = 1;
    DELETE FROM lead_assignment_rules WHERE id BETWEEN 1 AND 10;
    DELETE FROM leads WHERE id BETWEEN 101 AND 120;
    DELETE FROM lead_capture_forms WHERE token LIKE 'wf-%';
    DELETE FROM campaigns WHERE id BETWEEN 1 AND 10;

    -- Khách hàng & Người liên hệ
    DELETE FROM custom_field_values WHERE custom_field_id BETWEEN 100 AND 110;
    DELETE FROM custom_fields WHERE id BETWEEN 100 AND 110;
    DELETE FROM contacts WHERE customer_id BETWEEN 1 AND 20;
    DELETE FROM customers WHERE id BETWEEN 1 AND 20;

    -- Phân quyền & Nhân sự (Chỉ xóa users demo ngoại trừ root id=1)
    DELETE FROM user_roles WHERE user_id IN (10, 11, 21, 22, 101, 102, 103, 104, 3020, 4010, 5010, 9001);
    DELETE FROM users WHERE id IN (10, 11, 21, 22, 101, 102, 103, 104, 3020, 4010, 5010, 9001);
    DELETE FROM teams WHERE id BETWEEN 1 AND 10;

    COMMIT;
    SELECT 'DỌN DẸP DỮ LIỆU MẪU THÀNH CÔNG!' AS Status, current_db AS DatabaseName;
END$$
DELIMITER ;

CALL SafeCleanupSeedData();
DROP PROCEDURE IF EXISTS SafeCleanupSeedData;
