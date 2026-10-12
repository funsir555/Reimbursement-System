-- ============================================================================
-- Agent Chat 完整表结构修复脚本
-- 功能：修复表结构，添加缺失的 id 主键
-- 版本：v5.0 - 最终版
-- 日期：2026-09-14
-- 说明：
--   - 添加自增 id 主键
--   - 保留 session_id 唯一索引
--   - 仅允许活动表为空时执行，不自动恢复或猜测公司编码
--   - 有正式数据时必须使用单独审批的数据映射方案
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- P0 安全前置：Agent 尚未正式启用，只允许空表结构修正。
-- 若已有数据，必须停止并另行制定 company_id 映射，禁止把旧值 1 猜测为真实公司。
DROP PROCEDURE IF EXISTS finex_assert_agent_tables_empty;
DELIMITER //
CREATE PROCEDURE finex_assert_agent_tables_empty()
BEGIN
  DECLARE agent_row_count BIGINT DEFAULT 0;
  SELECT (SELECT COUNT(*) FROM ea_agent_conversation)
       + (SELECT COUNT(*) FROM ea_agent_message)
    INTO agent_row_count;
  IF agent_row_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Agent tables contain data; stop and prepare an explicit company mapping plan';
  END IF;
END//
DELIMITER ;
CALL finex_assert_agent_tables_empty();
DROP PROCEDURE finex_assert_agent_tables_empty;


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
  `company_id` VARCHAR(64) NOT NULL COMMENT '业务公司主体编码',
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
  KEY `idx_tenant_company_session` (`tenant_id`, `company_id`, `session_id`),
  KEY `idx_tenant_company_user` (`tenant_id`, `company_id`, `user_id`, `last_message_at`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 会话表';

-- ============================================================================
-- 步骤 3: 不自动恢复数据
-- ============================================================================
-- 前置过程已断言活动表为空。备份表若存在历史数据，也不得在没有显式公司编码映射时自动恢复。

-- ============================================================================
-- 步骤 4: 重建 ea_agent_message 表（添加 id 主键）
-- ============================================================================
DROP TABLE IF EXISTS ea_agent_message;

CREATE TABLE `ea_agent_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `tenant_id` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
  `company_id` VARCHAR(64) NOT NULL COMMENT '业务公司主体编码',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：user-用户，assistant-助手，system-系统',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `masked_content` TEXT NULL COMMENT '脱敏后内容',
  `intent` VARCHAR(50) NULL DEFAULT NULL COMMENT '意图分类',
  `tokens` INT NULL COMMENT 'Token 数量',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `trace_id` VARCHAR(64) NULL COMMENT '追踪ID',

  PRIMARY KEY (`id`),
  KEY `idx_tenant_company_session` (`tenant_id`, `company_id`, `session_id`, `created_at`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_role` (`role`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 消息表';

-- ============================================================================
-- 步骤 5: 不自动恢复消息数据
-- ============================================================================
-- 若备份表包含历史消息，停止本脚本并使用单独审批的数据迁移方案。

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

SELECT '空 Agent 表结构重建完成；未执行任何数据映射或恢复。' AS status;
