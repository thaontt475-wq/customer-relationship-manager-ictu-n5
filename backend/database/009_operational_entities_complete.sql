USE crm_db;

DELIMITER $$

DROP PROCEDURE IF EXISTS AddColumnIfNotExists$$
CREATE PROCEDURE AddColumnIfNotExists(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition VARCHAR(255)
)
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO col_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name;

    IF col_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- 1. Bảng customers
CALL AddColumnIfNotExists('customers', 'tax_code', 'VARCHAR(50) NULL');
CALL AddColumnIfNotExists('customers', 'status', 'VARCHAR(50) NOT NULL DEFAULT \'TIEM_NANG\'');
CALL AddColumnIfNotExists('customers', 'email', 'VARCHAR(255) NULL');
CALL AddColumnIfNotExists('customers', 'phone', 'VARCHAR(50) NULL');
CALL AddColumnIfNotExists('customers', 'website', 'VARCHAR(255) NULL');
CALL AddColumnIfNotExists('customers', 'address', 'VARCHAR(500) NULL');
CALL AddColumnIfNotExists('customers', 'is_deleted', 'TINYINT(1) NOT NULL DEFAULT 0');
CALL AddColumnIfNotExists('customers', 'updated_at', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

-- 2. Bảng opportunities
CALL AddColumnIfNotExists('opportunities', 'customer_id', 'BIGINT NULL');
CALL AddColumnIfNotExists('opportunities', 'expected_close_date', 'DATE NULL');
CALL AddColumnIfNotExists('opportunities', 'competitor_id', 'BIGINT NULL');
CALL AddColumnIfNotExists('opportunities', 'win_reason_id', 'BIGINT NULL');
CALL AddColumnIfNotExists('opportunities', 'loss_reason_id', 'BIGINT NULL');
CALL AddColumnIfNotExists('opportunities', 'status', 'VARCHAR(20) NOT NULL DEFAULT \'OPEN\'');
CALL AddColumnIfNotExists('opportunities', 'is_deleted', 'TINYINT(1) NOT NULL DEFAULT 0');
CALL AddColumnIfNotExists('opportunities', 'updated_at', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

-- 3. Bảng activities
CALL AddColumnIfNotExists('activities', 'customer_id', 'BIGINT NULL');
CALL AddColumnIfNotExists('activities', 'opportunity_id', 'BIGINT NULL');
CALL AddColumnIfNotExists('activities', 'type', 'VARCHAR(50) NOT NULL DEFAULT \'CALL\'');
CALL AddColumnIfNotExists('activities', 'description', 'TEXT NULL');
CALL AddColumnIfNotExists('activities', 'status', 'VARCHAR(50) NOT NULL DEFAULT \'COMPLETED\'');
CALL AddColumnIfNotExists('activities', 'due_date', 'DATETIME NULL');
CALL AddColumnIfNotExists('activities', 'is_deleted', 'TINYINT(1) NOT NULL DEFAULT 0');
CALL AddColumnIfNotExists('activities', 'updated_at', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

-- 4. Bảng quotes & quote_items
CALL AddColumnIfNotExists('quotes', 'customer_id', 'BIGINT NULL');
CALL AddColumnIfNotExists('quotes', 'opportunity_id', 'BIGINT NULL');
CALL AddColumnIfNotExists('quotes', 'title', 'VARCHAR(255) NULL');
CALL AddColumnIfNotExists('quotes', 'subtotal', 'DECIMAL(15,2) NOT NULL DEFAULT 0.00');
CALL AddColumnIfNotExists('quotes', 'total_amount', 'DECIMAL(15,2) NOT NULL DEFAULT 0.00');
CALL AddColumnIfNotExists('quotes', 'status', 'VARCHAR(50) NOT NULL DEFAULT \'DRAFT\'');
CALL AddColumnIfNotExists('quotes', 'requires_approval', 'TINYINT(1) NOT NULL DEFAULT 0');
CALL AddColumnIfNotExists('quotes', 'is_deleted', 'TINYINT(1) NOT NULL DEFAULT 0');
CALL AddColumnIfNotExists('quotes', 'updated_at', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');

CALL AddColumnIfNotExists('quote_items', 'discount_percent', 'DECIMAL(5,2) NOT NULL DEFAULT 0.00');
CALL AddColumnIfNotExists('quote_items', 'amount', 'DECIMAL(15,2) NOT NULL DEFAULT 0.00');

DROP PROCEDURE IF EXISTS AddColumnIfNotExists;
