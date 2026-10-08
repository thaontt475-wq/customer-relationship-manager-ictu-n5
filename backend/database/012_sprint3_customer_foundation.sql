-- S3 foundation for a fresh crm_db. Review against S3-01/S3-02 ownership before applying.
-- Requires users, permissions, role_permissions, role_module_scopes to exist.
USE crm_db;
CREATE TABLE IF NOT EXISTS customers (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 name VARCHAR(255) NOT NULL,
 tax_code VARCHAR(50) NULL,
 status VARCHAR(50) NOT NULL DEFAULT 'TIEM_NANG',
 email VARCHAR(255) NULL, phone VARCHAR(50) NULL,website VARCHAR(255) NULL,address VARCHAR(500) NULL,
 industry_id BIGINT NULL, company_size_id BIGINT NULL,
 owner_user_id BIGINT NOT NULL,
 parent_customer_id BIGINT NULL,
 is_deleted TINYINT(1) NOT NULL DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uq_customers_tax_code(tax_code),
 KEY ix_customer_owner(owner_user_id), KEY ix_customer_parent(parent_customer_id),
 FOREIGN KEY(owner_user_id) REFERENCES users(id),
 FOREIGN KEY(parent_customer_id) REFERENCES customers(id) ON DELETE SET NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS contacts (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,customer_id BIGINT NOT NULL,
 full_name VARCHAR(200) NOT NULL,email VARCHAR(255),phone VARCHAR(50),title VARCHAR(150),
 buying_role VARCHAR(40),is_primary TINYINT(1) DEFAULT 0,is_deleted TINYINT(1) DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY ix_contacts_customer(customer_id),FOREIGN KEY(customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS opportunities (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,name VARCHAR(255) NOT NULL,customer_id BIGINT,
 contact_name VARCHAR(255),amount DECIMAL(18,2) NOT NULL DEFAULT 0,
 stage_id BIGINT,probability INT DEFAULT 0,expected_close_date DATE,
 status VARCHAR(20) NOT NULL DEFAULT 'OPEN',win_reason_id BIGINT,loss_reason_id BIGINT,
 competitor_id BIGINT,lost_reason TEXT,owner_user_id BIGINT NOT NULL,
 is_deleted TINYINT(1) NOT NULL DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY ix_opportunities_customer(customer_id,status),
 FOREIGN KEY(customer_id) REFERENCES customers(id),FOREIGN KEY(owner_user_id) REFERENCES users(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS activities (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,subject VARCHAR(255) NOT NULL,type VARCHAR(50) NOT NULL DEFAULT 'CALL',
 description TEXT,status VARCHAR(50) DEFAULT 'COMPLETED',due_date DATETIME,
 customer_id BIGINT,opportunity_id BIGINT,owner_user_id BIGINT NOT NULL,
 is_deleted TINYINT(1) NOT NULL DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY ix_activities_customer_date(customer_id,is_deleted,created_at),
 FOREIGN KEY(customer_id) REFERENCES customers(id),FOREIGN KEY(opportunity_id) REFERENCES opportunities(id),
 FOREIGN KEY(owner_user_id) REFERENCES users(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS customer_attachments (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,customer_id BIGINT NOT NULL,
 file_name VARCHAR(255) NOT NULL,file_url VARCHAR(1000) NOT NULL,
 uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,is_deleted TINYINT(1) DEFAULT 0,
 KEY ix_attachments_customer(customer_id),FOREIGN KEY(customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS customer_contracts (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,customer_id BIGINT NOT NULL,
 total_amount DECIMAL(18,2) NOT NULL DEFAULT 0,status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
 signed_at DATETIME NULL,KEY ix_contract_customer(customer_id,status),
 FOREIGN KEY(customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS custom_field_values (
 custom_field_id BIGINT NOT NULL,record_id BIGINT NOT NULL,field_value TEXT,
 PRIMARY KEY (custom_field_id,record_id),
 FOREIGN KEY(custom_field_id) REFERENCES custom_fields(id) ON DELETE CASCADE
) ENGINE=InnoDB;
