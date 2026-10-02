-- CRM-50: actual quote-discount and monthly sales-target workflows (CRM-37 audit enhancement).
-- Additive MySQL 8 migration. Back up and inspect before execution.
USE crm_db;

SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='quotes' AND column_name='discount_percent'),
 'SELECT 1', 'ALTER TABLE quotes ADD COLUMN discount_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS sales_targets (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 period_month DATE NOT NULL,
 amount DECIMAL(15,2) NOT NULL,
 UNIQUE KEY uq_target_user_month(user_id,period_month),
 CONSTRAINT fk_target_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE RESTRICT,
 CONSTRAINT chk_target_amount CHECK(amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
