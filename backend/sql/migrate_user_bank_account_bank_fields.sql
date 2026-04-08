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
