package com.finex.auth.agent.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.mapper.ArchiveAgentDefinitionMapper;
import com.finex.auth.mapper.ArchiveAgentToolBindingMapper;
import com.finex.auth.mapper.ArchiveAgentVersionMapper;
import com.finex.auth.service.digitalemployee.DigitalEmployeeService;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowSelectorServiceTest {

    @Test
    void acceptsPublishedParallelToolsAndKeepsEveryPermission() {
        ToolRuntimeService runtime = mock(ToolRuntimeService.class);
        WorkflowSelectorService selector = new WorkflowSelectorService(
                mock(ArchiveAgentDefinitionMapper.class), mock(ArchiveAgentVersionMapper.class),
                mock(ArchiveAgentToolBindingMapper.class), new ObjectMapper(), runtime,
                mock(DigitalEmployeeService.class), mock(QueryAuthorizationService.class),
                mock(org.springframework.jdbc.core.JdbcTemplate.class));
        ArchiveAgentDefinition definition = new ArchiveAgentDefinition();
        definition.setId(11L);
        definition.setAgentName("并行财务查询");
        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setId(21L);
        List<String> codes = List.of("finance.query_account_subjects", "finance.query_project_classes",
                "finance.query_projects");
        codes.forEach(code -> when(runtime.supports(code)).thenReturn(true));
        Map<String, Object> config = Map.of("workflow", Map.of(
                "nodes", List.of(node("start", "start", Map.of()),
                        node("subject", "tool", Map.of("toolCode", codes.get(0),
                                "queryTemplateCode", ControlledQueryCatalog.require(codes.get(0)).template())),
                        node("class", "tool", Map.of("toolCode", codes.get(1),
                                "queryTemplateCode", ControlledQueryCatalog.require(codes.get(1)).template())),
                        node("project", "tool", Map.of("toolCode", codes.get(2),
                                "queryTemplateCode", ControlledQueryCatalog.require(codes.get(2)).template())),
                        node("llm", "llm", Map.of()), node("end", "end", Map.of())),
                "edges", List.of(edge("start", "subject"), edge("start", "class"),
                        edge("start", "project"), edge("subject", "llm"),
                        edge("class", "llm"), edge("project", "llm"), edge("llm", "end"))),
                "tools", codes.stream().map(code -> Map.<String, Object>of("toolCode", code, "enabled", true)).toList(),
                "modelConfig", Map.of("provider", "LOCAL_7B", "model", "test"));
        SelectedWorkflow selected = selector.selectConfigured(definition, version, config, null, null);
        assertEquals(codes, selected.getToolCodes());
        assertEquals(codes.stream().map(code -> ControlledQueryCatalog.require(code).permission()).toList(),
                selected.getQueryPermissionCodes());
    }

    @Test
    void selectsToolFromPublishedCanvasPath() throws Exception {
        ArchiveAgentDefinitionMapper definitionMapper = mock(ArchiveAgentDefinitionMapper.class);
        ArchiveAgentVersionMapper versionMapper = mock(ArchiveAgentVersionMapper.class);
        ArchiveAgentToolBindingMapper bindingMapper = mock(ArchiveAgentToolBindingMapper.class);
        ToolRuntimeService toolRuntime = mock(ToolRuntimeService.class);
        DigitalEmployeeService employees = mock(DigitalEmployeeService.class);
        QueryAuthorizationService authorizationService = mock(QueryAuthorizationService.class);
        org.springframework.jdbc.core.JdbcTemplate jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        WorkflowSelectorService selector = new WorkflowSelectorService(
                definitionMapper, versionMapper, bindingMapper, objectMapper, toolRuntime,
                employees, authorizationService, jdbc
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
        version.setAgentId(11L);
        version.setPublished(1);
        version.setConfigJson(objectMapper.writeValueAsString(config));

        when(employees.availableFor(7L)).thenReturn(List.of(Map.of(
                "id", 101L, "binding_id", 301L, "permission_code", QueryAuthorizationService.EXPENSE_QUERY,
                "responsible_user_id", 7L, "agent_id", 11L, "agent_version_id", 21L)));
        when(employees.companyLimit(101L, QueryAuthorizationService.EXPENSE_QUERY)).thenReturn(List.of());
        when(jdbc.queryForList(any(String.class),
                org.mockito.ArgumentMatchers.eq(String.class), org.mockito.ArgumentMatchers.eq(301L)))
                .thenReturn(List.of(QueryAuthorizationService.EXPENSE_QUERY));
        when(authorizationService.resolve(any(), any(), any(), org.mockito.ArgumentMatchers.nullable(Set.class)))
                .thenReturn(new QueryAuthorization(7L, 7L, QueryAuthorizationService.EXPENSE_QUERY,
                        List.of(new QueryAuthorization.Grant("SELF", Set.of("GROUP_HQ"), Set.of(7L))),
                        List.of(new QueryAuthorization.Grant("SELF", Set.of("GROUP_HQ"), Set.of(7L))), null));
        when(definitionMapper.selectById(11L)).thenReturn(definition);
        when(versionMapper.selectById(21L)).thenReturn(version);
        when(toolRuntime.supports(ExpenseDocumentControlledQueryTool.TOOL_CODE)).thenReturn(true);

        SelectedWorkflow selected = selector.select(7L, "query_expense", "查询报销单", "OTHER_PROVIDER", "other-model");

        assertNotNull(selected);
        assertEquals(ExpenseDocumentControlledQueryTool.TOOL_CODE, selected.getToolCode());
        assertEquals("LOCAL_7B", selected.getProviderCode());
        assertEquals("deepseek-r1:7b", selected.getModel());
    }

    private Map<String, Object> node(String key, String type, Map<String, Object> config) {
        return Map.of("nodeKey", key, "nodeType", type, "label", key, "config", config);
    }

    private Map<String, Object> edge(String source, String target) {
        return Map.of("source", source, "target", target);
    }
}
