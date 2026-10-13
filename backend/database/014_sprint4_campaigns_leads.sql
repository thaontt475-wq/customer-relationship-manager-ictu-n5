USE crm_db;

-- 1. Bảng quản lý Chiến dịch (campaigns)
CREATE TABLE IF NOT EXISTS campaigns (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'EMAIL',
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNING',
    start_date DATE NULL,
    end_date DATE NULL,
    budget DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    actual_cost DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    expected_revenue DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    description TEXT NULL,
    owner_id BIGINT NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_campaigns_status (status),
    INDEX idx_campaigns_owner (owner_id),
    INDEX idx_campaigns_is_deleted (is_deleted),
    CONSTRAINT fk_campaigns_owner
        FOREIGN KEY (owner_id) REFERENCES users(id)
        ON DELETE RESTRICT
);

-- 2. Bảng quản lý Lead (khách hàng tiềm năng)
CREATE TABLE IF NOT EXISTS leads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(150) NOT NULL,
    company_name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    phone VARCHAR(50) NULL,
    source VARCHAR(50) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    assigned_to BIGINT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_leads_status (status),
    INDEX idx_leads_assigned (assigned_to),
    INDEX idx_leads_is_deleted (is_deleted),
    CONSTRAINT fk_leads_assigned
        FOREIGN KEY (assigned_to) REFERENCES users(id)
        ON DELETE SET NULL
);

-- 3. Bảng liên kết Chiến dịch - Lead (campaign_leads)
CREATE TABLE IF NOT EXISTS campaign_leads (
    campaign_id BIGINT NOT NULL,
    lead_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SENT',
    notes VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (campaign_id, lead_id),
    INDEX idx_cl_status (status),
    CONSTRAINT fk_cl_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_cl_lead
        FOREIGN KEY (lead_id) REFERENCES leads(id)
        ON DELETE CASCADE
);

-- 4. Bổ sung Quyền cho Module Chiến dịch vào permissions
INSERT IGNORE INTO permissions (code, name) VALUES
    ('campaign.read', 'Xem chiến dịch'),
    ('campaign.create', 'Tạo chiến dịch'),
    ('campaign.update', 'Sửa chiến dịch'),
    ('campaign.delete', 'Xóa chiến dịch'),
    ('campaign.lead_manage', 'Quản lý Lead trong chiến dịch');

-- Gán quyền cho ADMIN
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code LIKE 'campaign.%';

-- 5. Dữ liệu mẫu ban đầu để kiểm thử luồng tích hợp
INSERT IGNORE INTO leads (id, full_name, company_name, email, phone, source, status) VALUES
    (1, 'Nguyễn Văn An', 'Công ty TNHH Giải Pháp Số', 'an.nguyen@giaiphapso.vn', '0912345678', 'WEBSITE', 'NEW'),
    (2, 'Trần Thị Bích', 'Tập đoàn Bán lẻ Á Châu', 'bich.tran@achau.com', '0987654321', 'REFERRAL', 'CONTACTED'),
    (3, 'Lê Hoàng Minh', 'Công ty CP Đầu Tư Alpha', 'minh.le@alphainvest.vn', '0903112233', 'CAMPAIGN', 'QUALIFIED');
