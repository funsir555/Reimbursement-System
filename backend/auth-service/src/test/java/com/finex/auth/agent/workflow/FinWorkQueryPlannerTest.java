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
        SelectedWorkflow balance = candidate(31L, "余额表工具", "finance.query_balance_sheet");
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

    @Test
    void clarificationUpdatesOnlyTheFixedWorkflowAndRecordsEvidence() {
        SelectedWorkflow expense = candidate(21L, "报销查询");
        when(selector.authorizedCandidates(9L, null, null)).thenReturn(List.of(expense));
        when(models.chatWithFallback(any(ModelCapability.ModelUseCase.class),
                any(ModelRoutingService.DataSensitivity.class), any()))
                .thenReturn(ChatModelProvider.ChatResponse.builder().success(true).content("""
                        {"newTask":false,"updates":[{"bindingId":21,
                         "parameters":{"tool":{"scope":"ASSIGNED_TO_ME"}},
                         "evidence":{"tool":{"scope":"待我审批"}}}]}
                        """).build());
        var current = new FinWorkQueryPlanner.PlannedStep(expense, "查询报销单");

        var revision = planner.revise(9L, "查询报销单", "看待我审批的",
                List.of(current), List.of(new FinWorkParameterService.Group("scope", "查询范围", List.of())),
                "session");

        assertThat(revision.newTask()).isFalse();
        assertThat(revision.changedFields()).containsExactly("21:tool:scope");
        assertThat(((Map<?, ?>) revision.steps().get(0).parameters().get("tool")).get("scope"))
                .isEqualTo("ASSIGNED_TO_ME");
        assertThat(revision.steps().get(0).evidence().get("tool")).containsEntry("scope", "待我审批");
    }

    @Test
    void clarificationCanRemoveAnEarlierFilter() {
        SelectedWorkflow expense = candidate(21L, "报销查询");
        when(selector.authorizedCandidates(9L, null, null)).thenReturn(List.of(expense));
        when(models.chatWithFallback(any(ModelCapability.ModelUseCase.class),
                any(ModelRoutingService.DataSensitivity.class), any()))
                .thenReturn(ChatModelProvider.ChatResponse.builder().success(true).content("""
                        {"newTask":false,"updates":[{"bindingId":21,
                         "parameters":{"tool":{"status":null}},
                         "evidence":{"tool":{"status":"不限制状态"}}}]}
                        """).build());
        var current = new FinWorkQueryPlanner.PlannedStep(expense, "查询已审批报销单",
                Map.of("tool", Map.of("status", "APPROVED")),
                Map.of("tool", Map.of("status", "已审批")));

        var revision = planner.revise(9L, "查询已审批报销单", "不限制状态",
                List.of(current), List.of(), "session");

        assertThat(revision.changedFields()).containsExactly("21:tool:status");
        assertThat(((Map<?, ?>) revision.steps().get(0).parameters().get("tool"))
                .containsKey("status")).isFalse();
    }

    @Test
    void rejectsFieldsAssignedToTheWrongToolNode() {
        SelectedWorkflow expense = candidate(21L, "报销查询");
        when(selector.authorizedCandidates(9L, null, null)).thenReturn(List.of(expense));
        when(models.chatWithFallback(any(ModelCapability.ModelUseCase.class),
                any(ModelRoutingService.DataSensitivity.class), any()))
                .thenReturn(ChatModelProvider.ChatResponse.builder().success(true).content("""
                        {"mode":"EXECUTE","steps":[{"bindingId":21,"queryText":"查报销单",
                         "parameters":{"tool":{"supplierCode":"V001"}},
                         "evidence":{"tool":{"supplierCode":"供应商"}}}]}
                        """).build());

        assertThatThrownBy(() -> planner.plan(9L, "查供应商报销单", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("受控工具契约");
    }

    private SelectedWorkflow candidate(Long bindingId, String name) {
        return candidate(bindingId, name, "expense.query_my_expenses");
    }

    private SelectedWorkflow candidate(Long bindingId, String name, String toolCode) {
        return SelectedWorkflow.builder()
                .workflowBindingId(bindingId)
                .agentVersionId(31L)
                .digitalEmployeeId(4L)
                .agentName(name)
                .agentDescription("查询授权报销单")
                .config(Map.of("promptConfig", Map.of("systemPrompt", "只读查询"),
                        "workflow", Map.of("nodes", List.of(
                                Map.of("nodeKey", "start", "nodeType", "start"),
                                Map.of("nodeKey", "tool", "nodeType", "tool", "config", Map.of(
                                        "toolCode", toolCode)),
                                Map.of("nodeKey", "llm", "nodeType", "llm"),
                                Map.of("nodeKey", "end", "nodeType", "end")),
                                "edges", List.of(Map.of("source", "start", "target", "tool"),
                                        Map.of("source", "tool", "target", "llm"),
                                        Map.of("source", "llm", "target", "end")))))
                .build();
    }
}
