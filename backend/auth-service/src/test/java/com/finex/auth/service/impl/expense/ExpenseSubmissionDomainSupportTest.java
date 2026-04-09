package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseSubmissionDomainSupportTest {

    @Mock
    private ExpenseDocumentTemplateSupport expenseDocumentTemplateSupport;
    @Mock
    private ExpenseDocumentMutationSupport expenseDocumentMutationSupport;

    @Test
    void templateReadsUseTemplateSupport() {
        List<ExpenseCreateTemplateSummaryVO> expected = List.of(new ExpenseCreateTemplateSummaryVO());
        ExpenseSubmissionDomainSupport support = new ExpenseSubmissionDomainSupport(
                expenseDocumentTemplateSupport,
                expenseDocumentMutationSupport
        );
        when(expenseDocumentTemplateSupport.listAvailableTemplates()).thenReturn(expected);

        List<ExpenseCreateTemplateSummaryVO> actual = support.listAvailableTemplates();

        assertSame(expected, actual);
        verify(expenseDocumentTemplateSupport).listAvailableTemplates();
    }

    @Test
    void submitUsesMutationSupport() {
        ExpenseDocumentSubmitDTO dto = new ExpenseDocumentSubmitDTO();
        ExpenseDocumentSubmitResultVO expected = new ExpenseDocumentSubmitResultVO();
        ExpenseSubmissionDomainSupport support = new ExpenseSubmissionDomainSupport(
                expenseDocumentTemplateSupport,
                expenseDocumentMutationSupport
        );
        when(expenseDocumentMutationSupport.submitDocument(1L, "tester", dto)).thenReturn(expected);

        ExpenseDocumentSubmitResultVO actual = support.submitDocument(1L, "tester", dto);

        assertSame(expected, actual);
        verify(expenseDocumentMutationSupport).submitDocument(1L, "tester", dto);
    }
}
