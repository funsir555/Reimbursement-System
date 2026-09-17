-- Idempotently upgrade Agent 20260406131739 to the editable v2 query contract.
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
SET @latest_is_v2 := IF(
    JSON_UNQUOTE(JSON_EXTRACT(@latest_config, '$.workflow.nodes[1].config.queryTemplateCode')) = 'expense.accessible_expenses.v2',
    1,
    0
);

-- A previous UI save may have removed migration metadata. Repair the marker in
-- place when the published behavior is already v2 so no duplicate version is created.
UPDATE ea_agent_version
SET config_json = JSON_SET(
        CAST(config_json AS JSON),
        '$.migration', JSON_MERGE_PATCH(
            COALESCE(JSON_EXTRACT(CAST(config_json AS JSON), '$.migration'), JSON_OBJECT()),
            JSON_OBJECT('accessibleExpensesV2', TRUE)
        )
    )
WHERE id = @latest_version_id
  AND @latest_is_v2 = 1
  AND JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.migration.accessibleExpensesV2')) IS NULL;

SET @latest_config := (
    SELECT config_json
    FROM ea_agent_version
    WHERE id = @latest_version_id
);
SET @latest_has_marker := IF(
    JSON_UNQUOTE(JSON_EXTRACT(@latest_config, '$.migration.accessibleExpensesV2')) = 'true',
    1,
    0
);
SET @needs_migration := IF(
    @latest_config IS NOT NULL AND COALESCE(@latest_has_marker, 0) = 0,
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
    CONCAT('v', @next_version_no, ' accessible expense query v2'),
    JSON_SET(
        CAST(@latest_config AS JSON),
        '$.migration', JSON_MERGE_PATCH(
            COALESCE(JSON_EXTRACT(CAST(@latest_config AS JSON), '$.migration'), JSON_OBJECT()),
            JSON_OBJECT('accessibleExpensesV2', TRUE)
        ),
        '$.inputSchema', JSON_OBJECT(
            'type', 'object',
            'properties', JSON_OBJECT(
                'query', JSON_OBJECT('type', 'string', 'description', '本次想让 Agent 处理的问题', 'default', '总结我当前待处理的报销事项'),
                'context', JSON_OBJECT('type', 'string', 'description', '补充上下文', 'default', '优先关注待审批和待付款单据'),
                'scope', JSON_OBJECT('type', 'string', 'enum', JSON_ARRAY('ASSIGNED_TO_ME', 'SUBMITTED_BY_ME'), 'default', 'ASSIGNED_TO_ME'),
                'keyword', JSON_OBJECT('type', JSON_ARRAY('string', 'null'), 'default', NULL),
                'status', JSON_OBJECT('type', JSON_ARRAY('string', 'null'), 'default', NULL),
                'startDate', JSON_OBJECT('type', JSON_ARRAY('string', 'null'), 'default', NULL),
                'endDate', JSON_OBJECT('type', JSON_ARRAY('string', 'null'), 'default', NULL),
                'minAmount', JSON_OBJECT('type', JSON_ARRAY('number', 'null'), 'default', NULL),
                'maxAmount', JSON_OBJECT('type', JSON_ARRAY('number', 'null'), 'default', NULL),
                'limit', JSON_OBJECT('type', 'integer', 'minimum', 1, 'maximum', 50, 'default', 10)
            )
        ),
        '$.tools', JSON_ARRAY(
            JSON_OBJECT(
                'toolCode', 'expense.query_my_expenses',
                'label', '受控查询：报销单与待办',
                'category', 'BUSINESS',
                'enabled', TRUE,
                'queryTemplateCode', 'expense.accessible_expenses.v2'
            )
        ),
        '$.workflow', JSON_OBJECT(
            'nodes', JSON_ARRAY(
                JSON_OBJECT(
                    'nodeKey', 'start',
                    'nodeType', 'start',
                    'label', '开始',
                    'config', JSON_OBJECT('canvasPosition', JSON_OBJECT('x', 100, 'y', 260))
                ),
                JSON_OBJECT(
                    'nodeKey', 'expense_query',
                    'nodeType', 'tool',
                    'label', '受控查询',
                    'config', JSON_OBJECT(
                        'toolCode', 'expense.query_my_expenses',
                        'queryTemplateCode', 'expense.accessible_expenses.v2',
                        'variableMapping', '{"scope":"{{input.scope}}","keyword":"{{input.keyword}}","status":"{{input.status}}","startDate":"{{input.startDate}}","endDate":"{{input.endDate}}","minAmount":"{{input.minAmount}}","maxAmount":"{{input.maxAmount}}","limit":"{{input.limit}}"}',
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
                    'config', JSON_OBJECT('canvasPosition', JSON_OBJECT('x', 850, 'y', 260))
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
    'accessible-query-v2-migration'
FROM ea_agent_definition d
WHERE d.id = @target_agent_id
  AND @needs_migration = 1;

SET @migration_version_id := CASE
    WHEN @needs_migration = 1 THEN LAST_INSERT_ID()
    WHEN @latest_has_marker = 1 THEN @latest_version_id
    ELSE NULL
END;

UPDATE ea_agent_tool_binding
SET enabled = 1,
    config_json = JSON_OBJECT(
        'toolCode', 'expense.query_my_expenses',
        'label', '受控查询：报销单与待办',
        'category', 'BUSINESS',
        'enabled', TRUE,
        'queryTemplateCode', 'expense.accessible_expenses.v2'
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
    d.latest_version_no,
    d.published_version_id,
    JSON_UNQUOTE(JSON_EXTRACT(v.config_json, '$.workflow.nodes[1].config.queryTemplateCode')) AS query_template_code,
    JSON_UNQUOTE(JSON_EXTRACT(v.config_json, '$.inputSchema.properties.scope.default')) AS default_scope,
    JSON_UNQUOTE(JSON_EXTRACT(v.config_json, '$.migration.accessibleExpensesV2')) AS migration_marker
FROM ea_agent_definition d
JOIN ea_agent_version v ON v.id = d.published_version_id
WHERE BINARY d.agent_code = BINARY @target_agent_code;
