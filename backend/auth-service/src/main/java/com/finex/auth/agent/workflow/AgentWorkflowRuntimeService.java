package com.finex.auth.agent.workflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.context.TraceContext;
import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.desensitization.DesensitizationService;
import com.finex.auth.agent.desensitization.DesensitizedData;
import com.finex.auth.agent.model.ChatModelProvider;
import com.finex.auth.agent.model.ModelCapability;
import com.finex.auth.agent.model.ModelRoutingService;
import com.finex.auth.agent.ontology.OntologyResultContextService;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentRun;
import com.finex.auth.entity.ArchiveAgentRunStep;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.mapper.ArchiveAgentDefinitionMapper;
import com.finex.auth.mapper.ArchiveAgentRunMapper;
import com.finex.auth.mapper.ArchiveAgentRunStepMapper;
import com.finex.auth.support.archiveagent.ArchiveAgentSupport;
import com.finex.auth.service.digitalemployee.DigitalEmployeeService;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
            Map.entry("keyword", "KEYWORD"), Map.entry("normalizedKeyword", "KEYWORD"),
            Map.entry("documentId", "RECORD"), Map.entry("documentCode", "DOC"),
            Map.entry("title", "DETAIL"), Map.entry("reason", "DETAIL"),
            Map.entry("cdigest", "DETAIL"), Map.entry("cbill", "DETAIL"),
            Map.entry("cname", "DETAIL"), Map.entry("cCusName", "DETAIL"),
            Map.entry("cVenName", "DETAIL"),
            Map.entry("cCusAbbName", "DETAIL"),
            Map.entry("cVenAbbName", "DETAIL"),
            Map.entry("supplierName", "DETAIL"),
            Map.entry("supplierCode", "RECORD"),
            Map.entry("subjectName", "DETAIL"),
            Map.entry("summary", "DETAIL"),
            Map.entry("voucherNo", "DOC"),
            Map.entry("citemname", "DETAIL"),
            Map.entry("dept_name", "DETAIL"),
            Map.entry("position", "DETAIL"),
            Map.entry("labor_relation_belong", "DETAIL"),
            Map.entry("username", "RECORD"),
            Map.entry("maker_user_id", "RECORD"),
            Map.entry("cperson_id", "RECORD")
    );

    private final WorkflowSelectorService workflowSelectorService;
    private final QueryParameterExtractor queryParameterExtractor;
    private final QueryTemplateVariableMapper queryTemplateVariableMapper;
    private final ToolRuntimeService toolRuntimeService;
    private final ControlledWorkflowToolInvoker controlledToolInvoker;
    private final ControlledQueryBranchRunner branchRunner;
    private final DesensitizationService desensitizationService;
    private final ModelRoutingService modelRoutingService;
    private final ArchiveAgentRunMapper runMapper;
    private final ArchiveAgentRunStepMapper stepMapper;
    private final ArchiveAgentDefinitionMapper definitionMapper;
    private final ObjectMapper objectMapper;
    private final QueryAuthorizationService authorizationService;
    private final DigitalEmployeeService digitalEmployeeService;
    private final OntologyResultContextService ontologyResultContextService;

    public WorkflowExecutionResult executeQueryWorkflow(
            String sessionId,
            Long userId,
            String userMessage,
            String intent,
            String providerCode,
            String model
    ) {
        return executeQueryWorkflow(sessionId, userId, userMessage, intent, providerCode, model, null, null);
    }

    /** Execute a version already chosen by the server-validated FinWork plan. */
    public WorkflowExecutionResult executePlannedWorkflow(
            String sessionId, Long userId, SelectedWorkflow workflow, String queryText) {
        return executePlannedWorkflow(sessionId, userId, workflow, queryText, Map.of());
    }

    public WorkflowExecutionResult executePlannedWorkflow(
            String sessionId, Long userId, SelectedWorkflow workflow, String queryText,
            Map<String, Object> parameters) {
        if (workflow == null || workflow.getDigitalEmployeeId() == null
                || queryText == null || queryText.isBlank()) {
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        }
        String intent = FinanceUnclosedCompanyTool.TOOL_CODE.equals(workflow.getToolCode())
                ? "query_finance_close" : "query_expense";
        ArchiveAgentRun run = createRun(workflow, userId, sessionId, queryText, ensureTraceId());
        return executeSelectedWorkflow(run, workflow, normalizeSessionId(sessionId, run),
                userId, queryText, intent, Map.of("query", queryText, "finworkPrepared", true,
                        "parameters", parameters == null ? Map.of() : parameters), true);
    }

    public WorkflowExecutionResult executeQueryWorkflow(
            String sessionId, Long userId, String userMessage, String intent,
            String providerCode, String model, Long employeeId, Long bindingId
    ) {
        String traceId = ensureTraceId();
        SelectedWorkflow workflow;
        try {
            workflow = workflowSelectorService.select(userId, intent, userMessage, providerCode, model,
                    employeeId, bindingId);
        } catch (WorkflowSelectorService.AmbiguousWorkflowException exception) {
            return WorkflowExecutionResult.builder().success(false).intent(intent).traceId(traceId)
                    .content(exception.getMessage()).evidenceRefs(List.of()).build();
        }
        if (workflow == null) {
            return WorkflowExecutionResult.builder()
                    .success(false)
                    .intent(intent)
                    .traceId(traceId)
                    .errorMessage(QueryAuthorizationService.NO_PERMISSION)
                    .content(QueryAuthorizationService.NO_PERMISSION)
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
            log.warn("Agent 工作台受控查询执行失败: runId={}, traceId={}, type={}",
                    run == null ? null : run.getId(), traceId, ex.getClass().getSimpleName());
            if (run != null) {
                if (run.getStartedAt() == null) {
                    markRunRunning(run);
                }
                markRunFailed(run, safeFailureMessage(ex));
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
        if (workflow.getToolCodes() != null && (workflow.getToolCodes().size() > 1
                || (!ExpenseDocumentControlledQueryTool.TOOL_CODE.equals(workflow.getToolCode())
                && !FinanceUnclosedCompanyTool.TOOL_CODE.equals(workflow.getToolCode()))))
            return executeMultiToolWorkflow(run, workflow, sessionId, userId, userMessage,
                    intent, inputPayload, includeSelectorStep);
        String traceId = ensureTraceId();
        try {
            QueryAuthorization authorization = resolveAuthorization(workflow, userId);
            markRunRunning(run);
            List<Map<String, Object>> orderedNodes = controlledNodes(workflow);
            Map<String, Object> startNode = nodeOfType(orderedNodes, "start");
            Map<String, Object> toolNode = nodeOfType(orderedNodes, "tool");
            Map<String, Object> llmNode = nodeOfType(orderedNodes, "llm");
            Map<String, Object> endNode = nodeOfType(orderedNodes, "end");
            Map<String, Object> toolConfig = WorkflowGraphPlan.nodeConfig(toolNode);
            Map<String, Object> llmConfig = WorkflowGraphPlan.nodeConfig(llmNode);

            QueryTemplateVariables extracted = queryParameterExtractor.extract(userMessage);
            QueryTemplateVariableMapper.PreparedQuery prepared = queryTemplateVariableMapper.prepare(
                    workflow.getToolCode(), extracted, toolConfig, inputPayload, nodeKey(toolNode), userMessage);
            QueryTemplateVariables variables = prepared.variables();
            String effectiveMessage = prepared.message();
            int stepNo = 1;
            if (includeSelectorStep) {
                saveStep(run.getId(), stepNo++, "selector_1", "selector", "工作流选择",
                        values("intent", intent),
                        values("agentName", workflow.getAgentName(), "builtInFallback", workflow.isBuiltInFallback()), null);
            }

            saveStep(run.getId(), stepNo++, nodeKey(startNode), "start", nodeLabel(startNode),
                    values("sessionId", sessionId),
                    values("message", "流程开始"), null);

            String queryTemplateCode = requiredText(toolConfig.get("queryTemplateCode"), "工具节点缺少查询模板编码");
            ControlledToolResult toolResult = toolRuntimeService.execute(workflow.getToolCode(), ToolExecutionRequest.builder()
                    .userId(userId)
                    .companyId(TenantContext.getCompanyId())
                    .sessionId(sessionId)
                    .traceId(traceId)
                    .queryTemplateCode(queryTemplateCode)
                    .variables(variables)
                    .authorization(authorization)
                    .userMessage(effectiveMessage)
                    .build());
            if (!toolResult.isSuccess()) {
                throw new IllegalStateException(toolResult.getErrorMessage());
            }
            saveStep(run.getId(), stepNo++, nodeKey(toolNode), "tool", nodeLabel(toolNode),
                    values("toolCode", workflow.getToolCode(), "queryTemplateCode", queryTemplateCode),
                    values("recordCount", safeRecords(toolResult).size()), null);

            if (FinanceUnclosedCompanyTool.TOOL_CODE.equals(workflow.getToolCode())) {
                assertSameAuthorization(workflow, userId, authorization);
                String content = withSource(toolResult.getSummary(), workflow);
                markRunSuccess(run, content, traceId, null, List.of(), List.of());
                return WorkflowExecutionResult.builder().success(true).content(content).intent(intent)
                        .runId(run.getId()).traceId(traceId).evidenceRefs(List.of())
                        .digitalEmployeeId(workflow.getDigitalEmployeeId())
                        .responsibleUserId(workflow.getResponsibleUserId())
                        .agentVersionId(workflow.getAgentVersionId())
                        .queryPermissionCode(workflow.getQueryPermissionCode())
                        .authorizationDigest(authorization.digest())
                        .resultCount(toolResult.getTotalCount().intValue())
                        .totalAmount(toolResult.getTotalAmount())
                        .displayedCount(safeRecords(toolResult).size())
                        .queryDescriptorJson("{}")
                        .sourceDigest(authorization.digest()).build();
            }

            Map<String, Object> contextPayload = buildContextPayload(traceId, userMessage, inputPayload,
                    variables, toolResult, workflow);
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
                    values("mappingId", desensitized.getMappingId()), null);

            ModelAnalysisResult analysis = callAnalysisModel(workflow, llmConfig, desensitized.getMaskedText());
            saveStep(run.getId(), stepNo++, nodeKey(llmNode), "llm", nodeLabel(llmNode),
                    values("provider", workflow.getProviderCode(), "model", workflow.getModel(), "mappingId", desensitized.getMappingId()),
                    values("responseModel", analysis.model()), null);

            String restoredAnalysis = desensitizationService.restore(desensitized.getMappingId(), analysis.content());
            List<String> evidenceRefs = validatedEvidenceRefs(restoredAnalysis, toolResult);
            List<String> entityRefs = entityRefs(toolResult);
            String finalContent = withSource(buildFinalContent(restoredAnalysis, toolResult, evidenceRefs), workflow);
            saveStep(run.getId(), stepNo++, nodeKey(llmNode) + "__restore", "restore", "本地还原与引用校验",
                    values("mappingId", desensitized.getMappingId(), "entityRefs", entityRefs, "evidenceRefs", evidenceRefs),
                    values("recordCount", safeRecords(toolResult).size()), null);

            Map<String, Object> finalOutput = values(
                    "message", "流程执行完成",
                    "recordCount", safeRecords(toolResult).size(),
                    "mappingId", desensitized.getMappingId(),
                    "entityRefs", entityRefs,
                    "evidenceRefs", evidenceRefs
            );
            saveStep(run.getId(), stepNo, nodeKey(endNode), "end", nodeLabel(endNode),
                    values("previousNode", nodeKey(llmNode)), finalOutput, null);

            assertSameAuthorization(workflow, userId, authorization);
            markRunSuccess(run, finalContent, traceId, desensitized.getMappingId(), entityRefs, evidenceRefs);
            return WorkflowExecutionResult.builder()
                    .success(true)
                    .content(finalContent)
                    .intent(intent)
                    .runId(run.getId())
                    .traceId(traceId)
                    .mappingId(desensitized.getMappingId())
                    .evidenceRefs(evidenceRefs)
                    .digitalEmployeeId(workflow.getDigitalEmployeeId())
                    .responsibleUserId(workflow.getResponsibleUserId())
                    .agentVersionId(workflow.getAgentVersionId())
                    .queryPermissionCode(workflow.getQueryPermissionCode())
                    .authorizationDigest(authorization.digest())
                    .resultCount(toolResult.getTotalCount() == null ? safeRecords(toolResult).size() : toolResult.getTotalCount().intValue())
                    .totalAmount(toolResult.getTotalAmount())
                    .displayedCount(safeRecords(toolResult).size())
                    .queryDescriptorJson(writeJson(toolResult.getQueryDescriptor()))
                    .sourceDigest(toolResult.getSourceDigest())
                    .build();
        } catch (Exception ex) {
            log.warn("Agent 工作流执行失败: runId={}, traceId={}, type={}",
                    run.getId(), traceId, ex.getClass().getSimpleName());
            markRunFailed(run, safeFailureMessage(ex));
            return failedResult(run, intent, traceId, ex);
        }
    }

    private WorkflowExecutionResult executeMultiToolWorkflow(
            ArchiveAgentRun run, SelectedWorkflow workflow, String sessionId, Long userId,
            String userMessage, String intent, Map<String, Object> inputPayload,
            boolean includeSelectorStep) {
        String traceId = ensureTraceId();
        try {
            Map<String, QueryAuthorization> granted = resolveAllAuthorizations(workflow, userId);
            WorkflowGraphPlan.ControlledQueryPlan plan = WorkflowGraphPlan.from(
                    readNestedMap(workflow.getConfig(), "workflow")).requireControlledQueryPlan();
            List<Map<String, Object>> nodes = plan.orderedNodes();
            markRunRunning(run);
            int stepNo = 1;
            if (includeSelectorStep) saveStep(run.getId(), stepNo++, "selector_1", "selector", "工作流选择",
                    values("bindingId", workflow.getWorkflowBindingId()),
                    values("capabilities", workflow.getQueryPermissionCodes()), null);
            Map<String, Object> startNode = nodeOfType(nodes, "start");
            saveStep(run.getId(), stepNo++, nodeKey(startNode), "start", nodeLabel(startNode),
                    values("sessionId", sessionId), values("message", "流程开始"), null);
            Map<String, ControlledToolResult> prior = new LinkedHashMap<>();
            List<WorkflowExecutionResult.ToolOrigin> origins = new ArrayList<>();
            List<Map<String, Object>> modelInputs = new ArrayList<>();
            long matchedCount = 0;
            int displayedCount = 0;
            java.math.BigDecimal expenseAmount = null;
            int expenseNodeCount = 0;
            Map<String, ControlledQueryBranchRunner.BranchResult> parallel = Map.of();
            if (!plan.parallel().isEmpty()) {
                Map<String, ControlledToolResult> prefix = new LinkedHashMap<>();
                for (Map<String, Object> node : plan.sequential()) {
                    assertAllAuthorizations(workflow, userId, granted);
                    String key = nodeKey(node);
                    try {
                        prefix.put(key, invokeControlledNode(node, workflow, userId, sessionId, traceId,
                                userMessage, inputPayload, prefix, granted));
                    } catch (Exception failure) {
                        saveStep(run.getId(), stepNo++, key, "tool", nodeLabel(node),
                                values("toolCode", WorkflowGraphPlan.nodeConfig(node).get("toolCode")),
                                values("status", "FAILED"), safeToolStepFailure(failure));
                        throw failure;
                    }
                }
                Map<String, Map<String, Object>> branchNodes = new LinkedHashMap<>();
                plan.parallel().forEach(node -> branchNodes.put(nodeKey(node), node));
                Map<String, ControlledToolResult> completedPrefix = Map.copyOf(prefix);
                try {
                    parallel = branchRunner.run(List.copyOf(branchNodes.keySet()), key -> {
                        assertAllAuthorizations(workflow, userId, granted);
                        return invokeControlledNode(branchNodes.get(key), workflow, userId, sessionId,
                                traceId, userMessage, inputPayload, completedPrefix, granted);
                    });
                } catch (ControlledQueryBranchRunner.BranchFailure failure) {
                    for (Map.Entry<String, ControlledQueryBranchRunner.BranchLog> item : failure.logs().entrySet()) {
                        Map<String, Object> branch = branchNodes.get(item.getKey());
                        String status = item.getValue().status();
                        saveStep(run.getId(), stepNo++, item.getKey(), "tool", nodeLabel(branch),
                                values("toolCode", WorkflowGraphPlan.nodeConfig(branch).get("toolCode")),
                                values("status", status), "SUCCESS".equals(status) ? null : "并行查询未完成",
                                item.getValue().startedAt(), item.getValue().finishedAt());
                    }
                    throw failure;
                }
                prior.putAll(prefix);
            }
            for (Map<String, Object> node : nodes) {
                if (!"tool".equalsIgnoreCase(text(node.get("nodeType")))) continue;
                String key = nodeKey(node);
                Map<String, Object> config = WorkflowGraphPlan.nodeConfig(node);
                String code = requiredText(config.get("toolCode"), "工具编码不能为空");
                ControlledQueryCatalog.Definition definition = ControlledQueryCatalog.require(code);
                QueryAuthorization auth = granted.get(definition.permission());
                if (auth == null) throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
                boolean close = FinanceUnclosedCompanyTool.TOOL_CODE.equals(code);
                ControlledQueryBranchRunner.BranchResult branchResult = parallel.get(key);
                ControlledToolResult result;
                if (branchResult != null) result = branchResult.result();
                else if (!plan.parallel().isEmpty()) result = prior.get(key);
                else {
                    assertAllAuthorizations(workflow, userId, granted);
                    try {
                        result = invokeControlledNode(node, workflow, userId, sessionId, traceId,
                                userMessage, inputPayload, prior, granted);
                    } catch (Exception failure) {
                        saveStep(run.getId(), stepNo++, key, "tool", nodeLabel(node),
                                values("toolCode", code), values("status", "FAILED"),
                                safeToolStepFailure(failure));
                        throw failure;
                    }
                }
                if (ExpenseDocumentControlledQueryTool.TOOL_CODE.equals(code)) {
                    expenseNodeCount++;
                    expenseAmount = result.getTotalAmount();
                }
                matchedCount += result.getTotalCount() == null
                        ? safeRecords(result).size() : result.getTotalCount();
                displayedCount += safeRecords(result).size();
                prior.put(key, result);
                modelInputs.add(values("nodeKey", key, "toolCode", code,
                        "permissionCode", definition.permission(),
                        "companyScope", auth.companyIds().stream().sorted().toList(),
                        "queryDescriptor", result.getQueryDescriptor(), "summary", result.getSummary(),
                        "totalCount", result.getTotalCount(), "page", result.getPage(),
                        "hasMore", result.getHasMore(), "records", result.getRecords()));
                origins.add(new WorkflowExecutionResult.ToolOrigin(key, code, definition.permission(),
                        auth.digest(), close ? "{}" : writeJson(result.getQueryDescriptor()),
                        close ? auth.digest() : result.getSourceDigest()));
                saveStep(run.getId(), stepNo++, key, "tool", nodeLabel(node),
                        values("toolCode", code, "permissionCode", definition.permission()),
                        values("totalCount", result.getTotalCount(), "page", result.getPage(),
                                "hasMore", result.getHasMore()), null,
                        branchResult == null ? null : branchResult.startedAt(),
                        branchResult == null ? null : branchResult.finishedAt());
            }
            assertAllAuthorizations(workflow, userId, granted);
            String contextJson = writeJson(values("question", userMessage, "tools", modelInputs,
                    "businessOntology", ontologyResultContextService.forAnalysis(userMessage, workflow, modelInputs)));
            DesensitizedData masked = desensitizationService.desensitizeJsonFields(
                    sessionId, contextJson, CLOUD_SENSITIVE_FIELDS, Set.of("KEYWORD"));
            if (!masked.isSuccess() || masked.getMappingId() == null)
                throw new IllegalStateException("受控工具结果脱敏失败");
            Map<String, Object> llmNode = nodeOfType(nodes, "llm");
            LocalDateTime llmStartedAt = LocalDateTime.now();
            ModelAnalysisResult analysis = callAnalysisModel(workflow,
                    WorkflowGraphPlan.nodeConfig(llmNode), masked.getMaskedText());
            saveStep(run.getId(), stepNo++, nodeKey(llmNode), "llm", nodeLabel(llmNode),
                    values("sourceCount", origins.size()), values("status", "SUCCESS"), null,
                    llmStartedAt, LocalDateTime.now());
            String restored = desensitizationService.restore(masked.getMappingId(), analysis.content());
            StringBuilder content = new StringBuilder();
            for (Map<String, Object> item : modelInputs) {
                if (!content.isEmpty()) content.append("\n");
                content.append(item.get("nodeKey")).append("（").append(item.get("toolCode"))
                        .append("）：").append(item.get("summary"));
                SupplierLedgerAnswerFormatter.appendRows(content, item);
            }
            String explanation = extractAnswer(restored);
            if (explanation != null && !explanation.isBlank())
                content.append("\n\n分析：").append(explanation);
            content.append("\n\n仅统计您有权查看的范围。");
            assertAllAuthorizations(workflow, userId, granted);
            String finalContent = withSource(content.toString(), workflow);
            Map<String, Object> endNode = nodeOfType(nodes, "end");
            saveStep(run.getId(), stepNo, nodeKey(endNode), "end", nodeLabel(endNode),
                    values("sourceCount", origins.size()), values("status", "SUCCESS"), null);
            markRunSuccess(run, finalContent, traceId, masked.getMappingId(), List.of(), List.of());
            return WorkflowExecutionResult.builder().success(true).content(finalContent).intent(intent)
                    .runId(run.getId()).traceId(traceId).mappingId(masked.getMappingId())
                    .evidenceRefs(List.of()).digitalEmployeeId(workflow.getDigitalEmployeeId())
                    .responsibleUserId(workflow.getResponsibleUserId())
                    .agentVersionId(workflow.getAgentVersionId())
                    .queryPermissionCode(workflow.getQueryPermissionCode())
                    .authorizationDigest(granted.get(workflow.getQueryPermissionCode()).digest())
                    .resultCount(plan.parallel().isEmpty() ? Integer.valueOf(Math.toIntExact(matchedCount))
                            : (matchedCount == 0 ? Integer.valueOf(0) : null)).displayedCount(displayedCount)
                    .totalAmount(expenseNodeCount == 1 ? expenseAmount : null)
                    .queryDescriptorJson("{}").sourceDigest("multi")
                    .toolOrigins(List.copyOf(origins)).build();
        } catch (Exception exception) {
            log.warn("Agent 多工具受控查询失败: runId={}, traceId={}, type={}, location={}",
                    run.getId(), traceId, exception.getClass().getSimpleName(),
                    exception.getStackTrace().length == 0 ? "unknown" : exception.getStackTrace()[0]);
            markRunFailed(run, safeFailureMessage(exception));
            return failedResult(run, intent, traceId, exception);
        }
    }

    private Map<String, QueryAuthorization> resolveAllAuthorizations(SelectedWorkflow workflow, Long userId) {
        if (workflow.getDigitalEmployeeId() != null) {
            boolean bound = digitalEmployeeService.availableFor(userId).stream().anyMatch(item ->
                    workflow.getDigitalEmployeeId().equals(((Number) item.get("id")).longValue())
                    && workflow.getWorkflowBindingId().equals(((Number) item.get("binding_id")).longValue())
                    && workflow.getAgentVersionId().equals(((Number) item.get("agent_version_id")).longValue()));
            if (!bound) throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        }
        Map<String, QueryAuthorization> grants = new LinkedHashMap<>();
        for (String code : workflow.getQueryPermissionCodes()) {
            List<String> limit = workflow.getDigitalEmployeeId() == null ? List.of()
                    : digitalEmployeeService.companyLimit(workflow.getDigitalEmployeeId(), code);
            grants.put(code, authorizationService.resolve(userId,
                    workflow.getResponsibleUserId() == null ? userId : workflow.getResponsibleUserId(),
                    code, limit.isEmpty() ? null : Set.copyOf(limit)));
        }
        return grants;
    }

    private ControlledToolResult invokeControlledNode(Map<String, Object> node, SelectedWorkflow workflow,
                                                       Long userId, String sessionId, String traceId,
                                                       String message, Map<String, Object> input,
                                                       Map<String, ControlledToolResult> prior,
                                                       Map<String, QueryAuthorization> granted) {
        String key = nodeKey(node);
        Map<String, Object> config = WorkflowGraphPlan.nodeConfig(node);
        String code = requiredText(config.get("toolCode"), "工具编码不能为空");
        QueryAuthorization auth = granted.get(ControlledQueryCatalog.require(code).permission());
        if (auth == null) throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        return controlledToolInvoker.invoke(key, config, input, prior, message, userId,
                sessionId, traceId, auth);
    }

    private void assertAllAuthorizations(SelectedWorkflow workflow, Long userId,
                                         Map<String, QueryAuthorization> original) {
        Map<String, QueryAuthorization> current = resolveAllAuthorizations(workflow, userId);
        for (String code : original.keySet()) {
            if (!original.get(code).digest().equals(current.get(code).digest()))
                throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        }
    }

    private QueryAuthorization resolveAuthorization(SelectedWorkflow workflow, Long userId) {
        if (workflow.getDigitalEmployeeId() != null) {
            boolean stillBound = digitalEmployeeService.availableFor(userId).stream().anyMatch(candidate ->
                    workflow.getDigitalEmployeeId().equals(((Number) candidate.get("id")).longValue())
                            && workflow.getWorkflowBindingId().equals(((Number) candidate.get("binding_id")).longValue())
                            && workflow.getAgentVersionId().equals(((Number) candidate.get("agent_version_id")).longValue())
                            && workflow.getQueryPermissionCode().equals(candidate.get("permission_code")));
            if (!stillBound) throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        }
        String permissionCode = workflow.getQueryPermissionCode();
        if (permissionCode == null) {
            permissionCode = FinanceUnclosedCompanyTool.TOOL_CODE.equals(workflow.getToolCode())
                    ? QueryAuthorizationService.FINANCE_CLOSE_QUERY : QueryAuthorizationService.EXPENSE_QUERY;
        }
        Long responsible = workflow.getResponsibleUserId() == null ? userId : workflow.getResponsibleUserId();
        List<String> limit = workflow.getDigitalEmployeeId() == null ? List.of()
                : digitalEmployeeService.companyLimit(workflow.getDigitalEmployeeId(), permissionCode);
        return authorizationService.resolve(userId, responsible, permissionCode,
                limit.isEmpty() ? null : Set.copyOf(limit));
    }

    private void assertSameAuthorization(SelectedWorkflow workflow, Long userId, QueryAuthorization original) {
        if (!original.digest().equals(resolveAuthorization(workflow, userId).digest())) {
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        }
    }

    private List<Map<String, Object>> controlledNodes(SelectedWorkflow workflow) {
        Map<String, Object> workflowConfig = readNestedMap(workflow.getConfig(), "workflow");
        return WorkflowGraphPlan.from(workflowConfig).requireControlledQueryPlan().orderedNodes();
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

    private Map<String, Object> buildContextPayload(String traceId, String userMessage,
                                                    Map<String, Object> inputPayload,
                                                    QueryTemplateVariables variables, ControlledToolResult toolResult,
                                                    SelectedWorkflow workflow) {
        return values(
                "traceId", traceId,
                "question", userMessage,
                "context", inputPayload == null ? null : text(inputPayload.get("context")),
                "variables", variables.toMap(),
                "records", safeRecords(toolResult),
                "businessOntology", ontologyResultContextService.forAnalysis(userMessage, workflow,
                        List.of(values("toolCode", workflow.getToolCode(), "records", safeRecords(toolResult)))),
                "totalCount", toolResult.getTotalCount(),
                "totalAmount", toolResult.getTotalAmount(),
                "evidenceRefs", safeEvidenceRefs(toolResult)
        );
    }

    private String buildFinalContent(
            String restoredAnalysis,
            ControlledToolResult toolResult,
            List<String> evidenceRefs
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(toolResult.getSummary() == null ? buildLocalSummary(toolResult) : toolResult.getSummary());
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
        builder.append("\n\n仅统计您有权查看的范围。");
        return builder.toString();
    }

    private String withSource(String answer, SelectedWorkflow workflow) {
        if (workflow.getDigitalEmployeeId() == null) return answer;
        String name = workflow.getDigitalEmployeeName();
        if (name == null || name.isBlank()) name = "数字员工 #" + workflow.getDigitalEmployeeId();
        return answer + "\n\n来源：" + name + " · 工作流固定版本 #" + workflow.getAgentVersionId();
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
        run.setInputJson(writeJson(values("sessionId", sessionId, "traceId", traceId,
                "digitalEmployeeId", workflow.getDigitalEmployeeId(),
                "queryPermissionCode", workflow.getQueryPermissionCode())));
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
        Map<String, Object> output = values(
                "traceId", traceId,
                "queryResultAvailableInFinWork", true,
                "recordCount", entityRefs.size()
        );
        if (ArchiveAgentSupport.TRIGGER_SOURCE_WORKBENCH_TEST.equals(run.getTriggerSource())) {
            output.put("testResult", finalContent);
            output.remove("queryResultAvailableInFinWork");
        }
        run.setOutputJson(writeJson(output));
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
        String message = safeFailureMessage(ex);
        return WorkflowExecutionResult.builder()
                .success(false)
                .content(message)
                .errorMessage(message)
                .intent(intent)
                .runId(run == null ? null : run.getId())
                .traceId(traceId)
                .evidenceRefs(List.of())
                .build();
    }

    private String safeFailureMessage(Exception exception) {
        if (exception instanceof SecurityException || exception.getCause() instanceof SecurityException
                || QueryAuthorizationService.NO_PERMISSION.equals(exception.getMessage()))
            return QueryAuthorizationService.NO_PERMISSION;
        if (exception.getMessage() != null && exception.getMessage().startsWith("请明确会计年度和期间"))
            return "请明确会计年度和期间，例如 2026 年 9 月";
        if (exception.getMessage() != null && exception.getMessage().startsWith("请提供有效的会计年度与期间"))
            return "请提供有效的会计年度与期间";
        return "查询执行失败，请稍后重试";
    }

    private String safeToolStepFailure(Exception exception) {
        if (exception instanceof SecurityException) return QueryAuthorizationService.NO_PERMISSION;
        if (exception instanceof IllegalArgumentException) {
            String message = exception.getMessage();
            if (message != null && message.contains("companyId")) return "缺少或无法使用账套";
            if (message != null && (message.contains("会计期间") || message.contains("yearFrom")))
                return "会计期间参数不完整或无效";
            if (message != null && message.contains("voucherStatus")) return "凭证范围参数无效";
            return "受控工具参数无效，请检查必填条件";
        }
        return "受控工具执行失败，请稍后重试";
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
        saveStep(runId, stepNo, nodeKey, nodeType, label, input, output, errorMessage, null, null);
    }

    private void saveStep(Long runId, int stepNo, String nodeKey, String nodeType, String label,
                          Map<String, Object> input, Map<String, Object> output, String errorMessage,
                          LocalDateTime actualStartedAt, LocalDateTime actualFinishedAt) {
        LocalDateTime startedAt = actualStartedAt != null ? actualStartedAt
                : actualFinishedAt == null ? LocalDateTime.now() : null;
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
        step.setFinishedAt(actualFinishedAt == null ? LocalDateTime.now() : actualFinishedAt);
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
