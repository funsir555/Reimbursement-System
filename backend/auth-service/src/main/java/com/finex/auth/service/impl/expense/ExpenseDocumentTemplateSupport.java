package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.entity.ProcessDocumentInstance;
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
        ProcessDocumentInstance instance = expenseDocumentMutationSupport.requireDocument(documentCode);
        expenseDocumentMutationSupport.requireSubmitter(instance, userId);
        return buildEditContext(userId, instance, null, "RESUBMIT");
    }

    ExpenseDocumentEditContextVO buildEditContext(Long userId, ProcessDocumentInstance instance, Long taskId, String editMode) {
        ExpenseCreateTemplateDetailVO templateDetail = getTemplateDetail(userId, instance.getTemplateCode());
        ExpenseDocumentEditContextVO context = new ExpenseDocumentEditContextVO();
        context.setEditMode(editMode);
        context.setDocumentCode(instance.getDocumentCode());
        context.setTaskId(taskId);
        copyTemplateDetail(templateDetail, context);
        context.setFormData(expenseDocumentMutationSupport.readFormData(instance.getFormDataJson()));
        context.setExpenseDetails(expenseDocumentMutationSupport.loadExpenseDetails(instance.getDocumentCode()).stream()
                .map(expenseDocumentMutationSupport::toRuntimeExpenseDetailDTO)
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
