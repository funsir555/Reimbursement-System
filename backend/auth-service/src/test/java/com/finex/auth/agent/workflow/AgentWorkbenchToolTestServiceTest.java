package com.finex.auth.agent.workflow;

import com.finex.auth.dto.ArchiveAgentDetailVO;
import com.finex.auth.dto.ArchiveAgentToolTestDTO;
import com.finex.auth.service.ArchiveAgentService;
import com.finex.auth.service.port.FinanceAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentWorkbenchToolTestServiceTest {
    @Mock ArchiveAgentService agents;
    @Mock QueryAuthorizationService authorizations;
    @Mock ToolRuntimeService runtime;
    @Mock FinanceAgentQueryPort finance;

    private ArchiveAgentToolTestDTO request(Map<String, Object> parameters) {
        ArchiveAgentToolTestDTO dto = new ArchiveAgentToolTestDTO();
        dto.setParameters(parameters);
        return dto;
    }

    private void bind(String code) {
        ArchiveAgentDetailVO detail = new ArchiveAgentDetailVO();
        detail.setTools(List.of(Map.of("toolCode", code, "enabled", true)));
        when(agents.getAgentDetail(7L, 9L)).thenReturn(detail);
    }

    @Test
    void expenseTestUsesCurrentBusinessAuthorizationAndOnlyTheControlledRuntime() {
        bind(ExpenseDocumentControlledQueryTool.TOOL_CODE);
        QueryAuthorization grant = new QueryAuthorization(7L, 7L, QueryAuthorizationService.EXPENSE_QUERY,
                List.of(new QueryAuthorization.Grant("SELF", Set.of("C1"), Set.of(7L))),
                List.of(new QueryAuthorization.Grant("SELF", Set.of("C1"), Set.of(7L))), null);
        when(authorizations.resolveForSelf(7L, QueryAuthorizationService.EXPENSE_QUERY)).thenReturn(grant);
        when(runtime.supports(ExpenseDocumentControlledQueryTool.TOOL_CODE)).thenReturn(true);
        when(runtime.execute(eq(ExpenseDocumentControlledQueryTool.TOOL_CODE), any())).thenReturn(
                ControlledToolResult.builder().success(true).summary("授权结果").totalCount(1L)
                        .records(List.of(Map.of("documentCode", "D1"))).evidenceRefs(List.of("D1")).build());

        var result = new AgentWorkbenchToolTestService(agents, authorizations, runtime, finance).test(7L, 9L,
                ExpenseDocumentControlledQueryTool.TOOL_CODE, request(Map.of("scope", "SUBMITTED_BY_ME", "limit", 10)));

        assertEquals(1L, result.get("totalCount"));
        ArgumentCaptor<ToolExecutionRequest> request = ArgumentCaptor.forClass(ToolExecutionRequest.class);
        verify(runtime).execute(eq(ExpenseDocumentControlledQueryTool.TOOL_CODE), request.capture());
        assertEquals(grant, request.getValue().getAuthorization());
        assertEquals("SUBMITTED_BY_ME", request.getValue().getVariables().getScope());
    }

    @Test
    void missingBusinessPermissionPreventsExecution() {
        bind(FinanceUnclosedCompanyTool.TOOL_CODE);
        when(authorizations.resolveForSelf(7L, QueryAuthorizationService.FINANCE_CLOSE_QUERY))
                .thenThrow(new SecurityException(QueryAuthorizationService.NO_PERMISSION));
        assertThrows(SecurityException.class, () -> new AgentWorkbenchToolTestService(agents, authorizations, runtime, finance)
                .test(7L, 9L, FinanceUnclosedCompanyTool.TOOL_CODE,
                        request(Map.of("year", 2026, "period", 9))));
        verify(runtime, never()).execute(any(), any());
    }

    @Test
    void revokingScopeDuringExecutionBlocksTheResponse() {
        bind(FinanceUnclosedCompanyTool.TOOL_CODE);
        QueryAuthorization original = new QueryAuthorization(7L, 7L, QueryAuthorizationService.FINANCE_CLOSE_QUERY,
                List.of(new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of("C1"), null)),
                List.of(new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of("C1"), null)), null);
        when(authorizations.resolveForSelf(7L, QueryAuthorizationService.FINANCE_CLOSE_QUERY))
                .thenReturn(original).thenThrow(new SecurityException(QueryAuthorizationService.NO_PERMISSION));
        when(runtime.execute(eq(FinanceUnclosedCompanyTool.TOOL_CODE), any())).thenReturn(
                ControlledToolResult.builder().success(true).summary("1 家未结账")
                        .records(List.of(Map.of("count", 1))).build());
        assertThrows(SecurityException.class, () -> new AgentWorkbenchToolTestService(agents, authorizations, runtime, finance)
                .test(7L, 9L, FinanceUnclosedCompanyTool.TOOL_CODE,
                        request(Map.of("year", 2026, "period", 9))));
    }

    @Test
    void unboundOrUnsupportedToolCannotExecute() {
        ArchiveAgentDetailVO detail = new ArchiveAgentDetailVO();
        detail.setTools(List.of());
        when(agents.getAgentDetail(7L, 9L)).thenReturn(detail);
        AgentWorkbenchToolTestService service = new AgentWorkbenchToolTestService(agents, authorizations, runtime, finance);
        assertThrows(IllegalArgumentException.class, () -> service.test(7L, 9L,
                ExpenseDocumentControlledQueryTool.TOOL_CODE, request(Map.of())));
        assertThrows(IllegalArgumentException.class, () -> service.test(7L, 9L,
                "notify.send_message", request(Map.of())));
        verify(runtime, never()).execute(any(), any());
    }

    @Test
    void supplierSearchReturnsOnlyFinanceAuthorizedNamesAndCodes() {
        bind("finance.query_supplier_detail_ledger");
        String permission = "finance:general_ledger:supplier_detail_ledger:view";
        QueryAuthorization grant = new QueryAuthorization(7L, 7L, permission,
                List.of(new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of("C1"), null)),
                List.of(new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of("C1"), null)), null);
        when(authorizations.resolveForSelf(7L, permission)).thenReturn(grant);
        when(finance.suppliers(grant, "C1", "供应商6"))
                .thenReturn(List.of(new FinanceAgentQueryPort.Supplier("40036", "供应商6")));

        var found = new AgentWorkbenchToolTestService(agents, authorizations, runtime, finance)
                .searchSuppliers(7L, 9L, "C1", "供应商6");

        assertEquals(List.of(new FinanceAgentQueryPort.Supplier("40036", "供应商6")), found);
        verify(finance).suppliers(grant, "C1", "供应商6");
    }

    @Test
    void supplierSearchRequiresCurrentPermissionAndBoundTool() {
        bind("finance.query_supplier_detail_ledger");
        String permission = "finance:general_ledger:supplier_detail_ledger:view";
        when(authorizations.resolveForSelf(7L, permission))
                .thenThrow(new SecurityException(QueryAuthorizationService.NO_PERMISSION));
        AgentWorkbenchToolTestService service = new AgentWorkbenchToolTestService(
                agents, authorizations, runtime, finance);
        assertThrows(SecurityException.class, () -> service.searchSuppliers(7L, 9L, "C1", "供应商6"));
        verify(finance, never()).suppliers(any(), any(), any());
    }

    @Test
    void supplierSearchDropsResultsWhenPermissionIsRevokedBeforeResponse() {
        bind("finance.query_supplier_detail_ledger");
        String permission = "finance:general_ledger:supplier_detail_ledger:view";
        QueryAuthorization grant = new QueryAuthorization(7L, 7L, permission,
                List.of(new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of("C1"), null)),
                List.of(new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of("C1"), null)), null);
        when(authorizations.resolveForSelf(7L, permission)).thenReturn(grant)
                .thenThrow(new SecurityException(QueryAuthorizationService.NO_PERMISSION));
        when(finance.suppliers(grant, "C1", "供应商6"))
                .thenReturn(List.of(new FinanceAgentQueryPort.Supplier("40036", "供应商6")));

        assertThrows(SecurityException.class, () -> new AgentWorkbenchToolTestService(
                agents, authorizations, runtime, finance).searchSuppliers(7L, 9L, "C1", "供应商6"));
    }
}
