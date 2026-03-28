USE finex_db;

SET NAMES utf8mb4;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_template'
      AND COLUMN_NAME = 'form_design_code'
);

SET @ddl = IF(
    @column_exists > 0,
    'SELECT 1',
    'ALTER TABLE pm_document_template ADD COLUMN form_design_code VARCHAR(64) NULL COMMENT ''表单设计编码'' AFTER numbering_rule'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE pm_document_template
SET form_design_code = CASE template_type
    WHEN 'application' THEN 'application-standard-form'
    WHEN 'loan' THEN 'loan-standard-form'
    ELSE 'expense-standard-form'
END
WHERE form_design_code IS NULL OR form_design_code = '';
