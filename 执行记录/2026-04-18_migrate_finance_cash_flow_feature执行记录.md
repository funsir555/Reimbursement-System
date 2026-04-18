# `migrate_finance_cash_flow_feature.sql` 本机执行记录

## 1. 基本信息
- 执行日期：2026-04-18
- 目标库：`127.0.0.1:3306/finex_db`
- 执行脚本：`C:\Users\funsir\Desktop\报销系统\backend\sql\migrate_finance_cash_flow_feature.sql`
- 执行对象：
  - `gl_accvouch`
  - `fin_cash_flow_item`
- 执行方式：本机 `mysql.exe` 直连执行，连接字符集 `utf8mb4`

## 2. 执行前校验
- 校验文件：`C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-18_100458_migrate_finance_cash_flow_feature\precheck.txt`
- 校验结论：
  - `gl_accvouch` 在执行前不存在 `cash_flow_item_id`、`cash_flow_item_name` 字段
  - `gl_accvouch` 在执行前不存在 `idx_gl_accvouch_company_cash_flow` 索引
  - `fin_cash_flow_item` 在执行前不存在
  - `fin_account_set` 中当前仅发现 1 个账套公司：`COMPANY202604050001`，状态为 `ACTIVE`
  - 执行前无需检查现金流量重复数据，原因是目标表尚未创建

## 3. 备份信息
- 备份目录：`C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-18_100458_migrate_finance_cash_flow_feature`
- 备份内容：
  - `gl_accvouch.sql`
  - `gl_accvouch_dump.log`
  - `fin_cash_flow_item.txt`（执行前该表不存在，记录为无需备份）
  - `precheck.txt`
  - `execute_first.txt`
  - `execute_second.txt`
  - `postcheck.txt`

## 4. 执行过程说明
- 首次执行时发现脚本在历史账套默认现金流量补种语句上触发字符集/排序规则冲突：
  - `fin_account_set.company_id` 为 `utf8mb4_0900_ai_ci`
  - 新建表 `fin_cash_flow_item.company_id` 为 `utf8mb4_unicode_ci`
- 为保证本机 MySQL 8 环境可执行，已对脚本中的补种查询做兼容修正：
  - `SELECT CONVERT(fas.company_id USING utf8mb4) COLLATE utf8mb4_unicode_ci`
  - `existing.company_id = CONVERT(fas.company_id USING utf8mb4) COLLATE utf8mb4_unicode_ci`
- 修正后重新执行脚本 2 次：
  - 第 1 次：正式执行成功
  - 第 2 次：幂等性重跑成功

## 5. 执行后验证
- 验证文件：`C:\Users\funsir\Desktop\报销系统\backend\sql\backup_local\2026-04-18_100458_migrate_finance_cash_flow_feature\postcheck.txt`
- 结构验证：
  - `gl_accvouch` 已新增字段 `cash_flow_item_id`
  - `gl_accvouch` 已新增字段 `cash_flow_item_name`
  - `gl_accvouch` 已新增索引 `idx_gl_accvouch_company_cash_flow (company_id, cash_flow_item_id)`
  - `fin_cash_flow_item` 已创建成功
  - `fin_cash_flow_item` 已存在唯一键 `uk_fin_cash_flow_item_company_code (company_id, cash_flow_code)`
  - `fin_cash_flow_item` 已存在索引 `idx_fin_cash_flow_item_company_status (company_id, status, sort_order)`
- 数据验证：
  - `ACTIVE` 账套公司 `COMPANY202604050001` 已补齐 10 条默认现金流量
  - `COMPANY202604050001` 的默认编码去重后仍为 10 条，未出现重复插入
  - 非 `ACTIVE` 账套公司未发现被错误补种
- 幂等性验证：
  - 第二次重跑后默认数据总量未增加
  - 未重复加字段、未重复建索引、未重复插入默认现金流量

## 6. 回滚说明
- 本次未自动执行回滚。
- 如需回滚，建议按以下顺序处理：
  1. 使用 `gl_accvouch.sql` 恢复 `gl_accvouch`
  2. 由于 `fin_cash_flow_item` 在执行前不存在，可在确认无业务数据需要保留后删除该表
  3. 同步移除 `gl_accvouch` 新增字段：
     - `cash_flow_item_id`
     - `cash_flow_item_name`
  4. 同步移除 `gl_accvouch` 新增索引：
     - `idx_gl_accvouch_company_cash_flow`

## 7. 结果结论
- 本次脚本已成功执行到本机 `finex_db`
- 现金流量功能所需表结构、字段、索引已补齐
- 历史 `ACTIVE` 账套默认现金流量已成功补种，且通过幂等性校验
- 执行过程中发现并修复了本机环境下的排序规则兼容问题，当前仓库脚本已同步更新，可用于后续同类环境执行
