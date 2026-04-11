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
    private ExpenseDocumentTemplateDomainSupport expenseDocumentTemplateDomainSupport;

    @Test
    void listAvailableTemplatesDelegatesToDomainSupport() {
        List<ExpenseCreateTemplateSummaryVO> expected = List.of(new ExpenseCreateTemplateSummaryVO());
        ExpenseDocumentTemplateSupport support = new ExpenseDocumentTemplateSupport(expenseDocumentTemplateDomainSupport);
        when(expenseDocumentTemplateDomainSupport.listAvailableTemplates()).thenReturn(expected);

        List<ExpenseCreateTemplateSummaryVO> actual = support.listAvailableTemplates();

        assertSame(expected, actual);
        verify(expenseDocumentTemplateDomainSupport).listAvailableTemplates();
    }

    @Test
    void getDocumentEditContextDelegatesToDomainSupport() {
        ExpenseDocumentEditContextVO expected = new ExpenseDocumentEditContextVO();
        expected.setDocumentCode("DOC-001");
        ExpenseDocumentTemplateSupport support = new ExpenseDocumentTemplateSupport(expenseDocumentTemplateDomainSupport);
        when(expenseDocumentTemplateDomainSupport.getDocumentEditContext(1L, "DOC-001")).thenReturn(expected);

        ExpenseDocumentEditContextVO actual = support.getDocumentEditContext(1L, "DOC-001");

        assertSame(expected, actual);
        verify(expenseDocumentTemplateDomainSupport).getDocumentEditContext(1L, "DOC-001");
    }
}