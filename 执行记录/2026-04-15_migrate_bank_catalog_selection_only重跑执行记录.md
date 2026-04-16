# 数据库执行记录：`migrate_bank_catalog_selection_only.sql` 修正后重跑

- 执行日期：2026-04-15 22:58:25
- 执行人：Codex（按用户指令执行）
- 目标库：127.0.0.1:3306/finex_db
- 目标对象：`sys_bank_catalog`、`sys_company_bank_account`
- 变更类型：DDL / DML / 注释整改
- 风险级别：一级 + 三级
- 上一轮失败记录：`C:\Users\funsir\Desktop\报销系统\执行记录\2026-04-15_migrate_bank_catalog_selection_only执行记录.md`

## 执行前校验 SQL

```sql
SHOW CREATE TABLE finex_db.sys_bank_catalog;
SHOW CREATE TABLE finex_db.sys_company_bank_account;

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'finex_db'
  AND (
    (TABLE_NAME = 'sys_bank_catalog' AND COLUMN_NAME = 'business_scope')
    OR (TABLE_NAME = 'sys_company_bank_account' AND COLUMN_NAME IN ('province', 'city'))
  )
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT 'sys_bank_catalog_rows', COUNT(*) FROM finex_db.sys_bank_catalog
UNION ALL
SELECT 'sys_company_bank_account_rows', COUNT(*) FROM finex_db.sys_company_bank_account;
```

## 正式执行脚本

- 文件：`C:\Users\funsir\Desktop\报销系统\backend\sql\migrate_bank_catalog_selection_only.sql`
- 执行方式：本机 `mysql.exe` 命令行，`--default-character-set=utf8mb4`
- 修正内容：将 3 处 `ADD COLUMN IF NOT EXISTS` 改为 `information_schema + IF + PREPARE/EXECUTE` 兼容写法

## 备份信息

- 备份方式：`mysqldump` 最小表级备份
- 备份位置：`C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-15_225825_migrate_bank_catalog_selection_only_rerun`
- 备份文件：
  - `C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-15_225825_migrate_bank_catalog_selection_only_rerun\sys_bank_catalog.sql`
  - `C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-15_225825_migrate_bank_catalog_selection_only_rerun\sys_company_bank_account.sql`
- 核查文件：
  - `C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-15_225825_migrate_bank_catalog_selection_only_rerun\precheck.txt`
  - `C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-15_225825_migrate_bank_catalog_selection_only_rerun\execute_first.txt`
  - `C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-15_225825_migrate_bank_catalog_selection_only_rerun\execute_second.txt`
  - `C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-15_225825_migrate_bank_catalog_selection_only_rerun\postcheck.txt`

## 回滚方案

```text
1. 如需整体回滚，使用本次备份文件恢复两张表：
   mysql -uroot -p123456 --default-character-set=utf8mb4 finex_db < sys_bank_catalog.sql
   mysql -uroot -p123456 --default-character-set=utf8mb4 finex_db < sys_company_bank_account.sql
2. 恢复前先确认当前库内是否已有后续新数据，避免覆盖误伤。
3. 若只需结构回退，也可先人工评估后执行：
   ALTER TABLE sys_bank_catalog DROP COLUMN business_scope;
   ALTER TABLE sys_company_bank_account DROP COLUMN province;
   ALTER TABLE sys_company_bank_account DROP COLUMN city;
   但由于本次同时调整了字段/表注释，默认仍以备份恢复为主。
```

## 执行后验证 SQL

```sql
SHOW CREATE TABLE finex_db.sys_bank_catalog;
SHOW CREATE TABLE finex_db.sys_company_bank_account;

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'finex_db'
  AND (
    (TABLE_NAME = 'sys_bank_catalog' AND COLUMN_NAME = 'business_scope')
    OR (TABLE_NAME = 'sys_company_bank_account' AND COLUMN_NAME IN ('province', 'city'))
  )
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT COUNT(*) AS blank_business_scope_count
FROM finex_db.sys_bank_catalog
WHERE business_scope IS NULL OR TRIM(business_scope) = '';

SELECT TABLE_NAME, TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'finex_db'
  AND TABLE_NAME IN ('sys_bank_catalog', 'sys_company_bank_account')
ORDER BY TABLE_NAME;

SELECT TABLE_NAME, COLUMN_NAME, COUNT(*) AS exact_comment_match
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'finex_db'
  AND (
    (TABLE_NAME = 'sys_bank_catalog' AND COLUMN_NAME = 'business_scope' AND COLUMN_COMMENT = '业务范围:PRIVATE/PUBLIC/BOTH')
    OR (TABLE_NAME = 'sys_company_bank_account' AND COLUMN_NAME = 'province' AND COLUMN_COMMENT = '开户省')
    OR (TABLE_NAME = 'sys_company_bank_account' AND COLUMN_NAME = 'city' AND COLUMN_COMMENT = '开户市')
  )
GROUP BY TABLE_NAME, COLUMN_NAME
ORDER BY TABLE_NAME, COLUMN_NAME;

SELECT TABLE_NAME, COUNT(*) AS exact_table_comment_match
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'finex_db'
  AND (
    (TABLE_NAME = 'sys_bank_catalog' AND TABLE_COMMENT = '国内银行目录表')
    OR (TABLE_NAME = 'sys_company_bank_account' AND TABLE_COMMENT = '公司银行账户表')
  )
GROUP BY TABLE_NAME
ORDER BY TABLE_NAME;
```

## 结果结论

- 实际影响结果：
  - `sys_bank_catalog.business_scope` 已新增成功
  - `sys_company_bank_account.province`、`sys_company_bank_account.city` 已新增成功
  - `sys_bank_catalog.business_scope` 空值/空串数为 `0`
  - 两张表表注释已精确匹配目标中文注释
  - 3 个目标字段注释已精确匹配目标中文注释
- 幂等性验证：
  - 修正后的脚本已连续执行 2 次
  - 首次执行退出码：`0`
  - 二次重跑退出码：`0`
  - 二次执行返回 `*.exists` 提示，未再次报错
- 是否成功：是
- 是否影响前端：否，数据库结构已补齐为前端银行选择场景所需字段
- 是否影响后端：否，当前结果消除了 `business_scope / province / city` 缺列风险
- 风险结论：本次迁移已成功完成，后续可继续基于该脚本作为旧库增量脚本执行
