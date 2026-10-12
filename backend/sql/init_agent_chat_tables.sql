-- ============================================================================
-- FinEx Agent Chat 数据库表初始化脚本
-- 功能：创建 Agent Chat (FinWork) 所需的数据库表
-- 版本：v1.0
-- 日期：2026-09-13
-- 说明：幂等脚本，可重复执行
-- ============================================================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------------
-- 表：ea_agent_conversation (Agent 会话表)
-- 说明：存储用户与 AI 助手的会话信息
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ea_agent_conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
  `company_id` VARCHAR(64) NOT NULL COMMENT '业务公司主体编码',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID，UUID格式',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `context_json` TEXT NULL COMMENT '上下文数据（JSON）',
  `summary_text` TEXT NULL COMMENT '会话摘要',
  `message_count` INT NOT NULL DEFAULT 0 COMMENT '消息数量',
  `last_message_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后一条消息时间',
  `expires_at` TIMESTAMP NULL DEFAULT NULL COMMENT '过期时间',
  `created_by` BIGINT NULL COMMENT '创建人',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `deleted_at` TIMESTAMP NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL COMMENT '删除人',
  `trace_id` VARCHAR(64) NULL COMMENT '追踪ID',
  `version_no` INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_tenant_company_session` (`tenant_id`, `company_id`, `session_id`),
  KEY `idx_tenant_company_user` (`tenant_id`, `company_id`, `user_id`, `last_message_at`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 会话表';

-- ----------------------------------------------------------------------------
-- 表：ea_agent_message (Agent 消息表)
-- 说明：存储会话中的消息记录
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ea_agent_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID，自增主键',
  `tenant_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
  `company_id` VARCHAR(64) NOT NULL COMMENT '业务公司主体编码',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID，关联 ea_agent_conversation.session_id',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：user-用户，assistant-助手，system-系统',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `masked_content` TEXT NULL COMMENT '脱敏后内容',
  `intent` VARCHAR(50) NULL DEFAULT NULL COMMENT '意图识别结果：chat-普通聊天，query_expense-查询报销单',
  `tokens` INT NULL COMMENT 'Token 数量',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `trace_id` VARCHAR(64) NULL COMMENT '追踪ID',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_company_session` (`tenant_id`, `company_id`, `session_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 消息表';

-- ----------------------------------------------------------------------------
-- 索引优化说明
-- ----------------------------------------------------------------------------
-- 1. ea_agent_conversation
--    - idx_tenant_company_session: 支持隔离后的会话精确查询
--    - idx_tenant_company_user: 支持隔离后的用户会话列表及时间排序
--    - idx_deleted: 支持 "过滤已删除会话" 场景
--
-- 2. ea_agent_message
--    - idx_tenant_company_session: 支持隔离后的会话消息时间序查询
--
-- 公司编码必须来自真实用户上下文，不允许写入默认值 1。

-- ----------------------------------------------------------------------------
-- 数据验证查询
-- ----------------------------------------------------------------------------
-- 验证表是否创建成功
-- SELECT TABLE_NAME, TABLE_COMMENT, ENGINE, TABLE_COLLATION
-- FROM information_schema.TABLES
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('ea_agent_conversation', 'ea_agent_message');

-- 验证索引是否创建成功
-- SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX, INDEX_TYPE
-- FROM information_schema.STATISTICS
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('ea_agent_conversation', 'ea_agent_message')
-- ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- 验证字段是否正确
-- DESC ea_agent_conversation;
-- DESC ea_agent_message;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 脚本执行完成
-- ============================================================================
