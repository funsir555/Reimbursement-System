package com.finex.auth.agent.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
