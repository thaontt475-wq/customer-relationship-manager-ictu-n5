USE crm_db;

DELIMITER $$

DROP PROCEDURE IF EXISTS AddCol$$
CREATE PROCEDURE AddCol(
    IN p_table VARCHAR(64),
    IN p_col VARCHAR(64),
    IN p_def VARCHAR(255)
)
BEGIN
    IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_col) = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_col, '` ', p_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- 1. products
CALL AddCol('products', 'type', 'VARCHAR(50) NOT NULL DEFAULT \'ONE_TIME\'');
CALL AddCol('products', 'active', 'TINYINT(1) NOT NULL DEFAULT 1');

-- 2. win_loss_reasons
CALL AddCol('win_loss_reasons', 'type', 'VARCHAR(10) NULL');
CALL AddCol('win_loss_reasons', 'name', 'VARCHAR(255) NULL');
CALL AddCol('win_loss_reasons', 'active', 'TINYINT(1) NOT NULL DEFAULT 1');

-- 3. competitors
CALL AddCol('competitors', 'note', 'VARCHAR(1000) NULL');
CALL AddCol('competitors', 'active', 'TINYINT(1) NOT NULL DEFAULT 1');

-- 4. pipeline_stages
CALL AddCol('pipeline_stages', 'order_no', 'INT NOT NULL DEFAULT 1');
CALL AddCol('pipeline_stages', 'probability', 'INT NOT NULL DEFAULT 0');
CALL AddCol('pipeline_stages', 'active', 'TINYINT(1) NOT NULL DEFAULT 1');
CALL AddCol('pipeline_stages', 'exit_condition', 'VARCHAR(255) NULL');
CALL AddCol('pipeline_stages', 'condition_required', 'TINYINT(1) NOT NULL DEFAULT 0');

-- 5. master_data
CALL AddCol('master_data', 'display_order', 'INT NOT NULL DEFAULT 1');

DROP PROCEDURE IF EXISTS AddCol;

-- Sync existing data
-- NOVACRM_LEGACY_SYNC_GUARD
SET @novacrm_cols = (
    SELECT COUNT(DISTINCT COLUMN_NAME)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'products'
      AND COLUMN_NAME IN ('active', 'is_active')
);

SET @novacrm_sql = IF(
    @novacrm_cols = 2,
    'UPDATE products SET active = is_active WHERE is_active IS NOT NULL;',
    'SELECT 1'
);

PREPARE novacrm_stmt FROM @novacrm_sql;
EXECUTE novacrm_stmt;
DEALLOCATE PREPARE novacrm_stmt;
-- NOVACRM_LEGACY_SYNC_GUARD
SET @novacrm_cols = (
    SELECT COUNT(DISTINCT COLUMN_NAME)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'win_loss_reasons'
      AND COLUMN_NAME IN ('type', 'name', 'active', 'reason_type', 'reason_text', 'is_active')
);

SET @novacrm_sql = IF(
    @novacrm_cols = 6,
    'UPDATE win_loss_reasons SET type = reason_type, name = reason_text, active = is_active;',
    'SELECT 1'
);

PREPARE novacrm_stmt FROM @novacrm_sql;
EXECUTE novacrm_stmt;
DEALLOCATE PREPARE novacrm_stmt;
-- NOVACRM_LEGACY_SYNC_GUARD
SET @novacrm_cols = (
    SELECT COUNT(DISTINCT COLUMN_NAME)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'competitors'
      AND COLUMN_NAME IN ('note', 'weaknesses', 'strengths', 'active', 'is_active')
);

SET @novacrm_sql = IF(
    @novacrm_cols = 5,
    'UPDATE competitors SET note = COALESCE(weaknesses, strengths, ''''), active = is_active;',
    'SELECT 1'
);

PREPARE novacrm_stmt FROM @novacrm_sql;
EXECUTE novacrm_stmt;
DEALLOCATE PREPARE novacrm_stmt;
-- NOVACRM_LEGACY_SYNC_GUARD
SET @novacrm_cols = (
    SELECT COUNT(DISTINCT COLUMN_NAME)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pipeline_stages'
      AND COLUMN_NAME IN ('order_no', 'stage_order', 'probability', 'win_probability', 'active', 'is_active')
);

SET @novacrm_sql = IF(
    @novacrm_cols = 6,
    'UPDATE pipeline_stages SET order_no = stage_order, probability = win_probability, active = is_active;',
    'SELECT 1'
);

PREPARE novacrm_stmt FROM @novacrm_sql;
EXECUTE novacrm_stmt;
DEALLOCATE PREPARE novacrm_stmt;
