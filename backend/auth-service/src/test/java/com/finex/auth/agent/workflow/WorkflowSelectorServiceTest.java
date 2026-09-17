package com.finex.auth.agent.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.mapper.ArchiveAgentDefinitionMapper;
import com.finex.auth.mapper.ArchiveAgentToolBindingMapper;
import com.finex.auth.mapper.ArchiveAgentVersionMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowSelectorServiceTest {

    @Test
    void selectsToolFromPublishedCanvasPath() throws Exception {
        ArchiveAgentDefinitionMapper definitionMapper = mock(ArchiveAgentDefinitionMapper.class);
        ArchiveAgentVersionMapper versionMapper = mock(ArchiveAgentVersionMapper.class);
        ArchiveAgentToolBindingMapper bindingMapper = mock(ArchiveAgentToolBindingMapper.class);
        ToolRuntimeService toolRuntime = mock(ToolRuntimeService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        WorkflowSelectorService selector = new WorkflowSelectorService(
                definitionMapper, versionMapper, bindingMapper, objectMapper, toolRuntime
        );

        ArchiveAgentDefinition definition = new ArchiveAgentDefinition();
        definition.setId(11L);
        definition.setOwnerUserId(7L);
        definition.setAgentName("费用查询");
        definition.setStatus("READY");
        definition.setPublishedVersionId(21L);

        Map<String, Object> workflow = Map.of(
                "nodes", List.of(
                        node("start", "start", Map.of()),
                        node("end", "end", Map.of()),
                        node("llm", "llm", Map.of()),
                        node("tool", "tool", Map.of(
                                "toolCode", ExpenseDocumentControlledQueryTool.TOOL_CODE,
                                "queryTemplateCode", ExpenseDocumentControlledQueryTool.QUERY_TEMPLATE_CODE
                        ))
                ),
                "edges", List.of(
                        edge("start", "tool"),
                        edge("tool", "llm"),
                        edge("llm", "end")
                )
        );
        Map<String, Object> config = Map.of(
                "workflow", workflow,
                "modelConfig", Map.of("provider", "LOCAL_7B", "model", "deepseek-r1:7b"),
                "tools", List.of(Map.of("toolCode", ExpenseDocumentControlledQueryTool.TOOL_CODE, "enabled", true))
        );
        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setId(21L);
        version.setPublished(1);
        version.setConfigJson(objectMapper.writeValueAsString(config));

        when(definitionMapper.selectList(any())).thenReturn(List.of(definition));
        when(versionMapper.selectById(21L)).thenReturn(version);
        when(toolRuntime.supports(ExpenseDocumentControlledQueryTool.TOOL_CODE)).thenReturn(true);

        SelectedWorkflow selected = selector.select(7L, "query_expense", "查询报销单", null, null);

        assertNotNull(selected);
        assertEquals(ExpenseDocumentControlledQueryTool.TOOL_CODE, selected.getToolCode());
        assertEquals("LOCAL_7B", selected.getProviderCode());
    }

    private Map<String, Object> node(String key, String type, Map<String, Object> config) {
        return Map.of("nodeKey", key, "nodeType", type, "label", key, "config", config);
    }

    private Map<String, Object> edge(String source, String target) {
        return Map.of("source", source, "target", target);
    }
}
