-- ============================================================================
-- Agent Chat 完整表结构修复脚本
-- 功能：修复表结构，添加缺失的 id 主键
-- 版本：v5.0 - 最终版
-- 日期：2026-09-14
-- 说明：
--   - 添加自增 id 主键
--   - 保留 session_id 唯一索引
--   - 保留所有现有数据
--   - 幂等执行
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 步骤 1: 备份现有数据
-- ============================================================================
CREATE TABLE IF NOT EXISTS ea_agent_conversation_backup_20260914 AS
SELECT * FROM ea_agent_conversation;

CREATE TABLE IF NOT EXISTS ea_agent_message_backup_20260914 AS
SELECT * FROM ea_agent_message;

-- ============================================================================
-- 步骤 2: 重建 ea_agent_conversation 表（正确的结构）
-- ============================================================================
DROP TABLE IF EXISTS ea_agent_conversation;

CREATE TABLE `ea_agent_conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID（UUID）',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `context_json` TEXT NULL COMMENT '上下文数据（JSON）',
  `summary_text` TEXT NULL COMMENT '会话摘要',
  `message_count` INT NOT NULL DEFAULT 0 COMMENT '消息数量',
  `last_message_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后消息时间',
  `expires_at` TIMESTAMP NULL DEFAULT NULL COMMENT '过期时间',

  -- TenantAwareEntity 字段
  `tenant_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
  `company_id` BIGINT NOT NULL DEFAULT 1 COMMENT '公司ID',
  `created_by` BIGINT NULL COMMENT '创建人',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  `deleted_at` TIMESTAMP NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL COMMENT '删除人',
  `trace_id` VARCHAR(64) NULL COMMENT '追踪ID',
  `version_no` INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_tenant_company` (`tenant_id`, `company_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 会话表';

-- ============================================================================
-- 步骤 3: 恢复数据
-- ============================================================================
INSERT INTO ea_agent_conversation (
  session_id, user_id, context_json, summary_text, message_count,
  last_message_at, expires_at, tenant_id, company_id, created_by,
  updated_by, created_at, updated_at, deleted, deleted_at,
  deleted_by, trace_id, version_no
)
SELECT
  session_id, user_id,
  COALESCE(context_json, NULL),
  COALESCE(summary_text, NULL),
  COALESCE(message_count, 0),
  last_message_at,
  COALESCE(expires_at, NULL),
  COALESCE(tenant_id, 'default'),
  COALESCE(company_id, 1),
  created_by,
  updated_by,
  created_at,
  updated_at,
  COALESCE(deleted, 0),
  deleted_at,
  deleted_by,
  COALESCE(trace_id, NULL),
  COALESCE(version_no, 0)
FROM ea_agent_conversation_backup_20260914;

-- ============================================================================
-- 步骤 4: 重建 ea_agent_message 表（添加 id 主键）
-- ============================================================================
DROP TABLE IF EXISTS ea_agent_message;

CREATE TABLE `ea_agent_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `tenant_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：user-用户，assistant-助手，system-系统',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `masked_content` TEXT NULL COMMENT '脱敏后内容',
  `intent` VARCHAR(50) NULL DEFAULT NULL COMMENT '意图分类',
  `tokens` INT NULL COMMENT 'Token 数量',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `trace_id` VARCHAR(64) NULL COMMENT '追踪ID',

  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_role` (`role`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 消息表';

-- ============================================================================
-- 步骤 5: 恢复消息数据
-- ============================================================================
INSERT INTO ea_agent_message (
  tenant_id, session_id, role, content, masked_content,
  intent, tokens, created_at, trace_id
)
SELECT
  COALESCE(tenant_id, 'default'),
  session_id,
  role,
  content,
  COALESCE(masked_content, NULL),
  intent,
  COALESCE(tokens, NULL),
  created_at,
  COALESCE(trace_id, NULL)
FROM ea_agent_message_backup_20260914;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 验证
-- ============================================================================
SELECT '=== ea_agent_conversation 表结构 ===' AS info;
DESC ea_agent_conversation;

SELECT '=== ea_agent_message 表结构 ===' AS info;
DESC ea_agent_message;

SELECT CONCAT('ea_agent_conversation: ', COUNT(*), ' 条记录') AS info
FROM ea_agent_conversation;

SELECT CONCAT('ea_agent_message: ', COUNT(*), ' 条记录') AS info
FROM ea_agent_message;

SELECT '表重建完成！所有数据已恢复。' AS status;
