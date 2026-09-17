-- FinEx Agent 平台 - 完整权限配置脚本
-- 版本: v2.0
-- 创建时间: 2026-09-13
-- 说明: Agent 平台的完整导航架构权限配置

-- ============================================================
-- Agent 模块权限配置
-- ============================================================

-- 1. Agent 一级菜单（替代原有的 agents:menu）
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
SELECT 'agent:menu', 'Agent', 'MENU', NULL, 'agent', '/agent', 56, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'agent:menu'
);

-- 2. FinWork - AI 助手对话（Phase 0 - 已实现）
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
SELECT 'agent:chat:view', 'FinWork', 'MENU', parent.id, 'agentChat', '/agent/chat', 5601, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:menu'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:chat:view'
  );

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
SELECT 'agent:chat:send', '发送消息', 'BUTTON', parent.id, 'agentChat', NULL, 56011, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:chat:view'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:chat:send'
  );

-- 3. Agent 工作台（已有功能，从 archives 迁移）
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
SELECT 'agent:workbench:view', 'Agent 工作台', 'MENU', parent.id, 'agentWorkbench', '/agent/workbench', 5602, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:menu'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:workbench:view'
  );

-- Agent 工作台的子权限（复用原有的 agents 权限）
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
SELECT 'agent:workbench:create', '新建 Agent', 'BUTTON', parent.id, 'agentWorkbench', NULL, 56021, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:workbench:view'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:workbench:create'
  );

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
SELECT 'agent:workbench:edit', '编辑 Agent', 'BUTTON', parent.id, 'agentWorkbench', NULL, 56022, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:workbench:view'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:workbench:edit'
  );

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
SELECT 'agent:workbench:delete', '删除 Agent', 'BUTTON', parent.id, 'agentWorkbench', NULL, 56023, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:workbench:view'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:workbench:delete'
  );

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
SELECT 'agent:workbench:run', '运行 Agent', 'BUTTON', parent.id, 'agentWorkbench', NULL, 56024, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:workbench:view'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:workbench:run'
  );

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
SELECT 'agent:workbench:publish', '发布 Agent', 'BUTTON', parent.id, 'agentWorkbench', NULL, 56025, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:workbench:view'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:workbench:publish'
  );

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
SELECT 'agent:workbench:view_logs', '查看运行日志', 'BUTTON', parent.id, 'agentWorkbench', NULL, 56026, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:workbench:view'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:workbench:view_logs'
  );

-- 4. 个人数字分身管理（Phase 1 - 占位）
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
SELECT 'agent:personal_avatar:view', '个人数字分身', 'MENU', parent.id, 'agentPersonalAvatar', '/agent/personal-avatar', 5603, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:menu'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:personal_avatar:view'
  );

-- 5. 数字员工管理（Phase 2 - 占位）
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
SELECT 'agent:digital_employee:view', '数字员工', 'MENU', parent.id, 'agentDigitalEmployee', '/agent/digital-employee', 5604, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:menu'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:digital_employee:view'
  );

-- 6. 业务本体查询（Phase 1 - 占位）
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
SELECT 'agent:business_ontology:view', '业务本体', 'MENU', parent.id, 'agentBusinessOntology', '/agent/business-ontology', 5605, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:menu'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:business_ontology:view'
  );

-- 7. 企业知识库管理（Phase 2 - 占位）
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
SELECT 'agent:knowledge_base:view', '企业知识库', 'MENU', parent.id, 'agentKnowledgeBase', '/agent/knowledge-base', 5606, 1
FROM sys_permission parent
WHERE parent.permission_code = 'agent:menu'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission WHERE permission_code = 'agent:knowledge_base:view'
  );

-- ============================================================
-- 为超级管理员角色授予所有 Agent 权限
-- ============================================================

-- 授予 agent:menu 权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'super_admin'
  AND p.permission_code = 'agent:menu'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 授予所有 agent 子权限（批量授权）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'super_admin'
  AND p.permission_code LIKE 'agent:%'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ============================================================
-- 验证查询（可选）
-- ============================================================

-- 查看新增的所有 Agent 权限
-- SELECT
--     permission_code,
--     permission_name,
--     permission_type,
--     module_code,
--     route_path,
--     sort_order
-- FROM sys_permission
-- WHERE permission_code LIKE 'agent:%'
-- ORDER BY sort_order;

-- 查看超级管理员的 Agent 权限
-- SELECT
--     r.role_name,
--     p.permission_code,
--     p.permission_name,
--     p.permission_type
-- FROM sys_role r
-- JOIN sys_role_permission rp ON r.id = rp.role_id
-- JOIN sys_permission p ON rp.permission_id = p.id
-- WHERE r.role_code = 'super_admin'
--   AND p.permission_code LIKE 'agent:%'
-- ORDER BY p.sort_order;
