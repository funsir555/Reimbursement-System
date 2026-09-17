-- ============================================================================
-- Agent Chat 数据库表修复脚本（真正幂等版本）
-- 功能：通过 ALTER TABLE 添加缺失字段，而非删除重建
-- 版本：v3.0 - 幂等修复
-- 日期：2026-09-13
-- 说明：
--   - 只添加缺失的字段，不删除任何数据
--   - 可以安全地多次执行
--   - 不会产生数据冗余
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 修复 ea_agent_conversation 表
-- ============================================================================

-- 添加缺失字段（如果不存在）
SET @db_name = DATABASE();

-- 检查并添加 context_json 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_conversation'
  AND COLUMN_NAME = 'context_json';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ea_agent_conversation ADD COLUMN context_json TEXT NULL COMMENT ''上下文数据（JSON）'' AFTER user_id',
    'SELECT ''context_json 字段已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 summary_text 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_conversation'
  AND COLUMN_NAME = 'summary_text';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ea_agent_conversation ADD COLUMN summary_text TEXT NULL COMMENT ''会话摘要'' AFTER context_json',
    'SELECT ''summary_text 字段已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 expires_at 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_conversation'
  AND COLUMN_NAME = 'expires_at';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ea_agent_conversation ADD COLUMN expires_at TIMESTAMP NULL DEFAULT NULL COMMENT ''过期时间'' AFTER last_message_at',
    'SELECT ''expires_at 字段已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 tenant_id 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_conversation'
  AND COLUMN_NAME = 'tenant_id';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ea_agent_conversation ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT ''default'' COMMENT ''租户ID'' AFTER expires_at',
    'SELECT ''tenant_id 字段已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 company_id 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_conversation'
  AND COLUMN_NAME = 'company_id';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ea_agent_conversation ADD COLUMN company_id BIGINT NOT NULL DEFAULT 1 COMMENT ''公司ID'' AFTER tenant_id',
    'SELECT ''company_id 字段已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 created_by 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_conversation'
  AND COLUMN_NAME = 'created_by';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ea_agent_conversation ADD COLUMN created_by BIGINT NULL COMMENT ''创建人'' AFTER company_id',
    'SELECT ''created_by 字段已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 updated_by 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_conversation'
  AND COLUMN_NAME = 'updated_by';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ea_agent_conversation ADD COLUMN updated_by BIGINT NULL COMMENT ''更新人'' AFTER created_by',
    'SELECT ''updated_by 字段已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 deleted_at 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_conversation'
  AND COLUMN_NAME = 'deleted_at';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ea_agent_conversation ADD COLUMN deleted_at TIMESTAMP NULL DEFAULT NULL COMMENT ''删除时间'' AFTER updated_at',
    'SELECT ''deleted_at 字段已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 deleted_by 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_conversation'
  AND COLUMN_NAME = 'deleted_by';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ea_agent_conversation ADD COLUMN deleted_by BIGINT NULL COMMENT ''删除人'' AFTER deleted_at',
    'SELECT ''deleted_by 字段已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 trace_id 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_conversation'
  AND COLUMN_NAME = 'trace_id';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ea_agent_conversation ADD COLUMN trace_id VARCHAR(64) NULL COMMENT ''追踪ID'' AFTER deleted_by',
    'SELECT ''trace_id 字段已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 version_no 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_conversation'
  AND COLUMN_NAME = 'version_no';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ea_agent_conversation ADD COLUMN version_no INT NOT NULL DEFAULT 0 COMMENT ''版本号（乐观锁）'' AFTER trace_id',
    'SELECT ''version_no 字段已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 id 主键（如果缺失）
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_conversation'
  AND COLUMN_NAME = 'id';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ea_agent_conversation ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST',
    'SELECT ''id 字段已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引（如果不存在）
SET @idx_exists = 0;
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_conversation'
  AND INDEX_NAME = 'idx_tenant_company';

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE ea_agent_conversation ADD INDEX idx_tenant_company (tenant_id, company_id)',
    'SELECT ''idx_tenant_company 索引已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================================
-- 修复 ea_agent_message 表
-- ============================================================================

-- 检查并添加 masked_content 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_message'
  AND COLUMN_NAME = 'masked_content';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ea_agent_message ADD COLUMN masked_content TEXT NULL COMMENT ''脱敏后内容'' AFTER content',
    'SELECT ''masked_content 字段已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 tokens 字段
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_message'
  AND COLUMN_NAME = 'tokens';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ea_agent_message ADD COLUMN tokens INT NULL COMMENT ''Token 数量'' AFTER intent',
    'SELECT ''tokens 字段已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 tenant_id 字段（message 表）
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_message'
  AND COLUMN_NAME = 'tenant_id';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ea_agent_message ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT ''default'' COMMENT ''租户ID'' AFTER id',
    'SELECT ''tenant_id 字段已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引（如果不存在）
SET @idx_exists = 0;
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'ea_agent_message'
  AND INDEX_NAME = 'idx_tenant_id';

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE ea_agent_message ADD INDEX idx_tenant_id (tenant_id)',
    'SELECT ''idx_tenant_id 索引已存在'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 验证结果
-- ============================================================================
SELECT '=== ea_agent_conversation 表结构 ===' AS info;
DESC ea_agent_conversation;

SELECT '=== ea_agent_message 表结构 ===' AS info;
DESC ea_agent_message;

SELECT '表结构修复完成！所有字段已添加，原有数据保留。' AS status;
