package com.finex.auth.agent.workflow;

import com.finex.auth.service.digitalemployee.DigitalEmployeeService;
import com.finex.auth.service.port.FinanceAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.YearMonth;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void periodAfterZaiIsNotMistakenForAnAccountName() {
        prepareAccounts(List.of(account("A", "广州市黄埔区远智自学考试辅导中心")));
        when(finance.suppliers(any(), eq("A"), eq("云之智")))
                .thenReturn(List.of(new FinanceAgentQueryPort.Supplier("V001", "云之智科技")));

        var prepared = service.prepare(9L,
                "查询供应商云之智在2022年12月的黄埔远智往来明细", List.of(step()),
                Map.of("voucherStatus", "POSTED_ONLY"));

        assertThat(prepared.groups()).isEmpty();
        Map<?, ?> values = (Map<?, ?>) prepared.steps().get(0).parameters().get("supplier");
        assertThat(values.get("companyId")).isEqualTo("A");
        assertThat(values.get("periodFrom")).isEqualTo(12);
    }

    @Test
    void uniqueOrderedCompanyShortNameResolvesWithinAuthorizedAccounts() {
        prepareAccounts(List.of(account("A", "广州市黄埔区远智自学考试辅导中心"),
                account("B", "广州市天河区新知咨询中心")));
        when(finance.suppliers(any(), eq("A"), eq("云之智")))
                .thenReturn(List.of(new FinanceAgentQueryPort.Supplier("V001", "云之智科技")));

        var prepared = service.prepare(9L,
                "请查询供应商云之智在黄埔远智的所有往来明细", List.of(step()),
                Map.of("voucherStatus", "POSTED_ONLY"));

        assertThat(prepared.groups()).isEmpty();
        assertThat(((Map<?, ?>) prepared.steps().get(0).parameters().get("supplier"))
                .get("companyId")).isEqualTo("A");
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

    @Test
    void beginningAndEndingWithoutDatesStillAskForPeriod() {
        prepareAccounts(List.of(account("A", "黄埔远智有限公司")));
        when(finance.suppliers(any(), eq("A"), eq("云之智")))
                .thenReturn(List.of(new FinanceAgentQueryPort.Supplier("V001", "云之智科技")));
        var prepared = service.prepare(9L,
                "查询黄埔远智供应商云之智的期初和期末，仅已记账",
                List.of(step()), Map.of());
        assertThat(prepared.groups()).extracting(FinWorkParameterService.Group::key)
                .containsExactly("periodRange");
    }

    @Test
    void modelPeriodWithEvidenceAndNegativeVoucherAreNormalized() {
        prepareAccounts(List.of(account("A", "黄埔远智有限公司")));
        when(finance.suppliers(any(), eq("A"), eq("云之智")))
                .thenReturn(List.of(new FinanceAgentQueryPort.Supplier("V001", "云之智科技")));
        var original = step();
        var modeled = new FinWorkQueryPlanner.PlannedStep(original.workflow(), original.queryText(),
                Map.of("supplier", Map.of("yearFrom", 2025, "periodFrom", 2,
                        "yearTo", 2025, "periodTo", 4,
                        "voucherStatus", "INCLUDE_SAVED_UNPOSTED")),
                Map.of("supplier", Map.of("yearFrom", "2025年2月至4月",
                        "periodFrom", "2025年2月至4月", "yearTo", "2025年2月至4月",
                        "periodTo", "2025年2月至4月", "voucherStatus", "不包含已保存未记账")));
        var prepared = service.prepare(9L,
                "查询黄埔远智供应商云之智，2025年2月至4月，不包含已保存未记账凭证",
                List.of(modeled), Map.of());
        assertThat(prepared.groups()).isEmpty();
        Map<?, ?> values = (Map<?, ?>) prepared.steps().get(0).parameters().get("supplier");
        assertThat(values.get("periodFrom")).isEqualTo(2);
        assertThat(values.get("periodTo")).isEqualTo(4);
        assertThat(values.get("voucherStatus")).isEqualTo("POSTED_ONLY");
    }

    @Test
    void fabricatedPeriodFromBeginningEndingPhraseDoesNotExecute() {
        prepareAccounts(List.of(account("A", "黄埔远智有限公司")));
        when(finance.suppliers(any(), eq("A"), eq("云之智")))
                .thenReturn(List.of(new FinanceAgentQueryPort.Supplier("V001", "云之智科技")));
        var original = step();
        var modeled = new FinWorkQueryPlanner.PlannedStep(original.workflow(), original.queryText(),
                Map.of("supplier", Map.of("yearFrom", 2022, "periodFrom", 1,
                        "yearTo", 2026, "periodTo", 10)),
                Map.of("supplier", Map.of("yearFrom", "期初期末", "periodFrom", "期初期末",
                        "yearTo", "期初期末", "periodTo", "期初期末")));
        var prepared = service.prepare(9L,
                "查询黄埔远智供应商云之智，期初期末，仅已记账", List.of(modeled), Map.of());
        assertThat(prepared.groups()).extracting(FinWorkParameterService.Group::key)
                .containsExactly("periodRange");
    }

    @Test
    void relativeQuarterUsesServerCalendarInsteadOfModelGuess() {
        YearMonth now = YearMonth.now(ZoneId.of("Asia/Shanghai"));
        prepareAccounts(List.of(new FinanceAgentQueryPort.AccountSet("A", "黄埔远智有限公司",
                2020, 1, now.getYear(), now.getMonthValue())));
        when(finance.suppliers(any(), eq("A"), eq("云之智")))
                .thenReturn(List.of(new FinanceAgentQueryPort.Supplier("V001", "云之智科技")));
        var original = step();
        var modeled = new FinWorkQueryPlanner.PlannedStep(original.workflow(), original.queryText(),
                Map.of("supplier", Map.of("yearFrom", 2020, "periodFrom", 1,
                        "yearTo", 2020, "periodTo", 3)),
                Map.of("supplier", Map.of("yearFrom", "去年一季度", "periodFrom", "去年一季度",
                        "yearTo", "去年一季度", "periodTo", "去年一季度")));
        var prepared = service.prepare(9L,
                "查询黄埔远智供应商云之智，去年一季度仅已记账", List.of(modeled), Map.of());
        Map<?, ?> values = (Map<?, ?>) prepared.steps().get(0).parameters().get("supplier");
        assertThat(values.get("yearFrom")).isEqualTo(now.getYear() - 1);
        assertThat(values.get("periodFrom")).isEqualTo(1);
        assertThat(values.get("yearTo")).isEqualTo(now.getYear() - 1);
        assertThat(values.get("periodTo")).isEqualTo(3);
        assertThat(prepared.groups()).isEmpty();
    }

    @Test
    void latestRelativePeriodOverridesPreviousPeriod() {
        YearMonth now = YearMonth.now(ZoneId.of("Asia/Shanghai"));
        prepareAccounts(List.of(new FinanceAgentQueryPort.AccountSet("A", "黄埔远智有限公司",
                2020, 1, now.getYear(), now.getMonthValue())));
        when(finance.suppliers(any(), eq("A"), eq("云之智")))
                .thenReturn(List.of(new FinanceAgentQueryPort.Supplier("V001", "云之智科技")));

        var prepared = service.prepare(9L,
                "查询黄埔远智供应商云之智的上个月明细\n补充说明：改成本月，仅已记账",
                List.of(step()), Map.of());

        Map<?, ?> values = (Map<?, ?>) prepared.steps().get(0).parameters().get("supplier");
        assertThat(values.get("yearFrom")).isEqualTo(now.getYear());
        assertThat(values.get("periodFrom")).isEqualTo(now.getMonthValue());
        assertThat(values.get("voucherStatus")).isEqualTo("POSTED_ONLY");
        assertThat(prepared.groups()).isEmpty();
    }

    @Test
    void resolvesBalanceSubjectNameThroughAuthorizedFinancePort() {
        String permission = "finance:general_ledger:balance_sheet:view";
        QueryAuthorization grant = new QueryAuthorization(9L, 8L, permission,
                List.of(new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of("A"), null)),
                List.of(new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of("A"), null)), null);
        when(employees.companyLimit(4L, permission)).thenReturn(List.of());
        when(authorizations.resolve(9L, 8L, permission, null)).thenReturn(grant);
        when(finance.accountSets(grant)).thenReturn(List.of(account("A", "黄埔远智有限公司")));
        when(finance.subjects(grant, "A", "应收账款"))
                .thenReturn(List.of(new FinanceAgentQueryPort.Subject("1122", "应收账款")));
        SelectedWorkflow workflow = SelectedWorkflow.builder().workflowBindingId(7L).agentVersionId(17L)
                .digitalEmployeeId(4L).responsibleUserId(8L).queryPermissionCode(permission)
                .queryPermissionCodes(List.of(permission)).config(Map.of("workflow", Map.of("nodes", List.of(
                        Map.of("nodeKey", "start", "nodeType", "start"),
                        Map.of("nodeKey", "balance", "nodeType", "tool", "config", Map.of(
                                "toolCode", "finance.query_balance_sheet")),
                        Map.of("nodeKey", "llm", "nodeType", "llm"),
                        Map.of("nodeKey", "end", "nodeType", "end")), "edges", List.of(
                        Map.of("source", "start", "target", "balance"),
                        Map.of("source", "balance", "target", "llm"),
                        Map.of("source", "llm", "target", "end")))))
                .build();
        var modeled = new FinWorkQueryPlanner.PlannedStep(workflow, "查询余额表",
                Map.of("balance", Map.of("accountNameFrom", "应收账款")),
                Map.of("balance", Map.of("accountNameFrom", "应收账款")));

        var prepared = service.prepare(9L,
                "查询黄埔远智2026年5月仅已记账的应收账款余额表", List.of(modeled), Map.of());

        assertThat(prepared.groups()).isEmpty();
        assertThat(((Map<?, ?>) prepared.steps().get(0).parameters().get("balance"))
                .get("accountCodeFrom")).isEqualTo("1122");
    }

    @Test
    void expenseAmountKeepsInclusiveBoundaryAndClarifiesStrictBoundary() {
        String permission = QueryAuthorizationService.EXPENSE_QUERY;
        when(employees.companyLimit(4L, permission)).thenReturn(List.of());
        when(authorizations.resolve(9L, 8L, permission, null)).thenReturn(
                new QueryAuthorization(9L, 8L, permission, List.of(), List.of(), null));
        SelectedWorkflow workflow = SelectedWorkflow.builder().workflowBindingId(7L).agentVersionId(17L)
                .digitalEmployeeId(4L).responsibleUserId(8L).queryPermissionCode(permission)
                .queryPermissionCodes(List.of(permission)).config(Map.of("workflow", Map.of("nodes", List.of(
                        Map.of("nodeKey", "start", "nodeType", "start"),
                        Map.of("nodeKey", "expense", "nodeType", "tool", "config", Map.of(
                                "toolCode", "expense.query_my_expenses")),
                        Map.of("nodeKey", "llm", "nodeType", "llm"),
                        Map.of("nodeKey", "end", "nodeType", "end")), "edges", List.of(
                        Map.of("source", "start", "target", "expense"),
                        Map.of("source", "expense", "target", "llm"),
                        Map.of("source", "llm", "target", "end")))))
                .build();
        var inclusive = new FinWorkQueryPlanner.PlannedStep(workflow, "查询我的报销单",
                Map.of("expense", Map.of("minAmount", 100)),
                Map.of("expense", Map.of("minAmount", "不少于100元")));
        var exact = service.prepare(9L, "查询我的报销单，不少于100元", List.of(inclusive), Map.of());
        assertThat(exact.groups()).isEmpty();
        assertThat(((Map<?, ?>) exact.steps().get(0).parameters().get("expense")).get("scope"))
                .isEqualTo("SUBMITTED_BY_ME");
        var strict = service.prepare(9L, "查询我的报销单，超过100元", List.of(inclusive), Map.of());
        assertThat(strict.groups()).extracting(FinWorkParameterService.Group::key)
                .containsExactly("amountBoundary");
        var missing = service.prepare(9L, "查询我的报销单，金额不少于100元", List.of(
                new FinWorkQueryPlanner.PlannedStep(workflow, "查询我的报销单")), Map.of());
        assertThat(missing.groups()).extracting(FinWorkParameterService.Group::key)
                .containsExactly("expenseAmountRange");
        var dateMissing = service.prepare(9L, "查询我的报销单，日期从2026-09-01开始", List.of(
                new FinWorkQueryPlanner.PlannedStep(workflow, "查询我的报销单")), Map.of());
        assertThat(dateMissing.groups()).extracting(FinWorkParameterService.Group::key)
                .containsExactly("expenseDateRange");
    }

    @Test
    void unsupportedOnlyUnpostedVoucherScopeDoesNotRun() {
        prepareAccounts(List.of(account("A", "黄埔远智有限公司")));
        when(finance.suppliers(any(), eq("A"), eq("云之智")))
                .thenReturn(List.of(new FinanceAgentQueryPort.Supplier("V001", "云之智科技")));
        assertThatThrownBy(() -> service.prepare(9L,
                "查询黄埔远智供应商云之智全部期间，仅已保存未记账凭证", List.of(step()), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持仅查询未记账");
        assertThat(service.detectVoucherStatus(
                "查询仅已记账的明细\n补充说明：不限制凭证范围")).isNull();
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
