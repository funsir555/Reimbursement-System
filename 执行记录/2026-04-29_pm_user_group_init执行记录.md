# 用户组专用迁移 SQL 初始化执行记录

- 执行时间：2026-04-29 11:50:05
- 目标数据库：127.0.0.1:3306/finex_db
- 执行脚本：[migrate_pm_user_group_init.sql](C:/Users/funsir/Desktop/报销系统/backend/sql/migrate_pm_user_group_init.sql)
- 本地执行产物目录：[2026-04-29_114729_migrate_pm_user_group_init](C:/Users/funsir/Desktop/报销系统/backend/sql/backup_local/2026-04-29_114729_migrate_pm_user_group_init)
- 执行方式：`mysql.exe --default-character-set=utf8mb4` + `source migrate_pm_user_group_init.sql`

## 执行前确认
- `pm_user_group` 不存在
- `fk_pm_user_group_parent_id` 不存在
- 本次为纯新增表初始化，不涉及已有业务表结构变更

## 备份说明
- 本次执行前目标库内不存在 `pm_user_group`，无需做表级备份
- 未触达其它成熟业务表，无需额外导出历史数据

## 本次落库内容
- 新增表 `pm_user_group`
- 新增唯一索引 `uk_pm_user_group_code`
- 新增普通索引 `idx_pm_user_group_parent_id`
- 新增自关联外键 `fk_pm_user_group_parent_id`
- 新增 1 条冒烟数据：`0001 / 冒烟一级组`

## 执行后校验
- `SHOW CREATE TABLE pm_user_group` 正常返回
- 表字符集为 `utf8mb4`，排序规则为 `utf8mb4_0900_ai_ci`
- 表注释为 `流程管理用户组`
- 字段中文注释已正常落库，无 `????` 或乱码
- 唯一索引、普通索引、自关联外键均已存在
- 冒烟数据 `group_code='0001'` 存在且仅 1 条

## 幂等性验证
- 同一脚本已连续执行 2 次
- 两次执行均未产生重复键错误
- 二次执行后冒烟数据仍仅 1 条

## 回滚说明
1. 若当前库中仍只有本次初始化产生的冒烟数据，可先删除冒烟数据，再删除整表：

```sql
DELETE FROM pm_user_group WHERE group_code = '0001';
DROP TABLE pm_user_group;
```

2. 若执行后已经开始承载真实用户组业务数据，则禁止直接删表回滚，需先做人工评估和数据迁移方案。
