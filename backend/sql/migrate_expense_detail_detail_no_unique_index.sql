USE finex_db;

SET NAMES utf8mb4;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'SELECT 1',
        'ALTER TABLE pm_document_expense_detail DROP INDEX uk_pm_document_expense_detail_no'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND INDEX_NAME = 'uk_pm_document_expense_detail_no'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_expense_detail ADD UNIQUE KEY uk_pm_document_expense_detail_doc_no (document_code, detail_no)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND INDEX_NAME = 'uk_pm_document_expense_detail_doc_no'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
