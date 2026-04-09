USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS exp_voucher_template_policy (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NULL,
    credit_account_code VARCHAR(64) NOT NULL,
    credit_account_name VARCHAR(128) NULL,
    voucher_type VARCHAR(16) NOT NULL DEFAULT '记',
    summary_rule VARCHAR(255) NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exp_voucher_template_policy (company_id, template_code),
    KEY idx_exp_voucher_template_policy_enabled (company_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Expense voucher template policy';

CREATE TABLE IF NOT EXISTS exp_voucher_subject_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NULL,
    expense_type_code VARCHAR(64) NOT NULL,
    expense_type_name VARCHAR(128) NULL,
    debit_account_code VARCHAR(64) NOT NULL,
    debit_account_name VARCHAR(128) NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exp_voucher_subject_mapping (company_id, template_code, expense_type_code),
    KEY idx_exp_voucher_subject_mapping_enabled (company_id, template_code, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Expense voucher subject mapping';

CREATE TABLE IF NOT EXISTS exp_voucher_push_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    document_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failure_count INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_by VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exp_voucher_push_batch (company_id, batch_no),
    KEY idx_exp_voucher_push_batch_status (company_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Expense voucher push batch';

CREATE TABLE IF NOT EXISTS exp_voucher_push_document (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    batch_id BIGINT NULL,
    batch_no VARCHAR(64) NULL,
    document_code VARCHAR(64) NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NULL,
    submitter_user_id BIGINT NULL,
    submitter_name VARCHAR(128) NULL,
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    push_status VARCHAR(16) NOT NULL,
    voucher_no VARCHAR(128) NULL,
    voucher_type VARCHAR(16) NULL,
    voucher_number INT NULL,
    fiscal_period INT NULL,
    bill_date DATE NULL,
    error_message VARCHAR(500) NULL,
    pushed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exp_voucher_push_document (company_id, document_code),
    KEY idx_exp_voucher_push_document_status (company_id, push_status),
    KEY idx_exp_voucher_push_document_voucher (company_id, voucher_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Expense voucher push document';

CREATE TABLE IF NOT EXISTS exp_voucher_push_entry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    push_document_id BIGINT NOT NULL,
    entry_no INT NOT NULL,
    direction VARCHAR(16) NOT NULL,
    digest VARCHAR(255) NULL,
    account_code VARCHAR(64) NOT NULL,
    account_name VARCHAR(128) NULL,
    expense_type_code VARCHAR(64) NULL,
    expense_type_name VARCHAR(128) NULL,
    amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_exp_voucher_push_entry_document (company_id, push_document_id),
    KEY idx_exp_voucher_push_entry_account (company_id, account_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Expense voucher push entry';

-- comment standardization begin
ALTER TABLE exp_voucher_push_batch
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN batch_no varchar(64) NOT NULL COMMENT '批次号',
    MODIFY COLUMN document_count int NOT NULL DEFAULT 0 COMMENT '单据数量',
    MODIFY COLUMN success_count int NOT NULL DEFAULT 0 COMMENT '成功数量',
    MODIFY COLUMN failure_count int NOT NULL DEFAULT 0 COMMENT '失败数量',
    MODIFY COLUMN status varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '推送状态',
    MODIFY COLUMN created_by varchar(64) NULL COMMENT '创建人',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '报销单推凭证批次表';

ALTER TABLE exp_voucher_push_document
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN batch_id bigint NULL COMMENT '批次ID',
    MODIFY COLUMN batch_no varchar(64) NULL COMMENT '批次号',
    MODIFY COLUMN document_code varchar(64) NOT NULL COMMENT '单据编码',
    MODIFY COLUMN template_code varchar(64) NOT NULL COMMENT '模板编码',
    MODIFY COLUMN template_name varchar(128) NULL COMMENT '模板名称',
    MODIFY COLUMN submitter_user_id bigint NULL COMMENT '提单人用户ID',
    MODIFY COLUMN submitter_name varchar(128) NULL COMMENT '提单人姓名',
    MODIFY COLUMN total_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
    MODIFY COLUMN push_status varchar(16) NOT NULL COMMENT '推送状态',
    MODIFY COLUMN voucher_no varchar(128) NULL COMMENT '凭证号',
    MODIFY COLUMN voucher_type varchar(16) NULL COMMENT '凭证类别字',
    MODIFY COLUMN voucher_number int NULL COMMENT '凭证编号',
    MODIFY COLUMN fiscal_period int NULL COMMENT '会计期间',
    MODIFY COLUMN bill_date date NULL COMMENT '单据日期',
    MODIFY COLUMN error_message varchar(500) NULL COMMENT '错误信息',
    MODIFY COLUMN pushed_at datetime NULL COMMENT '推送时间',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '报销单推凭证结果表';

ALTER TABLE exp_voucher_push_entry
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN push_document_id bigint NOT NULL COMMENT '推凭证单据ID',
    MODIFY COLUMN entry_no int NOT NULL COMMENT '分录序号',
    MODIFY COLUMN direction varchar(16) NOT NULL COMMENT '方向',
    MODIFY COLUMN digest varchar(255) NULL COMMENT '摘要',
    MODIFY COLUMN account_code varchar(64) NOT NULL COMMENT '科目编码',
    MODIFY COLUMN account_name varchar(128) NULL COMMENT '科目名称',
    MODIFY COLUMN expense_type_code varchar(64) NULL COMMENT '费用类型编码',
    MODIFY COLUMN expense_type_name varchar(128) NULL COMMENT '费用类型名称',
    MODIFY COLUMN amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '报销单推凭证明细表';

ALTER TABLE exp_voucher_subject_mapping
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN template_code varchar(64) NOT NULL COMMENT '模板编码',
    MODIFY COLUMN template_name varchar(128) NULL COMMENT '模板名称',
    MODIFY COLUMN expense_type_code varchar(64) NOT NULL COMMENT '费用类型编码',
    MODIFY COLUMN expense_type_name varchar(128) NULL COMMENT '费用类型名称',
    MODIFY COLUMN debit_account_code varchar(64) NOT NULL COMMENT '借方科目编码',
    MODIFY COLUMN debit_account_name varchar(128) NULL COMMENT '借方科目名称',
    MODIFY COLUMN enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用:1是 0否',
    MODIFY COLUMN created_by varchar(64) NULL COMMENT '创建人',
    MODIFY COLUMN updated_by varchar(64) NULL COMMENT '更新人',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '报销凭证科目映射表';

ALTER TABLE exp_voucher_template_policy
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN template_code varchar(64) NOT NULL COMMENT '模板编码',
    MODIFY COLUMN template_name varchar(128) NULL COMMENT '模板名称',
    MODIFY COLUMN credit_account_code varchar(64) NOT NULL COMMENT '贷方科目编码',
    MODIFY COLUMN credit_account_name varchar(128) NULL COMMENT '贷方科目名称',
    MODIFY COLUMN voucher_type varchar(16) NOT NULL DEFAULT '记' COMMENT '凭证类别字',
    MODIFY COLUMN summary_rule varchar(255) NULL COMMENT '摘要生成规则',
    MODIFY COLUMN enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用:1是 0否',
    MODIFY COLUMN created_by varchar(64) NULL COMMENT '创建人',
    MODIFY COLUMN updated_by varchar(64) NULL COMMENT '更新人',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '报销凭证模板策略表';

-- comment standardization end
