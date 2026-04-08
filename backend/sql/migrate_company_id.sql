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
    company_code VARCHAR(64) NOT NULL COMMENT '公司编码',
    company_name VARCHAR(128) NOT NULL COMMENT '公司名称',
    invoice_title VARCHAR(200) NULL COMMENT '公司抬头',
    tax_no VARCHAR(100) NULL COMMENT '税号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (company_id),
    CONSTRAINT uk_sys_company_company_code UNIQUE (company_code),
    KEY idx_sys_company_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司主体主数据表';

CREATE TABLE IF NOT EXISTS sys_company_bank_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    bank_name VARCHAR(200) NOT NULL COMMENT '银行名称',
    branch_name VARCHAR(200) NULL COMMENT '开户支行',
    bank_code VARCHAR(64) NULL COMMENT '银行编码',
    branch_code VARCHAR(64) NULL COMMENT '支行编码',
    cnaps_code VARCHAR(64) NULL COMMENT '联行号',
    account_name VARCHAR(200) NOT NULL COMMENT '账户名称',
    account_no VARCHAR(100) NOT NULL COMMENT '银行账号',
    account_type VARCHAR(64) NULL COMMENT '账户类型',
    account_usage VARCHAR(100) NULL COMMENT '账户用途',
    currency_code VARCHAR(32) NULL COMMENT '币种编码',
    default_account TINYINT NOT NULL DEFAULT 0 COMMENT '默认账户:1是 0否',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    remark VARCHAR(500) NULL COMMENT '备注',
    direct_connect_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用银企直连',
    direct_connect_provider VARCHAR(100) NULL COMMENT '银企直连服务商',
    direct_connect_channel VARCHAR(100) NULL COMMENT '银企直连渠道',
    direct_connect_protocol VARCHAR(100) NULL COMMENT '银企直连接口协议',
    direct_connect_customer_no VARCHAR(100) NULL COMMENT '银企直连客户号',
    direct_connect_app_id VARCHAR(100) NULL COMMENT '银企直连应用标识',
    direct_connect_account_alias VARCHAR(100) NULL COMMENT '银企直连账户别名',
    direct_connect_auth_mode VARCHAR(100) NULL COMMENT '银企直连认证方式',
    direct_connect_api_base_url VARCHAR(500) NULL COMMENT '银企直连接口地址',
    direct_connect_cert_ref VARCHAR(200) NULL COMMENT '银企直连证书引用',
    direct_connect_secret_ref VARCHAR(200) NULL COMMENT '银企直连密钥引用',
    direct_connect_sign_type VARCHAR(100) NULL COMMENT '银企直连签名方式',
    direct_connect_encrypt_type VARCHAR(100) NULL COMMENT '银企直连加密方式',
    direct_connect_last_sync_at DATETIME NULL COMMENT '银企直连最近同步时间',
    direct_connect_last_sync_status VARCHAR(64) NULL COMMENT '银企直连最近同步状态',
    direct_connect_last_error_msg VARCHAR(1000) NULL COMMENT '银企直连最近错误信息',
    direct_connect_ext_json JSON NULL COMMENT '银企直连扩展信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_sys_company_bank_account_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id),
    CONSTRAINT uk_sys_company_bank_account_company_no UNIQUE (company_id, account_no),
    KEY idx_sys_company_bank_account_company_status (company_id, status),
    KEY idx_sys_company_bank_account_company_default (company_id, default_account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司银行账户表';

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
        COUNT(*) = 1,
        'ALTER TABLE sys_company DROP COLUMN bank_name',
        'SELECT ''sys_company.bank_name not exists'''
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
        COUNT(*) = 1,
        'ALTER TABLE sys_company DROP COLUMN bank_account_name',
        'SELECT ''sys_company.bank_account_name not exists'''
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
        COUNT(*) = 1,
        'ALTER TABLE sys_company DROP COLUMN bank_account_no',
        'SELECT ''sys_company.bank_account_no not exists'''
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
/* 1.1 统一本次触达对象的中文注释                                              */
/* ========================================================================== */

ALTER TABLE sys_company COMMENT = '公司主体主数据表';

ALTER TABLE sys_company_bank_account
    MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN bank_name VARCHAR(200) NOT NULL COMMENT '银行名称',
    MODIFY COLUMN branch_name VARCHAR(200) NULL COMMENT '开户支行',
    MODIFY COLUMN bank_code VARCHAR(64) NULL COMMENT '银行编码',
    MODIFY COLUMN branch_code VARCHAR(64) NULL COMMENT '支行编码',
    MODIFY COLUMN cnaps_code VARCHAR(64) NULL COMMENT '联行号',
    MODIFY COLUMN account_name VARCHAR(200) NOT NULL COMMENT '账户名称',
    MODIFY COLUMN account_no VARCHAR(100) NOT NULL COMMENT '银行账号',
    MODIFY COLUMN account_type VARCHAR(64) NULL COMMENT '账户类型',
    MODIFY COLUMN account_usage VARCHAR(100) NULL COMMENT '账户用途',
    MODIFY COLUMN currency_code VARCHAR(32) NULL COMMENT '币种编码',
    MODIFY COLUMN default_account TINYINT NOT NULL DEFAULT 0 COMMENT '默认账户:1是 0否',
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    MODIFY COLUMN remark VARCHAR(500) NULL COMMENT '备注',
    MODIFY COLUMN direct_connect_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用银企直连',
    MODIFY COLUMN direct_connect_provider VARCHAR(100) NULL COMMENT '银企直连服务商',
    MODIFY COLUMN direct_connect_channel VARCHAR(100) NULL COMMENT '银企直连渠道',
    MODIFY COLUMN direct_connect_protocol VARCHAR(100) NULL COMMENT '银企直连接口协议',
    MODIFY COLUMN direct_connect_customer_no VARCHAR(100) NULL COMMENT '银企直连客户号',
    MODIFY COLUMN direct_connect_app_id VARCHAR(100) NULL COMMENT '银企直连应用标识',
    MODIFY COLUMN direct_connect_account_alias VARCHAR(100) NULL COMMENT '银企直连账户别名',
    MODIFY COLUMN direct_connect_auth_mode VARCHAR(100) NULL COMMENT '银企直连认证方式',
    MODIFY COLUMN direct_connect_api_base_url VARCHAR(500) NULL COMMENT '银企直连接口地址',
    MODIFY COLUMN direct_connect_cert_ref VARCHAR(200) NULL COMMENT '银企直连证书引用',
    MODIFY COLUMN direct_connect_secret_ref VARCHAR(200) NULL COMMENT '银企直连密钥引用',
    MODIFY COLUMN direct_connect_sign_type VARCHAR(100) NULL COMMENT '银企直连签名方式',
    MODIFY COLUMN direct_connect_encrypt_type VARCHAR(100) NULL COMMENT '银企直连加密方式',
    MODIFY COLUMN direct_connect_last_sync_at DATETIME NULL COMMENT '银企直连最近同步时间',
    MODIFY COLUMN direct_connect_last_sync_status VARCHAR(64) NULL COMMENT '银企直连最近同步状态',
    MODIFY COLUMN direct_connect_last_error_msg VARCHAR(1000) NULL COMMENT '银企直连最近错误信息',
    MODIFY COLUMN direct_connect_ext_json JSON NULL COMMENT '银企直连扩展信息',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '公司银行账户表';

ALTER TABLE sys_async_task COMMENT = '异步任务表';

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
