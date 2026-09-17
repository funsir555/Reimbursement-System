-- FinEx AI Agent Chat - 权限配置脚本
-- 版本: v1.0
-- 创建时间: 2026-09-13
-- 说明: Agent 聊天功能的权限配置

-- ============================================================
-- Agent 聊天权限配置
-- ============================================================

-- 插入 Agent 聊天菜单权限（与 agents:menu 平级）
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
SELECT 'agent:chat:menu', 'AI 助手', 'MENU', NULL, 'agentChat', '/agent/chat', 56, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'agent:chat:menu'
);

-- 插入 Agent 聊天查看权限
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
SELECT 'agent:chat:view', 'AI 助手对话', 'MENU', parent.id, 'agentChat', '/agent/chat', 561, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:chat:menu'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:chat:view'
  );

-- 插入 Agent 聊天发送消息权限
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
SELECT 'agent:chat:send', '发送消息', 'BUTTON', parent.id, 'agentChat', NULL, 5611, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:chat:view'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:chat:send'
  );

-- ============================================================
-- 为超级管理员角色授予权限
-- ============================================================

-- 授予 agent:chat:menu 权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'super_admin'
  AND p.permission_code = 'agent:chat:menu'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 授予 agent:chat:view 权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'super_admin'
  AND p.permission_code = 'agent:chat:view'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 授予 agent:chat:send 权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'super_admin'
  AND p.permission_code = 'agent:chat:send'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ============================================================
-- 验证查询（可选，用于确认配置成功）
-- ============================================================

-- 查看新增的权限
-- SELECT
--     permission_code,
--     permission_name,
--     permission_type,
--     module_code,
--     route_path,
--     sort_order
-- FROM sys_permission
-- WHERE permission_code LIKE 'agent:chat:%'
-- ORDER BY sort_order;

-- 查看超级管理员的 Agent Chat 权限
-- SELECT
--     r.role_name,
--     p.permission_code,
--     p.permission_name,
--     p.permission_type
-- FROM sys_role r
-- JOIN sys_role_permission rp ON r.id = rp.role_id
-- JOIN sys_permission p ON rp.permission_id = p.id
-- WHERE r.role_code = 'super_admin'
--   AND p.permission_code LIKE 'agent:chat:%'
-- ORDER BY p.sort_order;
