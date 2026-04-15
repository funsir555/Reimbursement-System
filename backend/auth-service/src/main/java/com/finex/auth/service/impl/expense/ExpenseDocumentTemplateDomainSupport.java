// 业务域：报销单录入、流转与查询
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

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

/**
 * ExpenseDocumentTemplateDomainSupport：领域规则支撑类。
 * 承接 报销单单据模板的核心业务规则。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
class ExpenseDocumentTemplateDomainSupport {

    private final AbstractExpenseDocumentSupport support;
    private final ExpenseDocumentReadSupport readSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
    ExpenseDocumentTemplateDomainSupport(
            AbstractExpenseDocumentSupport support,
            ExpenseDocumentReadSupport readSupport
    ) {
        this.support = support;
        this.readSupport = readSupport;
    }

    /**
     * 查询可用模板列表。
     */
    List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates() {
        return support.listAvailableTemplates();
    }

    /**
     * 获取模板明细。
     */
    ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode) {
        return support.getTemplateDetail(userId, templateCode);
    }

    /**
     * 查询供应商选项。
     */
    List<ExpenseCreateVendorOptionVO> listVendorOptions(Long userId, String keyword, Boolean includeDisabled) {
        return support.listVendorOptions(userId, keyword, includeDisabled);
    }

    /**
     * 查询收款方选项。
     */
    List<ExpenseCreatePayeeOptionVO> listPayeeOptions(Long userId, String keyword, Boolean personalOnly) {
        return support.listPayeeOptions(userId, keyword, personalOnly);
    }

    /**
     * 查询收款方账户选项。
     */
    List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(
            Long userId,
            String keyword,
            String linkageMode,
            String payeeName,
            String counterpartyCode
    ) {
        return support.listPayeeAccountOptions(userId, keyword, linkageMode, payeeName, counterpartyCode);
    }

    /**
     * 获取单据Edit上下文。
     */
    ExpenseDocumentEditContextVO getDocumentEditContext(Long userId, String documentCode) {
        ProcessDocumentInstance instance = readSupport.requireDocument(documentCode);
        readSupport.requireSubmitter(instance, userId);
        return buildEditContext(userId, instance, null, "RESUBMIT");
    }

    /**
     * 组装Edit上下文。
     */
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

    /**
     * 复制模板明细。
     */
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
