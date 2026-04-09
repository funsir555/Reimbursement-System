package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentTemplateSupportTest {

    @Mock
    private ExpenseDocumentMutationSupport expenseDocumentMutationSupport;

    @Test
    void listAvailableTemplatesDelegatesToMutationSupport() {
        List<ExpenseCreateTemplateSummaryVO> expected = List.of(new ExpenseCreateTemplateSummaryVO());
        ExpenseDocumentTemplateSupport support = new ExpenseDocumentTemplateSupport(expenseDocumentMutationSupport);
        when(expenseDocumentMutationSupport.listAvailableTemplates()).thenReturn(expected);

        List<ExpenseCreateTemplateSummaryVO> actual = support.listAvailableTemplates();

        assertSame(expected, actual);
        verify(expenseDocumentMutationSupport).listAvailableTemplates();
    }

    @Test
    void getDocumentEditContextDelegatesToMutationSupport() {
        ExpenseDocumentEditContextVO expected = new ExpenseDocumentEditContextVO();
        ExpenseDocumentTemplateSupport support = new ExpenseDocumentTemplateSupport(expenseDocumentMutationSupport);
        when(expenseDocumentMutationSupport.getDocumentEditContext(1L, "DOC-001")).thenReturn(expected);

        ExpenseDocumentEditContextVO actual = support.getDocumentEditContext(1L, "DOC-001");

        assertSame(expected, actual);
        verify(expenseDocumentMutationSupport).getDocumentEditContext(1L, "DOC-001");
    }
}
