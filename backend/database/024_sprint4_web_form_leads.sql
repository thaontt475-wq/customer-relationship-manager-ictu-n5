USE crm_db;

-- Sprint 4 (S4-01 / CRM-113): Thu thap Lead tu bieu mau website (Web Form Lead Capture)
CREATE TABLE IF NOT EXISTS leads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(150) NOT NULL,
    company_name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    phone VARCHAR(50) NULL,
    source VARCHAR(50) NULL DEFAULT 'WEB_FORM',
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    notes TEXT NULL,
    form_id VARCHAR(100) NULL,
    ip_address VARCHAR(50) NULL,
    assigned_to BIGINT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_leads_status (status),
    INDEX idx_leads_source (source),
    INDEX idx_leads_assigned (assigned_to),
    INDEX idx_leads_is_deleted (is_deleted),
    CONSTRAINT fk_leads_assigned
        FOREIGN KEY (assigned_to) REFERENCES users(id)
        ON DELETE SET NULL
);

-- Bo sung quyen cho module Lead
INSERT IGNORE INTO permissions (code, name) VALUES
    ('lead.read', 'Xem danh sách Lead'),
    ('lead.create', 'Tạo Lead mới'),
    ('lead.update', 'Cập nhật thông tin Lead'),
    ('lead.delete', 'Xóa Lead'),
    ('lead.assign', 'Phân bổ Lead cho nhân viên');

-- Gan quyen cho ADMIN
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code LIKE 'lead.%';

-- Apply Lead scope only; existing modules retain their configured scopes.
INSERT INTO role_module_scopes (role_id, module_code, scope_type)
SELECT r.id, 'lead', CASE r.code WHEN 'ADMIN' THEN 'ALL' WHEN 'DIRECTOR' THEN 'ALL'
    WHEN 'TEAM_LEAD' THEN 'TEAM' ELSE 'SELF' END
FROM roles r
WHERE r.code IN ('ADMIN','DIRECTOR','TEAM_LEAD','SALES_REP','MARKETING')
ON DUPLICATE KEY UPDATE scope_type = VALUES(scope_type);
