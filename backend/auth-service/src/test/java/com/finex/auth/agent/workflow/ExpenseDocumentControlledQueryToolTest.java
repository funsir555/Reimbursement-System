package com.finex.auth.agent.workflow;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpenseDocumentControlledQueryToolTest {

    private final ProcessDocumentInstanceMapper documentMapper = mock(ProcessDocumentInstanceMapper.class);
    private final ProcessDocumentTaskMapper taskMapper = mock(ProcessDocumentTaskMapper.class);
    private final ExpenseDocumentControlledQueryTool tool = new ExpenseDocumentControlledQueryTool(documentMapper, taskMapper);

    @Test
    void queriesPendingTasksAssignedToCurrentUser() {
        when(taskMapper.selectList(any())).thenReturn(List.of(
                task("DOC-001"),
                task("DOC-002")
        ));
        when(documentMapper.selectList(any())).thenReturn(List.of(
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
        verify(taskMapper).selectList(any(Wrapper.class));
        verify(documentMapper).selectList(any(Wrapper.class));
    }

    @Test
    void keepsLegacyTemplateScopedToDocumentsSubmittedByCurrentUser() {
        when(documentMapper.selectList(any())).thenReturn(List.of(document(1L, "DOC-001", "DRAFT")));

        ControlledToolResult result = tool.execute(request(
                ExpenseDocumentControlledQueryTool.QUERY_TEMPLATE_CODE,
                QueryTemplateVariables.builder()
                        .scope(QueryTemplateVariables.SCOPE_ASSIGNED_TO_ME)
                        .limit(10)
                        .build()
        ));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRecords()).hasSize(1);
        verify(taskMapper, never()).selectList(any(Wrapper.class));
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
        verify(documentMapper, never()).selectList(any(Wrapper.class));
    }

    private ToolExecutionRequest request(String templateCode, QueryTemplateVariables variables) {
        return ToolExecutionRequest.builder()
                .userId(1L)
                .queryTemplateCode(templateCode)
                .variables(variables)
                .build();
    }

    private ProcessDocumentTask task(String documentCode) {
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setDocumentCode(documentCode);
        task.setAssigneeUserId(1L);
        task.setStatus("PENDING");
        return task;
    }

    private ProcessDocumentInstance document(Long id, String documentCode, String status) {
        ProcessDocumentInstance document = new ProcessDocumentInstance();
        document.setId(id);
        document.setDocumentCode(documentCode);
        document.setTemplateType("report");
        document.setDocumentTitle("测试报销单");
        document.setTotalAmount(new BigDecimal("100.00"));
        document.setStatus(status);
        return document;
    }
}
