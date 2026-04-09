package com.finex.auth.service.impl;

import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.service.impl.expense.ExpenseSubmissionDomainSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentSubmissionServiceTest {

    @Mock
    private ExpenseSubmissionDomainSupport expenseSubmissionDomainSupport;

    @Test
    void listAvailableTemplatesDelegatesToMutationSupport() {
        ExpenseCreateTemplateSummaryVO summary = new ExpenseCreateTemplateSummaryVO();
        List<ExpenseCreateTemplateSummaryVO> expected = List.of(summary);
        ExpenseDocumentSubmissionService service = new ExpenseDocumentSubmissionService(expenseSubmissionDomainSupport);
        when(expenseSubmissionDomainSupport.listAvailableTemplates()).thenReturn(expected);

        List<ExpenseCreateTemplateSummaryVO> actual = service.listAvailableTemplates();

        assertSame(expected, actual);
        verify(expenseSubmissionDomainSupport).listAvailableTemplates();
    }

    @Test
    void submitDocumentDelegatesToMutationSupport() {
        ExpenseDocumentSubmitDTO dto = new ExpenseDocumentSubmitDTO();
        ExpenseDocumentSubmitResultVO expected = new ExpenseDocumentSubmitResultVO();
        ExpenseDocumentSubmissionService service = new ExpenseDocumentSubmissionService(expenseSubmissionDomainSupport);
        when(expenseSubmissionDomainSupport.submitDocument(1L, "tester", dto)).thenReturn(expected);

        ExpenseDocumentSubmitResultVO actual = service.submitDocument(1L, "tester", dto);

        assertSame(expected, actual);
        verify(expenseSubmissionDomainSupport).submitDocument(1L, "tester", dto);
    }

    @Test
    void resubmitDocumentDelegatesToMutationSupport() {
        ExpenseDocumentUpdateDTO dto = new ExpenseDocumentUpdateDTO();
        ExpenseDocumentSubmitResultVO expected = new ExpenseDocumentSubmitResultVO();
        ExpenseDocumentSubmissionService service = new ExpenseDocumentSubmissionService(expenseSubmissionDomainSupport);
        when(expenseSubmissionDomainSupport.resubmitDocument(1L, "tester", "DOC-1", dto)).thenReturn(expected);

        ExpenseDocumentSubmitResultVO actual = service.resubmitDocument(1L, "tester", "DOC-1", dto);

        assertSame(expected, actual);
        verify(expenseSubmissionDomainSupport).resubmitDocument(1L, "tester", "DOC-1", dto);
    }
}
