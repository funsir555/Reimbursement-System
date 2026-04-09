package com.finex.auth.service.impl;

import com.finex.auth.dto.ExpenseActionUserOptionVO;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.service.impl.expense.ExpenseApprovalDomainSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseApprovalWorkflowServiceTest {

    @Mock
    private ExpenseApprovalDomainSupport expenseApprovalDomainSupport;

    @Test
    void listPendingAndSearchUsersDelegateToMutationSupport() {
        List<ExpenseApprovalPendingItemVO> pendingItems = List.of(new ExpenseApprovalPendingItemVO());
        List<ExpenseActionUserOptionVO> users = List.of(new ExpenseActionUserOptionVO());
        ExpenseApprovalWorkflowService service = new ExpenseApprovalWorkflowService(expenseApprovalDomainSupport);
        when(expenseApprovalDomainSupport.listPendingApprovals(1L)).thenReturn(pendingItems);
        when(expenseApprovalDomainSupport.searchActionUsers(1L, "zhang")).thenReturn(users);

        assertSame(pendingItems, service.listPendingApprovals(1L));
        assertSame(users, service.searchActionUsers(1L, "zhang"));

        verify(expenseApprovalDomainSupport).listPendingApprovals(1L);
        verify(expenseApprovalDomainSupport).searchActionUsers(1L, "zhang");
    }

    @Test
    void approveAndRejectDelegateToMutationSupport() {
        ExpenseApprovalActionDTO dto = new ExpenseApprovalActionDTO();
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        ExpenseApprovalWorkflowService service = new ExpenseApprovalWorkflowService(expenseApprovalDomainSupport);
        when(expenseApprovalDomainSupport.approveTask(1L, "tester", 11L, dto)).thenReturn(detail);
        when(expenseApprovalDomainSupport.rejectTask(1L, "tester", 11L, dto)).thenReturn(detail);

        assertSame(detail, service.approveTask(1L, "tester", 11L, dto));
        assertSame(detail, service.rejectTask(1L, "tester", 11L, dto));

        verify(expenseApprovalDomainSupport).approveTask(1L, "tester", 11L, dto);
        verify(expenseApprovalDomainSupport).rejectTask(1L, "tester", 11L, dto);
    }

    @Test
    void modifyDelegatesToMutationSupport() {
        ExpenseDocumentUpdateDTO dto = new ExpenseDocumentUpdateDTO();
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        ExpenseApprovalWorkflowService service = new ExpenseApprovalWorkflowService(expenseApprovalDomainSupport);
        when(expenseApprovalDomainSupport.modifyTaskDocument(1L, "tester", 22L, dto)).thenReturn(detail);

        assertSame(detail, service.modifyTaskDocument(1L, "tester", 22L, dto));

        verify(expenseApprovalDomainSupport).modifyTaskDocument(1L, "tester", 22L, dto);
    }
}
