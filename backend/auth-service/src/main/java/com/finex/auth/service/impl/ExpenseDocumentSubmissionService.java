package com.finex.auth.service.impl;

import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.service.impl.expense.ExpenseSubmissionDomainSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseDocumentSubmissionService {

    private final ExpenseSubmissionDomainSupport expenseSubmissionDomainSupport;

    public List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates() {
        return expenseSubmissionDomainSupport.listAvailableTemplates();
    }

    public ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode) {
        return expenseSubmissionDomainSupport.getTemplateDetail(userId, templateCode);
    }

    public List<ExpenseCreateVendorOptionVO> listVendorOptions(Long userId, String keyword, Boolean includeDisabled) {
        return expenseSubmissionDomainSupport.listVendorOptions(userId, keyword, includeDisabled);
    }

    public List<ExpenseCreatePayeeOptionVO> listPayeeOptions(Long userId, String keyword, Boolean personalOnly) {
        return expenseSubmissionDomainSupport.listPayeeOptions(userId, keyword, personalOnly);
    }

    public List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(
            Long userId,
            String keyword,
            String linkageMode,
            String payeeName,
            String counterpartyCode
    ) {
        return expenseSubmissionDomainSupport.listPayeeAccountOptions(
                userId,
                keyword,
                linkageMode,
                payeeName,
                counterpartyCode
        );
    }

    public ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto) {
        return expenseSubmissionDomainSupport.submitDocument(userId, username, dto);
    }

    public ExpenseDocumentSubmitResultVO resubmitDocument(Long userId, String username, String documentCode, ExpenseDocumentUpdateDTO dto) {
        return expenseSubmissionDomainSupport.resubmitDocument(userId, username, documentCode, dto);
    }
}
