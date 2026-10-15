SET NAMES utf8mb4;

-- Run after ea_agent_platform_init.sql. Safe to rerun; no conversation data changes.
SET @finwork_history_index_exists = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'ea_agent_conversation'
    AND index_name = 'idx_ea_agent_conversation_user_history'
);
SET @finwork_history_index_sql = IF(@finwork_history_index_exists = 0,
  'ALTER TABLE ea_agent_conversation ADD INDEX idx_ea_agent_conversation_user_history (tenant_id, user_id, deleted, last_message_at, id)',
  'SELECT ''FinWork conversation history index exists''');
PREPARE finwork_history_index_stmt FROM @finwork_history_index_sql;
EXECUTE finwork_history_index_stmt;
DEALLOCATE PREPARE finwork_history_index_stmt;
