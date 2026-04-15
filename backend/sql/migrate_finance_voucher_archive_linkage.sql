USE finex_db;

SET NAMES utf8mb4;

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE gl_accvouch ADD COLUMN ccode_name VARCHAR(128) NULL COMMENT ''科目名称'' AFTER ccode',
        'SELECT ''gl_accvouch.ccode_name exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'gl_accvouch'
      AND COLUMN_NAME = 'ccode_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE gl_accvouch voucher
JOIN fin_account_subject subject
  ON subject.company_id = voucher.company_id
 AND subject.subject_code = voucher.ccode
SET voucher.ccode_name = subject.subject_name
WHERE voucher.ccode IS NOT NULL
  AND (voucher.ccode_name IS NULL OR voucher.ccode_name = '');

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE gl_accvouch ADD INDEX idx_gl_accvouch_company_ccode (company_id, ccode)',
        'SELECT ''idx_gl_accvouch_company_ccode exists'''
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'gl_accvouch'
      AND INDEX_NAME = 'idx_gl_accvouch_company_ccode'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE gl_accvouch ADD INDEX idx_gl_accvouch_company_cperson (company_id, cperson_id)',
        'SELECT ''idx_gl_accvouch_company_cperson exists'''
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'gl_accvouch'
      AND INDEX_NAME = 'idx_gl_accvouch_company_cperson'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE gl_accvouch ADD INDEX idx_gl_accvouch_company_ccus (company_id, ccus_id)',
        'SELECT ''idx_gl_accvouch_company_ccus exists'''
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'gl_accvouch'
      AND INDEX_NAME = 'idx_gl_accvouch_company_ccus'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE gl_accvouch ADD INDEX idx_gl_accvouch_company_csup (company_id, csup_id)',
        'SELECT ''idx_gl_accvouch_company_csup exists'''
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'gl_accvouch'
      AND INDEX_NAME = 'idx_gl_accvouch_company_csup'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE gl_accvouch ADD INDEX idx_gl_accvouch_company_citem_class (company_id, citem_class)',
        'SELECT ''idx_gl_accvouch_company_citem_class exists'''
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'gl_accvouch'
      AND INDEX_NAME = 'idx_gl_accvouch_company_citem_class'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE gl_accvouch ADD INDEX idx_gl_accvouch_company_citem_id (company_id, citem_id)',
        'SELECT ''idx_gl_accvouch_company_citem_id exists'''
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'gl_accvouch'
      AND INDEX_NAME = 'idx_gl_accvouch_company_citem_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE gl_accvouch ADD INDEX idx_gl_accvouch_company_citem_class_id (company_id, citem_class, citem_id)',
        'SELECT ''idx_gl_accvouch_company_citem_class_id exists'''
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'gl_accvouch'
      AND INDEX_NAME = 'idx_gl_accvouch_company_citem_class_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP TEMPORARY TABLE IF EXISTS tmp_fin_project_class_code_map;
CREATE TEMPORARY TABLE tmp_fin_project_class_code_map AS
SELECT
    id,
    company_id,
    project_class_code AS old_code,
    LPAD(CAST(ROW_NUMBER() OVER (
        PARTITION BY company_id
        ORDER BY sort_order, project_class_code, id
    ) AS CHAR), 2, '0') AS new_code
FROM fin_project_class;

DROP TEMPORARY TABLE IF EXISTS tmp_fin_project_code_map;
CREATE TEMPORARY TABLE tmp_fin_project_code_map AS
SELECT
    id,
    company_id,
    citemcode AS old_code,
    citemccode AS old_class_code,
    LPAD(CAST(ROW_NUMBER() OVER (
        PARTITION BY company_id
        ORDER BY sort_order, citemcode, id
    ) AS CHAR), 6, '0') AS new_code
FROM fin_project_archive;

SET FOREIGN_KEY_CHECKS = 0;

UPDATE fin_project_archive project
JOIN tmp_fin_project_class_code_map class_map
  ON class_map.company_id = project.company_id
 AND class_map.old_code = project.citemccode
SET project.citemccode = class_map.new_code;

UPDATE gl_accvouch voucher
JOIN tmp_fin_project_class_code_map class_map
  ON class_map.company_id = (voucher.company_id COLLATE utf8mb4_unicode_ci)
 AND class_map.old_code = (voucher.citem_class COLLATE utf8mb4_unicode_ci)
SET voucher.citem_class = class_map.new_code;

UPDATE fin_project_class project_class
JOIN tmp_fin_project_class_code_map class_map
  ON class_map.id = project_class.id
SET project_class.project_class_code = class_map.new_code;

UPDATE fin_project_archive project
JOIN tmp_fin_project_code_map project_map
  ON project_map.id = project.id
SET project.citemcode = project_map.new_code;

UPDATE gl_accvouch voucher
JOIN tmp_fin_project_code_map project_map
  ON project_map.company_id = (voucher.company_id COLLATE utf8mb4_unicode_ci)
 AND project_map.old_code = (voucher.citem_id COLLATE utf8mb4_unicode_ci)
SET voucher.citem_id = project_map.new_code;

SET FOREIGN_KEY_CHECKS = 1;

SET @sql = (
    SELECT IF(
        COUNT(*) = 1,
        'ALTER TABLE fin_project_archive DROP FOREIGN KEY fk_fin_project_archive_class',
        'SELECT ''fk_fin_project_archive_class not exists'''
    )
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'fin_project_archive'
      AND CONSTRAINT_NAME = 'fk_fin_project_archive_class'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE fin_project_class
    MODIFY COLUMN project_class_code VARCHAR(2) NOT NULL COMMENT '项目分类编码';

ALTER TABLE fin_project_archive
    MODIFY COLUMN citemcode VARCHAR(6) NOT NULL COMMENT '项目编码',
    MODIFY COLUMN citemccode VARCHAR(2) NOT NULL COMMENT '项目分类编码';

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE fin_project_archive ADD CONSTRAINT fk_fin_project_archive_class FOREIGN KEY (company_id, citemccode) REFERENCES fin_project_class (company_id, project_class_code)',
        'SELECT ''fk_fin_project_archive_class exists'''
    )
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'fin_project_archive'
      AND CONSTRAINT_NAME = 'fk_fin_project_archive_class'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE fin_project_class ADD CONSTRAINT ck_fin_project_class_code_format CHECK (project_class_code REGEXP ''^[0-9]{1,2}$'')',
        'SELECT ''ck_fin_project_class_code_format exists'''
    )
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'fin_project_class'
      AND CONSTRAINT_NAME = 'ck_fin_project_class_code_format'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE fin_project_archive ADD CONSTRAINT ck_fin_project_archive_code_format CHECK (citemcode REGEXP ''^[0-9]{1,6}$'')',
        'SELECT ''ck_fin_project_archive_code_format exists'''
    )
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'fin_project_archive'
      AND CONSTRAINT_NAME = 'ck_fin_project_archive_code_format'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE fin_project_archive ADD CONSTRAINT ck_fin_project_archive_class_format CHECK (citemccode REGEXP ''^[0-9]{1,2}$'')',
        'SELECT ''ck_fin_project_archive_class_format exists'''
    )
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'fin_project_archive'
      AND CONSTRAINT_NAME = 'ck_fin_project_archive_class_format'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP TEMPORARY TABLE IF EXISTS tmp_fin_project_code_map;
DROP TEMPORARY TABLE IF EXISTS tmp_fin_project_class_code_map;
