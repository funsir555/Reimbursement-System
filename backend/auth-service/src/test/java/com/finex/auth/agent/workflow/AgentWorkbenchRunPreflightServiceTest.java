package com.finex.auth.agent.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.model.ModelRoutingService;
import com.finex.auth.agent.model.ChatModelProvider;
import com.finex.auth.dto.ArchiveAgentRunDTO;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.mapper.ArchiveAgentDefinitionMapper;
import com.finex.auth.mapper.ArchiveAgentVersionMapper;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.port.FinanceAgentQueryPort;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentWorkbenchRunPreflightServiceTest {
    private final ArchiveAgentDefinitionMapper definitions = mock(ArchiveAgentDefinitionMapper.class);
    private final ArchiveAgentVersionMapper versions = mock(ArchiveAgentVersionMapper.class);
    private final WorkflowSelectorService selector = mock(WorkflowSelectorService.class);
    private final QueryAuthorizationService authorization = mock(QueryAuthorizationService.class);
    private final ModelRoutingService models = mock(ModelRoutingService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final FinanceAgentQueryPort finance = mock(FinanceAgentQueryPort.class);
    private final ObjectMapper json = new ObjectMapper();
    private final AgentWorkbenchRunPreflightService service = new AgentWorkbenchRunPreflightService(
            definitions, versions, selector, authorization, models, jdbc, json, finance);

    @Test
    void balanceExportWithoutCapabilityDoesNotCallModelOrCreateRun() {
        ArchiveAgentDefinition agent = agent();
        when(definitions.selectById(3L)).thenReturn(agent);
        when(definitions.selectList(any())).thenReturn(List.of(agent));
        AgentWorkbenchRunPreflightService.Result result = service.prepare(1L, 3L, Map.of(
                "query", "帮我导出黄埔远智的2022年12月的余额表", "context", "包含未记账凭证"));
        assertEquals("NO_CAPABILITY", result.mode());
        assertEquals(true, result.message().contains("余额表导出能力"));
        verifyNoInteractions(models);
    }

    @Test
    void directRunWithMissingCompanyIsRejectedBeforeQueueing() throws Exception {
        ArchiveAgentDefinition agent = agent();
        ArchiveAgentVersion version = version();
        when(definitions.selectById(3L)).thenReturn(agent);
        when(versions.selectOne(any())).thenReturn(version);
        when(selector.containsControlledTool(any())).thenReturn(true);
        when(selector.selectConfigured(any(), any(), any(), any(), any())).thenReturn(
                SelectedWorkflow.builder().config(json.readValue(version.getConfigJson(), Map.class))
                        .toolCode("finance.query_sequence_entries")
                        .toolCodes(List.of("finance.query_sequence_entries"))
                        .queryPermissionCode("finance:general_ledger:sequence_ledger:view")
                        .queryPermissionCodes(List.of("finance:general_ledger:sequence_ledger:view")).build());
        ArchiveAgentRunDTO dto = new ArchiveAgentRunDTO();
        dto.setInputPayload(Map.of("query", "查询2022年12月序时账", "parameters", Map.of()));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> service.validateSubmission(1L, 3L, dto));
        assertEquals(true, failure.getMessage().contains("缺少账套"));
        verifyNoInteractions(jdbc);
    }

    @Test
    void modelParametersNeedConfirmedAccountSetBeforeRun() throws Exception {
        ArchiveAgentDefinition agent = agent();
        ArchiveAgentVersion version = version();
        when(definitions.selectById(3L)).thenReturn(agent);
        when(definitions.selectList(any())).thenReturn(List.of(agent));
        when(versions.selectOne(any())).thenReturn(version);
        when(selector.selectConfigured(any(), any(), any(), any(), any())).thenReturn(workflow(version));
        when(authorization.resolveForSelf(1L, "finance:general_ledger:sequence_ledger:view"))
                .thenReturn(grant());
        when(jdbc.query(any(String.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of(new AgentWorkbenchRunPreflightService.CompanyOption("COMPANY1", "黄埔远智")));
        when(jdbc.queryForObject(any(String.class), any(Class.class), any(Object.class)))
                .thenReturn(1);
        when(models.chatWithFallback(any(), any(), any())).thenReturn(ChatModelProvider.ChatResponse.builder()
                .success(true).content("""
                {"mode":"EXECUTE","versionId":31,"parameters":{"tool":{
                  "yearFrom":2022,"periodFrom":12,"yearTo":2022,"periodTo":12,
                  "voucherStatus":"INCLUDE_SAVED_UNPOSTED"}}}
                """).build());
        Map<String, Object> question = Map.of("query", "查询2022年12月序时账", "context", "包含未记账凭证");
        var clarification = service.prepare(1L, 3L, question);
        assertEquals("CLARIFY", clarification.mode());
        assertEquals(1, clarification.companyOptions().size());
        Map<String, Object> chosen = new java.util.LinkedHashMap<>(question);
        chosen.put("selectedCompanyId", "COMPANY1");
        var ready = service.prepare(1L, 3L, chosen);
        assertEquals("READY", ready.mode());
        assertEquals("COMPANY1", ready.parameters().get("tool").get("companyId"));
    }

    @Test
    void directRunRejectsConflictingVoucherScopeBeforeQueueing() throws Exception {
        ArchiveAgentDefinition agent = agent();
        ArchiveAgentVersion version = version();
        when(definitions.selectById(3L)).thenReturn(agent);
        when(versions.selectOne(any())).thenReturn(version);
        when(selector.containsControlledTool(any())).thenReturn(true);
        when(selector.selectConfigured(any(), any(), any(), any(), any())).thenReturn(workflow(version));
        when(authorization.resolveForSelf(1L, "finance:general_ledger:sequence_ledger:view"))
                .thenReturn(grant());
        when(jdbc.queryForObject(any(String.class), any(Class.class), any(Object.class)))
                .thenReturn(1);
        ArchiveAgentRunDTO dto = new ArchiveAgentRunDTO();
        dto.setInputPayload(Map.of("query", "查询2022年12月序时账", "context", "包含未记账凭证",
                "parameters", Map.of("tool", Map.of("companyId", "COMPANY1", "yearFrom", 2022,
                        "periodFrom", 12, "yearTo", 2022, "periodTo", 12,
                        "voucherStatus", "POSTED_ONLY"))));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> service.validateSubmission(1L, 3L, dto));
        assertEquals(true, failure.getMessage().contains("凭证范围与问题不一致"));
    }

    @Test
    void directRunRejectsInvalidPeriodBeforeQueueing() throws Exception {
        ArchiveAgentDefinition agent = agent();
        ArchiveAgentVersion version = version();
        when(definitions.selectById(3L)).thenReturn(agent);
        when(versions.selectOne(any())).thenReturn(version);
        when(selector.containsControlledTool(any())).thenReturn(true);
        when(selector.selectConfigured(any(), any(), any(), any(), any())).thenReturn(workflow(version));
        when(authorization.resolveForSelf(1L, "finance:general_ledger:sequence_ledger:view"))
                .thenReturn(grant());
        when(jdbc.queryForObject(any(String.class), any(Class.class), any(Object.class)))
                .thenReturn(1);
        ArchiveAgentRunDTO dto = new ArchiveAgentRunDTO();
        dto.setInputPayload(Map.of("query", "查询序时账", "parameters", Map.of("tool",
                Map.of("companyId", "COMPANY1", "yearFrom", 2022, "periodFrom", 13,
                        "yearTo", 2022, "periodTo", 12, "voucherStatus", "POSTED_ONLY"))));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> service.validateSubmission(1L, 3L, dto));
        assertEquals(true, failure.getMessage().contains("会计期间无效"));
    }

    @Test
    void selectsAnotherOwnedPublishedWorkflowWhenCurrentCanvasDoesNotMatch() throws Exception {
        ArchiveAgentDefinition current = agent();
        ArchiveAgentVersion currentVersion = version();
        ArchiveAgentDefinition expense = agent();
        expense.setId(4L);
        expense.setAgentName("单据查询工具");
        expense.setPublishedVersionId(32L);
        ArchiveAgentVersion expenseVersion = new ArchiveAgentVersion();
        expenseVersion.setId(32L);
        expenseVersion.setAgentId(4L);
        expenseVersion.setConfigJson(json.writeValueAsString(Map.of("workflow", Map.of(
                "nodes", List.of(node("start", "start", Map.of()),
                        node("expenseTool", "tool", Map.of("toolCode", "expense.query_my_expenses")),
                        node("llm", "llm", Map.of()), node("end", "end", Map.of())),
                "edges", List.of(edge("start", "expenseTool"), edge("expenseTool", "llm"), edge("llm", "end"))))));
        SelectedWorkflow expenseWorkflow = SelectedWorkflow.builder()
                .config(json.readValue(expenseVersion.getConfigJson(), Map.class))
                .toolCode("expense.query_my_expenses")
                .toolCodes(List.of("expense.query_my_expenses"))
                .queryPermissionCode(QueryAuthorizationService.EXPENSE_QUERY)
                .queryPermissionCodes(List.of(QueryAuthorizationService.EXPENSE_QUERY)).build();
        when(definitions.selectById(3L)).thenReturn(current);
        when(definitions.selectList(any())).thenReturn(List.of(current, expense));
        when(versions.selectOne(any())).thenReturn(currentVersion);
        when(versions.selectById(32L)).thenReturn(expenseVersion);
        when(selector.selectConfigured(any(), any(), any(), any(), any())).thenAnswer(invocation ->
                ((ArchiveAgentDefinition) invocation.getArgument(0)).getId().equals(4L)
                        ? expenseWorkflow : workflow(currentVersion));
        when(authorization.resolveForSelf(1L, "finance:general_ledger:sequence_ledger:view"))
                .thenReturn(grant());
        when(authorization.resolveForSelf(1L, QueryAuthorizationService.EXPENSE_QUERY))
                .thenReturn(grant());
        when(models.chatWithFallback(any(), any(), any())).thenReturn(ChatModelProvider.ChatResponse.builder()
                .success(true).content("{\"mode\":\"EXECUTE\",\"versionId\":32,\"parameters\":{}}")
                .build());
        var result = service.prepare(1L, 3L, Map.of("query", "我的报销单有哪些"));
        assertEquals("READY", result.mode());
        assertEquals(4L, result.agentId());
        assertEquals(32L, result.versionId());
        assertEquals(true, result.usePublishedVersion());
    }

    @Test
    void explicitDifferentAccountSetsAreCheckedForTheirOwnTools() throws Exception {
        ArchiveAgentDefinition agent = agent();
        ArchiveAgentVersion version = version();
        version.setConfigJson(json.writeValueAsString(Map.of("workflow", Map.of(
                "nodes", List.of(node("start", "start", Map.of()),
                        node("sequence", "tool", Map.of("toolCode", "finance.query_sequence_entries")),
                        node("customer", "tool", Map.of("toolCode", "finance.query_customers")),
                        node("people", "tool", Map.of("toolCode", "system.query_people")),
                        node("llm", "llm", Map.of()), node("end", "end", Map.of())),
                "edges", List.of(edge("start", "sequence"), edge("sequence", "customer"),
                        edge("customer", "people"), edge("people", "llm"), edge("llm", "end"))))));
        SelectedWorkflow workflow = SelectedWorkflow.builder()
                .config(json.readValue(version.getConfigJson(), Map.class))
                .toolCode("finance.query_sequence_entries")
                .toolCodes(List.of("finance.query_sequence_entries", "finance.query_customers",
                        "system.query_people"))
                .queryPermissionCodes(List.of("finance:general_ledger:sequence_ledger:view",
                        "finance:agent:customers:query", "system:agent:people:query")).build();
        when(definitions.selectById(3L)).thenReturn(agent);
        when(definitions.selectList(any())).thenReturn(List.of(agent));
        when(versions.selectOne(any())).thenReturn(version);
        when(selector.selectConfigured(any(), any(), any(), any(), any())).thenReturn(workflow);
        when(authorization.resolveForSelf(1L, "finance:general_ledger:sequence_ledger:view"))
                .thenReturn(grant("finance:general_ledger:sequence_ledger:view", "COMPANY1"));
        when(authorization.resolveForSelf(1L, "finance:agent:customers:query"))
                .thenReturn(grant("finance:agent:customers:query", "COMPANY2"));
        when(authorization.resolveForSelf(1L, "system:agent:people:query"))
                .thenReturn(grant("system:agent:people:query", "COMPANY1"));
        when(jdbc.query(any(String.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of(new AgentWorkbenchRunPreflightService.CompanyOption("COMPANY1", "公司 A"),
                        new AgentWorkbenchRunPreflightService.CompanyOption("COMPANY2", "公司 B")));
        when(jdbc.queryForObject(any(String.class), any(Class.class), any(Object.class)))
                .thenReturn(1);
        when(models.chatWithFallback(any(), any(), any())).thenReturn(ChatModelProvider.ChatResponse.builder()
                .success(true).content("{\"mode\":\"EXECUTE\",\"versionId\":31,\"parameters\":{}}")
                .build());
        var result = service.prepare(1L, 3L, Map.of("query", "查询公司 A 的序时账及公司 B 的客户",
                "context", "仅已记账", "parameters", Map.of(
                        "sequence", Map.of("companyId", "COMPANY1", "yearFrom", 2022, "periodFrom", 12,
                                "yearTo", 2022, "periodTo", 12, "voucherStatus", "POSTED_ONLY"),
                        "customer", Map.of("companyId", "COMPANY2"),
                        "people", Map.of("name", "张三"))));
        assertEquals("READY", result.mode());
        assertEquals("COMPANY1", result.parameters().get("sequence").get("companyId"));
        assertEquals("COMPANY2", result.parameters().get("customer").get("companyId"));
        assertEquals(false, result.parameters().get("people").containsKey("companyId"));
    }

    @Test
    void supplierNameOverridesInventedModelCodeAndDirectRunRechecksIt() throws Exception {
        supplierSetup("{\"mode\":\"EXECUTE\",\"versionId\":31,\"parameters\":{\"supplier\":{"
                + "\"companyId\":\"COMPANY1\",\"supplierCode\":\"INVENTED\","
                + "\"yearFrom\":2022,\"periodFrom\":12,\"yearTo\":2022,\"periodTo\":12,"
                + "\"voucherStatus\":\"INCLUDE_SAVED_UNPOSTED\"}}}");
        var actual = new FinanceAgentQueryPort.Supplier("40036", "供应商6");
        when(finance.suppliers(any(), org.mockito.ArgumentMatchers.eq("COMPANY1"),
                org.mockito.ArgumentMatchers.eq("供应商6"))).thenReturn(List.of(actual));
        when(finance.suppliers(any(), org.mockito.ArgumentMatchers.eq("COMPANY1"),
                org.mockito.ArgumentMatchers.eq("40036"))).thenReturn(List.of(actual));

        var ready = service.prepare(1L, 3L, Map.of("query",
                "查询黄埔远智2022年12月跟供应商6所有往来明细，包含已保存未记账凭证",
                "selectedCompanyId", "COMPANY1"));
        assertEquals("READY", ready.mode());
        assertEquals("40036", ready.parameters().get("supplier").get("supplierCode"));

        ArchiveAgentRunDTO dto = new ArchiveAgentRunDTO();
        dto.setExpectedVersionId(31L);
        dto.setInputPayload(Map.of("query", "查询供应商明细", "parameters", Map.of("supplier", Map.of(
                "companyId", "COMPANY1", "supplierCode", "INVENTED", "yearFrom", 2022,
                "periodFrom", 12, "yearTo", 2022, "periodTo", 12,
                "voucherStatus", "POSTED_ONLY"))));
        assertThrows(IllegalArgumentException.class, () -> service.validateSubmission(1L, 3L, dto));
    }

    @Test
    void duplicateSupplierNamesOfferChoicesAndValidateSelectionOnRetry() throws Exception {
        supplierSetup("{\"mode\":\"EXECUTE\",\"versionId\":31,\"parameters\":{\"supplier\":{"
                + "\"companyId\":\"COMPANY1\",\"yearFrom\":2022,\"periodFrom\":12,"
                + "\"yearTo\":2022,\"periodTo\":12,\"voucherStatus\":\"POSTED_ONLY\"}}}");
        var first = new FinanceAgentQueryPort.Supplier("V1", "甲公司");
        var second = new FinanceAgentQueryPort.Supplier("V2", "甲公司");
        when(finance.suppliers(any(), org.mockito.ArgumentMatchers.eq("COMPANY1"),
                org.mockito.ArgumentMatchers.eq("甲公司"))).thenReturn(List.of(first, second));
        when(finance.suppliers(any(), org.mockito.ArgumentMatchers.eq("COMPANY1"),
                org.mockito.ArgumentMatchers.eq("V2"))).thenReturn(List.of(second));

        var question = Map.<String, Object>of("query", "查询黄埔远智供应商：甲公司的明细，仅已记账",
                "selectedCompanyId", "COMPANY1");
        var clarification = service.prepare(1L, 3L, question);
        assertEquals("CLARIFY", clarification.mode());
        assertEquals("supplier", clarification.supplierNodeKey());
        assertEquals(List.of(new AgentWorkbenchRunPreflightService.SupplierOption("V1", "甲公司"),
                new AgentWorkbenchRunPreflightService.SupplierOption("V2", "甲公司")),
                clarification.supplierOptions());

        var chosen = new java.util.LinkedHashMap<>(question);
        chosen.put("selectedVersionId", 31L);
        chosen.put("selectedSupplierCodes", Map.of("supplier", "V2"));
        var ready = service.prepare(1L, 3L, chosen);
        assertEquals("READY", ready.mode());
        assertEquals("V2", ready.parameters().get("supplier").get("supplierCode"));
    }

    @Test
    void modelRequestForSupplierCodeCannotBypassServerNameResolution() throws Exception {
        supplierSetup("{\"mode\":\"CLARIFY\",\"versionId\":31,"
                + "\"question\":\"请提供供应商编码\",\"parameters\":{\"supplier\":{"
                + "\"companyId\":\"COMPANY1\",\"yearFrom\":2022,\"periodFrom\":12,"
                + "\"yearTo\":2022,\"periodTo\":12,\"voucherStatus\":\"POSTED_ONLY\"}}}");
        var supplier = new FinanceAgentQueryPort.Supplier("40036", "供应商6");
        when(finance.suppliers(any(), org.mockito.ArgumentMatchers.eq("COMPANY1"),
                org.mockito.ArgumentMatchers.eq("供应商6"))).thenReturn(List.of(supplier));
        when(finance.suppliers(any(), org.mockito.ArgumentMatchers.eq("COMPANY1"),
                org.mockito.ArgumentMatchers.eq("40036"))).thenReturn(List.of(supplier));

        var result = service.prepare(1L, 3L, Map.of("query", "查询黄埔远智2022年12月供应商6明细，仅已记账",
                "selectedCompanyId", "COMPANY1"));

        assertEquals("READY", result.mode());
        assertEquals("40036", result.parameters().get("supplier").get("supplierCode"));
    }

    @Test
    void soleSupplierLedgerIsStillCheckedWhenModelIncorrectlyReportsNoCapability() throws Exception {
        supplierSetup("{\"mode\":\"NO_CAPABILITY\",\"parameters\":{\"supplier\":{"
                + "\"companyId\":\"COMPANY1\",\"yearFrom\":2022,\"periodFrom\":12,"
                + "\"yearTo\":2022,\"periodTo\":12,\"voucherStatus\":\"POSTED_ONLY\"}}}");
        var supplier = new FinanceAgentQueryPort.Supplier("40036", "供应商6");
        when(finance.suppliers(any(), org.mockito.ArgumentMatchers.eq("COMPANY1"),
                org.mockito.ArgumentMatchers.eq("供应商6"))).thenReturn(List.of(supplier));
        when(finance.suppliers(any(), org.mockito.ArgumentMatchers.eq("COMPANY1"),
                org.mockito.ArgumentMatchers.eq("40036"))).thenReturn(List.of(supplier));

        var result = service.prepare(1L, 3L, Map.of("query", "查询黄埔远智2022年12月供应商6明细，仅已记账",
                "selectedCompanyId", "COMPANY1"));

        assertEquals("READY", result.mode());
        assertEquals("40036", result.parameters().get("supplier").get("supplierCode"));
    }

    private void supplierSetup(String modelOutput) throws Exception {
        ArchiveAgentDefinition agent = agent();
        ArchiveAgentVersion version = version();
        version.setConfigJson(json.writeValueAsString(Map.of("workflow", Map.of(
                "nodes", List.of(node("start", "start", Map.of()),
                        node("supplier", "tool", Map.of("toolCode", "finance.query_supplier_detail_ledger")),
                        node("llm", "llm", Map.of()), node("end", "end", Map.of())),
                "edges", List.of(edge("start", "supplier"), edge("supplier", "llm"), edge("llm", "end"))))));
        SelectedWorkflow workflow = SelectedWorkflow.builder()
                .config(json.readValue(version.getConfigJson(), Map.class))
                .agentVersionId(31L).toolCode("finance.query_supplier_detail_ledger")
                .toolCodes(List.of("finance.query_supplier_detail_ledger"))
                .queryPermissionCode("finance:general_ledger:supplier_detail_ledger:view")
                .queryPermissionCodes(List.of("finance:general_ledger:supplier_detail_ledger:view")).build();
        when(definitions.selectById(3L)).thenReturn(agent);
        when(definitions.selectList(any())).thenReturn(List.of(agent));
        when(versions.selectOne(any())).thenReturn(version);
        when(selector.selectConfigured(any(), any(), any(), any(), any())).thenReturn(workflow);
        when(selector.containsControlledTool(any())).thenReturn(true);
        when(authorization.resolveForSelf(1L, "finance:general_ledger:supplier_detail_ledger:view"))
                .thenReturn(grant("finance:general_ledger:supplier_detail_ledger:view", "COMPANY1"));
        when(jdbc.query(any(String.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of(new AgentWorkbenchRunPreflightService.CompanyOption("COMPANY1", "黄埔远智")));
        when(jdbc.queryForObject(any(String.class), any(Class.class), any(Object.class))).thenReturn(1);
        when(models.chatWithFallback(any(), any(), any())).thenReturn(ChatModelProvider.ChatResponse.builder()
                .success(true).content(modelOutput).build());
    }

    private SelectedWorkflow workflow(ArchiveAgentVersion version) throws Exception {
        return SelectedWorkflow.builder().config(json.readValue(version.getConfigJson(), Map.class))
                .agentVersionId(version.getId()).toolCode("finance.query_sequence_entries")
                .toolCodes(List.of("finance.query_sequence_entries"))
                .queryPermissionCode("finance:general_ledger:sequence_ledger:view")
                .queryPermissionCodes(List.of("finance:general_ledger:sequence_ledger:view")).build();
    }
    private QueryAuthorization grant() {
        return grant("finance:general_ledger:sequence_ledger:view", "COMPANY1");
    }
    private QueryAuthorization grant(String code, String companyId) {
        var role = new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of(companyId), null);
        return new QueryAuthorization(1L, 1L, code,
                List.of(role), List.of(role), null);
    }

    private ArchiveAgentDefinition agent() {
        ArchiveAgentDefinition agent = new ArchiveAgentDefinition();
        agent.setId(3L);
        agent.setOwnerUserId(1L);
        agent.setAgentName("明细账查询v1");
        agent.setStatus("READY");
        return agent;
    }
    private ArchiveAgentVersion version() throws Exception {
        ArchiveAgentVersion version = new ArchiveAgentVersion();
        version.setId(31L);
        version.setAgentId(3L);
        version.setConfigJson(json.writeValueAsString(Map.of("workflow", Map.of(
                "nodes", List.of(node("start", "start", Map.of()),
                        node("tool", "tool", Map.of("toolCode", "finance.query_sequence_entries")),
                        node("llm", "llm", Map.of()), node("end", "end", Map.of())),
                "edges", List.of(edge("start", "tool"), edge("tool", "llm"), edge("llm", "end"))))));
        return version;
    }
    private Map<String, Object> node(String key, String type, Map<String, Object> config) {
        return Map.of("nodeKey", key, "nodeType", type, "config", config);
    }
    private Map<String, String> edge(String source, String target) {
        return Map.of("source", source, "target", target);
    }
}
