-- S4-08 Lead Conversion to Customer, Contact, and Opportunity (3-in-1 Conversion).
-- Requires migrations 001-023. Additive, repeatable, and idempotent.

-- 1. Ensure table leads exists
CREATE TABLE IF NOT EXISTS leads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(150) NOT NULL,
    company_name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    phone VARCHAR(50) NULL,
    source_id BIGINT NOT NULL,
    notes VARCHAR(2000) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    owner_user_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    import_batch_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_leads_owner_created (owner_user_id, created_at),
    KEY idx_leads_source (source_id),
    KEY idx_leads_email (email),
    KEY idx_leads_phone (phone),
    CONSTRAINT fk_s408_leads_source FOREIGN KEY (source_id) REFERENCES master_data(id),
    CONSTRAINT fk_s408_leads_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT fk_s408_leads_creator FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Add columns needed for Lead Conversion and Rich Profiles
DELIMITER $$
DROP PROCEDURE IF EXISTS EnsureS408LeadConversionColumns$$
CREATE PROCEDURE EnsureS408LeadConversionColumns()
BEGIN
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='title') THEN
        ALTER TABLE leads ADD COLUMN title VARCHAR(150) NULL;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='tax_code') THEN
        ALTER TABLE leads ADD COLUMN tax_code VARCHAR(50) NULL;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='address') THEN
        ALTER TABLE leads ADD COLUMN address VARCHAR(500) NULL;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='website') THEN
        ALTER TABLE leads ADD COLUMN website VARCHAR(255) NULL;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='company_phone') THEN
        ALTER TABLE leads ADD COLUMN company_phone VARCHAR(50) NULL;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='industry') THEN
        ALTER TABLE leads ADD COLUMN industry VARCHAR(100) NULL;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='score') THEN
        ALTER TABLE leads ADD COLUMN score INT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='tier') THEN
        ALTER TABLE leads ADD COLUMN tier VARCHAR(20) NULL;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='estimated_value') THEN
        ALTER TABLE leads ADD COLUMN estimated_value DECIMAL(18,2) NULL DEFAULT 0.00;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='buying_role') THEN
        ALTER TABLE leads ADD COLUMN buying_role VARCHAR(40) NULL;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='is_locked') THEN
        ALTER TABLE leads ADD COLUMN is_locked TINYINT(1) NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='converted_at') THEN
        ALTER TABLE leads ADD COLUMN converted_at DATETIME NULL;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='converted_by') THEN
        ALTER TABLE leads ADD COLUMN converted_by BIGINT NULL, ADD KEY ix_s4_08_leads_converted_by(converted_by);
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='converted_customer_id') THEN
        ALTER TABLE leads ADD COLUMN converted_customer_id BIGINT NULL, ADD KEY ix_s4_08_leads_conv_cust(converted_customer_id);
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='converted_contact_id') THEN
        ALTER TABLE leads ADD COLUMN converted_contact_id BIGINT NULL, ADD KEY ix_s4_08_leads_conv_contact(converted_contact_id);
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='converted_opportunity_id') THEN
        ALTER TABLE leads ADD COLUMN converted_opportunity_id BIGINT NULL, ADD KEY ix_s4_08_leads_conv_opp(converted_opportunity_id);
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='merged_into_id') THEN
        ALTER TABLE leads ADD COLUMN merged_into_id BIGINT NULL, ADD KEY ix_s4_08_leads_merged_into(merged_into_id);
    END IF;
END$$
CALL EnsureS408LeadConversionColumns()$$
DROP PROCEDURE EnsureS408LeadConversionColumns$$
DELIMITER ;

-- 3. Dedicated History table for Lead Conversions
CREATE TABLE IF NOT EXISTS lead_conversion_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lead_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    contact_id BIGINT NOT NULL,
    opportunity_id BIGINT NULL,
    converted_by BIGINT NOT NULL,
    converted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    customer_mode VARCHAR(30) NOT NULL,
    conversion_payload JSON NULL,
    lead_snapshot JSON NOT NULL,
    UNIQUE KEY uq_s4_08_conv_lead (lead_id),
    KEY ix_s4_08_conv_customer (customer_id),
    KEY ix_s4_08_conv_contact (contact_id),
    KEY ix_s4_08_conv_opp (opportunity_id),
    KEY ix_s4_08_conv_user (converted_by),
    CONSTRAINT fk_s408_conv_lead FOREIGN KEY (lead_id) REFERENCES leads(id),
    CONSTRAINT fk_s408_conv_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_s408_conv_contact FOREIGN KEY (contact_id) REFERENCES contacts(id),
    CONSTRAINT fk_s408_conv_user FOREIGN KEY (converted_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Permissions & Role Grants for S4-08
INSERT IGNORE INTO permissions(code, name) VALUES
('lead.read', 'Xem danh sách lead'),
('lead.convert', 'Chuyển đổi lead thành khách hàng và cơ hội');

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('lead.read', 'lead.convert')
WHERE r.code IN ('ADMIN', 'DIRECTOR', 'TEAM_LEAD', 'SALES_REP', 'MARKETING');

INSERT IGNORE INTO role_module_scopes(role_id, module_code, scope_type)
SELECT r.id, 'lead', CASE
    WHEN r.code IN ('ADMIN', 'DIRECTOR') THEN 'ALL'
    WHEN r.code = 'TEAM_LEAD' THEN 'TEAM'
    ELSE 'SELF'
END
FROM roles r WHERE r.code IN ('ADMIN', 'DIRECTOR', 'TEAM_LEAD', 'MARKETING', 'SALES_REP')
ON DUPLICATE KEY UPDATE scope_type = VALUES(scope_type);
