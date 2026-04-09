package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseApprovalDomainSupportTest {

    @Mock
    private ExpenseDocumentMutationSupport expenseDocumentMutationSupport;

    @Test
    void readSideMethodsDelegateToMutationSupport() {
        List<ExpenseApprovalPendingItemVO> expected = List.of(new ExpenseApprovalPendingItemVO());
        ExpenseApprovalDomainSupport support = new ExpenseApprovalDomainSupport(expenseDocumentMutationSupport);
        when(expenseDocumentMutationSupport.listPendingApprovals(1L)).thenReturn(expected);

        List<ExpenseApprovalPendingItemVO> actual = support.listPendingApprovals(1L);

        assertSame(expected, actual);
        verify(expenseDocumentMutationSupport).listPendingApprovals(1L);
    }

    @Test
    void approvalActionsDelegateToMutationSupport() {
        ExpenseApprovalActionDTO dto = new ExpenseApprovalActionDTO();
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        ExpenseApprovalDomainSupport support = new ExpenseApprovalDomainSupport(expenseDocumentMutationSupport);
        when(expenseDocumentMutationSupport.rejectTask(1L, "tester", 10L, dto)).thenReturn(detail);

        ExpenseDocumentDetailVO actual = support.rejectTask(1L, "tester", 10L, dto);

        assertSame(detail, actual);
        verify(expenseDocumentMutationSupport).rejectTask(1L, "tester", 10L, dto);
    }
}
