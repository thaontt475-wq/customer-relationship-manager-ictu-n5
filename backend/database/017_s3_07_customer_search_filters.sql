-- S3-07. Select the intended CRM database. Run after migrations 006, 009 and 012.
-- Region is explicit business data; existing addresses are never used to infer it.
-- Existing Customer/Contact data is not rewritten or deleted.
-- Saved-filter persistence is prepared internally; shared /api/saved-filters Contract is pending.
-- Region write API is pending; this migration does not backfill or infer any region values.
DELIMITER $$
DROP PROCEDURE IF EXISTS EnsureS307SearchSchema$$
CREATE PROCEDURE EnsureS307SearchSchema()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='customers' AND COLUMN_NAME='region') THEN
        ALTER TABLE customers ADD COLUMN region VARCHAR(20) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='customers' AND INDEX_NAME='ix_s3_07_owner_active_id') THEN
        ALTER TABLE customers ADD INDEX ix_s3_07_owner_active_id(owner_user_id,is_deleted,id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='contacts' AND INDEX_NAME='ix_s3_07_contact_customer_active') THEN
        ALTER TABLE contacts ADD INDEX ix_s3_07_contact_customer_active(customer_id,is_deleted);
    END IF;
END$$
CALL EnsureS307SearchSchema()$$
DROP PROCEDURE EnsureS307SearchSchema$$
DELIMITER ;

CREATE TABLE IF NOT EXISTS saved_customer_filters (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    criteria JSON NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY ix_s3_07_saved_filter_user(user_id,id),
    FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB;
