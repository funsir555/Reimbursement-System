-- FinWork 历史空会话清理。仅供人工核对、备份并暂停 FinWork 写入后执行。
-- 目标库只读快照：2026-10-13 前的安全候选为 83 条；脚本不会删除消息或日志。
-- 重复执行时 0 条候选视为已完成；其他数量一律中止。
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS cleanup_finwork_empty_conversations_20261013;
DELIMITER $$
CREATE PROCEDURE cleanup_finwork_empty_conversations_20261013()
BEGIN
  DECLARE candidate_count INT DEFAULT 0;
  DECLARE deleted_count INT DEFAULT 0;
  DECLARE still_safe INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS tmp_finwork_empty_conversations;
    RESIGNAL;
  END;

  IF DATABASE() <> 'finex_db' OR DATABASE() IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '数据库不是 finex_db，已停止清理';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_finwork_empty_conversations;
  CREATE TEMPORARY TABLE tmp_finwork_empty_conversations (
    id BIGINT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL
  ) ENGINE=MEMORY;

  START TRANSACTION;
  INSERT INTO tmp_finwork_empty_conversations (id, session_id)
  SELECT c.id, c.session_id
  FROM ea_agent_conversation c
  WHERE c.created_at < '2026-10-13 00:00:00'
    AND c.deleted = 0
    AND c.message_count = 0
    AND c.last_message_at IS NULL
    AND (c.summary_text IS NULL OR TRIM(c.summary_text) = '')
    AND c.context_json IS NULL
    AND NOT EXISTS (SELECT 1 FROM ea_agent_message m
                    WHERE BINARY m.session_id = BINARY c.session_id)
    AND NOT EXISTS (SELECT 1 FROM ea_agent_chat_pending_plan p
                    WHERE BINARY p.session_id = BINARY c.session_id)
    AND NOT EXISTS (SELECT 1 FROM ea_desensitization_mapping d
                    WHERE BINARY d.session_id = BINARY c.session_id)
    AND NOT EXISTS (SELECT 1 FROM ea_tool_execution_log t
                    WHERE BINARY t.session_id = BINARY c.session_id)
    AND NOT EXISTS (SELECT 1 FROM ea_llm_api_log l
                    WHERE BINARY l.session_id = BINARY c.session_id)
    AND NOT EXISTS (SELECT 1 FROM ea_conversation legacy
                    WHERE BINARY legacy.session_id = BINARY c.session_id)
    AND NOT EXISTS (SELECT 1 FROM ea_agent_run r
                    WHERE LOCATE(c.session_id, COALESCE(r.input_json, '')) > 0
                       OR LOCATE(c.session_id, COALESCE(r.output_json, '')) > 0
                       OR LOCATE(c.session_id, COALESCE(CAST(r.execution_context AS CHAR), '')) > 0)
    AND NOT EXISTS (SELECT 1 FROM ea_agent_run_step step_log
                    WHERE LOCATE(c.session_id, COALESCE(step_log.input_json, '')) > 0
                       OR LOCATE(c.session_id, COALESCE(step_log.output_json, '')) > 0)
    AND NOT EXISTS (SELECT 1 FROM ea_agent_run_artifact artifact
                    WHERE LOCATE(c.session_id, COALESCE(CAST(artifact.content_json AS CHAR), '')) > 0);

  SELECT COUNT(*) INTO candidate_count FROM tmp_finwork_empty_conversations;
  SELECT DATABASE() AS target_database, candidate_count AS candidate_count,
         83 AS first_run_expected_count;
  SELECT id, session_id FROM tmp_finwork_empty_conversations ORDER BY id;

  IF candidate_count NOT IN (0, 83) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '候选数量不是 83 或 0，已回滚；请重新核对';
  END IF;

  IF candidate_count = 83 THEN
    -- 停写之外再核对一次，任何新增消息或引用都会使本次清理整体回滚。
    SELECT COUNT(*) INTO still_safe
    FROM tmp_finwork_empty_conversations x
    JOIN ea_agent_conversation c ON c.id = x.id
    WHERE c.created_at < '2026-10-13 00:00:00'
      AND c.deleted = 0 AND c.message_count = 0 AND c.last_message_at IS NULL
      AND (c.summary_text IS NULL OR TRIM(c.summary_text) = '')
      AND c.context_json IS NULL
      AND NOT EXISTS (SELECT 1 FROM ea_agent_message m WHERE BINARY m.session_id = BINARY c.session_id)
      AND NOT EXISTS (SELECT 1 FROM ea_agent_chat_pending_plan p WHERE BINARY p.session_id = BINARY c.session_id)
      AND NOT EXISTS (SELECT 1 FROM ea_desensitization_mapping d WHERE BINARY d.session_id = BINARY c.session_id)
      AND NOT EXISTS (SELECT 1 FROM ea_tool_execution_log t WHERE BINARY t.session_id = BINARY c.session_id)
      AND NOT EXISTS (SELECT 1 FROM ea_llm_api_log l WHERE BINARY l.session_id = BINARY c.session_id)
      AND NOT EXISTS (SELECT 1 FROM ea_conversation legacy WHERE BINARY legacy.session_id = BINARY c.session_id)
      AND NOT EXISTS (SELECT 1 FROM ea_agent_run r
                      WHERE LOCATE(c.session_id, COALESCE(r.input_json, '')) > 0
                         OR LOCATE(c.session_id, COALESCE(r.output_json, '')) > 0
                         OR LOCATE(c.session_id, COALESCE(CAST(r.execution_context AS CHAR), '')) > 0)
      AND NOT EXISTS (SELECT 1 FROM ea_agent_run_step step_log
                      WHERE LOCATE(c.session_id, COALESCE(step_log.input_json, '')) > 0
                         OR LOCATE(c.session_id, COALESCE(step_log.output_json, '')) > 0)
      AND NOT EXISTS (SELECT 1 FROM ea_agent_run_artifact artifact
                      WHERE LOCATE(c.session_id, COALESCE(CAST(artifact.content_json AS CHAR), '')) > 0);
    IF still_safe <> candidate_count THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '候选状态已变化，已回滚；请重新核对';
    END IF;

    DELETE c FROM ea_agent_conversation c
    JOIN tmp_finwork_empty_conversations x ON x.id = c.id;
    SET deleted_count = ROW_COUNT();
    IF deleted_count <> candidate_count THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '删除数量不符，已回滚';
    END IF;
  END IF;

  COMMIT;
  SELECT deleted_count AS deleted_conversations,
         (SELECT COUNT(*) FROM ea_agent_conversation c
          JOIN tmp_finwork_empty_conversations x ON x.id = c.id) AS candidates_still_present;
  DROP TEMPORARY TABLE tmp_finwork_empty_conversations;
END$$
DELIMITER ;

CALL cleanup_finwork_empty_conversations_20261013();
DROP PROCEDURE cleanup_finwork_empty_conversations_20261013;
