-- ============================================================================
-- Agent Chat 数据库表修复脚本
-- 功能：修复表结构，使其与实体类匹配
-- 版本：v2.0
-- 日期：2026-09-13
-- 说明：幂等脚本，可安全重复执行
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 备份旧表
CREATE TABLE IF NOT EXISTS ea_agent_conversation_backup_20260913 AS
SELECT * FROM ea_agent_conversation;

CREATE TABLE IF NOT EXISTS ea_agent_message_backup_20260913 AS
SELECT * FROM ea_agent_message;

-- 删除旧表
DROP TABLE IF EXISTS ea_agent_conversation;
DROP TABLE IF EXISTS ea_agent_message;

-- ============================================================================
-- 重建 ea_agent_conversation 表（匹配实体类）
-- ============================================================================
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
-- 重建 ea_agent_message 表（匹配实体类）
-- ============================================================================
CREATE TABLE `ea_agent_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：user-用户，assistant-助手，system-系统',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `masked_content` TEXT NULL COMMENT '脱敏后内容',
  `intent` VARCHAR(50) NULL DEFAULT NULL COMMENT '意图分类',
  `tokens` INT NULL COMMENT 'Token 数量',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `trace_id` VARCHAR(64) NULL COMMENT '追踪ID',

  -- TenantAwareEntity 不需要的字段（Message 不继承它）
  `tenant_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',

  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_role` (`role`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 消息表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 验证
-- ============================================================================
SELECT '=== ea_agent_conversation 表结构 ===' AS info;
DESC ea_agent_conversation;

SELECT '=== ea_agent_message 表结构 ===' AS info;
DESC ea_agent_message;

SELECT '表重建完成！' AS status;
