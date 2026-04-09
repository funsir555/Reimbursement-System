package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseDocumentTemplateSupport {

    private final ExpenseDocumentMutationSupport expenseDocumentMutationSupport;

    public List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates() {
        return expenseDocumentMutationSupport.listAvailableTemplates();
    }

    public ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode) {
        return expenseDocumentMutationSupport.getTemplateDetail(userId, templateCode);
    }

    public List<ExpenseCreateVendorOptionVO> listVendorOptions(Long userId, String keyword, Boolean includeDisabled) {
        return expenseDocumentMutationSupport.listVendorOptions(userId, keyword, includeDisabled);
    }

    public List<ExpenseCreatePayeeOptionVO> listPayeeOptions(Long userId, String keyword, Boolean personalOnly) {
        return expenseDocumentMutationSupport.listPayeeOptions(userId, keyword, personalOnly);
    }

    public List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(
            Long userId,
            String keyword,
            String linkageMode,
            String payeeName,
            String counterpartyCode
    ) {
        return expenseDocumentMutationSupport.listPayeeAccountOptions(
                userId,
                keyword,
                linkageMode,
                payeeName,
                counterpartyCode
        );
    }

    public ExpenseDocumentEditContextVO getDocumentEditContext(Long userId, String documentCode) {
        return expenseDocumentMutationSupport.getDocumentEditContext(userId, documentCode);
    }
}
