-- CRM-47: Pipeline Stages and Transition Rules Management
USE crm_db;

-- Pipeline stages table
CREATE TABLE IF NOT EXISTS pipeline_stages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pipeline_id BIGINT NOT NULL DEFAULT 1,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    stage_order INT NOT NULL DEFAULT 1,
    win_probability INT NOT NULL DEFAULT 0,
    requirements TEXT NULL,
    is_won BOOLEAN NOT NULL DEFAULT FALSE,
    is_lost BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pipeline_stage_code (pipeline_id, code),
    INDEX idx_pipeline_stage_order (pipeline_id, stage_order ASC),
    INDEX idx_pipeline_stage_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Enhance opportunities table with stage_id, amount, contact_name, lost_reason
ALTER TABLE opportunities
    ADD COLUMN IF NOT EXISTS stage_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS amount DECIMAL(15, 2) NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS contact_name VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS lost_reason VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS probability INT NULL;

-- Seed default sales pipeline stages (Order: 1 to 6, Win probability: 10% to 100%)
INSERT IGNORE INTO pipeline_stages (pipeline_id, code, name, stage_order, win_probability, requirements, is_won, is_lost, is_active) VALUES
(1, 'PROSPECTING', 'Tìm kiếm & Tiếp cận', 1, 10, 'Xác định khách hàng tiềm năng và người liên hệ chính', FALSE, FALSE, TRUE),
(1, 'QUALIFICATION', 'Đánh giá & Xác định nhu cầu', 2, 25, 'Xác định rõ ngân sách, thẩm quyền, nhu cầu và tiến độ (BANT)', FALSE, FALSE, TRUE),
(1, 'PROPOSAL', 'Gửi đề xuất & Báo giá', 3, 50, 'Bắt buộc nhập giá trị cơ hội (amount) và gửi báo giá chính thức', FALSE, FALSE, TRUE),
(1, 'NEGOTIATION', 'Thương lượng & Đàm phán', 4, 75, 'Thống nhất điều khoản hợp đồng và chính sách giá/chiết khấu', FALSE, FALSE, TRUE),
(1, 'CLOSED_WON', 'Chốt thành công (Won)', 5, 100, 'Ký hợp đồng hoặc xác nhận đơn hàng thành công; giá trị phải lớn hơn 0', TRUE, FALSE, TRUE),
(1, 'CLOSED_LOST', 'Thất bại (Lost)', 6, 0, 'Bắt buộc ghi rõ lý do thất bại (lost_reason)', FALSE, TRUE, TRUE);
