USE finex_db;

SET NAMES utf8mb4;

SET @maker_user_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gl_accvouch'
      AND COLUMN_NAME = 'maker_user_id'
);
SET @maker_user_id_sql = IF(
    @maker_user_id_exists = 0,
    'ALTER TABLE gl_accvouch ADD COLUMN maker_user_id BIGINT NULL COMMENT ''制单人用户ID'' AFTER cbill',
    'SELECT ''gl_accvouch.maker_user_id exists'''
);
PREPARE stmt FROM @maker_user_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @void_flag_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gl_accvouch'
      AND COLUMN_NAME = 'void_flag'
);
SET @void_flag_sql = IF(
    @void_flag_exists = 0,
    'ALTER TABLE gl_accvouch ADD COLUMN void_flag TINYINT NULL COMMENT ''作废标记'' AFTER iflag',
    'SELECT ''gl_accvouch.void_flag exists'''
);
PREPARE stmt FROM @void_flag_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @voided_at_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gl_accvouch'
      AND COLUMN_NAME = 'voided_at'
);
SET @voided_at_sql = IF(
    @voided_at_exists = 0,
    'ALTER TABLE gl_accvouch ADD COLUMN voided_at DATETIME NULL COMMENT ''作废时间'' AFTER void_flag',
    'SELECT ''gl_accvouch.voided_at exists'''
);
PREPARE stmt FROM @voided_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @voided_by_user_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gl_accvouch'
      AND COLUMN_NAME = 'voided_by_user_id'
);
SET @voided_by_user_id_sql = IF(
    @voided_by_user_id_exists = 0,
    'ALTER TABLE gl_accvouch ADD COLUMN voided_by_user_id BIGINT NULL COMMENT ''作废人用户ID'' AFTER voided_at',
    'SELECT ''gl_accvouch.voided_by_user_id exists'''
);
PREPARE stmt FROM @voided_by_user_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @voided_by_name_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gl_accvouch'
      AND COLUMN_NAME = 'voided_by_name'
);
SET @voided_by_name_sql = IF(
    @voided_by_name_exists = 0,
    'ALTER TABLE gl_accvouch ADD COLUMN voided_by_name VARCHAR(64) NULL COMMENT ''作废人名称'' AFTER voided_by_user_id',
    'SELECT ''gl_accvouch.voided_by_name exists'''
);
PREPARE stmt FROM @voided_by_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @reversed_from_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gl_accvouch'
      AND COLUMN_NAME = 'reversed_from_voucher_no'
);
SET @reversed_from_sql = IF(
    @reversed_from_exists = 0,
    'ALTER TABLE gl_accvouch ADD COLUMN reversed_from_voucher_no VARCHAR(128) NULL COMMENT ''冲销来源凭证号'' AFTER voided_by_name',
    'SELECT ''gl_accvouch.reversed_from_voucher_no exists'''
);
PREPARE stmt FROM @reversed_from_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @reversed_by_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gl_accvouch'
      AND COLUMN_NAME = 'reversed_by_voucher_no'
);
SET @reversed_by_sql = IF(
    @reversed_by_exists = 0,
    'ALTER TABLE gl_accvouch ADD COLUMN reversed_by_voucher_no VARCHAR(128) NULL COMMENT ''被哪张冲销凭证冲销'' AFTER reversed_from_voucher_no',
    'SELECT ''gl_accvouch.reversed_by_voucher_no exists'''
);
PREPARE stmt FROM @reversed_by_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @unified_no_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gl_accvouch'
      AND INDEX_NAME = 'idx_gl_accvouch_company_year_period_unified_no'
);
SET @unified_no_index_sql = IF(
    @unified_no_index_exists = 0,
    'ALTER TABLE gl_accvouch ADD INDEX idx_gl_accvouch_company_year_period_unified_no (company_id, iyear, iyperiod, ino_id)',
    'SELECT ''idx_gl_accvouch_company_year_period_unified_no exists'''
);
PREPARE stmt FROM @unified_no_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
