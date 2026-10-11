-- NovaCRM migration 019: Quotes v2, MySQL 8.0+
-- Run AFTER migrations 001..018, against the intended database explicitly (mysql -D ...).
-- No DROP TABLE; no disabling foreign keys; no business-data copy.
-- IMPORTANT: existing legacy quotes with rows require a separate reviewed data migration.

DELIMITER $$
DROP PROCEDURE IF EXISTS novacrm_migrate_quotes_v2_019$$
CREATE PROCEDURE novacrm_migrate_quotes_v2_019()
BEGIN
    DECLARE has_quotes INT DEFAULT 0;
    DECLARE has_new_id INT DEFAULT 0;
    DECLARE has_new_number INT DEFAULT 0;
    DECLARE has_archive INT DEFAULT 0;
    DECLARE existing_rows BIGINT DEFAULT 0;

    SELECT COUNT(*) INTO has_quotes
      FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotes';

    IF has_quotes > 0 THEN
        SELECT COUNT(*) INTO has_new_id
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotes'
           AND COLUMN_NAME = 'id' AND DATA_TYPE = 'bigint';
        SELECT COUNT(*) INTO has_new_number
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotes'
           AND COLUMN_NAME = 'quote_number';

        IF has_new_id = 0 OR has_new_number = 0 THEN
            SELECT COUNT(*) INTO has_archive
              FROM information_schema.TABLES
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotes_legacy_before_upgrade';
            IF has_archive > 0 THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Legacy quotes archive already exists: stop and inspect manually';
            END IF;

            SELECT COUNT(*) INTO existing_rows FROM quotes;
            IF existing_rows > 0 THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Legacy quotes contains data: migration 019 stopped to prevent data loss';
            END IF;

            RENAME TABLE quotes TO quotes_legacy_before_upgrade;
        END IF;
    END IF;
END$$
DELIMITER ;

CALL novacrm_migrate_quotes_v2_019();
DROP PROCEDURE IF EXISTS novacrm_migrate_quotes_v2_019;

CREATE TABLE IF NOT EXISTS quotes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    quote_number VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    customer_id BIGINT DEFAULT NULL,
    opportunity_id BIGINT DEFAULT NULL,
    discount_percent DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
    subtotal DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    requires_approval TINYINT(1) NOT NULL DEFAULT 0,
    owner_user_id BIGINT NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_quotes_v2_number (quote_number),
    KEY idx_quotes_v2_customer (customer_id),
    KEY idx_quotes_v2_opportunity (opportunity_id),
    KEY idx_quotes_v2_owner (owner_user_id),
    CONSTRAINT fk_quotes_v2_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_quotes_v2_opportunity FOREIGN KEY (opportunity_id) REFERENCES opportunities (id),
    CONSTRAINT fk_quotes_v2_owner FOREIGN KEY (owner_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quote_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    quote_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    discount_percent DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
    amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    KEY idx_quote_items_v2_quote (quote_id),
    KEY idx_quote_items_v2_product (product_id),
    CONSTRAINT fk_quote_items_v2_quote FOREIGN KEY (quote_id) REFERENCES quotes (id),
    CONSTRAINT fk_quote_items_v2_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Verification: each query must return 1, 1, 5 respectively.
SELECT COUNT(*) AS quotes_v2_present
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quotes' AND COLUMN_NAME = 'quote_number';
SELECT COUNT(*) AS quote_items_present
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quote_items';
SELECT COUNT(*) AS quotes_v2_foreign_keys
  FROM information_schema.KEY_COLUMN_USAGE
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('quotes','quote_items')
   AND REFERENCED_TABLE_NAME IS NOT NULL;
