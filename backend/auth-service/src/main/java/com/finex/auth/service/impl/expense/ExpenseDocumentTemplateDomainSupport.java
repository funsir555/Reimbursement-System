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

    private final AbstractExpenseDocumentSupport support;
    private final ExpenseDocumentReadSupport readSupport;

    ExpenseDocumentTemplateDomainSupport(
            AbstractExpenseDocumentSupport support,
            ExpenseDocumentReadSupport readSupport
    ) {
        this.support = support;
        this.readSupport = readSupport;
    }

    List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates() {
        return support.listAvailableTemplates();
    }

    ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode) {
        return support.getTemplateDetail(userId, templateCode);
    }

    List<ExpenseCreateVendorOptionVO> listVendorOptions(Long userId, String keyword, Boolean includeDisabled) {
        return support.listVendorOptions(userId, keyword, includeDisabled);
    }

    List<ExpenseCreatePayeeOptionVO> listPayeeOptions(Long userId, String keyword, Boolean personalOnly) {
        return support.listPayeeOptions(userId, keyword, personalOnly);
    }

    List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(
            Long userId,
            String keyword,
            String linkageMode,
            String payeeName,
            String counterpartyCode
    ) {
        return support.listPayeeAccountOptions(userId, keyword, linkageMode, payeeName, counterpartyCode);
    }

    ExpenseDocumentEditContextVO getDocumentEditContext(Long userId, String documentCode) {
        ProcessDocumentInstance instance = readSupport.requireDocument(documentCode);
        readSupport.requireSubmitter(instance, userId);
        return buildEditContext(userId, instance, null, "RESUBMIT");
    }

    ExpenseDocumentEditContextVO buildEditContext(Long userId, ProcessDocumentInstance instance, Long taskId, String editMode) {
        ExpenseCreateTemplateDetailVO templateDetail = getTemplateDetail(userId, instance.getTemplateCode());
        ExpenseDocumentEditContextVO context = new ExpenseDocumentEditContextVO();
        context.setEditMode(editMode);
        context.setDocumentCode(instance.getDocumentCode());
        context.setTaskId(taskId);
        copyTemplateDetail(templateDetail, context);
        context.setFormData(readSupport.readFormData(instance.getFormDataJson()));
        context.setExpenseDetails(readSupport.loadExpenseDetails(instance.getDocumentCode()).stream()
                .map(readSupport::toRuntimeExpenseDetailDTO)
                .toList());
        return context;
    }

    private void copyTemplateDetail(ExpenseCreateTemplateDetailVO source, ExpenseDocumentEditContextVO target) {
        target.setTemplateCode(source.getTemplateCode());
        target.setTemplateName(source.getTemplateName());
        target.setTemplateType(source.getTemplateType());
        target.setTemplateTypeLabel(source.getTemplateTypeLabel());
        target.setCategoryCode(source.getCategoryCode());
        target.setTemplateDescription(source.getTemplateDescription());
        target.setFormDesignCode(source.getFormDesignCode());
        target.setApprovalFlowCode(source.getApprovalFlowCode());
        target.setFlowName(source.getFlowName());
        target.setFormName(source.getFormName());
        target.setSchema(source.getSchema());
        target.setExpenseDetailDesignCode(source.getExpenseDetailDesignCode());
        target.setExpenseDetailDesignName(source.getExpenseDetailDesignName());
        target.setExpenseDetailType(source.getExpenseDetailType());
        target.setExpenseDetailTypeLabel(source.getExpenseDetailTypeLabel());
        target.setExpenseDetailModeDefault(source.getExpenseDetailModeDefault());
        target.setExpenseDetailSchema(source.getExpenseDetailSchema());
        target.setSharedArchives(source.getSharedArchives());
        target.setExpenseDetailSharedArchives(source.getExpenseDetailSharedArchives());
        target.setCompanyOptions(source.getCompanyOptions());
        target.setDepartmentOptions(source.getDepartmentOptions());
        target.setExpenseTypeOptions(source.getExpenseTypeOptions());
        target.setExpenseTypeInvoiceFreeModeMap(source.getExpenseTypeInvoiceFreeModeMap());
        target.setCurrentUserDeptId(source.getCurrentUserDeptId());
        target.setCurrentUserDeptName(source.getCurrentUserDeptName());
    }
}
