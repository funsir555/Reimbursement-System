package com.finex.auth.agent.workflow;

import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.service.port.ExpenseAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpenseDocumentControlledQueryToolTest {

    private final ExpenseAgentQueryPort expenseQueryPort = mock(ExpenseAgentQueryPort.class);
    private final ExpenseDocumentControlledQueryTool tool = new ExpenseDocumentControlledQueryTool(expenseQueryPort);

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void queriesPendingTasksAssignedToCurrentUserThroughPort() {
        when(expenseQueryPort.aggregateAuthorizedDocuments(any(), any()))
                .thenReturn(new ExpenseAgentQueryPort.Aggregate(2L, new BigDecimal("200.00")));
        when(expenseQueryPort.fingerprintAuthorizedDocuments(any(), any())).thenReturn("test-digest");
        when(expenseQueryPort.queryAuthorizedDocuments(any(), any())).thenReturn(List.of(
                document(1L, "DOC-001", "PENDING_APPROVAL"),
                document(2L, "DOC-002", "PENDING_APPROVAL")
        ));

        ControlledToolResult result = tool.execute(request(
                ExpenseDocumentControlledQueryTool.ACCESSIBLE_QUERY_TEMPLATE_CODE,
                QueryTemplateVariables.builder()
                        .scope(QueryTemplateVariables.SCOPE_ASSIGNED_TO_ME)
                        .limit(10)
                        .build()
        ));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getEvidenceRefs()).containsExactly("EXPENSE_DOCUMENT:1", "EXPENSE_DOCUMENT:2");
        assertThat(result.getTotalCount()).isEqualTo(2L);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("200.00");

        ArgumentCaptor<ExpenseAgentQueryPort.Query> queryCaptor = ArgumentCaptor.forClass(ExpenseAgentQueryPort.Query.class);
        verify(expenseQueryPort).queryAuthorizedDocuments(queryCaptor.capture(), any());
        assertThat(queryCaptor.getValue().scope()).isEqualTo(ExpenseAgentQueryPort.Scope.ASSIGNED_TO_USER);
        assertThat(queryCaptor.getValue().companyId()).isNull();
        assertThat(queryCaptor.getValue().excludeDeleted()).isTrue();
    }

    @Test
    void keepsLegacyTemplateScopedToDocumentsSubmittedByCurrentUser() {
        when(expenseQueryPort.aggregateAuthorizedDocuments(any(), any()))
                .thenReturn(new ExpenseAgentQueryPort.Aggregate(1L, new BigDecimal("100.00")));
        when(expenseQueryPort.fingerprintAuthorizedDocuments(any(), any())).thenReturn("test-digest");
        when(expenseQueryPort.queryAuthorizedDocuments(any(), any()))
                .thenReturn(List.of(document(1L, "DOC-001", "DRAFT")));

        ControlledToolResult result = tool.execute(request(
                ExpenseDocumentControlledQueryTool.QUERY_TEMPLATE_CODE,
                QueryTemplateVariables.builder()
                        .scope(QueryTemplateVariables.SCOPE_ASSIGNED_TO_ME)
                        .limit(10)
                        .build()
        ));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRecords()).hasSize(1);
        ArgumentCaptor<ExpenseAgentQueryPort.Query> queryCaptor = ArgumentCaptor.forClass(ExpenseAgentQueryPort.Query.class);
        verify(expenseQueryPort).queryAuthorizedDocuments(queryCaptor.capture(), any());
        assertThat(queryCaptor.getValue().scope()).isEqualTo(ExpenseAgentQueryPort.Scope.SUBMITTED_BY_USER);
    }

    @Test
    void rejectsStatusOutsideTheControlledAllowlist() {
        ControlledToolResult result = tool.execute(request(
                ExpenseDocumentControlledQueryTool.ACCESSIBLE_QUERY_TEMPLATE_CODE,
                QueryTemplateVariables.builder()
                        .scope(QueryTemplateVariables.SCOPE_SUBMITTED_BY_ME)
                        .status("UNTRUSTED")
                        .build()
        ));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("不支持的报销单状态");
        verify(expenseQueryPort, never()).queryAuthorizedDocuments(any(), any());
    }

    @Test
    void rejectsIdentityMismatchBeforeCallingExpensePort() {
        TenantContext.setUserId(2L);
        TenantContext.setCompanyId("GROUP_HQ");

        ControlledToolResult result = tool.execute(request(
                ExpenseDocumentControlledQueryTool.QUERY_TEMPLATE_CODE, null));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("上下文不一致");
        verify(expenseQueryPort, never()).queryAuthorizedDocuments(any(), any());
    }

    private ToolExecutionRequest request(String templateCode, QueryTemplateVariables variables) {
        return ToolExecutionRequest.builder()
                .userId(1L)
                .companyId("GROUP_HQ")
                .queryTemplateCode(templateCode)
                .variables(variables)
                .authorization(new QueryAuthorization(1L, 1L, "expense:agent:query",
                        List.of(new QueryAuthorization.Grant("SELF", Set.of("GROUP_HQ"), Set.of(1L))),
                        List.of(new QueryAuthorization.Grant("SELF", Set.of("GROUP_HQ"), Set.of(1L))), null))
                .build();
    }

    private ExpenseAgentQueryPort.DocumentView document(Long id, String documentCode, String status) {
        return new ExpenseAgentQueryPort.DocumentView(
                id,
                documentCode,
                "测试报销单",
                null,
                new BigDecimal("100.00"),
                status,
                "测试模板",
                null
        );
    }
}
