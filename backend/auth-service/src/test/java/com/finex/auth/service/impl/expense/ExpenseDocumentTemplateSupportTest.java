package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.entity.ProcessDocumentInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void getDocumentEditContextBuildsFromTemplateAndDocumentSnapshots() {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");
        instance.setTemplateCode("TPL-001");
        instance.setFormDataJson("{}");

        ExpenseCreateTemplateDetailVO templateDetail = new ExpenseCreateTemplateDetailVO();
        templateDetail.setTemplateCode("TPL-001");
        templateDetail.setTemplateName("差旅报销");
        templateDetail.setTemplateType("report");

        ExpenseDocumentTemplateSupport support = new ExpenseDocumentTemplateSupport(expenseDocumentMutationSupport);
        when(expenseDocumentMutationSupport.requireDocument("DOC-001")).thenReturn(instance);
        when(expenseDocumentMutationSupport.getTemplateDetail(1L, "TPL-001")).thenReturn(templateDetail);
        when(expenseDocumentMutationSupport.readFormData("{}")).thenReturn(Map.of("reason", "trip"));
        when(expenseDocumentMutationSupport.loadExpenseDetails("DOC-001")).thenReturn(List.of());

        ExpenseDocumentEditContextVO actual = support.getDocumentEditContext(1L, "DOC-001");

        assertEquals("RESUBMIT", actual.getEditMode());
        assertEquals("DOC-001", actual.getDocumentCode());
        assertEquals("TPL-001", actual.getTemplateCode());
        assertEquals("差旅报销", actual.getTemplateName());
        assertEquals("trip", actual.getFormData().get("reason"));
        verify(expenseDocumentMutationSupport).requireSubmitter(instance, 1L);
    }
}
