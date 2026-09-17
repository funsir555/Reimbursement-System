package com.finex.auth.agent.workflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.context.TraceContext;
import com.finex.auth.agent.desensitization.DesensitizationService;
import com.finex.auth.agent.desensitization.DesensitizedData;
import com.finex.auth.agent.model.ChatModelProvider;
import com.finex.auth.agent.model.ModelCapability;
import com.finex.auth.agent.model.ModelRoutingService;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentRun;
import com.finex.auth.entity.ArchiveAgentRunStep;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.mapper.ArchiveAgentDefinitionMapper;
import com.finex.auth.mapper.ArchiveAgentRunMapper;
import com.finex.auth.mapper.ArchiveAgentRunStepMapper;
import com.finex.auth.support.archiveagent.ArchiveAgentSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shared controlled-query runtime used by FinWork and Agent workbench runs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentWorkflowRuntimeService {

    private static final Map<String, String> CLOUD_SENSITIVE_FIELDS = Map.ofEntries(
            Map.entry("keyword", "KEYWORD"),
            Map.entry("normalizedKeyword", "KEYWORD"),
            Map.entry("documentId", "RECORD"),
            Map.entry("documentCode", "DOC"),
            Map.entry("title", "DETAIL"),
            Map.entry("reason", "DETAIL"),
            Map.entry("amount", "AMOUNT")
    );

    private final WorkflowSelectorService workflowSelectorService;
    private final QueryParameterExtractor queryParameterExtractor;
    private final QueryTemplateVariableMapper queryTemplateVariableMapper;
    private final ToolRuntimeService toolRuntimeService;
    private final DesensitizationService desensitizationService;
    private final ModelRoutingService modelRoutingService;
    private final ArchiveAgentRunMapper runMapper;
    private final ArchiveAgentRunStepMapper stepMapper;
    private final ArchiveAgentDefinitionMapper definitionMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public WorkflowExecutionResult executeQueryWorkflow(
            String sessionId,
            Long userId,
            String userMessage,
            String intent,
            String providerCode,
            String model
    ) {
        String traceId = ensureTraceId();
        SelectedWorkflow workflow = workflowSelectorService.select(userId, intent, userMessage, providerCode, model);
        if (workflow == null) {
            return WorkflowExecutionResult.builder()
                    .success(false)
                    .intent(intent)
                    .traceId(traceId)
                    .errorMessage("未匹配到可执行工作流")
                    .content("未匹配到可执行工作流，请在 Agent 工作台发布查询工作流后重试。")
                    .evidenceRefs(List.of())
                    .build();
        }

        ArchiveAgentRun run = createRun(workflow, userId, sessionId, userMessage, traceId);
        return executeSelectedWorkflow(
                run,
                workflow,
                normalizeSessionId(sessionId, run),
                userId,
                userMessage,
                intent,
                Map.of("query", userMessage == null ? "" : userMessage),
                true
        );
    }

    @Transactional
    public WorkflowExecutionResult executeWorkbenchWorkflow(
            ArchiveAgentRun run,
            ArchiveAgentDefinition definition,
            ArchiveAgentVersion version,
            Map<String, Object> config
    ) {
        String traceId = ensureTraceId();
        Map<String, Object> inputPayload = readMap(run == null ? null : run.getInputJson());
        String userMessage = resolveUserMessage(inputPayload);
        String sessionId = normalizeSessionId(text(inputPayload.get("sessionId")), run);
        try {
            SelectedWorkflow workflow = workflowSelectorService.selectConfigured(definition, version, config, null, null);
            return executeSelectedWorkflow(
                    run,
                    workflow,
                    sessionId,
                    run != null && run.getOwnerUserId() != null ? run.getOwnerUserId() : definition.getOwnerUserId(),
                    userMessage,
                    "query_expense",
                    inputPayload,
                    false
            );
        } catch (Exception ex) {
            log.error("Agent 工作台受控查询执行失败: runId={}, traceId={}", run == null ? null : run.getId(), traceId, ex);
            if (run != null) {
                if (run.getStartedAt() == null) {
                    markRunRunning(run);
                }
                markRunFailed(run, ex.getMessage());
            }
            return failedResult(run, "query_expense", traceId, ex);
        }
    }

    public boolean isControlledQueryWorkflow(Map<String, Object> config) {
        return workflowSelectorService.containsControlledTool(config);
    }

    private WorkflowExecutionResult executeSelectedWorkflow(
            ArchiveAgentRun run,
            SelectedWorkflow workflow,
            String sessionId,
            Long userId,
            String userMessage,
            String intent,
            Map<String, Object> inputPayload,
            boolean includeSelectorStep
    ) {
        String traceId = ensureTraceId();
        try {
            markRunRunning(run);
            List<Map<String, Object>> orderedNodes = controlledNodes(workflow);
            Map<String, Object> startNode = nodeOfType(orderedNodes, "start");
            Map<String, Object> toolNode = nodeOfType(orderedNodes, "tool");
            Map<String, Object> llmNode = nodeOfType(orderedNodes, "llm");
            Map<String, Object> endNode = nodeOfType(orderedNodes, "end");
            Map<String, Object> toolConfig = WorkflowGraphPlan.nodeConfig(toolNode);
            Map<String, Object> llmConfig = WorkflowGraphPlan.nodeConfig(llmNode);

            QueryTemplateVariables extracted = queryParameterExtractor.extract(userMessage);
            QueryTemplateVariables variables = queryTemplateVariableMapper.map(extracted, toolConfig, inputPayload);
            int stepNo = 1;
            if (includeSelectorStep) {
                saveStep(run.getId(), stepNo++, "selector_1", "selector", "工作流选择",
                        values("message", userMessage, "intent", intent),
                        values("agentName", workflow.getAgentName(), "builtInFallback", workflow.isBuiltInFallback()), null);
            }

            saveStep(run.getId(), stepNo++, nodeKey(startNode), "start", nodeLabel(startNode),
                    inputPayload,
                    values("message", "流程开始", "variables", variables.toMap()), null);

            String queryTemplateCode = requiredText(toolConfig.get("queryTemplateCode"), "工具节点缺少查询模板编码");
            ControlledToolResult toolResult = toolRuntimeService.execute(workflow.getToolCode(), ToolExecutionRequest.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .traceId(traceId)
                    .queryTemplateCode(queryTemplateCode)
                    .variables(variables)
                    .build());
            if (!toolResult.isSuccess()) {
                throw new IllegalStateException(toolResult.getErrorMessage());
            }
            saveStep(run.getId(), stepNo++, nodeKey(toolNode), "tool", nodeLabel(toolNode),
                    values("toolCode", workflow.getToolCode(), "queryTemplateCode", queryTemplateCode, "variables", variables.toMap()),
                    values("summary", toolResult.getSummary(), "records", safeRecords(toolResult), "evidenceRefs", safeEvidenceRefs(toolResult)), null);

            Map<String, Object> contextPayload = buildContextPayload(traceId, userMessage, inputPayload, variables, toolResult);
            String contextJson = writeJson(contextPayload);
            DesensitizedData desensitized = desensitizationService.desensitizeJsonFields(
                    sessionId,
                    contextJson,
                    CLOUD_SENSITIVE_FIELDS,
                    Set.of("KEYWORD")
            );
            if (!desensitized.isSuccess() || desensitized.getMappingId() == null || desensitized.getMappingId().isBlank()) {
                throw new IllegalStateException("脱敏失败: " + desensitized.getErrorMessage());
            }
            saveStep(run.getId(), stepNo++, nodeKey(toolNode) + "__mask", "mask", "本地脱敏",
                    values("payloadLength", contextJson.length()),
                    values("mappingId", desensitized.getMappingId(), "maskedPayload", desensitized.getMaskedText()), null);

            ModelAnalysisResult analysis = callAnalysisModel(workflow, llmConfig, desensitized.getMaskedText());
            saveStep(run.getId(), stepNo++, nodeKey(llmNode), "llm", nodeLabel(llmNode),
                    values("provider", workflow.getProviderCode(), "model", workflow.getModel(), "mappingId", desensitized.getMappingId()),
                    values("maskedAnalysis", analysis.content(), "responseModel", analysis.model()), null);

            String restoredAnalysis = desensitizationService.restore(desensitized.getMappingId(), analysis.content());
            List<String> evidenceRefs = validatedEvidenceRefs(restoredAnalysis, toolResult);
            List<String> entityRefs = entityRefs(toolResult);
            String finalContent = buildFinalContent(restoredAnalysis, toolResult, evidenceRefs);
            saveStep(run.getId(), stepNo++, nodeKey(llmNode) + "__restore", "restore", "本地还原与引用校验",
                    values("mappingId", desensitized.getMappingId(), "entityRefs", entityRefs, "evidenceRefs", evidenceRefs),
                    values("content", finalContent), null);

            Map<String, Object> finalOutput = values(
                    "message", "流程执行完成",
                    "content", finalContent,
                    "mappingId", desensitized.getMappingId(),
                    "entityRefs", entityRefs,
                    "evidenceRefs", evidenceRefs
            );
            saveStep(run.getId(), stepNo, nodeKey(endNode), "end", nodeLabel(endNode),
                    values("previousNode", nodeKey(llmNode)), finalOutput, null);

            markRunSuccess(run, finalContent, traceId, desensitized.getMappingId(), entityRefs, evidenceRefs);
            return WorkflowExecutionResult.builder()
                    .success(true)
                    .content(finalContent)
                    .intent(intent)
                    .runId(run.getId())
                    .traceId(traceId)
                    .mappingId(desensitized.getMappingId())
                    .evidenceRefs(evidenceRefs)
                    .build();
        } catch (Exception ex) {
            log.error("Agent 工作流执行失败: runId={}, traceId={}", run.getId(), traceId, ex);
            markRunFailed(run, ex.getMessage());
            return failedResult(run, intent, traceId, ex);
        }
    }

    private List<Map<String, Object>> controlledNodes(SelectedWorkflow workflow) {
        Map<String, Object> workflowConfig = readNestedMap(workflow.getConfig(), "workflow");
        return WorkflowGraphPlan.from(workflowConfig).requireLinearNodes();
    }

    private Map<String, Object> nodeOfType(List<Map<String, Object>> nodes, String nodeType) {
        return nodes.stream()
                .filter(node -> nodeType.equalsIgnoreCase(text(node.get("nodeType"))))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("工作流缺少 " + nodeType + " 节点"));
    }

    private ModelAnalysisResult callAnalysisModel(
            SelectedWorkflow workflow,
            Map<String, Object> llmConfig,
            String maskedPayload
    ) {
        if ("MOCK".equalsIgnoreCase(text(workflow.getProviderCode()))) {
            throw new IllegalArgumentException("受控查询不能使用 Mock 模型，请选择 DeepSeek 或本地模型后重新发布");
        }
        Map<String, Object> promptConfig = readNestedMap(workflow.getConfig(), "promptConfig");
        String configuredPrompt = text(llmConfig.get("promptTemplate"));
        String globalPrompt = text(promptConfig.get("systemPrompt"));
        String systemPrompt = String.join("\n", List.of(
                globalPrompt,
                configuredPrompt,
                "你是 FinEx 受控查询分析助手。你只能基于输入的脱敏 JSON 作答。",
                "输出必须是 JSON，格式为 {\"answer\":\"简洁中文结论\",\"evidenceRefs\":[\"...\"]}。",
                "只能引用输入中已有的 evidenceRef，不要生成真实姓名、单号、金额或新的占位符。"
        ).stream().filter(item -> item != null && !item.isBlank()).toList());

        ChatModelProvider.ChatRequest request = ChatModelProvider.ChatRequest.builder()
                .systemPrompt(systemPrompt)
                .messages(List.of(ChatModelProvider.ChatMessage.builder()
                        .role("user")
                        .content(maskedPayload)
                        .build()))
                .temperature(0.2)
                .maxTokens(1200)
                .extraParams(buildModelExtraParams(workflow.getModel()))
                .build();

        ChatModelProvider.ChatResponse response = modelRoutingService.chatWithPreferredProvider(
                workflow.getProviderCode(),
                ModelCapability.ModelUseCase.CHAT_QUERY,
                ModelRoutingService.DataSensitivity.LOW,
                request
        );
        if (response == null || !response.isSuccess() || response.getContent() == null || response.getContent().isBlank()) {
            String error = response == null ? "模型未返回响应" : response.getErrorMessage();
            throw new IllegalStateException("模型分析失败: " + error);
        }
        return new ModelAnalysisResult(response.getContent(), response.getModel());
    }

    private Map<String, Object> buildContextPayload(
            String traceId,
            String userMessage,
            Map<String, Object> inputPayload,
            QueryTemplateVariables variables,
            ControlledToolResult toolResult
    ) {
        return values(
                "traceId", traceId,
                "question", userMessage,
                "context", inputPayload == null ? null : text(inputPayload.get("context")),
                "variables", variables.toMap(),
                "records", safeRecords(toolResult),
                "evidenceRefs", safeEvidenceRefs(toolResult)
        );
    }

    private String buildFinalContent(
            String restoredAnalysis,
            ControlledToolResult toolResult,
            List<String> evidenceRefs
    ) {
        String answer = extractAnswer(restoredAnalysis);
        StringBuilder builder = new StringBuilder();
        builder.append(answer == null || answer.isBlank() ? buildLocalSummary(toolResult) : answer.trim());
        if (!safeRecords(toolResult).isEmpty()) {
            builder.append("\n\n查询结果：");
            for (Map<String, Object> record : safeRecords(toolResult)) {
                builder.append("\n- ")
                        .append(nullToBlank(record.get("documentCode")))
                        .append("，")
                        .append(nullToBlank(record.get("title")))
                        .append("，金额：")
                        .append(nullToBlank(record.get("amount")))
                        .append("，状态：")
                        .append(nullToBlank(record.get("statusLabel")))
                        .append("，证据：")
                        .append(nullToBlank(record.get("evidenceRef")));
            }
        }
        if (!evidenceRefs.isEmpty()) {
            builder.append("\n\n证据引用：").append(String.join("、", evidenceRefs));
        }
        return builder.toString();
    }

    private List<String> validatedEvidenceRefs(String restoredAnalysis, ControlledToolResult toolResult) {
        Set<String> allowed = new LinkedHashSet<>(safeEvidenceRefs(toolResult));
        List<String> requested = extractEvidenceRefs(restoredAnalysis);
        List<String> validated = requested.stream().filter(allowed::contains).distinct().toList();
        return validated.isEmpty() ? List.copyOf(allowed) : validated;
    }

    private List<String> entityRefs(ControlledToolResult toolResult) {
        return safeRecords(toolResult).stream()
                .map(record -> record.get("entityRef"))
                .filter(value -> value != null && !String.valueOf(value).isBlank())
                .map(String::valueOf)
                .distinct()
                .toList();
    }

    private String buildLocalSummary(ControlledToolResult toolResult) {
        int count = safeRecords(toolResult).size();
        return count == 0 ? "未查询到符合条件的报销单。" : "已查询到 " + count + " 条符合条件的报销单。";
    }

    private String extractAnswer(String raw) {
        Map<String, Object> parsed = readPossiblyWrappedJson(raw);
        Object answer = parsed.get("answer");
        return answer == null ? raw : String.valueOf(answer);
    }

    private List<String> extractEvidenceRefs(String raw) {
        Object value = readPossiblyWrappedJson(raw).get("evidenceRefs");
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> refs = new ArrayList<>();
        for (Object item : list) {
            if (item != null && !String.valueOf(item).isBlank()) {
                refs.add(String.valueOf(item));
            }
        }
        return refs;
    }

    private Map<String, Object> readPossiblyWrappedJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashMap<>();
        }
        String normalized = raw.trim();
        int start = normalized.indexOf('{');
        int end = normalized.lastIndexOf('}');
        if (start >= 0 && end > start) {
            normalized = normalized.substring(start, end + 1);
        }
        try {
            return objectMapper.readValue(normalized, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private ArchiveAgentRun createRun(SelectedWorkflow workflow, Long userId, String sessionId, String userMessage, String traceId) {
        ArchiveAgentRun run = new ArchiveAgentRun();
        run.setRunNo(ArchiveAgentSupport.buildRunNo());
        run.setAgentId(workflow.getAgentId() == null ? 0L : workflow.getAgentId());
        run.setAgentVersionId(workflow.getAgentVersionId() == null ? 0L : workflow.getAgentVersionId());
        run.setOwnerUserId(userId);
        run.setTriggerType(ArchiveAgentSupport.TRIGGER_TYPE_MANUAL);
        run.setTriggerSource("FINWORK");
        run.setStatus(ArchiveAgentSupport.RUN_STATUS_PENDING);
        run.setSummary("FinWork 查询工作流已提交");
        run.setInputJson(writeJson(values(
                "sessionId", sessionId,
                "query", userMessage,
                "traceId", traceId,
                "workflow", workflow.getAgentName()
        )));
        runMapper.insert(run);
        return run;
    }

    private void markRunRunning(ArchiveAgentRun run) {
        run.setStatus(ArchiveAgentSupport.RUN_STATUS_RUNNING);
        run.setStartedAt(LocalDateTime.now());
        run.setErrorMessage(null);
        run.setSummary("Agent 工作流执行中");
        runMapper.updateById(run);
    }

    private void markRunSuccess(
            ArchiveAgentRun run,
            String finalContent,
            String traceId,
            String mappingId,
            List<String> entityRefs,
            List<String> evidenceRefs
    ) {
        LocalDateTime finishedAt = LocalDateTime.now();
        run.setStatus(ArchiveAgentSupport.RUN_STATUS_SUCCESS);
        run.setSummary("Agent 受控查询工作流执行成功");
        run.setOutputJson(writeJson(values(
                "content", finalContent,
                "traceId", traceId,
                "mappingId", mappingId,
                "entityRefs", entityRefs,
                "evidenceRefs", evidenceRefs
        )));
        run.setFinishedAt(finishedAt);
        run.setDurationMs(resolveDuration(run.getStartedAt(), finishedAt));
        runMapper.updateById(run);
        updateAgentLastRun(run);
    }

    private void markRunFailed(ArchiveAgentRun run, String message) {
        LocalDateTime finishedAt = LocalDateTime.now();
        run.setStatus(ArchiveAgentSupport.RUN_STATUS_FAILED);
        run.setErrorMessage(message);
        run.setSummary("Agent 工作流执行失败");
        run.setFinishedAt(finishedAt);
        run.setDurationMs(resolveDuration(run.getStartedAt(), finishedAt));
        runMapper.updateById(run);
        updateAgentLastRun(run);
    }

    private WorkflowExecutionResult failedResult(ArchiveAgentRun run, String intent, String traceId, Exception ex) {
        return WorkflowExecutionResult.builder()
                .success(false)
                .content("抱歉，工作流执行失败：" + ex.getMessage())
                .errorMessage(ex.getMessage())
                .intent(intent)
                .runId(run == null ? null : run.getId())
                .traceId(traceId)
                .evidenceRefs(List.of())
                .build();
    }

    private void saveStep(
            Long runId,
            int stepNo,
            String nodeKey,
            String nodeType,
            String label,
            Map<String, Object> input,
            Map<String, Object> output,
            String errorMessage
    ) {
        LocalDateTime startedAt = LocalDateTime.now();
        ArchiveAgentRunStep step = new ArchiveAgentRunStep();
        step.setRunId(runId);
        step.setStepNo(stepNo);
        step.setNodeKey(nodeKey);
        step.setNodeType(nodeType);
        step.setNodeLabel(label);
        step.setStatus(errorMessage == null ? ArchiveAgentSupport.RUN_STATUS_SUCCESS : ArchiveAgentSupport.RUN_STATUS_FAILED);
        step.setErrorMessage(errorMessage);
        step.setInputJson(writeJson(input == null ? Map.of() : input));
        step.setOutputJson(writeJson(output == null ? Map.of() : output));
        step.setStartedAt(startedAt);
        step.setFinishedAt(LocalDateTime.now());
        step.setDurationMs(resolveDuration(step.getStartedAt(), step.getFinishedAt()));
        stepMapper.insert(step);
    }

    private void updateAgentLastRun(ArchiveAgentRun run) {
        if (run.getAgentId() == null || run.getAgentId() <= 0) {
            return;
        }
        ArchiveAgentDefinition definition = definitionMapper.selectById(run.getAgentId());
        if (definition == null) {
            return;
        }
        definition.setLastRunId(run.getId());
        definition.setLastRunStatus(run.getStatus());
        definition.setLastRunSummary(run.getSummary());
        definition.setLastRunAt(run.getFinishedAt());
        definitionMapper.updateById(definition);
    }

    private Map<String, Object> buildModelExtraParams(String model) {
        return model == null || model.isBlank() ? Map.of() : Map.of("model", model.trim());
    }

    private List<Map<String, Object>> safeRecords(ControlledToolResult result) {
        return result.getRecords() == null ? List.of() : result.getRecords();
    }

    private List<String> safeEvidenceRefs(ControlledToolResult result) {
        return result.getEvidenceRefs() == null ? List.of() : result.getEvidenceRefs();
    }

    private String resolveUserMessage(Map<String, Object> inputPayload) {
        for (String key : List.of("query", "message", "context")) {
            String value = text(inputPayload.get(key));
            if (!value.isBlank()) {
                return value;
            }
        }
        return "查询我的报销单";
    }

    private String normalizeSessionId(String sessionId, ArchiveAgentRun run) {
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionId.trim();
        }
        return "agent-run-" + (run == null || run.getId() == null ? UUID.randomUUID() : run.getId());
    }

    private String nodeKey(Map<String, Object> node) {
        return requiredText(node.get("nodeKey"), "工作流节点缺少 nodeKey");
    }

    private String nodeLabel(Map<String, Object> node) {
        String label = text(node.get("label"));
        return label.isBlank() ? nodeKey(node) : label;
    }

    private String requiredText(Object value, String message) {
        String result = text(value);
        if (result.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return result;
    }

    private String ensureTraceId() {
        String traceId = TraceContext.getCurrentTraceId();
        if (traceId == null || traceId.isBlank()) {
            traceId = "trace-" + UUID.randomUUID();
            TraceContext.setTraceId(traceId);
        }
        return traceId;
    }

    private Long resolveDuration(LocalDateTime startedAt, LocalDateTime finishedAt) {
        return startedAt == null || finishedAt == null ? null : Duration.between(startedAt, finishedAt).toMillis();
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

    private Map<String, Object> readNestedMap(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key);
        if (!(value instanceof Map<?, ?> map)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((mapKey, mapValue) -> result.put(String.valueOf(mapKey), mapValue));
        return result;
    }

    private Map<String, Object> values(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index + 1 < pairs.length; index += 2) {
            result.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return result;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String nullToBlank(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record ModelAnalysisResult(String content, String model) {
    }
}
