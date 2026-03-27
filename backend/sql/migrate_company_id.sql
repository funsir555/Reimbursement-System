USE finex_db;

SET NAMES utf8mb4;

/*
集团主体标准脚本

用途:
1. 新增公司主体主数据表 sys_company
2. 为现有业务表统一补充 company_id 字段
3. 为现有业务表统一补充 company_id 索引
4. 为现有业务表统一补充 company_id -> sys_company(company_id) 外键
5. 作为后续所有新表建表的统一规范模板

执行前说明:
1. 本脚本适用于 MySQL 8.0
2. 现有表若存在非空 company_id 数据, 执行外键前必须先保证这些值已存在于 sys_company
3. 当前阶段 company_id 允许为空, 便于渐进式改造; 后续如数据治理完成, 可再收紧为 NOT NULL
*/

/* ========================================================================== */
/* 1. 公司主体主数据表                                                         */
/* ========================================================================== */

CREATE TABLE IF NOT EXISTS sys_company (
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    company_code VARCHAR(64) NOT NULL COMMENT '公司主体编号',
    company_name VARCHAR(128) NOT NULL COMMENT '公司主体名称',
    invoice_title VARCHAR(200) NULL COMMENT '公司抬头',
    tax_no VARCHAR(100) NULL COMMENT '税号',
    bank_name VARCHAR(200) NULL COMMENT '开户行',
    bank_account_name VARCHAR(200) NULL COMMENT '账户名',
    bank_account_no VARCHAR(100) NULL COMMENT '银行账号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (company_id),
    CONSTRAINT uk_sys_company_company_code UNIQUE (company_code),
    KEY idx_sys_company_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司主体主数据表';

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        COUNT(*) = 1,
        'ALTER TABLE sys_company DROP FOREIGN KEY fk_sys_company_parent_company_id',
        'SELECT ''sys_company.fk_sys_company_parent_company_id not exists'''
    )
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_company'
      AND CONSTRAINT_NAME = 'fk_sys_company_parent_company_id'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 1,
        'ALTER TABLE sys_company DROP INDEX idx_sys_company_parent_company_id',
        'SELECT ''sys_company.idx_sys_company_parent_company_id not exists'''
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_company'
      AND INDEX_NAME = 'idx_sys_company_parent_company_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 1,
        'ALTER TABLE sys_company DROP COLUMN parent_company_id',
        'SELECT ''sys_company.parent_company_id not exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_company'
      AND COLUMN_NAME = 'parent_company_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_company ADD COLUMN invoice_title VARCHAR(200) NULL COMMENT ''公司抬头'' AFTER company_name',
        'SELECT ''sys_company.invoice_title exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_company'
      AND COLUMN_NAME = 'invoice_title'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_company ADD COLUMN tax_no VARCHAR(100) NULL COMMENT ''税号'' AFTER invoice_title',
        'SELECT ''sys_company.tax_no exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_company'
      AND COLUMN_NAME = 'tax_no'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_company ADD COLUMN bank_name VARCHAR(200) NULL COMMENT ''开户行'' AFTER tax_no',
        'SELECT ''sys_company.bank_name exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_company'
      AND COLUMN_NAME = 'bank_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_company ADD COLUMN bank_account_name VARCHAR(200) NULL COMMENT ''账户名'' AFTER bank_name',
        'SELECT ''sys_company.bank_account_name exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_company'
      AND COLUMN_NAME = 'bank_account_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_company ADD COLUMN bank_account_no VARCHAR(100) NULL COMMENT ''银行账号'' AFTER bank_account_name',
        'SELECT ''sys_company.bank_account_no exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_company'
      AND COLUMN_NAME = 'bank_account_no'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

/* ========================================================================== */
/* 2. 为现有业务表补充或统一 company_id 字段                                   */
/* ========================================================================== */

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user'
          AND COLUMN_NAME = 'company_id'
    ),
    'ALTER TABLE sys_user MODIFY COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码''',
    'ALTER TABLE sys_user ADD COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码'' AFTER labor_relation_belong'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user_bank_account'
          AND COLUMN_NAME = 'company_id'
    ),
    'ALTER TABLE sys_user_bank_account MODIFY COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码''',
    'ALTER TABLE sys_user_bank_account ADD COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码'' AFTER user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_download_record'
          AND COLUMN_NAME = 'company_id'
    ),
    'ALTER TABLE sys_download_record MODIFY COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码''',
    'ALTER TABLE sys_download_record ADD COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码'' AFTER user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'pm_template_category'
          AND COLUMN_NAME = 'company_id'
    ),
    'ALTER TABLE pm_template_category MODIFY COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码''',
    'ALTER TABLE pm_template_category ADD COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码'' AFTER id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'pm_document_template'
          AND COLUMN_NAME = 'company_id'
    ),
    'ALTER TABLE pm_document_template MODIFY COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码''',
    'ALTER TABLE pm_document_template ADD COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码'' AFTER id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'pm_template_scope'
          AND COLUMN_NAME = 'company_id'
    ),
    'ALTER TABLE pm_template_scope MODIFY COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码''',
    'ALTER TABLE pm_template_scope ADD COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码'' AFTER template_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_async_task'
          AND COLUMN_NAME = 'company_id'
    ),
    'ALTER TABLE sys_async_task MODIFY COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码''',
    'ALTER TABLE sys_async_task ADD COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码'' AFTER user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_notification_record'
          AND COLUMN_NAME = 'company_id'
    ),
    'ALTER TABLE sys_notification_record MODIFY COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码''',
    'ALTER TABLE sys_notification_record ADD COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码'' AFTER user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accvouch'
          AND COLUMN_NAME = 'company_id'
    ),
    'ALTER TABLE gl_accvouch MODIFY COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码''',
    'ALTER TABLE gl_accvouch ADD COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码'' AFTER bCusSupInput'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accsum'
          AND COLUMN_NAME = 'company_id'
    ),
    'ALTER TABLE gl_accsum MODIFY COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码''',
    'ALTER TABLE gl_accsum ADD COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码'' AFTER iperiod'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accass'
          AND COLUMN_NAME = 'company_id'
    ),
    'ALTER TABLE gl_accass MODIFY COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码''',
    'ALTER TABLE gl_accass ADD COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码'' AFTER iperiod'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

/* ========================================================================== */
/* 3. 为现有业务表补充 company_id 索引                                          */
/* ========================================================================== */

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user'
          AND INDEX_NAME = 'idx_sys_user_company_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_sys_user_company_id ON sys_user (company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user_bank_account'
          AND INDEX_NAME = 'idx_sys_user_bank_account_company_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_sys_user_bank_account_company_id ON sys_user_bank_account (company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_download_record'
          AND INDEX_NAME = 'idx_sys_download_record_company_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_sys_download_record_company_id ON sys_download_record (company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'pm_template_category'
          AND INDEX_NAME = 'idx_pm_template_category_company_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_pm_template_category_company_id ON pm_template_category (company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'pm_document_template'
          AND INDEX_NAME = 'idx_pm_document_template_company_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_pm_document_template_company_id ON pm_document_template (company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'pm_template_scope'
          AND INDEX_NAME = 'idx_pm_template_scope_company_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_pm_template_scope_company_id ON pm_template_scope (company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_async_task'
          AND INDEX_NAME = 'idx_sys_async_task_company_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_sys_async_task_company_id ON sys_async_task (company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_notification_record'
          AND INDEX_NAME = 'idx_sys_notification_record_company_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_sys_notification_record_company_id ON sys_notification_record (company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accvouch'
          AND INDEX_NAME = 'idx_gl_accvouch_company_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_gl_accvouch_company_id ON gl_accvouch (company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accsum'
          AND INDEX_NAME = 'idx_gl_accsum_company_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_gl_accsum_company_id ON gl_accsum (company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accass'
          AND INDEX_NAME = 'idx_gl_accass_company_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_gl_accass_company_id ON gl_accass (company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

/* ========================================================================== */
/* 4. 为现有业务表补充 company_id 外键                                          */
/* ========================================================================== */

/*
注意:
如果现有业务表中 company_id 已有非空值, 且这些值尚未在 sys_company 中维护,
执行下面外键语句时会失败。请先插入 sys_company 主数据, 或先将不合法值置空。
*/

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user'
          AND CONSTRAINT_NAME = 'fk_sys_user_company_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE sys_user ADD CONSTRAINT fk_sys_user_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user_bank_account'
          AND CONSTRAINT_NAME = 'fk_sys_user_bank_account_company_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE sys_user_bank_account ADD CONSTRAINT fk_sys_user_bank_account_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_download_record'
          AND CONSTRAINT_NAME = 'fk_sys_download_record_company_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE sys_download_record ADD CONSTRAINT fk_sys_download_record_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'pm_template_category'
          AND CONSTRAINT_NAME = 'fk_pm_template_category_company_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE pm_template_category ADD CONSTRAINT fk_pm_template_category_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'pm_document_template'
          AND CONSTRAINT_NAME = 'fk_pm_document_template_company_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE pm_document_template ADD CONSTRAINT fk_pm_document_template_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'pm_template_scope'
          AND CONSTRAINT_NAME = 'fk_pm_template_scope_company_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE pm_template_scope ADD CONSTRAINT fk_pm_template_scope_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_async_task'
          AND CONSTRAINT_NAME = 'fk_sys_async_task_company_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE sys_async_task ADD CONSTRAINT fk_sys_async_task_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_notification_record'
          AND CONSTRAINT_NAME = 'fk_sys_notification_record_company_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE sys_notification_record ADD CONSTRAINT fk_sys_notification_record_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accvouch'
          AND CONSTRAINT_NAME = 'fk_gl_accvouch_company_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE gl_accvouch ADD CONSTRAINT fk_gl_accvouch_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accsum'
          AND CONSTRAINT_NAME = 'fk_gl_accsum_company_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE gl_accsum ADD CONSTRAINT fk_gl_accsum_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accass'
          AND CONSTRAINT_NAME = 'fk_gl_accass_company_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE gl_accass ADD CONSTRAINT fk_gl_accass_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

/* ========================================================================== */
/* 5. 后续新表建表规范模板                                                     */
/* ========================================================================== */

/*
以后所有新业务表, 统一遵循以下规则:

1. 必须包含字段:
   company_id VARCHAR(64) NULL COMMENT '公司主体编码'

2. 必须包含索引:
   KEY idx_<table_name>_company_id (company_id)

3. 必须包含外键:
   CONSTRAINT fk_<table_name>_company_id
       FOREIGN KEY (company_id) REFERENCES sys_company(company_id)

4. 财务类大表除了单列索引外, 还应保留组合索引策略, 例如:
   KEY idx_<table_name>_company_period (company_id, iperiod)
   KEY idx_<table_name>_company_ccode (company_id, ccode)

参考模板:

CREATE TABLE your_business_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    company_id VARCHAR(64) NULL COMMENT '公司主体编码',
    business_no VARCHAR(64) NOT NULL COMMENT '业务编号',
    status VARCHAR(32) COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_your_business_table_company_id (company_id),
    CONSTRAINT fk_your_business_table_company_id
        FOREIGN KEY (company_id) REFERENCES sys_company(company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务表';
*/
