package com.finex.auth.service.digitalemployee;

import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.workflow.AgentWorkflowRuntimeService;
import com.finex.auth.agent.workflow.WorkflowExecutionResult;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DigitalEmployeeScheduleRunnerTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final DigitalEmployeePlanService plans = mock(DigitalEmployeePlanService.class);
    private final DigitalEmployeeService employees = mock(DigitalEmployeeService.class);
    private final QueryAuthorizationService authorization = mock(QueryAuthorizationService.class);
    private final AgentWorkflowRuntimeService runtime = mock(AgentWorkflowRuntimeService.class);
    private final com.finex.auth.agent.workflow.WorkflowSelectorService selector =
            mock(com.finex.auth.agent.workflow.WorkflowSelectorService.class);
    private final DigitalEmployeeScheduleRunner runner =
            new DigitalEmployeeScheduleRunner(jdbc, plans, employees, authorization, runtime,
                    selector);

    @AfterEach void clearContext() { TenantContext.clear(); }

    @Test
    void repeatedSchedulerTickClaimsOneTriggerAndCreatesOneUserDelivery() {
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 10, 1, 8, 0);
        LocalDateTime next = scheduledAt.plusDays(1);
        Map<String, Object> plan = plan(scheduledAt);
        when(jdbc.queryForList(contains("FROM ea_digital_employee_plan p"), any(LocalDateTime.class)))
                .thenReturn(List.of(plan));
        when(plans.parameters("{}")) .thenReturn(Map.of("scope", "MY"));
        when(plans.nextRun(any(DigitalEmployeePlanService.PlanRequest.class), eq(scheduledAt)))
                .thenReturn(next);
        when(jdbc.update(contains("SET next_run_at=?"), eq(next), eq(7L), eq(scheduledAt)))
                .thenReturn(1, 0);
        when(jdbc.queryForList(contains("FROM ea_digital_employee_subscription s"),
                eq(Long.class), eq(5L), eq(7L))).thenReturn(List.of(11L));
        when(jdbc.queryForObject(contains("FROM ea_digital_employee_delivery"), eq(Integer.class), eq(5L)))
                .thenReturn(0);
        when(jdbc.update(contains("INSERT IGNORE INTO ea_digital_employee_delivery"),
                eq(7L), eq(11L), eq(scheduledAt), eq(QueryAuthorizationService.EXPENSE_QUERY),
                eq(5L), eq(22L), anyString())).thenReturn(1);
        when(jdbc.queryForList("SELECT company_id FROM sys_user WHERE id=? AND status=1",
                String.class, 11L)).thenReturn(List.of("A"));
        when(employees.companyLimit(5L, QueryAuthorizationService.EXPENSE_QUERY))
                .thenReturn(List.of());
        when(selector.authorizedCandidates(11L, 5L, 8L)).thenReturn(List.of(
                com.finex.auth.agent.workflow.SelectedWorkflow.builder()
                        .workflowBindingId(8L).toolCode("expense.query_my_expenses")
                        .toolCodes(List.of("expense.query_my_expenses"))
                        .queryPermissionCode(QueryAuthorizationService.EXPENSE_QUERY)
                        .queryPermissionCodes(List.of(QueryAuthorizationService.EXPENSE_QUERY))
                        .build()));
        var grant = new QueryAuthorization.Grant("ALL_COMPANIES", Set.of("A"), null);
        var access = new QueryAuthorization(11L, 99L, QueryAuthorizationService.EXPENSE_QUERY,
                List.of(grant), List.of(grant), null);
        when(authorization.resolve(11L, 99L, QueryAuthorizationService.EXPENSE_QUERY, null))
                .thenReturn(access);
        when(runtime.executeQueryWorkflow(anyString(), eq(11L), eq("查询我的报销单"),
                eq("query_expense"), eq(null), eq(null), eq(5L), eq(8L)))
                .thenReturn(WorkflowExecutionResult.builder().success(true).content("0 条")
                        .authorizationDigest(access.digest()).resultCount(0).build());

        runner.runDue();
        runner.runDue();

        verify(runtime, times(1)).executeQueryWorkflow(anyString(), eq(11L), eq("查询我的报销单"),
                eq("query_expense"), eq(null), eq(null), eq(5L), eq(8L));
        verify(jdbc, times(1)).update(contains("INSERT IGNORE INTO ea_digital_employee_delivery"),
                eq(7L), eq(11L), eq(scheduledAt), eq(QueryAuthorizationService.EXPENSE_QUERY),
                eq(5L), eq(22L), anyString());
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void multiToolDeliveryRecordsEveryOriginAndRequiresProvenanceOnRead() {
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        Map<String, Object> plan = plan(scheduledAt);
        String subject = "finance:agent:account_subjects:query";
        String project = "finance:agent:projects:query";
        plan.put("permission_code", subject);
        when(jdbc.queryForList(contains("FROM ea_digital_employee_plan p"), any(LocalDateTime.class)))
                .thenReturn(List.of(plan));
        when(plans.parameters("{}")) .thenReturn(Map.of("subject", Map.of("companyId", "A"),
                "project", Map.of("companyId", "A")));
        when(plans.nextRun(any(DigitalEmployeePlanService.PlanRequest.class), eq(scheduledAt)))
                .thenReturn(scheduledAt.plusDays(1));
        when(jdbc.update(contains("SET next_run_at=?"), eq(scheduledAt.plusDays(1)),
                eq(7L), eq(scheduledAt))).thenReturn(1);
        when(jdbc.queryForList(contains("FROM ea_digital_employee_subscription s"),
                eq(Long.class), eq(5L), eq(7L))).thenReturn(List.of(11L));
        when(jdbc.queryForObject(contains("FROM ea_digital_employee_delivery"),
                eq(Integer.class), eq(5L))).thenReturn(0);
        when(jdbc.update(contains("INSERT IGNORE INTO ea_digital_employee_delivery"),
                eq(7L), eq(11L), eq(scheduledAt), eq(subject), eq(5L), eq(22L), anyString()))
                .thenReturn(1);
        when(jdbc.queryForList("SELECT company_id FROM sys_user WHERE id=? AND status=1",
                String.class, 11L)).thenReturn(List.of("A"));
        var workflow = com.finex.auth.agent.workflow.SelectedWorkflow.builder()
                .workflowBindingId(8L).toolCode("finance.query_account_subjects")
                .toolCodes(List.of("finance.query_account_subjects", "finance.query_projects"))
                .queryPermissionCode(subject).queryPermissionCodes(List.of(subject, project)).build();
        when(selector.authorizedCandidates(11L, 5L, 8L)).thenReturn(List.of(workflow));
        var scope = new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of("A"), null);
        var subjectGrant = new QueryAuthorization(11L, 99L, subject,
                List.of(scope), List.of(scope), null);
        var projectGrant = new QueryAuthorization(11L, 99L, project,
                List.of(scope), List.of(scope), null);
        when(employees.companyLimit(5L, subject)).thenReturn(List.of());
        when(employees.companyLimit(5L, project)).thenReturn(List.of());
        when(authorization.resolve(11L, 99L, subject, null)).thenReturn(subjectGrant);
        when(authorization.resolve(11L, 99L, project, null)).thenReturn(projectGrant);
        var origins = List.of(
                new WorkflowExecutionResult.ToolOrigin("subject", "finance.query_account_subjects",
                        subject, subjectGrant.digest(), "{\"companyId\":\"A\"}", "digest1"),
                new WorkflowExecutionResult.ToolOrigin("project", "finance.query_projects",
                        project, projectGrant.digest(), "{\"companyId\":\"A\"}", "digest2"));
        when(runtime.executePlannedWorkflow(anyString(), eq(11L), eq(workflow), anyString(), any()))
                .thenReturn(WorkflowExecutionResult.builder().success(true).content("查询结果")
                        .authorizationDigest(subjectGrant.digest()).resultCount(1)
                        .toolOrigins(origins).build());
        when(jdbc.queryForObject(contains("SELECT id FROM ea_digital_employee_delivery"),
                eq(Long.class), eq(7L), eq(11L), eq(scheduledAt))).thenReturn(77L);

        runner.runDue();

        verify(jdbc, times(2)).update(contains("INSERT INTO ea_controlled_result_origin"),
                eq(77L), any(Integer.class), eq(2), eq(5L), eq(8L), eq(99L), eq(22L),
                anyString(), anyString(), anyString(), anyString(), anyString());
        verify(jdbc).update(contains("origin_required=?"), eq("SUCCESS"), eq("查询结果"),
                eq(subjectGrant.digest()), eq(1), eq(7L), eq(11L), eq(scheduledAt));
    }

    private Map<String, Object> plan(LocalDateTime scheduledAt) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 7L);
        row.put("employee_id", 5L);
        row.put("workflow_binding_id", 8L);
        row.put("frequency", "DAILY");
        row.put("run_hour", 8);
        row.put("run_minute", 0);
        row.put("parameter_json", "{}");
        row.put("version_no", 1L);
        row.put("next_run_at", Timestamp.valueOf(scheduledAt));
        row.put("tenant_id", "default");
        row.put("responsible_user_id", 99L);
        row.put("daily_run_limit", 10);
        row.put("concurrency_limit", 2);
        row.put("permission_code", QueryAuthorizationService.EXPENSE_QUERY);
        row.put("agent_version_id", 22L);
        row.put("binding_valid", 1);
        return row;
    }
}
