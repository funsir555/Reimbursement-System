USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS pm_expense_detail_design (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'column comment',
    detail_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    detail_name VARCHAR(100) NOT NULL COMMENT 'column comment',
    detail_type VARCHAR(32) NOT NULL COMMENT 'column comment',
    detail_description VARCHAR(500) NULL COMMENT 'column comment',
    schema_json LONGTEXT NULL COMMENT 'column comment',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'column comment',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'column comment',
    UNIQUE KEY uk_pm_expense_detail_design_code (detail_code),
    KEY idx_pm_expense_detail_design_type (detail_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='table comment';

CREATE TABLE IF NOT EXISTS pm_document_expense_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'column comment',
    document_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    detail_no VARCHAR(64) NOT NULL COMMENT 'column comment',
    detail_design_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    detail_type VARCHAR(32) NOT NULL COMMENT 'column comment',
    enterprise_mode VARCHAR(32) NULL COMMENT 'column comment',
    expense_type_code VARCHAR(64) NULL COMMENT 'column comment',
    business_scene_mode VARCHAR(32) NULL COMMENT 'column comment',
    detail_title VARCHAR(200) NULL COMMENT 'column comment',
    sort_order INT NOT NULL DEFAULT 1 COMMENT 'column comment',
    invoice_amount DECIMAL(18,2) NULL COMMENT 'column comment',
    actual_payment_amount DECIMAL(18,2) NULL COMMENT 'column comment',
    pending_write_off_amount DECIMAL(18,2) NULL COMMENT 'column comment',
    schema_snapshot_json LONGTEXT NOT NULL COMMENT 'column comment',
    form_data_json LONGTEXT NOT NULL COMMENT 'column comment',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'column comment',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'column comment',
    UNIQUE KEY uk_pm_document_expense_detail_doc_no (document_code, detail_no),
    KEY idx_pm_document_expense_detail_document (document_code, sort_order),
    KEY idx_pm_document_expense_detail_design (detail_design_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='table comment';

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

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_template ADD COLUMN expense_detail_design_code VARCHAR(64) NULL COMMENT ''column comment'' AFTER form_design_code',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_template'
      AND COLUMN_NAME = 'expense_detail_design_code'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_template ADD COLUMN expense_detail_mode_default VARCHAR(32) NULL COMMENT ''column comment'' AFTER expense_detail_design_code',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_template'
      AND COLUMN_NAME = 'expense_detail_mode_default'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
