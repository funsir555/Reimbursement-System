package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessTemplateCategory;
import com.finex.auth.mapper.ProcessTemplateCategoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseTemplateCategorySupportTest {

    @Mock
    private ProcessTemplateCategoryMapper processTemplateCategoryMapper;

    @Test
    void loadCategoryNameMapUsesEnabledCategoryConfigWhenPresent() {
        ExpenseTemplateCategorySupport support = new ExpenseTemplateCategorySupport(processTemplateCategoryMapper);
        ProcessTemplateCategory category = new ProcessTemplateCategory();
        category.setCategoryCode("enterprise-payment");
        category.setCategoryName("企业往来类");
        when(processTemplateCategoryMapper.selectList(any())).thenReturn(List.of(category));

        Map<String, String> actual = support.loadCategoryNameMap();

        assertEquals("企业往来类", actual.get("enterprise-payment"));
        assertEquals("员工费用类", actual.get("employee-expense"));
        assertEquals("事项申请类", actual.get("business-application"));
    }

    @Test
    void loadCategoryNameMapFallsBackToDefaultLabelsWhenConfigIsMissing() {
        ExpenseTemplateCategorySupport support = new ExpenseTemplateCategorySupport(processTemplateCategoryMapper);
        when(processTemplateCategoryMapper.selectList(any())).thenReturn(List.of());

        Map<String, String> actual = support.loadCategoryNameMap();

        assertEquals("企业往来类", actual.get("enterprise-payment"));
        assertEquals("员工费用类", actual.get("employee-expense"));
        assertEquals("事项申请类", actual.get("business-application"));
    }
}
