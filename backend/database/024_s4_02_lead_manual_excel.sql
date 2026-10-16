-- S4-02. Select the intended database before running; never auto-run from HTTP.
-- Requires migrations 001-023. Additive and repeatable; existing rows/grants preserved.
CREATE TABLE IF NOT EXISTS lead_import_batches (
    token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    total_rows INT NOT NULL,
    success_count INT NOT NULL,
    failure_count INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lead_batch_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    CONSTRAINT fk_leads_creator FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_leads_batch FOREIGN KEY (import_batch_hash) REFERENCES lead_import_batches(token_hash),
    CONSTRAINT chk_leads_contact CHECK (email IS NOT NULL OR phone IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO master_data(type,code,name,active) VALUES
('lead-source','WEBSITE','Website / Đăng ký trực tuyến',1),
('lead-source','REFERRAL','Giới thiệu từ đối tác / khách hàng',1),
('lead-source','COLD_CALL','Telesales / Tiếp cận trực tiếp',1),
('lead-source','SOCIAL_MEDIA','Mạng xã hội (Facebook, LinkedIn)',1),
('lead-source','EVENT','Hội thảo / Sự kiện / Triển lãm',1),
('lead-source','OTHER','Nguồn khác',1);

INSERT IGNORE INTO permissions(code,name) VALUES ('lead.read','Xem lead'),('lead.create','Tạo lead');
-- Standard Marketing/Admin grants, as defined by migration 007; do not grant every role.
INSERT IGNORE INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r JOIN permissions p ON p.code IN ('lead.read','lead.create')
WHERE r.code IN ('MARKETING','ADMIN');
INSERT IGNORE INTO role_module_scopes(role_id,module_code,scope_type)
SELECT DISTINCT r.id,'lead',CASE r.code WHEN 'ADMIN' THEN 'ALL' WHEN 'DIRECTOR' THEN 'ALL'
    WHEN 'TEAM_LEAD' THEN 'TEAM' ELSE 'SELF' END
FROM roles r JOIN role_permissions rp ON rp.role_id=r.id JOIN permissions p ON p.id=rp.permission_id
WHERE r.code IN ('MARKETING','ADMIN','DIRECTOR','TEAM_LEAD','SALES_REP') AND p.code IN ('lead.read','lead.create');
