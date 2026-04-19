package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.ProcessTemplateCategory;
import com.finex.auth.mapper.ProcessTemplateCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
class ExpenseTemplateCategorySupport {

    private static final Map<String, String> DEFAULT_CATEGORY_NAMES = Map.of(
            "enterprise-payment", "企业往来类",
            "employee-expense", "员工费用类",
            "business-application", "事项申请类"
    );

    private final ProcessTemplateCategoryMapper processTemplateCategoryMapper;

    Map<String, String> loadCategoryNameMap() {
        Map<String, String> categoryNameMap = new LinkedHashMap<>(DEFAULT_CATEGORY_NAMES);
        List<ProcessTemplateCategory> categories = processTemplateCategoryMapper.selectList(
                Wrappers.<ProcessTemplateCategory>lambdaQuery()
                        .eq(ProcessTemplateCategory::getStatus, 1)
                        .orderByAsc(ProcessTemplateCategory::getSortOrder, ProcessTemplateCategory::getId)
        );
        for (ProcessTemplateCategory category : categories) {
            String categoryCode = trimToNull(category.getCategoryCode());
            String categoryName = trimToNull(category.getCategoryName());
            if (categoryCode != null && categoryName != null) {
                categoryNameMap.put(categoryCode, categoryName);
            }
        }
        return categoryNameMap;
    }

    String resolveCategoryName(String categoryCode) {
        String normalizedCode = trimToNull(categoryCode);
        return normalizedCode == null ? null : loadCategoryNameMap().get(normalizedCode);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
