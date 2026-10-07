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

-- 1. users: email_signature
CALL AddCol('users', 'email_signature', 'TEXT NULL');
CALL AddCol('users', 'phone', 'VARCHAR(30) NULL');
CALL AddCol('users', 'avatar_url', 'VARCHAR(500) NULL');
CALL AddCol('users', 'avatar_thumbnail_url', 'VARCHAR(500) NULL');

-- 2. custom_fields: options_json
CALL AddCol('custom_fields', 'options_json', 'TEXT NULL');

DROP PROCEDURE IF EXISTS AddCol;

-- Sync existing data
UPDATE users SET email_signature = signature WHERE (email_signature IS NULL OR email_signature = '') AND signature IS NOT NULL;
UPDATE users SET signature = email_signature WHERE (signature IS NULL OR signature = '') AND email_signature IS NOT NULL;
