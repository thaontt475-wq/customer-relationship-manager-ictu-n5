USE crm_db;

ALTER TABLE audit_logs
    MODIFY COLUMN actor_user_id BIGINT NULL,
    MODIFY COLUMN object_type VARCHAR(50) NULL,
    MODIFY COLUMN object_id BIGINT NULL,
    MODIFY COLUMN before_value LONGTEXT NULL,
    MODIFY COLUMN after_value LONGTEXT NULL;

SET @col_user_id = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='crm_db' AND TABLE_NAME='audit_logs' AND COLUMN_NAME='user_id');
SET @sql = IF(@col_user_id = 0, 'ALTER TABLE audit_logs ADD COLUMN user_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_entity_type = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='crm_db' AND TABLE_NAME='audit_logs' AND COLUMN_NAME='entity_type');
SET @sql = IF(@col_entity_type = 0, 'ALTER TABLE audit_logs ADD COLUMN entity_type VARCHAR(100) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_entity_id = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='crm_db' AND TABLE_NAME='audit_logs' AND COLUMN_NAME='entity_id');
SET @sql = IF(@col_entity_id = 0, 'ALTER TABLE audit_logs ADD COLUMN entity_id VARCHAR(100) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_description = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='crm_db' AND TABLE_NAME='audit_logs' AND COLUMN_NAME='description');
SET @sql = IF(@col_description = 0, 'ALTER TABLE audit_logs ADD COLUMN description VARCHAR(1000) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE audit_logs SET user_id = actor_user_id WHERE user_id IS NULL AND actor_user_id IS NOT NULL;
UPDATE audit_logs SET entity_type = object_type WHERE entity_type IS NULL AND object_type IS NOT NULL;
UPDATE audit_logs SET entity_id = CAST(object_id AS CHAR) WHERE entity_id IS NULL AND object_id IS NOT NULL;

-- Trigger hoặc đồng bộ 2 chiều
DROP TRIGGER IF EXISTS trg_audit_logs_sync_bi;
DELIMITER $$
CREATE TRIGGER trg_audit_logs_sync_bi
BEFORE INSERT ON audit_logs
FOR EACH ROW
BEGIN
    IF NEW.user_id IS NOT NULL AND NEW.actor_user_id IS NULL THEN
        SET NEW.actor_user_id = NEW.user_id;
    ELSEIF NEW.actor_user_id IS NOT NULL AND NEW.user_id IS NULL THEN
        SET NEW.user_id = NEW.actor_user_id;
    END IF;

    IF NEW.entity_type IS NOT NULL AND NEW.object_type IS NULL THEN
        SET NEW.object_type = NEW.entity_type;
    ELSEIF NEW.object_type IS NOT NULL AND NEW.entity_type IS NULL THEN
        SET NEW.entity_type = NEW.object_type;
    END IF;

    IF NEW.entity_id IS NOT NULL AND NEW.object_id IS NULL THEN
        BEGIN
            DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;
            SET NEW.object_id = CAST(NEW.entity_id AS UNSIGNED);
        END;
    ELSEIF NEW.object_id IS NOT NULL AND NEW.entity_id IS NULL THEN
        SET NEW.entity_id = CAST(NEW.object_id AS CHAR);
    END IF;
END$$
DELIMITER ;
