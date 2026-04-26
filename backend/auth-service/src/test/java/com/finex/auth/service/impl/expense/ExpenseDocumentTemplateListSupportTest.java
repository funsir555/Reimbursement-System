package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentTemplateListSupportTest {

    @Mock private ProcessDocumentTemplateMapper templateMapper;
    @Mock private ExpenseTemplateCategorySupport expenseTemplateCategorySupport;
    @Mock private AbstractExpenseDocumentSupport support;

    @Test
    void listAvailableTemplatesFiltersAndMapsWithCategoryNames() {
        ProcessDocumentTemplate available = new ProcessDocumentTemplate();
        available.setTemplateCode("TPL-1");
        ProcessDocumentTemplate hidden = new ProcessDocumentTemplate();
        hidden.setTemplateCode("TPL-2");
        ExpenseCreateTemplateSummaryVO summary = new ExpenseCreateTemplateSummaryVO();
        ExpenseDocumentTemplateListSupport listSupport = new ExpenseDocumentTemplateListSupport(
                templateMapper,
                expenseTemplateCategorySupport,
                support
        );
        when(expenseTemplateCategorySupport.loadCategoryNameMap()).thenReturn(Map.of("TRAVEL", "Travel"));
        when(templateMapper.selectList(any())).thenReturn(List.of(available, hidden));
        when(support.isTemplateAvailableForCreate(available)).thenReturn(true);
        when(support.isTemplateAvailableForCreate(hidden)).thenReturn(false);
        when(support.toTemplateSummary(available, Map.of("TRAVEL", "Travel"))).thenReturn(summary);

        List<ExpenseCreateTemplateSummaryVO> actual = listSupport.listAvailableTemplates();

        assertEquals(1, actual.size());
        assertSame(summary, actual.get(0));
        verify(support).isTemplateAvailableForCreate(available);
        verify(support).toTemplateSummary(available, Map.of("TRAVEL", "Travel"));
    }
}
