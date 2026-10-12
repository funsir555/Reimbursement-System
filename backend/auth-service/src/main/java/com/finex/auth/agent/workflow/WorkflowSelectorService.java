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
import com.finex.auth.service.digitalemployee.DigitalEmployeeService;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private final ArchiveAgentDefinitionMapper definitionMapper;
    private final ArchiveAgentVersionMapper versionMapper;
    private final ArchiveAgentToolBindingMapper toolBindingMapper;
    private final ObjectMapper objectMapper;
    private final ToolRuntimeService toolRuntimeService;
    private final DigitalEmployeeService digitalEmployeeService;
    private final QueryAuthorizationService authorizationService;
    private final JdbcTemplate jdbc;

    public SelectedWorkflow select(Long userId, String intent, String message, String preferredProvider, String preferredModel) {
        return select(userId, intent, message, preferredProvider, preferredModel, null, null);
    }

    public SelectedWorkflow select(Long userId, String intent, String message, String preferredProvider,
                                   String preferredModel, Long preferredEmployeeId, Long preferredBindingId) {
        String requiredPermission = switch (intent) {
            case "query_expense" -> QueryAuthorizationService.EXPENSE_QUERY;
            case "query_finance_close" -> QueryAuthorizationService.FINANCE_CLOSE_QUERY;
            default -> null;
        };
        if (requiredPermission == null) return null;
        List<SelectedWorkflow> matches = new java.util.ArrayList<>();
        for (SelectedWorkflow candidate : authorizedCandidates(userId, preferredEmployeeId, preferredBindingId)) {
            if (requiredPermission.equals(candidate.getQueryPermissionCode())) matches.add(candidate);
        }
        if (matches.size() > 1) throw new AmbiguousWorkflowException();
        return matches.isEmpty() ? null : matches.get(0);
    }

    /** Only reviewed, published, assigned and currently authorized fixed versions. */
    public List<SelectedWorkflow> authorizedCandidates(Long userId, Long preferredEmployeeId,
                                                        Long preferredBindingId) {
        List<SelectedWorkflow> matches = new java.util.ArrayList<>();
        for (Map<String, Object> candidate : digitalEmployeeService.availableFor(userId)) {
            String requiredPermission = (String) candidate.get("permission_code");
            if (!ControlledQueryCatalog.TOOLS.stream().anyMatch(item -> item.permission().equals(requiredPermission))) continue;
            Long employeeId = ((Number) candidate.get("id")).longValue();
            Long bindingId = ((Number) candidate.get("binding_id")).longValue();
            if (preferredEmployeeId != null && !preferredEmployeeId.equals(employeeId)) continue;
            if (preferredBindingId != null && !preferredBindingId.equals(bindingId)) continue;
            Long responsibleId = ((Number) candidate.get("responsible_user_id")).longValue();
            try {
                List<String> requiredCodes = jdbc.queryForList("""
                        SELECT permission_code FROM ea_workflow_share_capability
                        WHERE share_id=(SELECT share_id FROM ea_digital_employee_workflow WHERE id=?)
                        ORDER BY permission_code
                        """, String.class, bindingId);
                if (requiredCodes.isEmpty()) continue;
                for (String code : requiredCodes) {
                    List<String> limit = digitalEmployeeService.companyLimit(employeeId, code);
                    authorizationService.resolve(userId, responsibleId, code,
                            limit.isEmpty() ? null : Set.copyOf(limit));
                }
                ArchiveAgentDefinition definition = definitionMapper.selectById(
                        ((Number) candidate.get("agent_id")).longValue());
                ArchiveAgentVersion version = versionMapper.selectById(
                        ((Number) candidate.get("agent_version_id")).longValue());
                if (definition == null || version == null || version.getPublished() == null
                        || version.getPublished() != 1
                        || !ArchiveAgentSupport.AGENT_STATUS_READY.equals(definition.getStatus())
                        || !definition.getId().equals(version.getAgentId())) continue;
                // Controlled queries execute the reviewed fixed version, including its model choice.
                SelectedWorkflow selected = selectConfigured(definition, version, readMap(version.getConfigJson()),
                        null, null);
                if (!Set.copyOf(requiredCodes).equals(Set.copyOf(selected.getQueryPermissionCodes()))) continue;
                selected.setAgentDescription(definition.getAgentDescription());
                selected.setDigitalEmployeeId(employeeId);
                selected.setDigitalEmployeeName((String) candidate.get("name"));
                selected.setResponsibleUserId(responsibleId);
                selected.setQueryPermissionCode(requiredPermission);
                selected.setQueryPermissionCodes(List.copyOf(requiredCodes));
                selected.setWorkflowBindingId(bindingId);
                matches.add(selected);
            } catch (SecurityException | IllegalArgumentException ignored) {
                // A revoked permission, invalid scope or invalid canvas removes this candidate.
            }
        }
        return List.copyOf(matches);
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
        List<Map<String, Object>> orderedNodes = WorkflowGraphPlan.from(readNestedMap(config, "workflow"))
                .requireControlledQueryPlan().orderedNodes();
        List<Map<String, Object>> toolNodes = orderedNodes.stream()
                .filter(node -> "tool".equalsIgnoreCase(String.valueOf(node.get("nodeType"))))
                .toList();
        List<Map<String, Object>> llmNodes = orderedNodes.stream()
                .filter(node -> "llm".equalsIgnoreCase(String.valueOf(node.get("nodeType"))))
                .toList();
        if (toolNodes.isEmpty() || toolNodes.size() > 9 || llmNodes.size() != 1) {
            throw new IllegalArgumentException("受控查询流程需要 1 至 9 个工具节点和一个 LLM 节点");
        }
        Set<String> supportedTypes = Set.of("start", "tool", "llm", "end");
        for (Map<String, Object> node : orderedNodes) {
            String nodeType = String.valueOf(node.get("nodeType")).toLowerCase();
            if (!supportedTypes.contains(nodeType)) {
                throw new IllegalArgumentException("受控查询流程暂不支持节点类型: " + nodeType);
            }
        }
        int llmIndex = orderedNodes.indexOf(llmNodes.get(0));
        if (toolNodes.stream().anyMatch(node -> orderedNodes.indexOf(node) >= llmIndex)) {
            throw new IllegalArgumentException("受控查询流程必须先执行全部工具节点，再执行 LLM 节点");
        }
        List<String> toolCodes = toolNodes.stream().map(node -> String.valueOf(
                WorkflowGraphPlan.nodeConfig(node).getOrDefault("toolCode", "")).trim()).toList();
        for (int index = 0; index < toolCodes.size(); index++) {
            String code = toolCodes.get(index);
            if (!toolRuntimeService.supports(code) || !isToolEnabled(config, definition.getId(), code))
                throw new IllegalArgumentException("工具节点未绑定已注册的受控工具: " + code);
            String template = String.valueOf(WorkflowGraphPlan.nodeConfig(toolNodes.get(index))
                    .getOrDefault("queryTemplateCode", ""));
            if (!ControlledQueryCatalog.require(code).template().equals(template)
                    && !(ExpenseDocumentControlledQueryTool.TOOL_CODE.equals(code)
                    && ExpenseDocumentControlledQueryTool.QUERY_TEMPLATE_CODE.equals(template)))
                throw new IllegalArgumentException("工具节点查询模板不匹配: " + code);
        }
        List<String> permissions = toolCodes.stream().map(code -> ControlledQueryCatalog.require(code).permission())
                .distinct().toList();

        Map<String, Object> modelConfig = readNestedMap(config, "modelConfig");
        return SelectedWorkflow.builder()
                .agentId(definition.getId())
                .agentVersionId(version.getId())
                .ownerUserId(definition.getOwnerUserId())
                .agentName(definition.getAgentName())
                .agentDescription(definition.getAgentDescription())
                .toolCode(toolCodes.get(0))
                .toolCodes(toolCodes)
                .queryPermissionCode(permissions.get(0))
                .queryPermissionCodes(permissions)
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

    public static class AmbiguousWorkflowException extends RuntimeException {
        public AmbiguousWorkflowException() { super("多个数字员工可处理此查询，请先选择数字员工或工作流"); }
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
