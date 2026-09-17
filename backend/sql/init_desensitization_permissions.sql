-- 脱敏管理权限初始化脚本
-- 创建时间: 2026-09-14
-- 说明: 为 Agent 平台添加脱敏配置功能的权限

USE finex_db;

-- ============================================================
-- 脱敏规则配置权限
-- ============================================================

-- 1. 脱敏规则配置 - 页面访问权限
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
)
SELECT 'agent:desensitization:view', '脱敏规则配置', 'MENU', parent.id, 'agentDesensitization', '/agent/desensitization', 5607, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:menu'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:desensitization:view'
  );

-- 2. 脱敏规则配置 - 管理权限（添加、编辑、删除实体类型）
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
)
SELECT 'agent:desensitization:manage', '管理脱敏规则', 'BUTTON', parent.id, 'agentDesensitization', NULL, 56071, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:desensitization:view'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:desensitization:manage'
  );

-- 3. 脱敏测试权限
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
)
SELECT 'agent:desensitization:test', '脱敏测试', 'BUTTON', parent.id, 'agentDesensitization', NULL, 56072, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:desensitization:view'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:desensitization:test'
  );

-- 4. 映射表管理权限
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
)
SELECT 'agent:desensitization:mapping', '映射表管理', 'BUTTON', parent.id, 'agentDesensitization', NULL, 56073, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:desensitization:view'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:desensitization:mapping'
  );

-- ============================================================
-- 为超级管理员角色授予脱敏权限
-- ============================================================

-- 授予所有 agent:desensitization 相关权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'super_admin'
  AND p.permission_code LIKE 'agent:desensitization:%'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ============================================================
-- 验证查询
-- ============================================================

-- 查看新增的脱敏权限
SELECT
    permission_code,
    permission_name,
    permission_type,
    module_code,
    route_path,
    sort_order,
    status
FROM sys_permission
WHERE permission_code LIKE 'agent:desensitization:%'
ORDER BY sort_order;

-- 查看超级管理员的脱敏权限
SELECT
    r.role_name,
    p.permission_code,
    p.permission_name,
    p.permission_type
FROM sys_role r
JOIN sys_role_permission rp ON r.id = rp.role_id
JOIN sys_permission p ON rp.permission_id = p.id
WHERE r.role_code = 'super_admin'
  AND p.permission_code LIKE 'agent:desensitization:%'
ORDER BY p.sort_order;
