package com.finex.auth.agent.workflow;

import com.finex.auth.service.digitalemployee.DigitalEmployeeService;
import com.finex.auth.service.port.FinanceAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FinWorkParameterServiceTest {
    private static final String PERMISSION = "finance:general_ledger:supplier_detail_ledger:view";
    private final QueryAuthorizationService authorizations = mock(QueryAuthorizationService.class);
    private final DigitalEmployeeService employees = mock(DigitalEmployeeService.class);
    private final FinanceAgentQueryPort finance = mock(FinanceAgentQueryPort.class);
    private final FinWorkParameterService service = new FinWorkParameterService(authorizations, employees, finance);

    @Test
    void allPeriodsAreDerivedButVoucherStatusMustBeChosen() {
        prepareAccounts(List.of(account("A", "黄埔远智有限公司")));
        when(finance.suppliers(any(), eq("A"), eq("云之智")))
                .thenReturn(List.of(new FinanceAgentQueryPort.Supplier("V001", "云之智科技")));

        var prepared = service.prepare(9L, "请帮我查询一下供应商云之智在黄埔远智的所有往来明细",
                List.of(step()), Map.of());

        assertThat(prepared.groups()).extracting(FinWorkParameterService.Group::key)
                .containsExactly("voucherStatus");
        Map<?, ?> values = (Map<?, ?>) prepared.steps().get(0).parameters().get("supplier");
        assertThat(values.get("companyId")).isEqualTo("A");
        assertThat(values.get("supplierCode")).isEqualTo("V001");
        assertThat(values.get("yearFrom")).isEqualTo(2022);
        assertThat(values.get("periodFrom")).isEqualTo(1);
        assertThat(values.get("yearTo")).isEqualTo(2026);
        assertThat(values.get("periodTo")).isEqualTo(10);
    }

    @Test
    void selectedVoucherStatusCompletesTheSameAuthorizedPlan() {
        prepareAccounts(List.of(account("A", "黄埔远智有限公司")));
        when(finance.suppliers(any(), eq("A"), eq("云之智")))
                .thenReturn(List.of(new FinanceAgentQueryPort.Supplier("V001", "云之智科技")));

        var prepared = service.prepare(9L, "请查询供应商云之智在黄埔远智的所有往来明细\n用户已确认参数：{companyId=COMPANY202604050001}",
                List.of(step()), Map.of("voucherStatus", "POSTED_ONLY"));

        assertThat(prepared.groups()).isEmpty();
        Map<?, ?> values = (Map<?, ?>) prepared.steps().get(0).parameters().get("supplier");
        assertThat(values.get("voucherStatus")).isEqualTo("POSTED_ONLY");
    }

    @Test
    void ambiguousAccountNameProducesAuthorizedChoices() {
        prepareAccounts(List.of(account("A", "黄埔远智一部"), account("B", "黄埔远智二部")));

        var prepared = service.prepare(9L, "请查询供应商云之智在黄埔远智的所有往来明细",
                List.of(step()), Map.of());

        assertThat(prepared.groups()).extracting(FinWorkParameterService.Group::key)
                .contains("companyId", "voucherStatus");
        assertThat(prepared.groups().get(0).options()).extracting(FinWorkParameterService.Choice::id)
                .containsExactly("A", "B");
    }

    @Test
    void companyAndVoucherStatusCanBeAnsweredInOneTurn() {
        prepareAccounts(List.of(account("A", "黄埔远智一部"), account("B", "黄埔远智二部")));
        when(finance.suppliers(any(), eq("B"), eq("云之智")))
                .thenReturn(List.of(new FinanceAgentQueryPort.Supplier("V002", "云之智科技")));

        var prepared = service.prepare(9L, "请查询供应商云之智在黄埔远智的所有往来明细",
                List.of(step()), Map.of("companyId", "B", "voucherStatus", "INCLUDE_SAVED_UNPOSTED"));

        assertThat(prepared.groups()).isEmpty();
        Map<?, ?> values = (Map<?, ?>) prepared.steps().get(0).parameters().get("supplier");
        assertThat(values.get("companyId")).isEqualTo("B");
        assertThat(values.get("supplierCode")).isEqualTo("V002");
        assertThat(values.get("voucherStatus")).isEqualTo("INCLUDE_SAVED_UNPOSTED");
    }

    @Test
    void separateWorkflowsCanChooseDifferentAuthorizedCompanies() {
        var first = step();
        var secondWorkflow = SelectedWorkflow.builder().workflowBindingId(8L).agentVersionId(18L)
                .digitalEmployeeId(5L).responsibleUserId(10L).queryPermissionCode(PERMISSION)
                .queryPermissionCodes(List.of(PERMISSION)).config(first.workflow().getConfig()).build();
        var second = new FinWorkQueryPlanner.PlannedStep(secondWorkflow, "供应商往来明细", Map.of());
        var firstGrant = grant("A", 8L);
        var secondGrant = grant("B", 10L);
        when(employees.companyLimit(4L, PERMISSION)).thenReturn(List.of());
        when(employees.companyLimit(5L, PERMISSION)).thenReturn(List.of());
        when(authorizations.resolve(9L, 8L, PERMISSION, null)).thenReturn(firstGrant);
        when(authorizations.resolve(9L, 10L, PERMISSION, null)).thenReturn(secondGrant);
        when(finance.accountSets(firstGrant)).thenReturn(List.of(account("A", "甲公司")));
        when(finance.accountSets(secondGrant)).thenReturn(List.of(account("B", "乙公司")));
        when(finance.suppliers(any(), eq("A"), eq("云之智")))
                .thenReturn(List.of(new FinanceAgentQueryPort.Supplier("V001", "云之智甲")));
        when(finance.suppliers(any(), eq("B"), eq("云之智")))
                .thenReturn(List.of(new FinanceAgentQueryPort.Supplier("V002", "云之智乙")));

        var prepared = service.prepare(9L, "请查询供应商云之智在黄埔远智的所有往来明细",
                List.of(first, second), Map.of(
                        "7:supplier:companyId", "A", "8:supplier:companyId", "B",
                        "7:supplier:voucherStatus", "POSTED_ONLY",
                        "8:supplier:voucherStatus", "INCLUDE_SAVED_UNPOSTED"));

        assertThat(prepared.groups()).isEmpty();
        assertThat(((Map<?, ?>) prepared.steps().get(0).parameters().get("supplier")).get("companyId"))
                .isEqualTo("A");
        assertThat(((Map<?, ?>) prepared.steps().get(1).parameters().get("supplier")).get("companyId"))
                .isEqualTo("B");
    }

    private void prepareAccounts(List<FinanceAgentQueryPort.AccountSet> accounts) {
        QueryAuthorization grant = grant(Set.of("A", "B"), 8L);
        when(employees.companyLimit(4L, PERMISSION)).thenReturn(List.of());
        when(authorizations.resolve(9L, 8L, PERMISSION, null)).thenReturn(grant);
        when(finance.accountSets(grant)).thenReturn(accounts);
    }

    private FinanceAgentQueryPort.AccountSet account(String id, String name) {
        return new FinanceAgentQueryPort.AccountSet(id, name, 2022, 1, 2026, 10);
    }

    private QueryAuthorization grant(String company, Long responsible) {
        return grant(Set.of(company), responsible);
    }

    private QueryAuthorization grant(Set<String> companies, Long responsible) {
        var scope = new QueryAuthorization.Grant("SELECTED_COMPANIES", companies, null);
        return new QueryAuthorization(9L, responsible, PERMISSION,
                List.of(scope), List.of(scope), null);
    }

    private FinWorkQueryPlanner.PlannedStep step() {
        SelectedWorkflow workflow = SelectedWorkflow.builder().workflowBindingId(7L).agentVersionId(17L)
                .digitalEmployeeId(4L).responsibleUserId(8L).queryPermissionCode(PERMISSION)
                .queryPermissionCodes(List.of(PERMISSION))
                .config(Map.of("workflow", Map.of("nodes", List.of(
                        Map.of("nodeKey", "start", "nodeType", "start"),
                        Map.of("nodeKey", "supplier", "nodeType", "tool", "config", Map.of(
                                "toolCode", "finance.query_supplier_detail_ledger",
                                "queryTemplateCode", "finance.supplier_detail_ledger.v1")),
                        Map.of("nodeKey", "llm", "nodeType", "llm"),
                        Map.of("nodeKey", "end", "nodeType", "end")),
                        "edges", List.of(Map.of("source", "start", "target", "supplier"),
                                Map.of("source", "supplier", "target", "llm"),
                                Map.of("source", "llm", "target", "end")))))
                .build();
        return new FinWorkQueryPlanner.PlannedStep(workflow, "供应商往来明细", Map.of());
    }
}
