-- S3-06 only. Select the intended database before executing.
-- Run after Customer schema migrations 009/012. No existing rows are rewritten.
-- Normalized key makes 10-digit and 13-digit branch tax codes consistent with/without "-".
DELIMITER $$
DROP PROCEDURE IF EXISTS EnsureS306TaxKey$$
CREATE PROCEDURE EnsureS306TaxKey()
BEGIN
    IF EXISTS (
        SELECT 1 FROM customers
        WHERE NULLIF(REPLACE(TRIM(tax_code),'-',''),'') IS NOT NULL
        GROUP BY NULLIF(REPLACE(TRIM(tax_code),'-',''),'')
        HAVING COUNT(*)>1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT='Duplicate customer tax codes exist; resolve with data owner before S3-06 migration';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='customers' AND COLUMN_NAME='tax_code_import_key'
    ) THEN
        ALTER TABLE customers ADD COLUMN tax_code_import_key VARCHAR(50)
            GENERATED ALWAYS AS (NULLIF(REPLACE(TRIM(tax_code),'-',''),'')) STORED;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='customers'
            AND INDEX_NAME='uq_s3_06_customer_tax_key'
    ) THEN
        ALTER TABLE customers ADD UNIQUE KEY uq_s3_06_customer_tax_key(tax_code_import_key);
    END IF;
END$$
CALL EnsureS306TaxKey()$$
DROP PROCEDURE EnsureS306TaxKey$$
DELIMITER ;
