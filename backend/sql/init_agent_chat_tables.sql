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
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID，UUID格式',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `message_count` INT NOT NULL DEFAULT 0 COMMENT '消息数量',
  `last_message_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后一条消息时间',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`session_id`),
  KEY `idx_user_id` (`user_id`) COMMENT '用户ID索引，用于查询用户的会话列表',
  KEY `idx_created_at` (`created_at`) COMMENT '创建时间索引，用于按时间排序',
  KEY `idx_deleted` (`deleted`) COMMENT '删除标记索引，用于过滤已删除数据'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 会话表';

-- ----------------------------------------------------------------------------
-- 表：ea_agent_message (Agent 消息表)
-- 说明：存储会话中的消息记录
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ea_agent_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID，自增主键',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID，关联 ea_agent_conversation.session_id',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：user-用户，assistant-助手，system-系统',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `intent` VARCHAR(50) NULL DEFAULT NULL COMMENT '意图识别结果：chat-普通聊天，query_expense-查询报销单',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`) COMMENT '会话ID索引，用于查询会话的消息列表',
  KEY `idx_created_at` (`created_at`) COMMENT '创建时间索引，用于按时间排序',
  KEY `idx_role` (`role`) COMMENT '角色索引，用于按角色过滤消息'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 消息表';

-- ----------------------------------------------------------------------------
-- 索引优化说明
-- ----------------------------------------------------------------------------
-- 1. ea_agent_conversation
--    - idx_user_id: 支持 "查询用户的会话列表" 场景
--    - idx_created_at: 支持 "按创建时间排序" 场景
--    - idx_deleted: 支持 "过滤已删除会话" 场景
--
-- 2. ea_agent_message
--    - idx_session_id: 支持 "查询会话的所有消息" 场景（最常用）
--    - idx_created_at: 支持 "按时间排序消息" 场景
--    - idx_role: 支持 "按角色过滤消息" 场景（较少用）
--
-- 复合索引考虑：
--    - (session_id, created_at): 如果经常需要按会话+时间查询，可以考虑添加
--    - (user_id, deleted, created_at): 如果经常查询用户的未删除会话列表，可以考虑添加

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
