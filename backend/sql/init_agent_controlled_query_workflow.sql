-- FinWork 受控查询工作流初始化脚本
-- 说明：幂等创建全局默认报销单查询工作流，供 Workflow Selector 匹配。
-- 可重复执行，不会产生重复 Agent 或重复版本。

START TRANSACTION;

SET @agent_code = 'FINWORK_QUERY_EXPENSE';
SET @owner_user_id = 0;

INSERT INTO ea_agent_definition (
    agent_code,
    owner_user_id,
    agent_name,
    agent_description,
    icon_key,
    theme_key,
    cover_color,
    tags_json,
    status,
    latest_version_no
) VALUES (
    @agent_code,
    @owner_user_id,
    'FinWork 默认报销单受控查询',
    '全局默认工作流：FinWork 识别报销单查询意图后，通过受控工具查询当前用户自己的报销单，脱敏后交给模型分析，再本地还原输出。',
    'pixel-duck',
    'duck-lake',
    '#1f9f9f',
    JSON_ARRAY('FinWork', '受控查询', '报销单'),
    'READY',
    1
) ON DUPLICATE KEY UPDATE
    agent_name = VALUES(agent_name),
    agent_description = VALUES(agent_description),
    icon_key = VALUES(icon_key),
    theme_key = VALUES(theme_key),
    cover_color = VALUES(cover_color),
    tags_json = VALUES(tags_json),
    status = 'READY',
    latest_version_no = 1,
    updated_at = CURRENT_TIMESTAMP;

SELECT id INTO @agent_id
FROM ea_agent_definition
WHERE agent_code = @agent_code
LIMIT 1;

SET @config_json = JSON_OBJECT(
    'meta', JSON_OBJECT(
        'agentCode', @agent_code,
        'agentName', 'FinWork 默认报销单受控查询',
        'agentDescription', '全局默认报销单查询工作流',
        'tags', JSON_ARRAY('FinWork', '受控查询', '报销单')
    ),
    'promptConfig', JSON_OBJECT(
        'systemPrompt', '你是 FinEx 受控查询分析助手。只能基于脱敏后的结构化数据回答，并保留 evidenceRef。',
        'rolePrompt', '你只服务当前登录用户，不能越权查看其他人的数据。',
        'outputSchema', '{"answer":"","evidenceRefs":[]}',
        'guardrails', '不得生成输入中不存在的真实名称、单号、金额或证据引用。'
    ),
    'modelConfig', JSON_OBJECT(
        'provider', 'DEEPSEEK',
        'model', 'deepseek-chat',
        'temperature', 0.2,
        'maxTokens', 1200,
        'timeoutSeconds', 60,
        'credentialCode', ''
    ),
    'tools', JSON_ARRAY(
        JSON_OBJECT(
            'toolCode', 'expense.query_my_expenses',
            'label', '受控查询：报销单与待办',
            'category', 'BUSINESS',
            'enabled', true,
            'queryTemplateCode', 'expense.accessible_expenses.v2',
            'variableMapping', JSON_OBJECT(
                'scope', '{{input.scope}}',
                'keyword', '{{input.keyword}}',
                'status', '{{input.status}}',
                'startDate', '{{input.startDate}}',
                'endDate', '{{input.endDate}}',
                'minAmount', '{{input.minAmount}}',
                'maxAmount', '{{input.maxAmount}}',
                'limit', '{{input.limit}}'
            )
        )
    ),
    'workflow', JSON_OBJECT(
        'nodes', JSON_ARRAY(
            JSON_OBJECT('nodeKey', 'start_1', 'nodeType', 'start', 'label', '开始', 'config', JSON_OBJECT()),
            JSON_OBJECT('nodeKey', 'selector_1', 'nodeType', 'transform', 'label', '关键词映射', 'config', JSON_OBJECT('template', 'extract_query_variables')),
            JSON_OBJECT('nodeKey', 'tool_1', 'nodeType', 'tool', 'label', '受控查询', 'config', JSON_OBJECT(
                'toolCode', 'expense.query_my_expenses',
                'queryTemplateCode', 'expense.accessible_expenses.v2',
                'variableMapping', '{"scope":"{{input.scope}}","keyword":"{{input.keyword}}","status":"{{input.status}}","startDate":"{{input.startDate}}","endDate":"{{input.endDate}}","minAmount":"{{input.minAmount}}","maxAmount":"{{input.maxAmount}}","limit":"{{input.limit}}"}'
            )),
            JSON_OBJECT('nodeKey', 'mask_1', 'nodeType', 'transform', 'label', '本地脱敏', 'config', JSON_OBJECT('template', 'regex_desensitization')),
            JSON_OBJECT('nodeKey', 'llm_1', 'nodeType', 'llm', 'label', '脱敏数据分析', 'config', JSON_OBJECT('promptTemplate', '基于脱敏 JSON 输出 answer 和 evidenceRefs')),
            JSON_OBJECT('nodeKey', 'restore_1', 'nodeType', 'transform', 'label', '本地还原', 'config', JSON_OBJECT('template', 'restore_by_mapping_id')),
            JSON_OBJECT('nodeKey', 'end_1', 'nodeType', 'end', 'label', '结束', 'config', JSON_OBJECT())
        ),
        'edges', JSON_ARRAY(
            JSON_OBJECT('source', 'start_1', 'target', 'selector_1'),
            JSON_OBJECT('source', 'selector_1', 'target', 'tool_1'),
            JSON_OBJECT('source', 'tool_1', 'target', 'mask_1'),
            JSON_OBJECT('source', 'mask_1', 'target', 'llm_1'),
            JSON_OBJECT('source', 'llm_1', 'target', 'restore_1'),
            JSON_OBJECT('source', 'restore_1', 'target', 'end_1')
        )
    ),
    'triggers', JSON_ARRAY(
        JSON_OBJECT('triggerType', 'MANUAL', 'enabled', true)
    ),
    'inputSchema', JSON_OBJECT(
        'type', 'object',
        'properties', JSON_OBJECT(
            'query', JSON_OBJECT('type', 'string', 'description', '用户查询原文'),
            'keyword', JSON_OBJECT('type', 'string', 'description', '关键词'),
            'status', JSON_OBJECT('type', 'string', 'description', '报销单状态')
        )
    )
);

INSERT INTO ea_agent_version (
    agent_id,
    version_no,
    version_label,
    config_json,
    published,
    created_by_user_id,
    created_by_name
) VALUES (
    @agent_id,
    1,
    'v1 controlled query published',
    CAST(@config_json AS CHAR),
    1,
    @owner_user_id,
    'system'
) ON DUPLICATE KEY UPDATE
    version_label = VALUES(version_label),
    config_json = VALUES(config_json),
    published = 1,
    created_by_name = VALUES(created_by_name);

SELECT id INTO @version_id
FROM ea_agent_version
WHERE agent_id = @agent_id AND version_no = 1
LIMIT 1;

UPDATE ea_agent_definition
SET published_version_id = @version_id,
    status = 'READY',
    latest_version_no = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = @agent_id;

DELETE FROM ea_agent_tool_binding
WHERE agent_id = @agent_id
  AND tool_code = 'expense.query_my_expenses';

INSERT INTO ea_agent_tool_binding (
    agent_id,
    tool_code,
    enabled,
    credential_ref_code,
    config_json
) VALUES (
    @agent_id,
    'expense.query_my_expenses',
    1,
    NULL,
    JSON_OBJECT(
        'toolCode', 'expense.query_my_expenses',
        'enabled', true,
        'queryTemplateCode', 'expense.accessible_expenses.v2',
        'variableMapping', JSON_OBJECT(
            'scope', '{{input.scope}}',
            'keyword', '{{input.keyword}}',
            'status', '{{input.status}}',
            'startDate', '{{input.startDate}}',
            'endDate', '{{input.endDate}}',
            'minAmount', '{{input.minAmount}}',
            'maxAmount', '{{input.maxAmount}}',
            'limit', '{{input.limit}}'
        )
    )
);

DELETE FROM ea_agent_trigger
WHERE agent_id = @agent_id
  AND trigger_type = 'MANUAL';

INSERT INTO ea_agent_trigger (
    agent_id,
    trigger_type,
    enabled,
    config_json
) VALUES (
    @agent_id,
    'MANUAL',
    1,
    JSON_OBJECT('triggerType', 'MANUAL', 'enabled', true)
);

COMMIT;
