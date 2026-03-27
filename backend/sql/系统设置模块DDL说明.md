# 系统设置模块 DDL 说明

## 本次更新目标
- 系统设置权限目录改为中文名称
- 权限树按前端当前展示架构排序
- 兼容报销管理中“流程管理”重构后的菜单层级
- 保持 `SUPER_ADMIN` 拥有全部启用权限，并确保 `admin` 绑定该角色

## 前端架构基准
当前前端菜单顺序以 [MainLayout.vue](/C:/Users/funsir/Desktop/报销系统/frontend/admin-web/src/layouts/MainLayout.vue) 和 [index.ts](/C:/Users/funsir/Desktop/报销系统/frontend/admin-web/src/router/index.ts) 为准：

1. 首页
2. 个人中心
3. 报销管理
4. 财务管理
5. 电子档案
6. 系统设置

其中报销管理下的层级为：
- 新建报销
- 我的报销
- 待我审批
- 支付
  - 银企直连
- 单据查询
- 凭证生成
- 管理工作台
  - 流程管理
  - 预算管理

流程管理页面内部虽然重构为“单据与流程 / 自定义档案 / 费用类型”，但当前前后端实际仍共用：
- `expense:process_management:view`
- `expense:process_management:create`
- `expense:process_management:edit`
- `expense:process_management:publish`
- `expense:process_management:disable`

## 已更新的 SQL 文件

### 1. [refresh_system_settings_permissions.sql](/C:/Users/funsir/Desktop/报销系统/backend/sql/refresh_system_settings_permissions.sql)
用途：
- 把现有 `sys_permission` 刷新成中文名称
- 修正父子层级与排序
- 补齐前端分组菜单节点
- 刷新 `SUPER_ADMIN` 和 `admin` 的权限关系

适用场景：
- 线上或已有环境，只想修复权限目录与超管授权

### 2. [grant_super_admin_all_permissions.sql](/C:/Users/funsir/Desktop/报销系统/backend/sql/grant_super_admin_all_permissions.sql)
用途：
- 现在已经和 `refresh_system_settings_permissions.sql` 对齐
- 可单独用于修复“超管权限不全 / 权限名称还是英文 / 菜单层级不一致”

适用场景：
- 只想执行一份“超管权限补丁”

### 3. [migrate_system_settings.sql](/C:/Users/funsir/Desktop/报销系统/backend/sql/migrate_system_settings.sql)
用途：
- 初始化系统设置相关表结构、字段和同步表
- 文件末尾已追加权限目录规范化修复块
- 即使前面旧 seed 仍是英文，最终结果也会被统一修正为中文和最新层级

适用场景：
- 新环境初始化
- 老环境补齐系统设置表结构

### 4. [migrate_company_id.sql](/C:/Users/funsir/Desktop/报销系统/backend/sql/migrate_company_id.sql)
用途：
- 给流程管理相关表和其他业务表补齐 `company_id`
- 让公司主体能和流程模板、单据模板、作用域等新架构对齐

本次没有额外改动权限数据，但如果你的流程管理重构库还没有补 `company_id`，这份也需要执行。

## 推荐执行顺序

### 旧环境仅修权限显示和超管授权
执行：
1. [refresh_system_settings_permissions.sql](/C:/Users/funsir/Desktop/报销系统/backend/sql/refresh_system_settings_permissions.sql)

或执行：
1. [grant_super_admin_all_permissions.sql](/C:/Users/funsir/Desktop/报销系统/backend/sql/grant_super_admin_all_permissions.sql)

二选一即可，这两份现在都能完成中文化、排序修复和超管全量授权。

### 旧环境需要同时补齐系统设置结构
执行：
1. [migrate_system_settings.sql](/C:/Users/funsir/Desktop/报销系统/backend/sql/migrate_system_settings.sql)
2. [refresh_system_settings_permissions.sql](/C:/Users/funsir/Desktop/报销系统/backend/sql/refresh_system_settings_permissions.sql)

### 旧环境同时包含流程管理公司主体改造
执行：
1. [migrate_company_id.sql](/C:/Users/funsir/Desktop/报销系统/backend/sql/migrate_company_id.sql)
2. [migrate_system_settings.sql](/C:/Users/funsir/Desktop/报销系统/backend/sql/migrate_system_settings.sql)
3. [refresh_system_settings_permissions.sql](/C:/Users/funsir/Desktop/报销系统/backend/sql/refresh_system_settings_permissions.sql)

### 新环境初始化
执行：
1. [migrate_company_id.sql](/C:/Users/funsir/Desktop/报销系统/backend/sql/migrate_company_id.sql)
2. [migrate_system_settings.sql](/C:/Users/funsir/Desktop/报销系统/backend/sql/migrate_system_settings.sql)

`migrate_system_settings.sql` 末尾已经自带权限规范化块，新环境理论上可以不再单独执行 `refresh_system_settings_permissions.sql`。如果你想再做一次权限兜底，重复执行也安全。

## 变更结果预期
- `sys_permission.permission_name` 改为中文
- 权限树层级与前端菜单一致
- 新增这些中间菜单节点：
  - `expense:payment:menu`
  - `expense:workbench:menu`
  - `finance:general_ledger:menu`
  - `finance:reports:menu`
  - `finance:archives:menu`
- `SUPER_ADMIN` 角色名称改为“超级管理员”
- `SUPER_ADMIN` 自动绑定全部 `status = 1` 的权限
- `admin` 自动绑定 `SUPER_ADMIN`

## 建议验证 SQL

```sql
SELECT permission_code, permission_name, sort_order
FROM sys_permission
WHERE parent_id IS NULL
ORDER BY sort_order;
```

```sql
SELECT r.role_code, r.role_name, COUNT(rp.permission_id) AS permission_count
FROM sys_role r
LEFT JOIN sys_role_permission rp ON rp.role_id = r.id
WHERE r.role_code = 'SUPER_ADMIN'
GROUP BY r.role_code, r.role_name;
```

```sql
SELECT u.username, r.role_code, r.role_name
FROM sys_user_role ur
JOIN sys_user u ON u.id = ur.user_id
JOIN sys_role r ON r.id = ur.role_id
WHERE u.username = 'admin';
```

## 执行后验证
- 重启后端服务
- `admin` 退出后重新登录
- 清理前端本地缓存中的旧用户信息
- 打开系统设置，检查权限树是否为中文且顺序正确
