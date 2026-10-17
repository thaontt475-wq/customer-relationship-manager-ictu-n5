-- Select the intended database explicitly. No USE, demo users, or business seeds.
-- Additive replacement for the structural part of 025_sprint4_lead_assignment_rules.sql.
CREATE TABLE IF NOT EXISTS lead_assignment_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(50) NULL,
    name VARCHAR(255) NOT NULL,
    priority_order INT NOT NULL DEFAULT 1,
    region VARCHAR(50) NOT NULL DEFAULT 'NATIONWIDE',
    industry VARCHAR(100) NOT NULL DEFAULT 'ALL',
    mechanism VARCHAR(50) NOT NULL DEFAULT 'ROUND_ROBIN',
    reps TEXT NULL,
    description TEXT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    total_assigned INT NOT NULL DEFAULT 0,
    last_assigned_index INT NOT NULL DEFAULT 0,
    last_assigned_user_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rules_priority(priority_order,is_active),
    CONSTRAINT fk_s406_rule_last_user FOREIGN KEY(last_assigned_user_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS lead_assignment_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lead_id BIGINT NULL,
    lead_name VARCHAR(150) NOT NULL,
    lead_phone VARCHAR(50) NULL,
    company_name VARCHAR(255) NULL,
    rep_id VARCHAR(50) NOT NULL,
    rep_name VARCHAR(150) NULL,
    rep_dept VARCHAR(150) NULL,
    method VARCHAR(255) NOT NULL,
    action_type VARCHAR(50) NOT NULL DEFAULT 'ASSIGN',
    note TEXT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_lah_lead(lead_id),
    INDEX idx_lah_assigned_at(assigned_at)
);
INSERT IGNORE INTO permissions(code,name) VALUES
 ('lead.assign','Phân bổ Lead'),('lead.assignment_rules','Cấu hình quy tắc phân bổ Lead'),
 ('lead.auto_assign','Kích hoạt phân bổ Lead tự động');
INSERT IGNORE INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r JOIN permissions p
 ON p.code IN ('lead.assign','lead.assignment_rules','lead.auto_assign')
WHERE r.code IN ('ADMIN','DIRECTOR','TEAM_LEAD');
-- S4-07's selected-schema migration 024_s4_07_lead_sla.sql must already be applied.
-- Legacy assignment data is not rewritten here: audit references before any repair.
