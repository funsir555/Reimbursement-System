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
