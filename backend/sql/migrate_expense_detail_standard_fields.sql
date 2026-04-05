USE finex_db;

SET NAMES utf8mb4;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_expense_detail ADD COLUMN expense_type_code VARCHAR(64) NULL COMMENT ''expense type code'' AFTER enterprise_mode',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND COLUMN_NAME = 'expense_type_code'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_expense_detail ADD COLUMN business_scene_mode VARCHAR(32) NULL COMMENT ''business scene mode'' AFTER expense_type_code',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND COLUMN_NAME = 'business_scene_mode'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_expense_detail ADD COLUMN invoice_amount DECIMAL(18,2) NULL COMMENT ''invoice amount'' AFTER sort_order',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND COLUMN_NAME = 'invoice_amount'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_expense_detail ADD COLUMN actual_payment_amount DECIMAL(18,2) NULL COMMENT ''actual payment amount'' AFTER invoice_amount',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND COLUMN_NAME = 'actual_payment_amount'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_expense_detail ADD COLUMN pending_write_off_amount DECIMAL(18,2) NULL COMMENT ''pending write off amount'' AFTER actual_payment_amount',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND COLUMN_NAME = 'pending_write_off_amount'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
