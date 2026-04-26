package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
class ExpenseDocumentTemplateListSupport {

    private final ProcessDocumentTemplateMapper templateMapper;
    private final ExpenseTemplateCategorySupport expenseTemplateCategorySupport;
    private final AbstractExpenseDocumentSupport support;

    List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates() {
        Map<String, String> categoryNameMap = expenseTemplateCategorySupport.loadCategoryNameMap();
        return templateMapper.selectList(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getEnabled, 1)
                        .orderByAsc(ProcessDocumentTemplate::getSortOrder, ProcessDocumentTemplate::getId)
        ).stream()
                .filter(support::isTemplateAvailableForCreate)
                .map(template -> support.toTemplateSummary(template, categoryNameMap))
                .toList();
    }
}
