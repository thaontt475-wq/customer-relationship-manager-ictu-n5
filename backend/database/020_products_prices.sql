-- NOVACRM 020: Product pricing columns required by ProductDAO.
-- Safe to rerun. Review existing legacy pricing data before production use.
USE crm_db;

DROP PROCEDURE IF EXISTS NovacrmEnsureProductPrices;

DELIMITER $$

CREATE PROCEDURE NovacrmEnsureProductPrices()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'products'
          AND COLUMN_NAME = 'list_price'
    ) THEN
        ALTER TABLE products
            ADD COLUMN list_price DECIMAL(18,2) NOT NULL DEFAULT 0;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'products'
          AND COLUMN_NAME = 'floor_price'
    ) THEN
        ALTER TABLE products
            ADD COLUMN floor_price DECIMAL(18,2) NOT NULL DEFAULT 0;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'products'
          AND COLUMN_NAME = 'cost_price'
    ) THEN
        ALTER TABLE products
            ADD COLUMN cost_price DECIMAL(18,2) NOT NULL DEFAULT 0;
    END IF;
END$$

DELIMITER ;

CALL NovacrmEnsureProductPrices();

DROP PROCEDURE IF EXISTS NovacrmEnsureProductPrices;