USE finex_db;

SET NAMES utf8mb4;

/*
用途:
1. 为用户收款账户增加账户所属用户字段
2. 按 account_name = sys_user.name 回填账户所属用户ID
3. 为账户所属用户ID建立索引和外键

说明:
- 本脚本可重复执行
- account_id 允许为空；无法按姓名匹配的历史账户不会被强制绑定
- user_id 仍表示维护该收款账户的用户，account_id 表示实际账户所属用户
*/

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user_bank_account'
          AND COLUMN_NAME = 'account_id'
    ),
    'SELECT 1',
    'ALTER TABLE sys_user_bank_account ADD COLUMN account_id BIGINT NULL COMMENT ''账户所属用户ID'' AFTER user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sys_user_bank_account account
LEFT JOIN sys_user owner ON TRIM(account.account_name) = TRIM(owner.name)
SET account.account_id = owner.id;

SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_user_bank_account'
          AND INDEX_NAME = 'idx_sys_user_bank_account_account_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_sys_user_bank_account_account_id ON sys_user_bank_account (account_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_user_bank_account'
          AND CONSTRAINT_NAME = 'fk_sys_user_bank_account_account_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE sys_user_bank_account ADD CONSTRAINT fk_sys_user_bank_account_account_id FOREIGN KEY (account_id) REFERENCES sys_user(id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
