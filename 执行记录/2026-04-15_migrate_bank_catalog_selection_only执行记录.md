# 2026-04-15 `migrate_bank_catalog_selection_only.sql` 执行记录

## 1. 执行摘要

- 目标脚本：`C:\Users\funsir\Desktop\报销系统\backend\sql\migrate_bank_catalog_selection_only.sql`
- 目标数据库：`localhost:3306/finex_db`
- 连接口径：`C:\Users\funsir\Desktop\报销系统\backend\.env.local.cmd`
- 执行时间：`2026-04-15 22:47`
- 执行结果：`失败，未完成结构变更`

## 2. 执行前核查

- `sys_bank_catalog`、`sys_company_bank_account` 两张表均存在。
- `sys_bank_catalog.business_scope` 不存在。
- `sys_company_bank_account.province`、`sys_company_bank_account.city` 不存在。
- `sys_bank_catalog` 当前数据量：`5`
- `sys_company_bank_account` 当前数据量：`0`

## 3. 备份与核查文件

- 备份目录：`C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-15_224659_migrate_bank_catalog_selection_only`
- 结构与数据备份：
  - `C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-15_224659_migrate_bank_catalog_selection_only\sys_bank_catalog.sql`
  - `C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-15_224659_migrate_bank_catalog_selection_only\sys_company_bank_account.sql`
- 前置核查输出：
  - `C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-15_224659_migrate_bank_catalog_selection_only\precheck.txt`
- 执行后核查输出：
  - `C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-15_224659_migrate_bank_catalog_selection_only\postcheck.txt`

## 4. 失败原因

- 使用本机 MySQL 客户端执行脚本时，在首个 `ALTER TABLE sys_bank_catalog ADD COLUMN IF NOT EXISTS ...` 处报 SQL 语法错误。
- 原始报错：

```text
ERROR 1064 (42000) at line 5: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near 'IF NOT EXISTS business_scope VARCHAR(16) NOT NULL DEFAULT 'BOTH' COMMENT '业务' at line 2
```

- 由于脚本在新增 `business_scope` 时已中断，后续校验语句继续读取该列时又触发：

```text
ERROR 1054 (42S22) at line 1: Unknown column 'business_scope' in 'where clause'
```

## 5. 执行后状态

- 当前数据库结构未达到目标状态：
  - `sys_bank_catalog.business_scope` 仍不存在
  - `sys_company_bank_account.province` 仍不存在
  - `sys_company_bank_account.city` 仍不存在
- 本次未继续执行其它迁移脚本，也未对目标表做人工补改。

## 6. 后续处理建议

- 先修正 `migrate_bank_catalog_selection_only.sql` 中 `ADD COLUMN IF NOT EXISTS` 的兼容写法，再重新执行。
- 重跑前可继续复用本次已生成的结构与数据备份作为回滚基础，但建议重新生成一份新的执行记录与执行时间戳。
