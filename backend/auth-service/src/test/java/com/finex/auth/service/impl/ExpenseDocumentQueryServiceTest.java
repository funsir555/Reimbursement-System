package com.finex.auth.service.impl;

import com.finex.auth.dto.ExpenseDocumentCommentDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentReminderDTO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.service.impl.expense.ExpenseQueryDomainSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentQueryServiceTest {

    @Mock
    private ExpenseQueryDomainSupport expenseQueryDomainSupport;

    @Test
    void listOutstandingDocumentsDelegatesToMutationSupport() {
        List<ExpenseSummaryVO> expected = List.of(new ExpenseSummaryVO());
        ExpenseDocumentQueryService service = new ExpenseDocumentQueryService(expenseQueryDomainSupport);
        when(expenseQueryDomainSupport.listOutstandingDocuments(1L, "LOAN")).thenReturn(expected);

        List<ExpenseSummaryVO> actual = service.listOutstandingDocuments(1L, "LOAN");

        assertSame(expected, actual);
        verify(expenseQueryDomainSupport).listOutstandingDocuments(1L, "LOAN");
    }

    @Test
    void getDocumentDetailDelegatesToMutationSupport() {
        ExpenseDocumentDetailVO expected = new ExpenseDocumentDetailVO();
        ExpenseDocumentQueryService service = new ExpenseDocumentQueryService(expenseQueryDomainSupport);
        when(expenseQueryDomainSupport.getDocumentDetail(1L, "DOC-1", false)).thenReturn(expected);

        ExpenseDocumentDetailVO actual = service.getDocumentDetail(1L, "DOC-1", false);

        assertSame(expected, actual);
        verify(expenseQueryDomainSupport).getDocumentDetail(1L, "DOC-1", false);
    }

    @Test
    void recallAndEditContextDelegateToMutationSupport() {
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        ExpenseDocumentEditContextVO context = new ExpenseDocumentEditContextVO();
        ExpenseDocumentQueryService service = new ExpenseDocumentQueryService(expenseQueryDomainSupport);
        when(expenseQueryDomainSupport.recallDocument(1L, "tester", "DOC-1")).thenReturn(detail);
        when(expenseQueryDomainSupport.getDocumentEditContext(1L, "DOC-1")).thenReturn(context);

        assertSame(detail, service.recallDocument(1L, "tester", "DOC-1"));
        assertSame(context, service.getDocumentEditContext(1L, "DOC-1"));

        verify(expenseQueryDomainSupport).recallDocument(1L, "tester", "DOC-1");
        verify(expenseQueryDomainSupport).getDocumentEditContext(1L, "DOC-1");
    }

    @Test
    void commentAndRemindDelegateToMutationSupport() {
        ExpenseDocumentCommentDTO commentDTO = new ExpenseDocumentCommentDTO();
        ExpenseDocumentReminderDTO reminderDTO = new ExpenseDocumentReminderDTO();
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        ExpenseDocumentQueryService service = new ExpenseDocumentQueryService(expenseQueryDomainSupport);
        when(expenseQueryDomainSupport.commentOnDocument(1L, "tester", "DOC-1", commentDTO, true)).thenReturn(detail);
        when(expenseQueryDomainSupport.remindDocument(1L, "tester", "DOC-1", reminderDTO)).thenReturn(detail);

        assertSame(detail, service.commentOnDocument(1L, "tester", "DOC-1", commentDTO, true));
        assertSame(detail, service.remindDocument(1L, "tester", "DOC-1", reminderDTO));

        verify(expenseQueryDomainSupport).commentOnDocument(1L, "tester", "DOC-1", commentDTO, true);
        verify(expenseQueryDomainSupport).remindDocument(1L, "tester", "DOC-1", reminderDTO);
    }
}
