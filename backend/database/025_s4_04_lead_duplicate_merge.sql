-- S4-04 Lead Duplicate Detection and Merge.
-- Requires migrations 001-023 and base Lead migration (024_s4_02_lead_manual_excel).
-- Additive, idempotent, and repeatable; existing rows and schemas preserved.

-- 1. Ensure table leads exists and has necessary columns for S4-04 duplicate & merge
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
    CONSTRAINT fk_leads_source FOREIGN KEY (source_id) REFERENCES master_data(id),
    CONSTRAINT fk_leads_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT fk_leads_creator FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELIMITER $$
DROP PROCEDURE IF EXISTS EnsureS404LeadMergeColumns$$
CREATE PROCEDURE EnsureS404LeadMergeColumns()
BEGIN
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='title') THEN
        ALTER TABLE leads ADD COLUMN title VARCHAR(150) NULL;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='merged_into_id') THEN
        ALTER TABLE leads ADD COLUMN merged_into_id BIGINT NULL, ADD KEY ix_s4_04_leads_merged_into(merged_into_id);
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='merged_at') THEN
        ALTER TABLE leads ADD COLUMN merged_at DATETIME NULL;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='merged_by') THEN
        ALTER TABLE leads ADD COLUMN merged_by BIGINT NULL;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='leads' AND COLUMN_NAME='customer_id') THEN
        ALTER TABLE leads ADD COLUMN customer_id BIGINT NULL, ADD KEY ix_s4_04_leads_customer(customer_id);
    END IF;
END$$
CALL EnsureS404LeadMergeColumns()$$
DROP PROCEDURE EnsureS404LeadMergeColumns$$
DELIMITER ;

-- 2. Audit and history table for lead merges
CREATE TABLE IF NOT EXISTS lead_merge_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    master_lead_id BIGINT NOT NULL,
    duplicate_lead_id BIGINT NOT NULL,
    performed_by BIGINT NOT NULL,
    performed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    before_master JSON NOT NULL,
    before_duplicate JSON NOT NULL,
    after_master JSON NOT NULL,
    field_overrides JSON NOT NULL,
    retain_history BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uq_s4_04_merge_duplicate (duplicate_lead_id),
    KEY ix_s4_04_merge_master (master_lead_id, performed_at),
    FOREIGN KEY (master_lead_id) REFERENCES leads(id),
    FOREIGN KEY (duplicate_lead_id) REFERENCES leads(id),
    FOREIGN KEY (performed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Linkage table for Lead attached to existing Customer (AC2)
CREATE TABLE IF NOT EXISTS lead_customer_links (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lead_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    linked_by BIGINT NOT NULL,
    linked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT NULL,
    UNIQUE KEY uq_s4_04_lead_customer (lead_id),
    KEY ix_s4_04_cust_lead (customer_id),
    FOREIGN KEY (lead_id) REFERENCES leads(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (linked_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Permissions & Role Grants for S4-04
INSERT IGNORE INTO permissions(code, name) VALUES
('lead.read', 'Xem danh sách lead'),
('lead.merge', 'Phát hiện và gộp lead trùng lặp'),
('lead.link_customer', 'Liên kết lead vào khách hàng CRM');

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('lead.read', 'lead.merge', 'lead.link_customer')
WHERE r.code IN ('ADMIN', 'MARKETING', 'DIRECTOR', 'TEAM_LEAD');

INSERT IGNORE INTO role_module_scopes(role_id, module_code, scope_type)
SELECT r.id, 'lead', CASE
    WHEN r.code IN ('ADMIN', 'DIRECTOR') THEN 'ALL'
    WHEN r.code = 'TEAM_LEAD' THEN 'TEAM'
    ELSE 'SELF'
END
FROM roles r WHERE r.code IN ('ADMIN', 'DIRECTOR', 'TEAM_LEAD', 'MARKETING', 'SALES_REP')
ON DUPLICATE KEY UPDATE scope_type = VALUES(scope_type);
