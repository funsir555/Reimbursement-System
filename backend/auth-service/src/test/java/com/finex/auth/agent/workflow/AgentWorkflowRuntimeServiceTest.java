package com.finex.auth.agent.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentWorkflowRuntimeServiceTest {

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
        AgentWorkflowRuntimeService service = new AgentWorkflowRuntimeService(
                selector,
                new QueryParameterExtractor(),
                new QueryTemplateVariableMapper(objectMapper),
                toolRuntime,
                desensitizationService,
                modelRoutingService,
                runMapper,
                stepMapper,
                definitionMapper,
                objectMapper
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

        WorkflowExecutionResult result = service.executeWorkbenchWorkflow(run, definition, version, config);

        assertTrue(result.isSuccess());
        assertEquals("mapping-1", result.getMappingId());
        assertTrue(result.getContent().contains("远智有一条待审批报销单"));
        assertTrue(result.getContent().contains("EXPENSE_DOCUMENT:1"));
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
