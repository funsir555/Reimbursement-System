-- ============================================================================
-- Agent 模块权限混乱一键修复脚本
-- 功能：清理旧权限，重建新权限体系，确保前后端链路一致
-- 版本：v1.0
-- 日期：2026-09-13
-- 说明：幂等脚本，可安全重复执行
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 第一步：备份现有权限（安全措施）
-- ============================================================================

-- 创建备份表
CREATE TABLE IF NOT EXISTS sys_permission_backup_agent_20260913 AS
SELECT * FROM sys_permission
WHERE permission_code LIKE 'agent%' OR permission_code LIKE 'agents:%';

CREATE TABLE IF NOT EXISTS sys_role_permission_backup_agent_20260913 AS
SELECT rp.* FROM sys_role_permission rp
JOIN sys_permission p ON rp.permission_id = p.id
WHERE p.permission_code LIKE 'agent%' OR p.permission_code LIKE 'agents:%';

-- ============================================================================
-- 第二步：删除所有旧的 Agent 权限
-- ============================================================================

-- 删除角色权限关联
DELETE FROM sys_role_permission
WHERE permission_id IN (
    SELECT id FROM sys_permission
    WHERE permission_code LIKE 'agent%' OR permission_code LIKE 'agents:%'
);

-- 删除所有 agent 相关权限（包括 agents: 和 agent:）
DELETE FROM sys_permission
WHERE permission_code LIKE 'agent%' OR permission_code LIKE 'agents:%';

-- ============================================================================
-- 第三步：创建新的权限体系（与前端路由完全匹配）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Agent 一级菜单
-- ----------------------------------------------------------------------------
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
) VALUES (
    'agent:menu',
    'Agent',
    'MENU',
    NULL,
    'agent',
    '/agent',
    56,
    1
);

-- 获取 agent:menu 的 ID（用于设置子权限的 parent_id）
SET @agent_menu_id = LAST_INSERT_ID();

-- ----------------------------------------------------------------------------
-- 2. FinWork - AI 助手对话（Phase 0）
-- ----------------------------------------------------------------------------
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
) VALUES (
    'agent:chat:view',
    'FinWork',
    'MENU',
    @agent_menu_id,
    'agentChat',
    '/agent/chat',
    5601,
    1
);

SET @agent_chat_id = LAST_INSERT_ID();

-- FinWork - 发送消息权限
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
) VALUES (
    'agent:chat:send',
    '发送消息',
    'BUTTON',
    @agent_chat_id,
    'agentChat',
    NULL,
    56011,
    1
);

-- ----------------------------------------------------------------------------
-- 3. Agent 工作台
-- ----------------------------------------------------------------------------
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
) VALUES (
    'agent:workbench:view',
    'Agent 工作台',
    'MENU',
    @agent_menu_id,
    'agentWorkbench',
    '/agent/workbench',
    5602,
    1
);

SET @agent_workbench_id = LAST_INSERT_ID();

-- Agent 工作台 - 新建 Agent
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
) VALUES (
    'agent:workbench:create',
    '新建 Agent',
    'BUTTON',
    @agent_workbench_id,
    'agentWorkbench',
    NULL,
    56021,
    1
);

-- Agent 工作台 - 编辑 Agent
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
) VALUES (
    'agent:workbench:edit',
    '编辑 Agent',
    'BUTTON',
    @agent_workbench_id,
    'agentWorkbench',
    NULL,
    56022,
    1
);

-- Agent 工作台 - 删除 Agent
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
) VALUES (
    'agent:workbench:delete',
    '删除 Agent',
    'BUTTON',
    @agent_workbench_id,
    'agentWorkbench',
    NULL,
    56023,
    1
);

-- Agent 工作台 - 运行 Agent
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
) VALUES (
    'agent:workbench:run',
    '运行 Agent',
    'BUTTON',
    @agent_workbench_id,
    'agentWorkbench',
    NULL,
    56024,
    1
);

-- Agent 工作台 - 发布 Agent
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
) VALUES (
    'agent:workbench:publish',
    '发布 Agent',
    'BUTTON',
    @agent_workbench_id,
    'agentWorkbench',
    NULL,
    56025,
    1
);

-- Agent 工作台 - 查看日志
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
) VALUES (
    'agent:workbench:view_logs',
    '查看日志',
    'BUTTON',
    @agent_workbench_id,
    'agentWorkbench',
    NULL,
    56026,
    1
);

-- ----------------------------------------------------------------------------
-- 4. 个人数字分身管理（Phase 1 占位）
-- ----------------------------------------------------------------------------
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
) VALUES (
    'agent:personal_avatar:view',
    '个人数字分身',
    'MENU',
    @agent_menu_id,
    'agentPersonalAvatar',
    '/agent/personal-avatar',
    5603,
    1
);

-- ----------------------------------------------------------------------------
-- 5. 数字员工管理（Phase 2 占位）
-- ----------------------------------------------------------------------------
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
) VALUES (
    'agent:digital_employee:view',
    '数字员工',
    'MENU',
    @agent_menu_id,
    'agentDigitalEmployee',
    '/agent/digital-employee',
    5604,
    1
);

-- ----------------------------------------------------------------------------
-- 6. 业务本体查询（Phase 1 占位）
-- ----------------------------------------------------------------------------
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
) VALUES (
    'agent:business_ontology:view',
    '业务本体',
    'MENU',
    @agent_menu_id,
    'agentBusinessOntology',
    '/agent/business-ontology',
    5605,
    1
);

-- ----------------------------------------------------------------------------
-- 7. 企业知识库管理（Phase 2 占位）
-- ----------------------------------------------------------------------------
INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
) VALUES (
    'agent:knowledge_base:view',
    '企业知识库',
    'MENU',
    @agent_menu_id,
    'agentKnowledgeBase',
    '/agent/knowledge-base',
    5606,
    1
);

-- ============================================================================
-- 第四步：分配权限给 super_admin 角色
-- ============================================================================

-- 获取 super_admin 角色 ID
SET @super_admin_role_id = (SELECT id FROM sys_role WHERE role_code = 'super_admin' LIMIT 1);

-- 分配所有 agent 权限给 super_admin
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT @super_admin_role_id, id
FROM sys_permission
WHERE permission_code LIKE 'agent:%'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission
      WHERE role_id = @super_admin_role_id
        AND permission_id = sys_permission.id
  );

-- ============================================================================
-- 第五步：验证权限配置
-- ============================================================================

-- 显示创建的权限
SELECT '=== 新创建的 Agent 权限 ===' AS info;
SELECT permission_code, permission_name, permission_type, sort_order
FROM sys_permission
WHERE permission_code LIKE 'agent:%'
ORDER BY sort_order;

-- 显示 super_admin 的权限
SELECT '=== super_admin 的 Agent 权限 ===' AS info;
SELECT p.permission_code, p.permission_name
FROM sys_role_permission rp
JOIN sys_permission p ON rp.permission_id = p.id
WHERE rp.role_id = @super_admin_role_id
  AND p.permission_code LIKE 'agent:%'
ORDER BY p.sort_order;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 脚本执行完成
-- ============================================================================
SELECT '权限修复完成！请检查上方的验证结果。' AS status;
