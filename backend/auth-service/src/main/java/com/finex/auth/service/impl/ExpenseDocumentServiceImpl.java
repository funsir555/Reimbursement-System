// 业务域：报销单录入、流转与查询
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl;

// Phase 1 governance freeze: do not add new public business entrypoints here.
// New expense use cases should land in split domain services and be wired here as delegation only.

import com.finex.auth.dto.ExpenseActionUserOptionVO;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseBankLinkConfigVO;
import com.finex.auth.dto.ExpenseBankLinkSaveDTO;
import com.finex.auth.dto.ExpenseBankLinkSummaryVO;
import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseDocumentCommentDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentNavigationVO;
import com.finex.auth.dto.ExpenseDocumentPickerVO;
import com.finex.auth.dto.ExpenseDocumentReminderDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.ExpenseTaskAddSignDTO;
import com.finex.auth.dto.ExpenseTaskTransferDTO;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.impl.expense.ExpenseRelationWriteOffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ExpenseDocumentServiceImpl：service 入口实现。
 * 接住上层请求，并把 报销单单据相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
@RequiredArgsConstructor
public class ExpenseDocumentServiceImpl implements ExpenseDocumentService {

    private final ExpenseDocumentSubmissionService expenseDocumentSubmissionService;
    private final ExpenseDocumentQueryService expenseDocumentQueryService;
    private final ExpenseApprovalWorkflowService expenseApprovalWorkflowService;
    private final ExpensePaymentWorkflowService expensePaymentWorkflowService;
    private final ExpenseMaintenanceService expenseMaintenanceService;
    private final ExpenseRelationWriteOffService expenseRelationWriteOffService;

    /**
     * 查询可用模板列表。
     */
    @Override
    public List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates() {
        return expenseDocumentSubmissionService.listAvailableTemplates();
    }

    /**
     * 获取模板明细。
     */
    @Override
    public ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode) {
        return expenseDocumentSubmissionService.getTemplateDetail(userId, templateCode);
    }

    /**
     * 查询供应商选项。
     */
    @Override
    public List<ExpenseCreateVendorOptionVO> listVendorOptions(
            Long userId,
            String keyword,
            Boolean includeDisabled,
            String paymentCompanyId
    ) {
        return expenseDocumentSubmissionService.listVendorOptions(userId, keyword, includeDisabled, paymentCompanyId);
    }

    /**
     * 查询收款方选项。
     */
    @Override
    public List<ExpenseCreatePayeeOptionVO> listPayeeOptions(Long userId, String keyword, Boolean personalOnly) {
        return expenseDocumentSubmissionService.listPayeeOptions(userId, keyword, personalOnly);
    }

    /**
     * 查询收款方账户选项。
     */
    @Override
    public List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(
            Long userId,
            String keyword,
            String linkageMode,
            String payeeName,
            String counterpartyCode,
            String paymentCompanyId
    ) {
        return expenseDocumentSubmissionService.listPayeeAccountOptions(
                userId,
                keyword,
                linkageMode,
                payeeName,
                counterpartyCode,
                paymentCompanyId
        );
    }

    /**
     * 提交单据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto) {
        return expenseDocumentSubmissionService.submitDocument(userId, username, dto);
    }

    /**
     * 查询报销单Summaries列表。
     */
    @Override
    public List<ExpenseSummaryVO> listExpenseSummaries(Long userId) {
        return expenseDocumentQueryService.listExpenseSummaries(userId);
    }

    /**
     * 查询查询单据Summaries列表。
     */
    @Override
    public List<ExpenseSummaryVO> listQueryDocumentSummaries(Long userId) {
        return expenseDocumentQueryService.listQueryDocumentSummaries(userId);
    }

    /**
     * 查询Outstanding单据列表。
     */
    @Override
    public List<ExpenseSummaryVO> listOutstandingDocuments(Long userId, String kind) {
        return expenseDocumentQueryService.listOutstandingDocuments(userId, kind);
    }

    /**
     * 修复Misapproved单据按RootContainerBug。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<String> repairMisapprovedDocumentsByRootContainerBug() {
        return expenseMaintenanceService.repairMisapprovedDocumentsByRootContainerBug();
    }

    /**
     * 获取单据明细。
     */
    @Override
    public ExpenseDocumentDetailVO getDocumentDetail(Long userId, String documentCode, boolean allowCrossView) {
        return expenseDocumentQueryService.getDocumentDetail(userId, documentCode, allowCrossView);
    }

    /**
     * 获取报销单明细。
     */
    @Override
    public ExpenseDetailInstanceDetailVO getExpenseDetail(Long userId, String documentCode, String detailNo, boolean allowCrossView) {
        return expenseDocumentQueryService.getExpenseDetail(userId, documentCode, detailNo, allowCrossView);
    }

    /**
     * 获取单据Picker。
     */
    @Override
    public ExpenseDocumentPickerVO getDocumentPicker(
            Long userId,
            String relationType,
            List<String> templateTypes,
            String keyword,
            Integer page,
            Integer pageSize,
            String excludeDocumentCode,
            boolean allowCrossView
    ) {
        return expenseRelationWriteOffService.getDocumentPicker(
                userId,
                relationType,
                templateTypes,
                keyword,
                page,
                pageSize,
                excludeDocumentCode,
                allowCrossView
        );
    }

    /**
     * 获取首页看板写入OffSourceReportPicker。
     */
    @Override
    public ExpenseDocumentPickerVO getDashboardWriteOffSourceReportPicker(
            Long userId,
            String targetDocumentCode,
            String keyword,
            Integer page,
            Integer pageSize
    ) {
        return expenseRelationWriteOffService.getDashboardWriteOffSourceReportPicker(
                userId,
                targetDocumentCode,
                keyword,
                page,
                pageSize
        );
    }

    /**
     * 处理报销单单据中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindDashboardWriteOff(Long userId, String targetDocumentCode, String sourceReportDocumentCode) {
        return expenseRelationWriteOffService.bindDashboardWriteOff(userId, targetDocumentCode, sourceReportDocumentCode);
    }

    /**
     * 处理报销单单据中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO recallDocument(Long userId, String username, String documentCode) {
        return expenseDocumentQueryService.recallDocument(userId, username, documentCode);
    }

    /**
     * 处理报销单单据中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO commentOnDocument(Long userId, String username, String documentCode, ExpenseDocumentCommentDTO dto, boolean allowCrossView) {
        return expenseDocumentQueryService.commentOnDocument(userId, username, documentCode, dto, allowCrossView);
    }

    /**
     * 处理报销单单据中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO remindDocument(Long userId, String username, String documentCode, ExpenseDocumentReminderDTO dto) {
        return expenseDocumentQueryService.remindDocument(userId, username, documentCode, dto);
    }

    /**
     * 获取单据Navigation。
     */
    @Override
    public ExpenseDocumentNavigationVO getDocumentNavigation(Long userId, String documentCode, boolean approvalViewer) {
        return expenseDocumentQueryService.getDocumentNavigation(userId, documentCode, approvalViewer);
    }

    /**
     * 获取单据Edit上下文。
     */
    @Override
    public ExpenseDocumentEditContextVO getDocumentEditContext(Long userId, String documentCode) {
        return expenseDocumentQueryService.getDocumentEditContext(userId, documentCode);
    }

    /**
     * 重新提交单据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentSubmitResultVO resubmitDocument(Long userId, String username, String documentCode, ExpenseDocumentUpdateDTO dto) {
        return expenseDocumentSubmissionService.resubmitDocument(userId, username, documentCode, dto);
    }

    /**
     * 查询Pending审批列表。
     */
    @Override
    public List<ExpenseApprovalPendingItemVO> listPendingApprovals(Long userId) {
        return expenseApprovalWorkflowService.listPendingApprovals(userId);
    }

    /**
     * 查询付款Orders列表。
     */
    @Override
    public List<ExpensePaymentOrderVO> listPaymentOrders(Long userId, String status) {
        return expensePaymentWorkflowService.listPaymentOrders(userId, status);
    }

    /**
     * 查询银行Links列表。
     */
    @Override
    public List<ExpenseBankLinkSummaryVO> listBankLinks() {
        return expensePaymentWorkflowService.listBankLinks();
    }

    /**
     * 获取银行Link。
     */
    @Override
    public ExpenseBankLinkConfigVO getBankLink(Long companyBankAccountId) {
        return expensePaymentWorkflowService.getBankLink(companyBankAccountId);
    }

    /**
     * 更新银行Link。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseBankLinkConfigVO updateBankLink(Long companyBankAccountId, ExpenseBankLinkSaveDTO dto) {
        return expensePaymentWorkflowService.updateBankLink(companyBankAccountId, dto);
    }

    /**
     * 处理报销单单据中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO handleCmbCloudCallback(ExpenseBankCallbackDTO dto) {
        return expensePaymentWorkflowService.handleCmbCloudCallback(dto);
    }

    /**
     * 执行银行回执Polling。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void runBankReceiptPolling() {
        expensePaymentWorkflowService.runBankReceiptPolling();
    }

    /**
     * 审批通过任务。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO approveTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expenseApprovalWorkflowService.approveTask(userId, username, taskId, dto);
    }

    /**
     * 审批驳回任务。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO rejectTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expenseApprovalWorkflowService.rejectTask(userId, username, taskId, dto);
    }

    /**
     * 处理报销单单据中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO startPaymentTask(Long userId, String username, Long taskId) {
        return expensePaymentWorkflowService.startPaymentTask(userId, username, taskId);
    }

    /**
     * 处理报销单单据中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO completePaymentTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expensePaymentWorkflowService.completePaymentTask(userId, username, taskId, dto);
    }

    /**
     * 处理报销单单据中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO markPaymentTaskException(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expensePaymentWorkflowService.markPaymentTaskException(userId, username, taskId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectPaymentTasks(Long userId, String username, List<Long> taskIds, ExpenseApprovalActionDTO dto) {
        return expensePaymentWorkflowService.rejectPaymentTasks(userId, username, taskIds, dto);
    }

    /**
     * 获取任务Modify上下文。
     */
    @Override
    public ExpenseDocumentEditContextVO getTaskModifyContext(Long userId, Long taskId) {
        return expenseApprovalWorkflowService.getTaskModifyContext(userId, taskId);
    }

    /**
     * 处理报销单单据中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO modifyTaskDocument(Long userId, String username, Long taskId, ExpenseDocumentUpdateDTO dto) {
        return expenseApprovalWorkflowService.modifyTaskDocument(userId, username, taskId, dto);
    }

    /**
     * 处理报销单单据中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO transferTask(Long userId, String username, Long taskId, ExpenseTaskTransferDTO dto) {
        return expenseApprovalWorkflowService.transferTask(userId, username, taskId, dto);
    }

    /**
     * 处理报销单单据中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO addSignTask(Long userId, String username, Long taskId, ExpenseTaskAddSignDTO dto) {
        return expenseApprovalWorkflowService.addSignTask(userId, username, taskId, dto);
    }

    /**
     * 查询Action用户。
     */
    @Override
    public List<ExpenseActionUserOptionVO> searchActionUsers(Long userId, String keyword) {
        return expenseApprovalWorkflowService.searchActionUsers(userId, keyword);
    }
}
