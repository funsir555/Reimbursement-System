package com.finex.auth.agent.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.desensitization.DesensitizationService;
import com.finex.auth.agent.desensitization.DesensitizedData;
import com.finex.auth.agent.model.ChatModelProvider;
import com.finex.auth.agent.model.ModelRoutingService;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentRun;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.mapper.ArchiveAgentDefinitionMapper;
import com.finex.auth.mapper.ArchiveAgentRunMapper;
import com.finex.auth.mapper.ArchiveAgentRunStepMapper;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import com.finex.auth.service.digitalemployee.DigitalEmployeeService;
import com.finex.auth.support.archiveagent.ArchiveAgentSupport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class AgentWorkflowRuntimeServiceTest {

    @Test
    void failingParallelBranchDoesNotSendPartialResultsToModel() throws Exception {
        ObjectMapper json = new ObjectMapper();
        WorkflowSelectorService selector = mock(WorkflowSelectorService.class);
        ControlledWorkflowToolInvoker invoker = mock(ControlledWorkflowToolInvoker.class);
        ControlledQueryBranchRunner branches = new ControlledQueryBranchRunner();
        QueryAuthorizationService authorization = mock(QueryAuthorizationService.class);
        String permission = ControlledQueryCatalog.require("finance.query_account_subjects").permission();
        QueryAuthorization grant = new QueryAuthorization(7L, 7L, permission,
                List.of(new QueryAuthorization.Grant("ALL_COMPANIES", Set.of("COMPANY-A"), Set.of())),
                List.of(new QueryAuthorization.Grant("ALL_COMPANIES", Set.of("COMPANY-A"), Set.of())), null);
        when(authorization.resolve(any(), any(), anyString(), any())).thenReturn(grant);
        ArchiveAgentDefinition definition = new ArchiveAgentDefinition();
        definition.setId(10L);
        definition.setOwnerUserId(7L);
        definition.setAgentName("并行查询");
        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setId(20L);
        ArchiveAgentRun run = new ArchiveAgentRun();
        run.setId(30L);
        run.setAgentId(10L);
        run.setInputJson("{\"query\":\"查询科目\"}");
        Map<String, Object> config = Map.of("workflow", Map.of(
                "nodes", List.of(
                        Map.of("nodeKey", "start", "nodeType", "start"),
                        Map.of("nodeKey", "a", "nodeType", "tool", "config", Map.of("toolCode", "finance.query_account_subjects")),
                        Map.of("nodeKey", "b", "nodeType", "tool", "config", Map.of("toolCode", "finance.query_account_subjects")),
                        Map.of("nodeKey", "llm", "nodeType", "llm"),
                        Map.of("nodeKey", "end", "nodeType", "end")),
                "edges", List.of(Map.of("source", "start", "target", "a"),
                        Map.of("source", "start", "target", "b"),
                        Map.of("source", "a", "target", "llm"),
                        Map.of("source", "b", "target", "llm"),
                        Map.of("source", "llm", "target", "end"))));
        SelectedWorkflow selected = SelectedWorkflow.builder().agentId(10L).agentVersionId(20L)
                .agentName("并行查询").toolCode("finance.query_account_subjects")
                .toolCodes(List.of("finance.query_account_subjects", "finance.query_account_subjects"))
                .queryPermissionCode(permission).queryPermissionCodes(List.of(permission))
                .providerCode("LOCAL_7B").model("test").config(config).build();
        when(selector.selectConfigured(definition, version, config, null, null)).thenReturn(selected);
        AtomicBoolean fail = new AtomicBoolean(true);
        when(invoker.invoke(anyString(), any(), any(), any(), anyString(), any(), anyString(), anyString(), any()))
                .thenAnswer(call -> {
                    String key = call.getArgument(0);
                    if (fail.get() && "b".equals(key)) throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
                    if (fail.get()) Thread.sleep(5000);
                    return ControlledToolResult.builder().success(true).summary(key)
                            .records(List.of(Map.of("id", key))).totalCount(1L)
                            .queryDescriptor(Map.of("toolCode", "finance.query_account_subjects"))
                            .sourceDigest(key).build();
                });
        ModelRoutingService model = mock(ModelRoutingService.class);
        DesensitizationService masking = mock(DesensitizationService.class);
        AgentWorkflowRuntimeService service = new AgentWorkflowRuntimeService(selector,
                new QueryParameterExtractor(), new QueryTemplateVariableMapper(json),
                mock(ToolRuntimeService.class), invoker, branches,
                masking, model,
                mock(ArchiveAgentRunMapper.class), mock(ArchiveAgentRunStepMapper.class),
                mock(ArchiveAgentDefinitionMapper.class), json, authorization,
                mock(DigitalEmployeeService.class));
        TenantContext.setTenantId("default");
        TenantContext.setCompanyId("COMPANY-A");
        TenantContext.setUserId(7L);
        try {
            WorkflowExecutionResult result = service.executeWorkbenchWorkflow(run, definition, version, config);
            assertEquals(false, result.isSuccess());
            assertEquals(QueryAuthorizationService.NO_PERMISSION, result.getContent());
            verify(model, never()).chatWithPreferredProvider(any(), any(), any(), any());
            fail.set(false);
            when(masking.desensitizeJsonFields(anyString(), anyString(), any(), any()))
                    .thenAnswer(call -> {
                        String payload = call.getArgument(1);
                        assertTrue(payload.indexOf("\"nodeKey\":\"a\"")
                                < payload.indexOf("\"nodeKey\":\"b\""));
                        return DesensitizedData.builder().success(true).mappingId("masked-1")
                                .maskedText(payload).build();
                    });
            when(model.chatWithPreferredProvider(any(), any(), any(), any()))
                    .thenReturn(ChatModelProvider.ChatResponse.builder().success(true)
                            .content("{\"answer\":\"已完成\"}").build());
            when(masking.restore("masked-1", "{\"answer\":\"已完成\"}"))
                    .thenReturn("{\"answer\":\"已完成\"}");
            run.setId(31L);
            WorkflowExecutionResult success = service.executeWorkbenchWorkflow(run, definition, version, config);
            assertTrue(success.isSuccess());
            assertTrue(!success.getContent().contains("分析："));
            assertEquals(List.of("a", "b"), success.getToolOrigins().stream()
                    .map(WorkflowExecutionResult.ToolOrigin::nodeKey).toList());
            assertEquals(null, success.getResultCount());
            run.setId(32L);
            run.setInputJson("{\"query\":\"分析科目\"}");
            assertTrue(service.executeWorkbenchWorkflow(run, definition, version, config)
                    .getContent().contains("分析：已完成"));
        } finally {
            TenantContext.clear();
            branches.shutdown();
        }
    }

    @Test
    void workbenchRunExecutesControlledQueryMaskModelAndRestore() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        WorkflowSelectorService selector = mock(WorkflowSelectorService.class);
        ToolRuntimeService toolRuntime = mock(ToolRuntimeService.class);
        DesensitizationService desensitizationService = mock(DesensitizationService.class);
        ModelRoutingService modelRoutingService = mock(ModelRoutingService.class);
        ArchiveAgentRunMapper runMapper = mock(ArchiveAgentRunMapper.class);
        ArchiveAgentRunStepMapper stepMapper = mock(ArchiveAgentRunStepMapper.class);
        ArchiveAgentDefinitionMapper definitionMapper = mock(ArchiveAgentDefinitionMapper.class);
        QueryAuthorizationService authorizationService = mock(QueryAuthorizationService.class);
        QueryAuthorization authorization = new QueryAuthorization(7L, 7L,
                QueryAuthorizationService.EXPENSE_QUERY,
                List.of(new QueryAuthorization.Grant("SELF", Set.of("GROUP_HQ"), Set.of(7L))),
                List.of(new QueryAuthorization.Grant("SELF", Set.of("GROUP_HQ"), Set.of(7L))), null);
        when(authorizationService.resolve(any(), any(), anyString(), any())).thenReturn(authorization);
        AgentWorkflowRuntimeService service = new AgentWorkflowRuntimeService(
                selector,
                new QueryParameterExtractor(),
                new QueryTemplateVariableMapper(objectMapper),
                toolRuntime,
                new ControlledWorkflowToolInvoker(new QueryParameterExtractor(),
                        new QueryTemplateVariableMapper(objectMapper),
                        new ControlledToolParameterMapper(objectMapper), toolRuntime),
                new ControlledQueryBranchRunner(),
                desensitizationService,
                modelRoutingService,
                runMapper,
                stepMapper,
                definitionMapper,
                objectMapper,
                authorizationService,
                mock(DigitalEmployeeService.class)
        );

        Map<String, Object> config = workflowConfig();
        ArchiveAgentDefinition definition = new ArchiveAgentDefinition();
        definition.setId(10L);
        definition.setOwnerUserId(7L);
        definition.setAgentName("费用查询");
        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setId(20L);
        ArchiveAgentRun run = new ArchiveAgentRun();
        run.setId(30L);
        run.setAgentId(10L);
        run.setTriggerSource(ArchiveAgentSupport.TRIGGER_SOURCE_WORKBENCH_TEST);
        run.setInputJson("{\"query\":\"查询远智的待审批报销单\"}");

        SelectedWorkflow selected = SelectedWorkflow.builder()
                .agentId(10L)
                .agentVersionId(20L)
                .ownerUserId(7L)
                .agentName("费用查询")
                .toolCode(ExpenseDocumentControlledQueryTool.TOOL_CODE)
                .providerCode("LOCAL_7B")
                .model("deepseek-r1:7b")
                .config(config)
                .build();
        Map<String, Object> record = Map.of(
                "documentCode", "EXP001",
                "title", "远智差旅费",
                "amount", 100D,
                "statusLabel", "待审批",
                "entityRef", "EXPENSE_DOCUMENT:1",
                "evidenceRef", "EXPENSE_DOCUMENT:1"
        );
        when(selector.selectConfigured(definition, version, config, null, null)).thenReturn(selected);
        when(toolRuntime.execute(anyString(), any())).thenReturn(ControlledToolResult.builder()
                .success(true)
                .summary("查询到 1 条报销单")
                .records(List.of(record))
                .evidenceRefs(List.of("EXPENSE_DOCUMENT:1"))
                .build());
        when(desensitizationService.desensitizeJsonFields(anyString(), anyString(), any(), any())).thenReturn(DesensitizedData.builder()
                .success(true)
                .mappingId("mapping-1")
                .maskedText("{\"title\":\"[PERSON_001]差旅费\"}")
                .build());
        when(modelRoutingService.chatWithPreferredProvider(any(), any(), any(), any())).thenReturn(
                ChatModelProvider.ChatResponse.builder()
                        .success(true)
                        .model("deepseek-r1:7b")
                        .content("{\"answer\":\"[PERSON_001]有一条待审批报销单\",\"evidenceRefs\":[\"EXPENSE_DOCUMENT:1\"]}")
                        .build()
        );
        when(desensitizationService.restore("mapping-1", "{\"answer\":\"[PERSON_001]有一条待审批报销单\",\"evidenceRefs\":[\"EXPENSE_DOCUMENT:1\"]}"))
                .thenReturn("{\"answer\":\"远智有一条待审批报销单\",\"evidenceRefs\":[\"EXPENSE_DOCUMENT:1\"]}");
        when(definitionMapper.selectById(10L)).thenReturn(definition);

        TenantContext.setTenantId("default");
        TenantContext.setCompanyId("GROUP_HQ");
        TenantContext.setUserId(7L);
        WorkflowExecutionResult result;
        try {
            result = service.executeWorkbenchWorkflow(run, definition, version, config);
        } finally {
            TenantContext.clear();
        }

        assertTrue(result.isSuccess());
        assertEquals("mapping-1", result.getMappingId());
        assertTrue(result.getContent().contains("查询到 1 条报销单"));
        assertTrue(result.getContent().contains("EXPENSE_DOCUMENT:1"));
        Map<?, ?> testOutput = objectMapper.readValue(run.getOutputJson(), Map.class);
        assertEquals(result.getContent(), testOutput.get("testResult"));
        assertTrue(!testOutput.containsKey("queryResultAvailableInFinWork"));
        verify(stepMapper, atLeast(6)).insert(any());
        verify(runMapper, atLeast(2)).updateById(run);
        verify(definitionMapper).updateById(definition);
    }

    private Map<String, Object> workflowConfig() {
        Map<String, Object> workflow = Map.of(
                "nodes", List.of(
                        node("start", "start", Map.of()),
                        node("end", "end", Map.of()),
                        node("tool", "tool", Map.of(
                                "toolCode", ExpenseDocumentControlledQueryTool.TOOL_CODE,
                                "queryTemplateCode", ExpenseDocumentControlledQueryTool.QUERY_TEMPLATE_CODE,
                                "variableMapping", "{\"keyword\":\"{{user.keyword}}\",\"status\":\"{{user.status}}\"}"
                        )),
                        node("llm", "llm", Map.of("promptTemplate", "分析待审批单据"))
                ),
                "edges", List.of(
                        edge("start", "tool"),
                        edge("tool", "llm"),
                        edge("llm", "end")
                )
        );
        return Map.of(
                "workflow", workflow,
                "modelConfig", Map.of("provider", "LOCAL_7B", "model", "deepseek-r1:7b"),
                "promptConfig", Map.of("systemPrompt", "仅处理当前用户数据"),
                "tools", List.of(Map.of("toolCode", ExpenseDocumentControlledQueryTool.TOOL_CODE, "enabled", true))
        );
    }

    private Map<String, Object> node(String key, String type, Map<String, Object> config) {
        return Map.of("nodeKey", key, "nodeType", type, "label", key, "config", config);
    }

    private Map<String, Object> edge(String source, String target) {
        return Map.of("source", source, "target", target);
    }
}
