-- S3-04 only. Select the target CRM database first. Apply after Customer foundation.
-- Existing rows are not rewritten or deleted.
DELIMITER $$
DROP PROCEDURE IF EXISTS EnsureS304MergeColumns$$
CREATE PROCEDURE EnsureS304MergeColumns()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='customers' AND COLUMN_NAME='merged_into_id') THEN
  ALTER TABLE customers ADD COLUMN merged_into_id BIGINT NULL, ADD KEY ix_s3_04_merged_into(merged_into_id);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='customers' AND COLUMN_NAME='merged_at') THEN
  ALTER TABLE customers ADD COLUMN merged_at DATETIME NULL;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='customers' AND COLUMN_NAME='merged_by') THEN
  ALTER TABLE customers ADD COLUMN merged_by BIGINT NULL;
 END IF;
END$$
CALL EnsureS304MergeColumns()$$
DROP PROCEDURE EnsureS304MergeColumns$$
DELIMITER ;
CREATE TABLE IF NOT EXISTS customer_merge_history (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 source_customer_id BIGINT NOT NULL,target_customer_id BIGINT NOT NULL,
 performed_by BIGINT NOT NULL,performed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 before_source JSON NOT NULL,before_target JSON NOT NULL,after_target JSON NOT NULL,
 field_overrides JSON NOT NULL,transfer_counts JSON NOT NULL,
 UNIQUE KEY uq_s3_04_merge_source(source_customer_id),
 KEY ix_s3_04_merge_target(target_customer_id,performed_at),
 FOREIGN KEY(source_customer_id) REFERENCES customers(id),
 FOREIGN KEY(target_customer_id) REFERENCES customers(id),
 FOREIGN KEY(performed_by) REFERENCES users(id)
) ENGINE=InnoDB;
INSERT IGNORE INTO permissions(code,name)
VALUES ('customer.merge','Merge duplicate customers');
INSERT IGNORE INTO role_permissions(role_id,permission_id)
 SELECT r.id,p.id FROM roles r JOIN permissions p ON p.code='customer.merge'
 WHERE r.code IN ('ADMIN','DIRECTOR','TEAM_LEAD');
