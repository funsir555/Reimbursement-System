package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseSubmissionDomainSupport {

    private final ExpenseDocumentTemplateDomainSupport expenseDocumentTemplateDomainSupport;
    private final ExpenseDocumentMutationDomainSupport expenseDocumentMutationDomainSupport;

    public List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates() {
        return expenseDocumentTemplateDomainSupport.listAvailableTemplates();
    }

    public ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode) {
        return expenseDocumentTemplateDomainSupport.getTemplateDetail(userId, templateCode);
    }

    public List<ExpenseCreateVendorOptionVO> listVendorOptions(Long userId, String keyword, Boolean includeDisabled) {
        return expenseDocumentTemplateDomainSupport.listVendorOptions(userId, keyword, includeDisabled);
    }

    public List<ExpenseCreatePayeeOptionVO> listPayeeOptions(Long userId, String keyword, Boolean personalOnly) {
        return expenseDocumentTemplateDomainSupport.listPayeeOptions(userId, keyword, personalOnly);
    }

    public List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(
            Long userId,
            String keyword,
            String linkageMode,
            String payeeName,
            String counterpartyCode
    ) {
        return expenseDocumentTemplateDomainSupport.listPayeeAccountOptions(
                userId,
                keyword,
                linkageMode,
                payeeName,
                counterpartyCode
        );
    }

    public ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto) {
        return expenseDocumentMutationDomainSupport.submitDocument(userId, username, dto);
    }

    public ExpenseDocumentSubmitResultVO resubmitDocument(Long userId, String username, String documentCode, ExpenseDocumentUpdateDTO dto) {
        return expenseDocumentMutationDomainSupport.resubmitDocument(userId, username, documentCode, dto);
    }
}
