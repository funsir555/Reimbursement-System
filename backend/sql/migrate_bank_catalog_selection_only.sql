USE finex_db;

SET NAMES utf8mb4;

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_bank_catalog ADD COLUMN business_scope VARCHAR(16) NOT NULL DEFAULT ''BOTH'' COMMENT ''业务范围:PRIVATE/PUBLIC/BOTH'' AFTER bank_name',
        'SELECT ''sys_bank_catalog.business_scope exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_bank_catalog'
      AND COLUMN_NAME = 'business_scope'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_bank_catalog
SET business_scope = 'BOTH'
WHERE business_scope IS NULL OR TRIM(business_scope) = '';

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_company_bank_account ADD COLUMN province VARCHAR(64) NULL COMMENT ''开户省'' AFTER bank_code',
        'SELECT ''sys_company_bank_account.province exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_company_bank_account'
      AND COLUMN_NAME = 'province'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_company_bank_account ADD COLUMN city VARCHAR(64) NULL COMMENT ''开户市'' AFTER province',
        'SELECT ''sys_company_bank_account.city exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_company_bank_account'
      AND COLUMN_NAME = 'city'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE sys_bank_catalog
    MODIFY COLUMN bank_code VARCHAR(64) NOT NULL COMMENT '银行编码',
    MODIFY COLUMN bank_name VARCHAR(200) NOT NULL COMMENT '银行名称',
    MODIFY COLUMN business_scope VARCHAR(16) NOT NULL DEFAULT 'BOTH' COMMENT '业务范围:PRIVATE/PUBLIC/BOTH',
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    MODIFY COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '国内银行目录表';

ALTER TABLE sys_company_bank_account
    MODIFY COLUMN bank_name VARCHAR(200) NOT NULL COMMENT '银行名称',
    MODIFY COLUMN bank_code VARCHAR(64) NULL COMMENT '银行编码',
    MODIFY COLUMN province VARCHAR(64) NULL COMMENT '开户省',
    MODIFY COLUMN city VARCHAR(64) NULL COMMENT '开户市',
    MODIFY COLUMN branch_name VARCHAR(200) NULL COMMENT '开户支行',
    MODIFY COLUMN branch_code VARCHAR(64) NULL COMMENT '支行编码',
    MODIFY COLUMN cnaps_code VARCHAR(64) NULL COMMENT '联行号',
    COMMENT = '公司银行账户表';
