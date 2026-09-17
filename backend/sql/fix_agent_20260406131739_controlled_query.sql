-- Idempotently migrate Agent 20260406131739 from the obsolete archive.lookup
-- binding to the supported controlled expense query workflow.
START TRANSACTION;

SET @target_agent_code := 'AG2604061317744';
SET @target_agent_id := (
    SELECT id
    FROM ea_agent_definition
    WHERE BINARY agent_code = BINARY @target_agent_code
    LIMIT 1
);
SET @latest_version_id := (
    SELECT id
    FROM ea_agent_version
    WHERE agent_id = @target_agent_id
    ORDER BY version_no DESC, id DESC
    LIMIT 1
);
SET @latest_config := (
    SELECT config_json
    FROM ea_agent_version
    WHERE id = @latest_version_id
);
SET @needs_migration := IF(
    @latest_config IS NOT NULL
        AND JSON_SEARCH(
            JSON_EXTRACT(@latest_config, '$.workflow.nodes'),
            'one',
            'archive.lookup',
            NULL,
            '$[*].config.toolCode'
        ) IS NOT NULL,
    1,
    0
);
SET @latest_has_marker := IF(
    JSON_UNQUOTE(JSON_EXTRACT(@latest_config, '$.migration.archiveLookupToExpenseV1')) = 'true',
    1,
    0
);
SET @next_version_no := (
    SELECT COALESCE(MAX(version_no), 0) + 1
    FROM ea_agent_version
    WHERE agent_id = @target_agent_id
);

INSERT INTO ea_agent_version (
    agent_id,
    version_no,
    version_label,
    config_json,
    published,
    created_by_user_id,
    created_by_name
)
SELECT
    @target_agent_id,
    @next_version_no,
    CONCAT('v', @next_version_no, ' controlled expense query'),
    JSON_SET(
        CAST(@latest_config AS JSON),
        '$.migration', JSON_OBJECT('archiveLookupToExpenseV1', TRUE),
        '$.tools', JSON_ARRAY(
            JSON_OBJECT(
                'toolCode', 'expense.query_my_expenses',
                'label', '受控查询：我的报销单',
                'category', 'BUSINESS',
                'enabled', TRUE,
                'queryTemplateCode', 'expense.my_expenses.v1'
            )
        ),
        '$.workflow', JSON_OBJECT(
            'nodes', JSON_ARRAY(
                JSON_OBJECT(
                    'nodeKey', 'start',
                    'nodeType', 'start',
                    'label', '开始',
                    'config', JSON_OBJECT(
                        'canvasPosition', JSON_OBJECT('x', 100, 'y', 260)
                    )
                ),
                JSON_OBJECT(
                    'nodeKey', 'expense_query',
                    'nodeType', 'tool',
                    'label', '受控查询',
                    'config', JSON_OBJECT(
                        'toolCode', 'expense.query_my_expenses',
                        'queryTemplateCode', 'expense.my_expenses.v1',
                        'variableMapping', '{"keyword":"{{user.keyword}}","status":"{{user.status}}","dateRange":"{{user.dateRange}}","amountRange":"{{user.amountRange}}"}',
                        'canvasPosition', JSON_OBJECT('x', 350, 'y', 260)
                    )
                ),
                JSON_OBJECT(
                    'nodeKey', 'expense_analysis',
                    'nodeType', 'llm',
                    'label', '模型分析',
                    'config', JSON_OBJECT(
                        'promptTemplate', '请仅依据脱敏后的报销查询结果回答用户问题，输出结论并保留 evidenceRefs。',
                        'canvasPosition', JSON_OBJECT('x', 600, 'y', 260)
                    )
                ),
                JSON_OBJECT(
                    'nodeKey', 'end',
                    'nodeType', 'end',
                    'label', '结束',
                    'config', JSON_OBJECT(
                        'canvasPosition', JSON_OBJECT('x', 850, 'y', 260)
                    )
                )
            ),
            'edges', JSON_ARRAY(
                JSON_OBJECT('source', 'start', 'target', 'expense_query'),
                JSON_OBJECT('source', 'expense_query', 'target', 'expense_analysis'),
                JSON_OBJECT('source', 'expense_analysis', 'target', 'end')
            )
        )
    ),
    0,
    d.owner_user_id,
    'controlled-query-migration'
FROM ea_agent_definition d
WHERE d.id = @target_agent_id
  AND @needs_migration = 1;

SET @migration_version_id := CASE
    WHEN @needs_migration = 1 THEN LAST_INSERT_ID()
    WHEN @latest_has_marker = 1 THEN @latest_version_id
    ELSE NULL
END;

DELETE FROM ea_agent_tool_binding
WHERE agent_id = @target_agent_id
  AND tool_code = 'archive.lookup'
  AND @migration_version_id IS NOT NULL;

INSERT INTO ea_agent_tool_binding (
    agent_id,
    tool_code,
    enabled,
    credential_ref_code,
    config_json
)
SELECT
    @target_agent_id,
    'expense.query_my_expenses',
    1,
    NULL,
    JSON_OBJECT(
        'toolCode', 'expense.query_my_expenses',
        'label', '受控查询：我的报销单',
        'category', 'BUSINESS',
        'enabled', TRUE,
        'queryTemplateCode', 'expense.my_expenses.v1'
    )
WHERE @migration_version_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM ea_agent_tool_binding
      WHERE agent_id = @target_agent_id
        AND tool_code = 'expense.query_my_expenses'
  );

UPDATE ea_agent_tool_binding
SET enabled = 1,
    config_json = JSON_OBJECT(
        'toolCode', 'expense.query_my_expenses',
        'label', '受控查询：我的报销单',
        'category', 'BUSINESS',
        'enabled', TRUE,
        'queryTemplateCode', 'expense.my_expenses.v1'
    )
WHERE agent_id = @target_agent_id
  AND tool_code = 'expense.query_my_expenses'
  AND @migration_version_id IS NOT NULL;

UPDATE ea_agent_version
SET published = IF(id = @migration_version_id, 1, 0)
WHERE agent_id = @target_agent_id
  AND @migration_version_id IS NOT NULL;

UPDATE ea_agent_definition d
JOIN ea_agent_version v ON v.id = @migration_version_id
SET d.latest_version_no = v.version_no,
    d.published_version_id = v.id,
    d.status = IF(d.status = 'DISABLED', 'DISABLED', 'READY')
WHERE d.id = @target_agent_id;

COMMIT;

SELECT
    d.id AS agent_id,
    d.agent_code,
    d.agent_name,
    d.status,
    d.latest_version_no,
    d.published_version_id,
    v.version_no AS published_version_no,
    JSON_UNQUOTE(JSON_EXTRACT(v.config_json, '$.workflow.nodes[1].config.toolCode')) AS tool_code,
    JSON_LENGTH(JSON_EXTRACT(v.config_json, '$.workflow.nodes')) AS node_count,
    JSON_LENGTH(JSON_EXTRACT(v.config_json, '$.workflow.edges')) AS edge_count
FROM ea_agent_definition d
JOIN ea_agent_version v ON v.id = d.published_version_id
WHERE BINARY d.agent_code = BINARY @target_agent_code;
