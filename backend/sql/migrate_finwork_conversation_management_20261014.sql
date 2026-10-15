-- FinWork 会话重命名与置顶。先备份目标库；可重复执行，不改写既有会话内容。
SET NAMES utf8mb4;

SET @finwork_custom_title_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ea_agent_conversation'
    AND column_name = 'custom_title'
);
SET @finwork_custom_title_sql = IF(@finwork_custom_title_exists = 0,
  'ALTER TABLE ea_agent_conversation ADD COLUMN custom_title VARCHAR(80) NULL COMMENT ''用户自定义会话名称''',
  'SELECT ''custom_title already exists''');
PREPARE finwork_custom_title_stmt FROM @finwork_custom_title_sql;
EXECUTE finwork_custom_title_stmt;
DEALLOCATE PREPARE finwork_custom_title_stmt;

SET @finwork_is_pinned_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ea_agent_conversation'
    AND column_name = 'is_pinned'
);
SET @finwork_is_pinned_sql = IF(@finwork_is_pinned_exists = 0,
  'ALTER TABLE ea_agent_conversation ADD COLUMN is_pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''个人置顶''',
  'SELECT ''is_pinned already exists''');
PREPARE finwork_is_pinned_stmt FROM @finwork_is_pinned_sql;
EXECUTE finwork_is_pinned_stmt;
DEALLOCATE PREPARE finwork_is_pinned_stmt;

SELECT COUNT(*) AS conversation_count,
       SUM(is_pinned = 1) AS pinned_count,
       SUM(custom_title IS NOT NULL) AS renamed_count
FROM ea_agent_conversation;
