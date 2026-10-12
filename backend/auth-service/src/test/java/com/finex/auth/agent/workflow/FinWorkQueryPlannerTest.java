package com.finex.auth.agent.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.model.ChatModelProvider;
import com.finex.auth.agent.model.ModelCapability;
import com.finex.auth.agent.model.ModelRoutingService;
import com.finex.auth.agent.desensitization.DesensitizationService;
import com.finex.auth.agent.desensitization.DesensitizedData;
import com.finex.auth.agent.ontology.BusinessOntologyService;
import com.finex.auth.service.digitalemployee.DigitalEmployeeService;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

class FinWorkQueryPlannerTest {
    private final WorkflowSelectorService selector = mock(WorkflowSelectorService.class);
    private final DigitalEmployeeService employees = mock(DigitalEmployeeService.class);
    private final ModelRoutingService models = mock(ModelRoutingService.class);
    private final DesensitizationService desensitization = mock(DesensitizationService.class);
    private final BusinessOntologyService ontology = mock(BusinessOntologyService.class);
    private final FinWorkQueryPlanner planner = new FinWorkQueryPlanner(
            selector, employees, models, desensitization, ontology, new ObjectMapper());

    @org.junit.jupiter.api.BeforeEach
    void maskedPlannerInput() {
        when(ontology.modelContext(any(), any())).thenReturn(List.of());
        when(desensitization.desensitize(any(), any())).thenAnswer(call ->
                DesensitizedData.builder().success(true).maskedText(call.getArgument(1)).build());
    }

    @Test
    void separatesExpenseQueryFromAmountAnalysisAndKeepsReviewedBinding() {
        SelectedWorkflow expense = candidate(21L, "单据查询工具");
        when(selector.authorizedCandidates(9L, null, null)).thenReturn(List.of(expense));
        when(ontology.modelContext(any(), any())).thenReturn(List.of(Map.of(
                "kind", "ENTITY", "name", "报销单", "meaning", "通用业务单据")));
        when(models.chatWithFallback(any(ModelCapability.ModelUseCase.class),
                any(ModelRoutingService.DataSensitivity.class), any()))
                .thenReturn(ChatModelProvider.ChatResponse.builder().success(true).content("""
                        {"mode":"EXECUTE","steps":[{"bindingId":21,"queryText":"我的报销单有哪些"}],
                         "analysisGoal":"计算全部符合条件单据的总金额"}
                        """).build());

        var decision = planner.plan(9L, "我的报销单有哪些？总金额是多少？", null, null);

        assertThat(decision.mode()).isEqualTo(FinWorkQueryPlanner.Mode.EXECUTE);
        assertThat(decision.steps()).singleElement().satisfies(step -> {
            assertThat(step.workflow()).isSameAs(expense);
            assertThat(step.queryText()).isEqualTo("我的报销单有哪些");
        });
        assertThat(decision.analysisGoal()).contains("总金额");
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(desensitization).desensitize(any(), payload.capture());
        assertThat(payload.getValue()).contains("businessOntology", "通用业务单据");
    }

    @Test
    void rejectsModelInventedBindingBeforeExecution() {
        when(selector.authorizedCandidates(9L, null, null)).thenReturn(List.of(candidate(21L, "单据查询工具")));
        when(models.chatWithFallback(any(ModelCapability.ModelUseCase.class),
                any(ModelRoutingService.DataSensitivity.class), any()))
                .thenReturn(ChatModelProvider.ChatResponse.builder().success(true).content("""
                        {"mode":"EXECUTE","steps":[{"bindingId":99,"queryText":"我的报销单"}]}
                        """).build());

        assertThatThrownBy(() -> planner.plan(9L, "我的报销单", null, null))
                .isInstanceOf(SecurityException.class)
                .hasMessage(QueryAuthorizationService.NO_PERMISSION);
    }

    @Test
    void leavesVoucherStatusToServerPreflightEvenWhenModelInventsADefault() {
        SelectedWorkflow balance = candidate(31L, "余额表工具");
        balance.setToolCodes(List.of("finance.query_balance_sheet"));
        when(selector.authorizedCandidates(9L, null, null)).thenReturn(List.of(balance));
        when(models.chatWithFallback(any(ModelCapability.ModelUseCase.class),
                any(ModelRoutingService.DataSensitivity.class), any()))
                .thenReturn(ChatModelProvider.ChatResponse.builder().success(true).content("""
                        {"mode":"EXECUTE","steps":[{"bindingId":31,"queryText":"查询余额表",
                         "parameters":{"tool":{"voucherStatus":"POSTED_ONLY"}}}]}
                        """).build());

        var decision = planner.plan(9L, "查询余额表", null, null);

        assertThat(decision.mode()).isEqualTo(FinWorkQueryPlanner.Mode.EXECUTE);
    }

    private SelectedWorkflow candidate(Long bindingId, String name) {
        return SelectedWorkflow.builder()
                .workflowBindingId(bindingId)
                .digitalEmployeeId(4L)
                .agentName(name)
                .agentDescription("查询授权报销单")
                .config(Map.of("promptConfig", Map.of("systemPrompt", "只读查询"),
                        "workflow", Map.of("nodes", List.of(
                                Map.of("nodeKey", "start", "nodeType", "start"),
                                Map.of("nodeKey", "tool", "nodeType", "tool", "config", Map.of(
                                        "toolCode", "expense.query_my_expenses")),
                                Map.of("nodeKey", "llm", "nodeType", "llm"),
                                Map.of("nodeKey", "end", "nodeType", "end")),
                                "edges", List.of(Map.of("source", "start", "target", "tool"),
                                        Map.of("source", "tool", "target", "llm"),
                                        Map.of("source", "llm", "target", "end")))))
                .build();
    }
}
