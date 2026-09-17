package com.finex.auth.agent.workflow;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentToolBinding;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.mapper.ArchiveAgentDefinitionMapper;
import com.finex.auth.mapper.ArchiveAgentToolBindingMapper;
import com.finex.auth.mapper.ArchiveAgentVersionMapper;
import com.finex.auth.support.archiveagent.ArchiveAgentSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 工作流选择器。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowSelectorService {

    private static final String QUERY_INTENT = "query_expense";

    private final ArchiveAgentDefinitionMapper definitionMapper;
    private final ArchiveAgentVersionMapper versionMapper;
    private final ArchiveAgentToolBindingMapper toolBindingMapper;
    private final ObjectMapper objectMapper;
    private final ToolRuntimeService toolRuntimeService;

    public SelectedWorkflow select(Long userId, String intent, String message, String preferredProvider, String preferredModel) {
        if (!QUERY_INTENT.equals(intent)) {
            return null;
        }

        List<ArchiveAgentDefinition> candidates = definitionMapper.selectList(
                Wrappers.<ArchiveAgentDefinition>lambdaQuery()
                        .in(ArchiveAgentDefinition::getOwnerUserId, List.of(userId, 0L))
                        .eq(ArchiveAgentDefinition::getStatus, ArchiveAgentSupport.AGENT_STATUS_READY)
                        .isNotNull(ArchiveAgentDefinition::getPublishedVersionId)
                        .orderByDesc(ArchiveAgentDefinition::getOwnerUserId, ArchiveAgentDefinition::getUpdatedAt, ArchiveAgentDefinition::getId)
        );

        for (ArchiveAgentDefinition definition : candidates) {
            SelectedWorkflow selected = tryBuildSelection(definition, preferredProvider, preferredModel);
            if (selected != null) {
                return selected;
            }
        }

        return builtInExpenseWorkflow(userId, preferredProvider, preferredModel);
    }

    private SelectedWorkflow tryBuildSelection(ArchiveAgentDefinition definition, String preferredProvider, String preferredModel) {
        ArchiveAgentVersion version = versionMapper.selectById(definition.getPublishedVersionId());
        if (version == null || version.getPublished() == null || version.getPublished() != 1) {
            return null;
        }
        Map<String, Object> config = readMap(version.getConfigJson());
        try {
            return selectConfigured(definition, version, config, preferredProvider, preferredModel);
        } catch (IllegalArgumentException ex) {
            log.warn("跳过不可执行的已发布 Agent 工作流: agentId={}, reason={}", definition.getId(), ex.getMessage());
            return null;
        }
    }

    public SelectedWorkflow selectConfigured(
            ArchiveAgentDefinition definition,
            ArchiveAgentVersion version,
            Map<String, Object> config,
            String preferredProvider,
            String preferredModel
    ) {
        if (definition == null || version == null) {
            throw new IllegalArgumentException("Agent 或版本不存在");
        }
        List<Map<String, Object>> orderedNodes = WorkflowGraphPlan.from(readNestedMap(config, "workflow")).requireLinearNodes();
        List<Map<String, Object>> toolNodes = orderedNodes.stream()
                .filter(node -> "tool".equalsIgnoreCase(String.valueOf(node.get("nodeType"))))
                .toList();
        List<Map<String, Object>> llmNodes = orderedNodes.stream()
                .filter(node -> "llm".equalsIgnoreCase(String.valueOf(node.get("nodeType"))))
                .toList();
        if (toolNodes.size() != 1 || llmNodes.size() != 1) {
            throw new IllegalArgumentException("受控查询流程必须包含且只能包含一个工具节点和一个 LLM 节点");
        }
        Set<String> supportedTypes = Set.of("start", "tool", "llm", "end");
        for (Map<String, Object> node : orderedNodes) {
            String nodeType = String.valueOf(node.get("nodeType")).toLowerCase();
            if (!supportedTypes.contains(nodeType)) {
                throw new IllegalArgumentException("受控查询流程暂不支持节点类型: " + nodeType);
            }
        }
        int toolIndex = orderedNodes.indexOf(toolNodes.get(0));
        int llmIndex = orderedNodes.indexOf(llmNodes.get(0));
        if (toolIndex >= llmIndex) {
            throw new IllegalArgumentException("受控查询流程必须先执行工具节点，再执行 LLM 节点");
        }

        Map<String, Object> toolConfig = WorkflowGraphPlan.nodeConfig(toolNodes.get(0));
        String toolCode = String.valueOf(toolConfig.getOrDefault("toolCode", "")).trim();
        if (toolCode.isBlank() || !toolRuntimeService.supports(toolCode)) {
            throw new IllegalArgumentException("工具节点未配置已注册的受控工具: " + toolCode);
        }
        if (!isToolEnabled(config, definition.getId(), toolCode)) {
            throw new IllegalArgumentException("工具节点引用的工具未在 Agent 中启用: " + toolCode);
        }

        Map<String, Object> modelConfig = readNestedMap(config, "modelConfig");
        return SelectedWorkflow.builder()
                .agentId(definition.getId())
                .agentVersionId(version.getId())
                .ownerUserId(definition.getOwnerUserId())
                .agentName(definition.getAgentName())
                .toolCode(toolCode)
                .providerCode(resolveProvider(preferredProvider, modelConfig))
                .model(resolveModel(preferredModel, modelConfig))
                .config(config)
                .builtInFallback(false)
                .build();
    }

    public boolean containsControlledTool(Map<String, Object> config) {
        Map<String, Object> workflow = readNestedMap(config, "workflow");
        return readList(workflow.get("nodes")).stream()
                .filter(node -> "tool".equalsIgnoreCase(String.valueOf(node.get("nodeType"))))
                .map(WorkflowGraphPlan::nodeConfig)
                .map(nodeConfig -> String.valueOf(nodeConfig.getOrDefault("toolCode", "")))
                .anyMatch(toolRuntimeService::supports);
    }

    private boolean isToolEnabled(Map<String, Object> config, Long agentId, String toolCode) {
        List<Map<String, Object>> tools = readList(config.get("tools"));
        if (!tools.isEmpty()) {
            return tools.stream().anyMatch(tool -> toolCode.equalsIgnoreCase(String.valueOf(tool.get("toolCode")))
                    && !Boolean.FALSE.equals(tool.get("enabled")));
        }
        return Objects.equals(toolCode, resolveEnabledToolCode(agentId));
    }

    private String resolveEnabledToolCode(Long agentId) {
        List<ArchiveAgentToolBinding> bindings = toolBindingMapper.selectList(
                Wrappers.<ArchiveAgentToolBinding>lambdaQuery()
                        .eq(ArchiveAgentToolBinding::getAgentId, agentId)
                        .eq(ArchiveAgentToolBinding::getEnabled, 1)
        );
        return bindings.stream()
                .map(ArchiveAgentToolBinding::getToolCode)
                .filter(Objects::nonNull)
                .filter(toolRuntimeService::supports)
                .findFirst()
                .orElse(null);
    }

    private SelectedWorkflow builtInExpenseWorkflow(Long userId, String preferredProvider, String preferredModel) {
        Map<String, Object> modelConfig = new LinkedHashMap<>();
        modelConfig.put("provider", preferredProvider);
        modelConfig.put("model", preferredModel);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("modelConfig", modelConfig);
        config.put("workflow", Map.of(
                "nodes", List.of(
                        Map.of("nodeKey", "start_1", "nodeType", "start", "label", "开始"),
                        Map.of("nodeKey", "tool_1", "nodeType", "tool", "label", "受控查询报销事项", "config", Map.of(
                                "toolCode", ExpenseDocumentControlledQueryTool.TOOL_CODE,
                                "queryTemplateCode", ExpenseDocumentControlledQueryTool.ACCESSIBLE_QUERY_TEMPLATE_CODE,
                                "variableMapping", Map.of(
                                        "scope", "{{user.scope}}",
                                        "keyword", "{{user.keyword}}",
                                        "status", "{{user.status}}"
                                )
                        )),
                        Map.of("nodeKey", "llm_1", "nodeType", "llm", "label", "脱敏分析"),
                        Map.of("nodeKey", "end_1", "nodeType", "end", "label", "结束")
                ),
                "edges", List.of(
                        Map.of("source", "start_1", "target", "tool_1"),
                        Map.of("source", "tool_1", "target", "llm_1"),
                        Map.of("source", "llm_1", "target", "end_1")
                )
        ));

        return SelectedWorkflow.builder()
                .agentId(0L)
                .agentVersionId(0L)
                .ownerUserId(userId)
                .agentName("内置报销查询工作流")
                .toolCode(ExpenseDocumentControlledQueryTool.TOOL_CODE)
                .providerCode(resolveProvider(preferredProvider, modelConfig))
                .model(resolveModel(preferredModel, modelConfig))
                .config(config)
                .builtInFallback(true)
                .build();
    }

    private String resolveProvider(String preferredProvider, Map<String, Object> modelConfig) {
        if (preferredProvider != null && !preferredProvider.isBlank()) {
            return preferredProvider.trim();
        }
        Object value = modelConfig.get("provider");
        return value == null ? null : String.valueOf(value);
    }

    private String resolveModel(String preferredModel, Map<String, Object> modelConfig) {
        if (preferredModel != null && !preferredModel.isBlank()) {
            return preferredModel.trim();
        }
        Object value = modelConfig.get("model");
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> readNestedMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((mapKey, mapValue) -> result.put(String.valueOf(mapKey), mapValue));
            return result;
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> readMap(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private List<Map<String, Object>> readList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    ((Map<?, ?>) item).forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
                    return result;
                })
                .toList();
    }
}
