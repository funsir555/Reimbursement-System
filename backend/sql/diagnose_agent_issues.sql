-- ============================================================
-- Agent 模块诊断脚本
-- 功能：全面检查 Agent 模块的配置状态
-- 版本：v1.0
-- 日期：2026-09-13
-- ============================================================

-- 1. 检查权限配置
SELECT '=== Agent 权限配置 ===' AS section;
SELECT permission_code, permission_name, permission_type, sort_order, status
FROM sys_permission
WHERE permission_code LIKE 'agent%'
ORDER BY sort_order;

-- 2. 检查旧权限是否存在
SELECT '=== 旧 agents 权限（如有，需清理）===' AS section;
SELECT permission_code, permission_name, permission_type
FROM sys_permission
WHERE permission_code LIKE 'agents:%';

-- 3. 检查 super_admin 的 agent 权限
SELECT '=== super_admin 的 Agent 权限 ===' AS section;
SELECT p.permission_code, p.permission_name
FROM sys_role_permission rp
JOIN sys_permission p ON rp.permission_id = p.id
JOIN sys_role r ON rp.role_id = r.id
WHERE r.role_code = 'super_admin'
  AND (p.permission_code LIKE 'agent%' OR p.permission_code LIKE 'agents:%')
ORDER BY p.sort_order;

-- 4. 检查表是否存在
SELECT '=== Agent 数据库表 ===' AS section;
SELECT TABLE_NAME, TABLE_COMMENT, ENGINE, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME LIKE 'ea_agent_%';

-- 5. 检查 ea_agent_conversation 表结构
SELECT '=== ea_agent_conversation 表结构 ===' AS section;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ea_agent_conversation'
ORDER BY ORDINAL_POSITION;

-- 6. 检查 ea_agent_message 表结构
SELECT '=== ea_agent_message 表结构 ===' AS section;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ea_agent_message'
ORDER BY ORDINAL_POSITION;

-- 7. 检查会话数据
SELECT '=== 会话数据统计 ===' AS section;
SELECT
    COUNT(*) AS total_conversations,
    COUNT(DISTINCT user_id) AS unique_users
FROM ea_agent_conversation
WHERE deleted = 0;

-- 8. 检查消息数据
SELECT '=== 消息数据统计 ===' AS section;
SELECT
    role,
    COUNT(*) AS message_count
FROM ea_agent_message
GROUP BY role;

-- 9. 检查用户表（验证 admin 用户存在）
SELECT '=== Admin 用户信息 ===' AS section;
SELECT id, username, name, status
FROM sys_user
WHERE username = 'admin';

-- 10. 检查 super_admin 角色
SELECT '=== super_admin 角色信息 ===' AS section;
SELECT id, role_code, role_name, status
FROM sys_role
WHERE role_code = 'super_admin';
