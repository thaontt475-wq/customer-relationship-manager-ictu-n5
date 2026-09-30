-- CRM-46 / S2-08: Custom fields. Safe to run repeatedly.
CREATE TABLE IF NOT EXISTS custom_field_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    field_label VARCHAR(255) NOT NULL,
    field_type VARCHAR(20) NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    show_on_form BOOLEAN NOT NULL DEFAULT TRUE,
    usable_in_filter BOOLEAN NOT NULL DEFAULT TRUE,
    exportable BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_custom_field_entity_name (entity_type, field_name),
    INDEX idx_custom_field_entity_order (entity_type, sort_order),
    INDEX idx_custom_field_active (is_active),
    CONSTRAINT chk_custom_field_type CHECK (field_type IN ('TEXT', 'NUMBER', 'DATE', 'SELECT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS custom_field_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    custom_field_id BIGINT NOT NULL,
    option_value VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_custom_field_option (custom_field_id, option_value),
    INDEX idx_custom_field_option_order (custom_field_id, sort_order),
    CONSTRAINT fk_custom_field_option_definition
        FOREIGN KEY (custom_field_id) REFERENCES custom_field_definitions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS custom_field_values (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    custom_field_id BIGINT NOT NULL,
    record_id BIGINT NOT NULL,
    field_value TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_custom_field_record (custom_field_id, record_id),
    INDEX idx_custom_field_value_record (record_id),
    CONSTRAINT fk_custom_field_value_definition
        FOREIGN KEY (custom_field_id) REFERENCES custom_field_definitions(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
