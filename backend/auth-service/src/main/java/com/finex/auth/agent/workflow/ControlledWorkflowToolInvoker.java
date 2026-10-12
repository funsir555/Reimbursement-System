package com.finex.auth.agent.workflow;

import com.finex.auth.service.queryauth.QueryAuthorization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Parameter validation and business-port dispatch for one saved controlled tool node. */
@Component
@RequiredArgsConstructor
public class ControlledWorkflowToolInvoker {
    private final QueryParameterExtractor queryParameterExtractor;
    private final QueryTemplateVariableMapper queryTemplateVariableMapper;
    private final ControlledToolParameterMapper parameterMapper;
    private final ToolRuntimeService toolRuntimeService;

    public ControlledToolResult invoke(String nodeKey, Map<String, Object> config,
                                       Map<String, Object> input, Map<String, ControlledToolResult> prior,
                                       String message, Long userId, String sessionId, String traceId,
                                       QueryAuthorization authorization) {
        String code = String.valueOf(config.get("toolCode")).trim();
        ControlledQueryCatalog.Definition definition = ControlledQueryCatalog.require(code);
        boolean expense = ExpenseDocumentControlledQueryTool.TOOL_CODE.equals(code);
        boolean close = FinanceUnclosedCompanyTool.TOOL_CODE.equals(code);
        Map<String, Object> parameters = expense || close ? Map.of()
                : parameterMapper.map(nodeKey, code, config.get("variableMapping"), input, prior);
        QueryTemplateVariables variables = expense ? expenseVariables(nodeKey, config, input, message) : null;
        String effectiveMessage = message;
        if (close) {
            Object configured = input.get("parameters");
            Object nodeInput = configured instanceof Map<?, ?> all ? all.get(nodeKey) : null;
            if (!(nodeInput instanceof Map<?, ?> periodInput)
                    || periodInput.get("year") == null || periodInput.get("period") == null)
                throw new IllegalArgumentException("请明确未结账查询的会计年度和期间");
            effectiveMessage = periodInput.get("year") + "年" + periodInput.get("period") + "月";
        }
        ControlledToolResult result = toolRuntimeService.execute(code, ToolExecutionRequest.builder()
                .userId(userId).sessionId(sessionId).traceId(traceId)
                .queryTemplateCode(expense ? String.valueOf(config.get("queryTemplateCode")) : definition.template())
                .authorization(authorization).variables(variables)
                .parameters(parameters).userMessage(effectiveMessage).build());
        if (!result.isSuccess()) throw new IllegalStateException(result.getErrorMessage());
        return result;
    }

    private QueryTemplateVariables expenseVariables(String nodeKey, Map<String, Object> config,
                                                     Map<String, Object> input, String message) {
        QueryTemplateVariables extracted = "执行已配置的定时受控查询".equals(message)
                ? QueryTemplateVariables.builder()
                        .scope(QueryTemplateVariables.SCOPE_SUBMITTED_BY_ME).limit(10).build()
                : queryParameterExtractor.extract(message);
        Map<String, Object> merged = new LinkedHashMap<>(input);
        Object all = input.get("parameters");
        Map<?, ?> node = all instanceof Map<?, ?> map && map.get(nodeKey) instanceof Map<?, ?> selected
                ? selected : Map.of();
        if (!Set.of("scope", "keyword", "status").containsAll(node.keySet()))
            throw new IllegalArgumentException("报销工具参数不在固定模板中");
        node.forEach((key, value) -> merged.put(String.valueOf(key), value));
        QueryTemplateVariables variables = queryTemplateVariableMapper.map(extracted, config, merged);
        if (node.containsKey("scope")) {
            variables.setScope(switch (String.valueOf(node.get("scope"))) {
                case "MY", "SUBMITTED_BY_ME" -> QueryTemplateVariables.SCOPE_SUBMITTED_BY_ME;
                case "ASSIGNED", "ASSIGNED_TO_ME" -> QueryTemplateVariables.SCOPE_ASSIGNED_TO_ME;
                case "VISIBLE" -> QueryTemplateVariables.SCOPE_VISIBLE;
                default -> throw new IllegalArgumentException("报销查询范围参数无效");
            });
        }
        if (node.containsKey("keyword")) {
            String keyword = String.valueOf(node.get("keyword")).trim();
            if (keyword.length() > 200) throw new IllegalArgumentException("报销关键词过长");
            variables.setNormalizedKeyword(keyword.isBlank() ? null : keyword);
            variables.setRawKeyword(keyword.isBlank() ? null : keyword);
        }
        if (node.containsKey("status")) {
            String status = String.valueOf(node.get("status")).trim();
            if (status.length() > 64) throw new IllegalArgumentException("单据状态参数无效");
            variables.setStatus(status.isBlank() ? null : status);
        }
        return variables;
    }
}
