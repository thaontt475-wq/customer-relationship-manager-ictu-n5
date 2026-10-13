-- Migration: 020_contracts_crud.sql
-- Description: NovaCRM Contract Management Schema & Permissions

CREATE TABLE IF NOT EXISTS contracts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contract_number VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    customer_id BIGINT NOT NULL,
    opportunity_id BIGINT DEFAULT NULL,
    quote_id BIGINT DEFAULT NULL,
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    signed_date DATE DEFAULT NULL,
    start_date DATE DEFAULT NULL,
    end_date DATE DEFAULT NULL,
    payment_terms TEXT DEFAULT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    notes TEXT DEFAULT NULL,
    owner_user_id BIGINT NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_contracts_number (contract_number),
    KEY idx_contracts_customer (customer_id),
    KEY idx_contracts_opportunity (opportunity_id),
    KEY idx_contracts_quote (quote_id),
    KEY idx_contracts_owner (owner_user_id),
    KEY idx_contracts_status (status),
    CONSTRAINT fk_contracts_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_contracts_opportunity FOREIGN KEY (opportunity_id) REFERENCES opportunities (id) ON DELETE SET NULL,
    CONSTRAINT fk_contracts_quote FOREIGN KEY (quote_id) REFERENCES quotes (id) ON DELETE SET NULL,
    CONSTRAINT fk_contracts_owner FOREIGN KEY (owner_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Register permissions for contracts
INSERT IGNORE INTO permissions (code, name) VALUES
('contract.read', 'Xem hợp đồng'),
('contract.create', 'Tạo hợp đồng'),
('contract.update', 'Sửa hợp đồng'),
('contract.delete', 'Xóa hợp đồng');

-- Map permissions to roles
INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code IN ('contract.read', 'contract.create', 'contract.update', 'contract.delete');

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DIRECTOR' AND p.code IN ('contract.read', 'contract.create', 'contract.update', 'contract.delete');

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('TEAM_LEAD', 'SALES_REP', 'ACCOUNTANT') AND p.code IN ('contract.read', 'contract.create', 'contract.update');

-- Map role module scopes for contract if role_module_scopes exists
INSERT INTO role_module_scopes(role_id, module_code, scope_type)
SELECT r.id, 'contract',
    CASE r.code WHEN 'ADMIN' THEN 'ALL' WHEN 'DIRECTOR' THEN 'ALL'
        WHEN 'TEAM_LEAD' THEN 'TEAM' ELSE 'SELF' END
FROM roles r
WHERE r.code IN ('ADMIN','DIRECTOR','TEAM_LEAD','SALES_REP','MARKETING','CUSTOMER_SUCCESS','ACCOUNTANT')
ON DUPLICATE KEY UPDATE scope_type = VALUES(scope_type);
