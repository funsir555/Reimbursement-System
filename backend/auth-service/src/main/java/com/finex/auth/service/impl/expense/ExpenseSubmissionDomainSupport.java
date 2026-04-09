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

    private final ExpenseDocumentTemplateSupport expenseDocumentTemplateSupport;
    private final ExpenseDocumentMutationSupport expenseDocumentMutationSupport;

    public List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates() {
        return expenseDocumentTemplateSupport.listAvailableTemplates();
    }

    public ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode) {
        return expenseDocumentTemplateSupport.getTemplateDetail(userId, templateCode);
    }

    public List<ExpenseCreateVendorOptionVO> listVendorOptions(Long userId, String keyword, Boolean includeDisabled) {
        return expenseDocumentTemplateSupport.listVendorOptions(userId, keyword, includeDisabled);
    }

    public List<ExpenseCreatePayeeOptionVO> listPayeeOptions(Long userId, String keyword, Boolean personalOnly) {
        return expenseDocumentTemplateSupport.listPayeeOptions(userId, keyword, personalOnly);
    }

    public List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(
            Long userId,
            String keyword,
            String linkageMode,
            String payeeName,
            String counterpartyCode
    ) {
        return expenseDocumentTemplateSupport.listPayeeAccountOptions(
                userId,
                keyword,
                linkageMode,
                payeeName,
                counterpartyCode
        );
    }

    public ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto) {
        return expenseDocumentMutationSupport.submitDocument(userId, username, dto);
    }

    public ExpenseDocumentSubmitResultVO resubmitDocument(Long userId, String username, String documentCode, ExpenseDocumentUpdateDTO dto) {
        return expenseDocumentMutationSupport.resubmitDocument(userId, username, documentCode, dto);
    }
}
