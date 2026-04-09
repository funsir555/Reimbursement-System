package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseQueryDomainSupportTest {

    @Mock
    private ExpenseDocumentMutationSupport expenseDocumentMutationSupport;
    @Mock
    private ExpenseDocumentTemplateSupport expenseDocumentTemplateSupport;

    @Test
    void summaryAndDetailReadsUseMutationSupport() {
        List<ExpenseSummaryVO> summaries = List.of(new ExpenseSummaryVO());
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        ExpenseQueryDomainSupport support = new ExpenseQueryDomainSupport(
                expenseDocumentMutationSupport,
                expenseDocumentTemplateSupport
        );
        when(expenseDocumentMutationSupport.listExpenseSummaries(1L)).thenReturn(summaries);
        when(expenseDocumentMutationSupport.getDocumentDetail(1L, "DOC-001", false)).thenReturn(detail);

        assertSame(summaries, support.listExpenseSummaries(1L));
        assertSame(detail, support.getDocumentDetail(1L, "DOC-001", false));

        verify(expenseDocumentMutationSupport).listExpenseSummaries(1L);
        verify(expenseDocumentMutationSupport).getDocumentDetail(1L, "DOC-001", false);
    }

    @Test
    void editContextUsesTemplateSupport() {
        ExpenseDocumentEditContextVO context = new ExpenseDocumentEditContextVO();
        ExpenseQueryDomainSupport support = new ExpenseQueryDomainSupport(
                expenseDocumentMutationSupport,
                expenseDocumentTemplateSupport
        );
        when(expenseDocumentTemplateSupport.getDocumentEditContext(1L, "DOC-001")).thenReturn(context);

        ExpenseDocumentEditContextVO actual = support.getDocumentEditContext(1L, "DOC-001");

        assertSame(context, actual);
        verify(expenseDocumentTemplateSupport).getDocumentEditContext(1L, "DOC-001");
    }
}
