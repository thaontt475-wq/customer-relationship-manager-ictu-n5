-- Apply to the selected development/test database after existing Sprint 4 migrations.
-- Intentionally no USE crm_db: the operator selects the target schema.
CREATE TABLE IF NOT EXISTS lead_capture_forms (
    token CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    form_name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_capture_owner_name(owner_user_id,form_name),
    FOREIGN KEY (owner_user_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS lead_capture_submissions (
    form_token CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    payload_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    lead_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(form_token,request_id),
    FOREIGN KEY (form_token) REFERENCES lead_capture_forms(token),
    FOREIGN KEY (lead_id) REFERENCES leads(id)
);
INSERT IGNORE INTO permissions(code,name) VALUES ('lead.read','Xem lead'),('lead.create','Tạo lead');
INSERT IGNORE INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r JOIN permissions p ON p.code IN ('lead.read','lead.create')
WHERE r.code IN ('MARKETING','ADMIN');
-- Keep existing scope policies; only fill missing Lead policies.
INSERT IGNORE INTO role_module_scopes(role_id,module_code,scope_type)
SELECT id,'lead',IF(code='ADMIN','ALL','SELF') FROM roles WHERE code IN ('MARKETING','ADMIN');

-- Explicit replacement for legacy constructor DDL; additive and repeatable.
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='code')=0, 'ALTER TABLE leads ADD COLUMN code VARCHAR(50) NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='notes')=0, 'ALTER TABLE leads ADD COLUMN notes TEXT NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='source')=0, 'ALTER TABLE leads ADD COLUMN source VARCHAR(50) NULL DEFAULT ''WEB_FORM''', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='source_name')=0, 'ALTER TABLE leads ADD COLUMN source_name VARCHAR(100) NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='form_id')=0, 'ALTER TABLE leads ADD COLUMN form_id VARCHAR(100) NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='ip_address')=0, 'ALTER TABLE leads ADD COLUMN ip_address VARCHAR(50) NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='assigned_to')=0, 'ALTER TABLE leads ADD COLUMN assigned_to BIGINT NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='source_id')=0, 'ALTER TABLE leads ADD COLUMN source_id BIGINT NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='owner_user_id')=0, 'ALTER TABLE leads ADD COLUMN owner_user_id BIGINT NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='created_by')=0, 'ALTER TABLE leads ADD COLUMN created_by BIGINT NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='import_batch_hash')=0, 'ALTER TABLE leads ADD COLUMN import_batch_hash VARCHAR(64) NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='region')=0, 'ALTER TABLE leads ADD COLUMN region VARCHAR(100) NULL DEFAULT ''Miền Bắc (Hà Nội)''', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='region_code')=0, 'ALTER TABLE leads ADD COLUMN region_code VARCHAR(50) NULL DEFAULT ''NORTH''', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='industry')=0, 'ALTER TABLE leads ADD COLUMN industry VARCHAR(100) NULL DEFAULT ''ALL''', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='title')=0, 'ALTER TABLE leads ADD COLUMN title VARCHAR(150) NULL DEFAULT ''Khách hàng tiềm năng''', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='tax_code')=0, 'ALTER TABLE leads ADD COLUMN tax_code VARCHAR(50) NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='rating')=0, 'ALTER TABLE leads ADD COLUMN rating VARCHAR(50) NULL DEFAULT ''WARM''', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='tier')=0, 'ALTER TABLE leads ADD COLUMN tier VARCHAR(50) NULL DEFAULT ''WARM''', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='estimated_value')=0, 'ALTER TABLE leads ADD COLUMN estimated_value DECIMAL(15,2) NULL DEFAULT 0.00', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='sla_minutes_total')=0, 'ALTER TABLE leads ADD COLUMN sla_minutes_total INT NULL DEFAULT 120', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='last_contacted_at')=0, 'ALTER TABLE leads ADD COLUMN last_contacted_at DATETIME NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='unassigned_reason')=0, 'ALTER TABLE leads ADD COLUMN unassigned_reason VARCHAR(255) NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='is_deleted')=0, 'ALTER TABLE leads ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='assigned_to_user_id')=0, 'ALTER TABLE leads ADD COLUMN assigned_to_user_id BIGINT NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='assigned_at')=0, 'ALTER TABLE leads ADD COLUMN assigned_at DATETIME NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='sla_total_minutes')=0, 'ALTER TABLE leads ADD COLUMN sla_total_minutes INT NULL DEFAULT 120', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='sla_deadline')=0, 'ALTER TABLE leads ADD COLUMN sla_deadline DATETIME NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='accepted_at')=0, 'ALTER TABLE leads ADD COLUMN accepted_at DATETIME NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='rejected_at')=0, 'ALTER TABLE leads ADD COLUMN rejected_at DATETIME NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='reject_reason')=0, 'ALTER TABLE leads ADD COLUMN reject_reason VARCHAR(100) NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='reject_details')=0, 'ALTER TABLE leads ADD COLUMN reject_details TEXT NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='sla_result')=0, 'ALTER TABLE leads ADD COLUMN sla_result VARCHAR(50) NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='response_time_text')=0, 'ALTER TABLE leads ADD COLUMN response_time_text VARCHAR(255) NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='contacting_stage')=0, 'ALTER TABLE leads ADD COLUMN contacting_stage VARCHAR(255) NULL', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='is_locked')=0, 'ALTER TABLE leads ADD COLUMN is_locked TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='employee_count')=0, 'ALTER TABLE leads ADD COLUMN employee_count INT NULL DEFAULT 0', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;
SET @capture_sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='leads' AND column_name='score')=0, 'ALTER TABLE leads ADD COLUMN score INT NULL DEFAULT 0', 'SELECT 1');
PREPARE capture_stmt FROM @capture_sql; EXECUTE capture_stmt; DEALLOCATE PREPARE capture_stmt;

-- Canonical active Website catalog source, matching the S4-02 sourceId contract.
INSERT INTO master_data(type,code,name,active)
SELECT 'lead-source','WEBSITE','Website',1 WHERE NOT EXISTS
 (SELECT 1 FROM master_data WHERE type='lead-source' AND code='WEBSITE');
