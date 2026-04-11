package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseApprovalDomainSupportTest {

    @Mock
    private ExpenseDocumentReadSupport expenseDocumentReadSupport;
    @Mock
    private ExpenseDocumentActionLogSupport expenseDocumentActionLogSupport;
    @Mock
    private ExpenseDocumentMutationDomainSupport expenseDocumentMutationDomainSupport;
    @Mock
    private ExpenseDocumentTemplateSupport expenseDocumentTemplateSupport;
    @Mock
    private ExpenseSummaryAssembler expenseSummaryAssembler;
    @Mock
    private ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;
    @Mock
    private ExpenseRelationWriteOffService expenseRelationWriteOffService;
    @Mock
    private ProcessDocumentTaskMapper processDocumentTaskMapper;
    @Mock
    private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SystemDepartmentMapper systemDepartmentMapper;

    @Test
    void listPendingApprovalsUsesSummaryAssembler() {
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(10L);
        task.setDocumentCode("DOC-001");
        task.setNodeType("APPROVAL");
        task.setStatus("PENDING");
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");
        List<ExpenseApprovalPendingItemVO> expected = List.of(new ExpenseApprovalPendingItemVO());
        ExpenseApprovalDomainSupport support = newSupport();
        when(processDocumentTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(instance));
        when(expenseSummaryAssembler.toPendingItems(anyList(), anyMap())).thenReturn(expected);

        List<ExpenseApprovalPendingItemVO> actual = support.listPendingApprovals(1L);

        assertSame(expected, actual);
        verify(expenseSummaryAssembler).toPendingItems(anyList(), anyMap());
    }

    @Test
    void getTaskModifyContextBuildsViaTemplateSupport() {
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(10L);
        task.setDocumentCode("DOC-001");
        task.setAssigneeUserId(1L);
        task.setNodeType("APPROVAL");
        task.setStatus("PENDING");
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");
        ExpenseDocumentEditContextVO expected = new ExpenseDocumentEditContextVO();
        ExpenseApprovalDomainSupport support = newSupport();
        when(processDocumentTaskMapper.selectById(10L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-001")).thenReturn(instance);
        when(expenseDocumentTemplateSupport.buildEditContext(1L, instance, 10L, "MODIFY")).thenReturn(expected);

        ExpenseDocumentEditContextVO actual = support.getTaskModifyContext(1L, 10L);

        assertSame(expected, actual);
    }

    @Test
    void rejectTaskUsesRuntimeOwnerAndWriteOffOwner() {
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(10L);
        task.setDocumentCode("DOC-001");
        task.setAssigneeUserId(1L);
        task.setNodeType("APPROVAL");
        task.setStatus("PENDING");
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        ExpenseApprovalDomainSupport support = newSupport();
        when(processDocumentTaskMapper.selectById(10L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-001")).thenReturn(instance, instance);
        when(expenseDocumentReadSupport.buildDocumentDetail(instance)).thenReturn(detail);

        ExpenseDocumentDetailVO actual = support.rejectTask(1L, "tester", 10L, new ExpenseApprovalActionDTO());

        assertSame(detail, actual);
        verify(expenseWorkflowRuntimeSupport).rejectPendingTask(any(), any(), any(), any(), any());
        verify(expenseRelationWriteOffService).voidPendingWriteOffs("DOC-001");
    }

    private ExpenseApprovalDomainSupport newSupport() {
        return new ExpenseApprovalDomainSupport(
                expenseDocumentReadSupport,
                expenseDocumentActionLogSupport,
                expenseDocumentMutationDomainSupport,
                expenseDocumentTemplateSupport,
                expenseSummaryAssembler,
                expenseWorkflowRuntimeSupport,
                expenseRelationWriteOffService,
                processDocumentTaskMapper,
                processDocumentInstanceMapper,
                userMapper,
                systemDepartmentMapper
        );
    }
}