# 数据库执行记录：总账凭证作废/冲销/统一编号增量脚本执行

- 执行日期：2026-05-22 15:33:41
- 执行人：Codex
- 目标库：127.0.0.1:3306/finex_db
- 目标对象：`gl_accvouch`
- 变更类型：DDL
- 风险级别：一级

## 变更目标

基于本次“总账模块：已结账期间只读、统一凭证号与作废/冲销/删除落地方案”，执行以下结构补充：

- 新增 `maker_user_id`
- 新增 `void_flag`
- 新增 `voided_at`
- 新增 `voided_by_user_id`
- 新增 `voided_by_name`
- 新增 `reversed_from_voucher_no`
- 新增 `reversed_by_voucher_no`
- 新增索引 `idx_gl_accvouch_company_year_period_unified_no (company_id, iyear, iyperiod, ino_id)`

本次仅执行幂等增量脚本，不执行任何 `init*.sql`，不做批量改数，不删除现有业务数据。

## 执行前校验 SQL

```sql
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'finex_db'
  AND TABLE_NAME = 'gl_accvouch'
  AND COLUMN_NAME IN (
    'maker_user_id',
    'void_flag',
    'voided_at',
    'voided_by_user_id',
    'voided_by_name',
    'reversed_from_voucher_no',
    'reversed_by_voucher_no'
  )
ORDER BY ORDINAL_POSITION;

SELECT INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'finex_db'
  AND TABLE_NAME = 'gl_accvouch'
  AND INDEX_NAME = 'idx_gl_accvouch_company_year_period_unified_no'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

SHOW CREATE TABLE finex_db.gl_accvouch;
```

## 正式执行脚本

- 文件：`backend/sql/migrate_finance_voucher_void_reverse_unified_no.sql`
- 执行方式：本机 `mysql.exe` 命令行，连接字符集 `utf8mb4`

## 备份信息

- 备份方式：执行前读取 `SHOW CREATE TABLE finex_db.gl_accvouch` 作为结构快照
- 备份位置：本次执行输出日志
- 说明：本次仅新增可空字段与索引，未执行任何 DML，未触碰现有业务数据

## 回滚方案

```sql
ALTER TABLE gl_accvouch
    DROP INDEX idx_gl_accvouch_company_year_period_unified_no;

ALTER TABLE gl_accvouch
    DROP COLUMN reversed_by_voucher_no,
    DROP COLUMN reversed_from_voucher_no,
    DROP COLUMN voided_by_name,
    DROP COLUMN voided_by_user_id,
    DROP COLUMN voided_at,
    DROP COLUMN void_flag,
    DROP COLUMN maker_user_id;
```

说明：本次为结构增量，回滚会移除本次新增字段与索引；由于未改历史数据，无需额外数据回滚。

## 执行后验证 SQL

```sql
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'finex_db'
  AND TABLE_NAME = 'gl_accvouch'
  AND COLUMN_NAME IN (
    'maker_user_id',
    'void_flag',
    'voided_at',
    'voided_by_user_id',
    'voided_by_name',
    'reversed_from_voucher_no',
    'reversed_by_voucher_no'
  )
ORDER BY ORDINAL_POSITION;

SELECT INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'finex_db'
  AND TABLE_NAME = 'gl_accvouch'
  AND INDEX_NAME = 'idx_gl_accvouch_company_year_period_unified_no'
ORDER BY SEQ_IN_INDEX;

SHOW CREATE TABLE finex_db.gl_accvouch;
```

## 结果结论

- 实际影响结果：
  - 第一次执行成功新增 7 个字段与 1 个索引
  - 第二次重跑成功返回 `exists`，确认脚本可重复执行
- 是否成功：是
- 是否影响前端：否，本次只做数据库结构补充
- 是否影响后端：是，为本次新增的总账作废/冲销/统一编号能力提供落库结构
- 风险结论：低风险，未发现乱码，未执行数据删除或批量改数，未误伤现有业务数据
