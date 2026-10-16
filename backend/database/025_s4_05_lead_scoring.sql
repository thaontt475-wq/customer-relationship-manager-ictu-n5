-- S4-05. Requires 001-023 and approved S4-02 migration 024.
-- Select a TEST database first. Additive, repeatable, no business data removed.
CREATE TABLE IF NOT EXISTS lead_scoring_config (
    id TINYINT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    hot INT NULL, warm INT NULL,
    aggregation VARCHAR(30) NULL,
    min_score INT NULL, max_score INT NULL,
    rules_json JSON NOT NULL,
    updated_by BIGINT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_scoring_singleton CHECK (id = 1),
    CONSTRAINT fk_scoring_config_user FOREIGN KEY (updated_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO lead_scoring_config(id,rules_json) VALUES (1,JSON_ARRAY());

-- Optional inputs absent from S4-02; missing values do not match any rule.
CREATE TABLE IF NOT EXISTS lead_scoring_profiles (
    lead_id BIGINT PRIMARY KEY,
    industry VARCHAR(255) NULL,
    employee_count INT NULL,
    title VARCHAR(150) NULL,
    recent_activity VARCHAR(1000) NULL,
    updated_by BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_scoring_profile_lead FOREIGN KEY (lead_id) REFERENCES leads(id),
    CONSTRAINT fk_scoring_profile_user FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT chk_scoring_employee CHECK (employee_count IS NULL OR employee_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS lead_scores (
    lead_id BIGINT PRIMARY KEY,
    raw_score INT NOT NULL,
    score INT NOT NULL,
    classification VARCHAR(10) NOT NULL,
    breakdown_json JSON NOT NULL,
    config_version BIGINT NOT NULL,
    input_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    scored_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_lead_scores_class_score (classification,score),
    CONSTRAINT fk_lead_score_lead FOREIGN KEY (lead_id) REFERENCES leads(id),
    CONSTRAINT chk_lead_classification CHECK (classification IN ('HOT','WARM','COLD'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO permissions(code,name) VALUES
('lead.score','Chấm lại điểm Lead trong phạm vi'),
('lead.scoring.manage','Quản lý cấu hình chấm điểm Lead toàn hệ thống');
-- Global rule management changes all leads: grant ADMIN only by default.
INSERT IGNORE INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r JOIN permissions p ON p.code='lead.scoring.manage' WHERE r.code='ADMIN';
INSERT IGNORE INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r JOIN permissions p ON p.code='lead.score' WHERE r.code IN ('ADMIN','MARKETING');
INSERT IGNORE INTO role_module_scopes(role_id,module_code,scope_type)
SELECT r.id,'lead',CASE WHEN r.code IN ('ADMIN','DIRECTOR') THEN 'ALL' WHEN r.code='TEAM_LEAD' THEN 'TEAM' ELSE 'SELF' END
FROM roles r WHERE EXISTS (SELECT 1 FROM role_permissions rp JOIN permissions p ON p.id=rp.permission_id
WHERE rp.role_id=r.id AND p.code LIKE 'lead.%');
