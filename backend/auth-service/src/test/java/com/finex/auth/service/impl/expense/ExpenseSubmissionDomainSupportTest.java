package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
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
    private ExpenseDocumentTemplateDomainSupport expenseDocumentTemplateDomainSupport;
    @Mock
    private ExpenseDocumentMutationDomainSupport expenseDocumentMutationDomainSupport;
    @Mock
    private ExpenseManualApproverPreviewSupport expenseManualApproverPreviewSupport;

    @Test
    void templateReadsUseTemplateDomainSupport() {
        List<ExpenseCreateTemplateSummaryVO> expected = List.of(new ExpenseCreateTemplateSummaryVO());
        ExpenseSubmissionDomainSupport support = new ExpenseSubmissionDomainSupport(
                expenseDocumentTemplateDomainSupport,
                expenseDocumentMutationDomainSupport,
                expenseManualApproverPreviewSupport
        );
        when(expenseDocumentTemplateDomainSupport.listAvailableTemplates()).thenReturn(expected);

        List<ExpenseCreateTemplateSummaryVO> actual = support.listAvailableTemplates();

        assertSame(expected, actual);
        verify(expenseDocumentTemplateDomainSupport).listAvailableTemplates();
    }

    @Test
    void submitUsesMutationDomainSupport() {
        ExpenseDocumentSubmitDTO dto = new ExpenseDocumentSubmitDTO();
        ExpenseDocumentSubmitResultVO expected = new ExpenseDocumentSubmitResultVO();
        ExpenseSubmissionDomainSupport support = new ExpenseSubmissionDomainSupport(
                expenseDocumentTemplateDomainSupport,
                expenseDocumentMutationDomainSupport,
                expenseManualApproverPreviewSupport
        );
        when(expenseDocumentMutationDomainSupport.submitDocument(1L, "tester", dto)).thenReturn(expected);

        ExpenseDocumentSubmitResultVO actual = support.submitDocument(1L, "tester", dto);

        assertSame(expected, actual);
        verify(expenseDocumentMutationDomainSupport).submitDocument(1L, "tester", dto);
    }

    @Test
    void saveDraftUsesMutationAndTemplateSupports() {
        ExpenseDocumentUpdateDTO dto = new ExpenseDocumentUpdateDTO();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        ExpenseDocumentEditContextVO expected = new ExpenseDocumentEditContextVO();
        ExpenseSubmissionDomainSupport support = new ExpenseSubmissionDomainSupport(
                expenseDocumentTemplateDomainSupport,
                expenseDocumentMutationDomainSupport,
                expenseManualApproverPreviewSupport
        );
        when(expenseDocumentMutationDomainSupport.saveDraftDocument(1L, "DOC-1", dto)).thenReturn(instance);
        when(expenseDocumentTemplateDomainSupport.buildEditContext(1L, instance, null, "RESUBMIT")).thenReturn(expected);

        ExpenseDocumentEditContextVO actual = support.saveDraftDocument(1L, "DOC-1", dto);

        assertSame(expected, actual);
        verify(expenseDocumentMutationDomainSupport).saveDraftDocument(1L, "DOC-1", dto);
        verify(expenseDocumentTemplateDomainSupport).buildEditContext(1L, instance, null, "RESUBMIT");
    }
}
