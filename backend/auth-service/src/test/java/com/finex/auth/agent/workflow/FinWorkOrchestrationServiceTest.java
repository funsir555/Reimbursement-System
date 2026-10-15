package com.finex.auth.agent.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FinWorkOrchestrationServiceTest {
    private final FinWorkQueryPlanner planner = mock(FinWorkQueryPlanner.class);
    private final FinWorkParameterService parameters = mock(FinWorkParameterService.class);
    private final WorkflowSelectorService selector = mock(WorkflowSelectorService.class);
    private final AgentWorkflowRuntimeService runtime = mock(AgentWorkflowRuntimeService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final FinWorkOrchestrationService orchestration = new FinWorkOrchestrationService(
            planner, parameters, selector, runtime, jdbc, new ObjectMapper());

    @org.junit.jupiter.api.BeforeEach
    void noPendingClarification() {
        when(jdbc.queryForList(anyString(), eq(String.class), anyString(), anyLong()))
                .thenReturn(List.of());
        when(parameters.prepare(anyLong(), anyString(), any(), any()))
                .thenAnswer(call -> new FinWorkParameterService.Preparation(call.getArgument(2), List.of()));
    }

    @Test
    void oneExpenseWorkflowUsesItsFullAuthorizedAmount() {
        SelectedWorkflow expense = expense(11L);
        when(planner.plan(anyLong(), anyString(), any(), any(), anyString())).thenReturn(decision(List.of(expense)));
        when(runtime.executePlannedWorkflow(anyString(), anyLong(), any(), anyString(), any()))
                .thenReturn(result("符合条件共 12 条，展示 5 条", "1234.56"));

        var outcome = orchestration.respond("session", 9L,
                "我的报销单有哪些？总金额是多少？", null, null, List.of());

        assertThat(outcome.kind()).isEqualTo("ANSWER");
        assertThat(outcome.content()).contains("1234.56", "全部符合条件");
        assertThat(outcome.executions()).hasSize(1);
    }

    @Test
    void overlappingExpenseWorkflowsAreNotBlindlySummed() {
        SelectedWorkflow first = expense(11L);
        SelectedWorkflow second = expense(12L);
        when(planner.plan(anyLong(), anyString(), any(), any(), anyString())).thenReturn(decision(List.of(first, second)));
        when(runtime.executePlannedWorkflow(anyString(), anyLong(), any(), anyString(), any()))
                .thenReturn(result("第一组单据", "100"), result("第二组单据", "200"));

        var outcome = orchestration.respond("session", 9L,
                "查询两组报销单，总金额是多少？", null, null, List.of());

        assertThat(outcome.content()).contains("可能包含同一单据")
                .doesNotContain("总金额：300");
        verify(runtime, times(2)).executePlannedWorkflow(anyString(), anyLong(), any(), anyString(), any());
    }

    @Test
    void modelFailureDoesNotCreateControlledQueryRun() {
        when(planner.plan(anyLong(), anyString(), any(), any(), anyString()))
                .thenThrow(new IllegalStateException("FinWork 任务分析暂不可用"));

        assertThatThrownBy(() -> orchestration.respond("session", 9L,
                "查询供应商明细账", null, null, List.of()))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(runtime);
    }

    @Test
    void ambiguousWorkflowsProduceOptionsAndSaveClarificationState() {
        SelectedWorkflow first = expense(11L);
        first.setDigitalEmployeeName("财务助手");
        first.setAgentName("我的单据");
        SelectedWorkflow second = expense(12L);
        second.setDigitalEmployeeName("财务助手");
        second.setAgentName("待审批单据");
        when(planner.plan(anyLong(), anyString(), any(), any(), anyString())).thenReturn(
                new FinWorkQueryPlanner.Decision(FinWorkQueryPlanner.Mode.CLARIFY,
                        List.of(), "需要查询本人单据还是待审批单据？", "", List.of(first, second)));

        var outcome = orchestration.respond("session", 9L, "帮我查单据", null, null, List.of());

        assertThat(outcome.kind()).isEqualTo("CLARIFY");
        assertThat(outcome.options()).extracting(FinWorkOrchestrationService.Option::id)
                .containsExactly("binding:11", "binding:12", "all");
        verify(jdbc).update(anyString(), any(), any(), any(), any());
    }

    @Test
    void freeTextClarificationKeepsTheFixedWorkflowInsteadOfReplanning() {
        SelectedWorkflow expense = expense(11L);
        expense.setConfig(Map.of("workflow", Map.of("nodes", List.of(
                Map.of("nodeKey", "start", "nodeType", "start"),
                Map.of("nodeKey", "tool", "nodeType", "tool", "config",
                        Map.of("toolCode", "expense.query_my_expenses")),
                Map.of("nodeKey", "llm", "nodeType", "llm"),
                Map.of("nodeKey", "end", "nodeType", "end")), "edges", List.of(
                Map.of("source", "start", "target", "tool"),
                Map.of("source", "tool", "target", "llm"),
                Map.of("source", "llm", "target", "end")))));
        AtomicReference<String> pendingJson = new AtomicReference<>();
        when(jdbc.queryForList(anyString(), eq(String.class), anyString(), anyLong()))
                .thenAnswer(call -> pendingJson.get() == null ? List.of() : List.of(pendingJson.get()));
        doAnswer(call -> {
            if (call.getArgument(0, String.class).contains("INSERT INTO ea_agent_chat_pending_plan"))
                pendingJson.set(call.getArgument(3, String.class));
            return 1;
        }).when(jdbc).update(anyString(), any(), any(), any(), any());
        when(selector.authorizedCandidates(9L, null, null)).thenReturn(List.of(expense));
        when(parameters.authorizationSignatures(anyLong(), any())).thenReturn(Map.of());
        when(planner.plan(anyLong(), anyString(), any(), any(), anyString()))
                .thenReturn(decision(List.of(expense)));
        var group = new FinWorkParameterService.Group("periodRange", "请说明期间，例如 2022年12月", List.of());
        var statusGroup = new FinWorkParameterService.Group("voucherStatus", "请选择凭证范围", List.of(
                new FinWorkParameterService.Choice("POSTED_ONLY", "仅已记账"),
                new FinWorkParameterService.Choice("INCLUDE_SAVED_UNPOSTED", "包含已保存未记账")));
        when(parameters.prepare(anyLong(), anyString(), any(), any()))
                .thenAnswer(call -> pendingJson.get() == null
                        ? new FinWorkParameterService.Preparation(call.getArgument(2), List.of(group, statusGroup))
                        : new FinWorkParameterService.Preparation(call.getArgument(2), List.of()));
        when(parameters.detectVoucherStatus("包含已保存未记账"))
                .thenReturn("INCLUDE_SAVED_UNPOSTED");
        AtomicReference<String> revisionAnswer = new AtomicReference<>();
        when(planner.revise(anyLong(), anyString(), anyString(), any(), any(), anyString()))
                .thenAnswer(call -> {
                    revisionAnswer.set(call.getArgument(2));
                    return new FinWorkQueryPlanner.Revision(false, call.getArgument(3));
                });
        when(runtime.executePlannedWorkflow(anyString(), anyLong(), any(), anyString(), any()))
                .thenReturn(result("查询完成", "0"));

        assertThat(orchestration.respond("session", 9L, "查询报销单", null, null, List.of()).kind())
                .isEqualTo("CLARIFY");
        assertThat(orchestration.respond("session", 9L, "包含已保存未记账", null, null,
                List.of(), Map.of("voucherStatus", "POSTED_ONLY")).kind()).isEqualTo("CLARIFY");
        assertThat(orchestration.respond("session", 9L,
                "请说明期间，例如 2022年12月：去年一季度", null, null,
                List.of(), Map.of("periodRange", "去年一季度")).kind())
                .isEqualTo("ANSWER");
        assertThat(revisionAnswer.get()).contains("periodRange：去年一季度")
                .doesNotContain("2022年12月");
        verify(planner, times(1)).plan(anyLong(), anyString(), any(), any(), anyString());
        verify(planner, times(2)).revise(anyLong(), anyString(), anyString(), any(), any(), anyString());
        verify(runtime, times(1)).executePlannedWorkflow(anyString(), anyLong(), any(), anyString(), any());
    }

    private FinWorkQueryPlanner.Decision decision(List<SelectedWorkflow> workflows) {
        return new FinWorkQueryPlanner.Decision(FinWorkQueryPlanner.Mode.EXECUTE,
                workflows.stream().map(workflow -> new FinWorkQueryPlanner.PlannedStep(
                        workflow, "我的报销单有哪些")).toList(), "", "计算总金额", workflows);
    }

    private SelectedWorkflow expense(Long bindingId) {
        return SelectedWorkflow.builder().workflowBindingId(bindingId)
                .agentVersionId(bindingId + 100)
                .toolCode(ExpenseDocumentControlledQueryTool.TOOL_CODE).build();
    }

    private WorkflowExecutionResult result(String content, String total) {
        return WorkflowExecutionResult.builder().success(true).content(content)
                .totalAmount(new BigDecimal(total)).build();
    }
}
