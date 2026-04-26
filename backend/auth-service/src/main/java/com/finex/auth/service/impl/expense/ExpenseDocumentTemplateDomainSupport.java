package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.entity.ProcessDocumentInstance;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class ExpenseDocumentTemplateDomainSupport {

    private final ExpenseDocumentTemplateListSupport templateListSupport;
    private final ExpenseDocumentTemplateDetailSupport templateDetailSupport;
    private final ExpenseDocumentCounterpartyOptionSupport counterpartyOptionSupport;
    private final ExpenseDocumentEditContextSupport editContextSupport;

    ExpenseDocumentTemplateDomainSupport(
            ExpenseDocumentTemplateListSupport templateListSupport,
            ExpenseDocumentTemplateDetailSupport templateDetailSupport,
            ExpenseDocumentCounterpartyOptionSupport counterpartyOptionSupport,
            ExpenseDocumentEditContextSupport editContextSupport
    ) {
        this.templateListSupport = templateListSupport;
        this.templateDetailSupport = templateDetailSupport;
        this.counterpartyOptionSupport = counterpartyOptionSupport;
        this.editContextSupport = editContextSupport;
    }

    List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates() {
        return templateListSupport.listAvailableTemplates();
    }

    ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode) {
        return templateDetailSupport.getTemplateDetail(userId, templateCode);
    }

    List<ExpenseCreateVendorOptionVO> listVendorOptions(
            Long userId,
            String keyword,
            Boolean includeDisabled,
            String paymentCompanyId
    ) {
        return counterpartyOptionSupport.listVendorOptions(userId, keyword, includeDisabled, paymentCompanyId);
    }

    List<ExpenseCreatePayeeOptionVO> listPayeeOptions(Long userId, String keyword, Boolean personalOnly) {
        return counterpartyOptionSupport.listPayeeOptions(userId, keyword, personalOnly);
    }

    List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(
            Long userId,
            String keyword,
            String linkageMode,
            String payeeName,
            String counterpartyCode,
            String paymentCompanyId
    ) {
        return counterpartyOptionSupport.listPayeeAccountOptions(
                userId,
                keyword,
                linkageMode,
                payeeName,
                counterpartyCode,
                paymentCompanyId
        );
    }

    ExpenseDocumentEditContextVO getDocumentEditContext(Long userId, String documentCode) {
        return editContextSupport.getDocumentEditContext(userId, documentCode);
    }

    ExpenseDocumentEditContextVO buildEditContext(Long userId, ProcessDocumentInstance instance, Long taskId, String editMode) {
        return editContextSupport.buildEditContext(userId, instance, taskId, editMode);
    }
}
