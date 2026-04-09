USE finex_db;

SET NAMES utf8mb4;

ALTER TABLE sys_user_bank_account
    ADD COLUMN IF NOT EXISTS bank_code VARCHAR(64) NULL COMMENT '开户银行编码' AFTER branch_name;

ALTER TABLE sys_user_bank_account
    ADD COLUMN IF NOT EXISTS branch_code VARCHAR(64) NULL COMMENT '分支行编码' AFTER bank_code;

ALTER TABLE sys_user_bank_account
    ADD COLUMN IF NOT EXISTS cnaps_code VARCHAR(64) NULL COMMENT '联行号' AFTER branch_code;

ALTER TABLE sys_user_bank_account
    ADD COLUMN IF NOT EXISTS province VARCHAR(64) NULL COMMENT '开户省' AFTER cnaps_code;

ALTER TABLE sys_user_bank_account
    ADD COLUMN IF NOT EXISTS city VARCHAR(64) NULL COMMENT '开户市' AFTER province;

-- comment standardization begin
ALTER TABLE sys_user_bank_account
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '银行账户ID',
    MODIFY COLUMN user_id bigint NOT NULL COMMENT '用户ID',
    MODIFY COLUMN company_id varchar(64) NULL COMMENT '公司主体编码',
    MODIFY COLUMN bank_name varchar(100) NOT NULL COMMENT '银行名称',
    MODIFY COLUMN branch_name varchar(100) NULL COMMENT '支行名称',
    MODIFY COLUMN bank_code varchar(64) NULL COMMENT '开户银行编码',
    MODIFY COLUMN branch_code varchar(64) NULL COMMENT '分支行编码',
    MODIFY COLUMN cnaps_code varchar(64) NULL COMMENT '联行号',
    MODIFY COLUMN province varchar(64) NULL COMMENT '开户省',
    MODIFY COLUMN city varchar(64) NULL COMMENT '开户市',
    MODIFY COLUMN account_name varchar(100) NOT NULL COMMENT '科目名称',
    MODIFY COLUMN account_no varchar(50) NOT NULL COMMENT '银行卡号',
    MODIFY COLUMN account_type varchar(50) NULL DEFAULT '对私账户' COMMENT '账户类型',
    MODIFY COLUMN default_account tinyint NULL DEFAULT 0 COMMENT '是否默认账户',
    MODIFY COLUMN status tinyint NULL DEFAULT 1 COMMENT '状态',
    MODIFY COLUMN created_at datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '用户银行卡表';

-- comment standardization end
