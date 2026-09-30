-- CRM-48 / S2-10: Reference data for win/loss reasons and competitors.
-- No Opportunity close-flow columns are introduced by this story.
CREATE TABLE IF NOT EXISTS win_loss_reasons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reason_type VARCHAR(10) NOT NULL,
    reason_text VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_win_loss_reason_type_text (reason_type, reason_text),
    INDEX idx_win_loss_reason_order (reason_type, is_active, display_order),
    CONSTRAINT chk_win_loss_reason_type CHECK (reason_type IN ('WIN', 'LOSS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS competitors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    strengths VARCHAR(1000) NULL,
    weaknesses VARCHAR(1000) NULL,
    website VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_competitor_name (name),
    INDEX idx_competitor_active_order (is_active, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
