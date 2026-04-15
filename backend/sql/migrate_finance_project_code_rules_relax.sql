USE finex_db;

SET NAMES utf8mb4;

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'SELECT ''skip drop ck_fin_project_class_code_format''',
        'ALTER TABLE `fin_project_class` DROP CONSTRAINT `ck_fin_project_class_code_format`'
    )
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'fin_project_class'
      AND CONSTRAINT_NAME = 'ck_fin_project_class_code_format'
      AND CONSTRAINT_TYPE = 'CHECK'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `fin_project_class`
    ADD CONSTRAINT `ck_fin_project_class_code_format`
    CHECK (REGEXP_LIKE(`project_class_code`, '^[0-9]{1,2}$'));

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'SELECT ''skip drop ck_fin_project_archive_code_format''',
        'ALTER TABLE `fin_project_archive` DROP CONSTRAINT `ck_fin_project_archive_code_format`'
    )
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'fin_project_archive'
      AND CONSTRAINT_NAME = 'ck_fin_project_archive_code_format'
      AND CONSTRAINT_TYPE = 'CHECK'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `fin_project_archive`
    ADD CONSTRAINT `ck_fin_project_archive_code_format`
    CHECK (REGEXP_LIKE(`citemcode`, '^[0-9]{1,6}$'));

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'SELECT ''skip drop ck_fin_project_archive_class_format''',
        'ALTER TABLE `fin_project_archive` DROP CONSTRAINT `ck_fin_project_archive_class_format`'
    )
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'fin_project_archive'
      AND CONSTRAINT_NAME = 'ck_fin_project_archive_class_format'
      AND CONSTRAINT_TYPE = 'CHECK'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `fin_project_archive`
    ADD CONSTRAINT `ck_fin_project_archive_class_format`
    CHECK (REGEXP_LIKE(`citemccode`, '^[0-9]{1,2}$'));
