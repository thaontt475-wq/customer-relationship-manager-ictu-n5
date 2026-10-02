-- CRM-51: real product references and discount-approval workflow (CRM-39 enhancement).
-- Additive MySQL 8 migration. Back up and inspect before execution.

CREATE TABLE IF NOT EXISTS quote_items (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 quote_id BIGINT NOT NULL, product_id BIGINT NOT NULL,
 product_code VARCHAR(50) NOT NULL, product_name VARCHAR(255) NOT NULL, unit VARCHAR(80) NULL,
 quantity DECIMAL(12,2) NOT NULL, unit_price DECIMAL(15,2) NOT NULL,
 list_price_snapshot DECIMAL(15,2) NOT NULL, floor_price_snapshot DECIMAL(15,2) NOT NULL,
 CONSTRAINT fk_quote_item_quote FOREIGN KEY(quote_id) REFERENCES quotes(id) ON DELETE RESTRICT,
 CONSTRAINT fk_quote_item_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE RESTRICT,
 CONSTRAINT chk_quote_item_quantity CHECK(quantity > 0),
 CONSTRAINT chk_quote_item_price CHECK(unit_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS quote_pricing_approvals (
 quote_id BIGINT PRIMARY KEY,
 status VARCHAR(30) NOT NULL,
 approved_by BIGINT NULL,
 approved_at DATETIME NULL,
 CONSTRAINT fk_quote_approval_quote FOREIGN KEY(quote_id) REFERENCES quotes(id) ON DELETE RESTRICT,
 CONSTRAINT fk_quote_approval_actor FOREIGN KEY(approved_by) REFERENCES users(id) ON DELETE RESTRICT,
 CONSTRAINT chk_quote_approval_status CHECK(status IN ('DRAFT','PENDING_APPROVAL','APPROVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
