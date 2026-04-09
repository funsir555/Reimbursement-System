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

@Service
@RequiredArgsConstructor
public class ExpenseDocumentServiceImpl implements ExpenseDocumentService {

    private final ExpenseDocumentSubmissionService expenseDocumentSubmissionService;
    private final ExpenseDocumentQueryService expenseDocumentQueryService;
    private final ExpenseApprovalWorkflowService expenseApprovalWorkflowService;
    private final ExpensePaymentWorkflowService expensePaymentWorkflowService;
    private final ExpenseMaintenanceService expenseMaintenanceService;
    private final ExpenseRelationWriteOffService expenseRelationWriteOffService;

    @Override
    public List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates() {
        return expenseDocumentSubmissionService.listAvailableTemplates();
    }

    @Override
    public ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode) {
        return expenseDocumentSubmissionService.getTemplateDetail(userId, templateCode);
    }

    @Override
    public List<ExpenseCreateVendorOptionVO> listVendorOptions(Long userId, String keyword, Boolean includeDisabled) {
        return expenseDocumentSubmissionService.listVendorOptions(userId, keyword, includeDisabled);
    }

    @Override
    public List<ExpenseCreatePayeeOptionVO> listPayeeOptions(Long userId, String keyword, Boolean personalOnly) {
        return expenseDocumentSubmissionService.listPayeeOptions(userId, keyword, personalOnly);
    }

    @Override
    public List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(
            Long userId,
            String keyword,
            String linkageMode,
            String payeeName,
            String counterpartyCode
    ) {
        return expenseDocumentSubmissionService.listPayeeAccountOptions(userId, keyword, linkageMode, payeeName, counterpartyCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto) {
        return expenseDocumentSubmissionService.submitDocument(userId, username, dto);
    }

    @Override
    public List<ExpenseSummaryVO> listExpenseSummaries(Long userId) {
        return expenseDocumentQueryService.listExpenseSummaries(userId);
    }

    @Override
    public List<ExpenseSummaryVO> listQueryDocumentSummaries(Long userId) {
        return expenseDocumentQueryService.listQueryDocumentSummaries(userId);
    }

    @Override
    public List<ExpenseSummaryVO> listOutstandingDocuments(Long userId, String kind) {
        return expenseDocumentQueryService.listOutstandingDocuments(userId, kind);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<String> repairMisapprovedDocumentsByRootContainerBug() {
        return expenseMaintenanceService.repairMisapprovedDocumentsByRootContainerBug();
    }

    @Override
    public ExpenseDocumentDetailVO getDocumentDetail(Long userId, String documentCode, boolean allowCrossView) {
        return expenseDocumentQueryService.getDocumentDetail(userId, documentCode, allowCrossView);
    }

    @Override
    public ExpenseDetailInstanceDetailVO getExpenseDetail(Long userId, String documentCode, String detailNo, boolean allowCrossView) {
        return expenseDocumentQueryService.getExpenseDetail(userId, documentCode, detailNo, allowCrossView);
    }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindDashboardWriteOff(Long userId, String targetDocumentCode, String sourceReportDocumentCode) {
        return expenseRelationWriteOffService.bindDashboardWriteOff(userId, targetDocumentCode, sourceReportDocumentCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO recallDocument(Long userId, String username, String documentCode) {
        return expenseDocumentQueryService.recallDocument(userId, username, documentCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO commentOnDocument(Long userId, String username, String documentCode, ExpenseDocumentCommentDTO dto, boolean allowCrossView) {
        return expenseDocumentQueryService.commentOnDocument(userId, username, documentCode, dto, allowCrossView);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO remindDocument(Long userId, String username, String documentCode, ExpenseDocumentReminderDTO dto) {
        return expenseDocumentQueryService.remindDocument(userId, username, documentCode, dto);
    }

    @Override
    public ExpenseDocumentNavigationVO getDocumentNavigation(Long userId, String documentCode, boolean approvalViewer) {
        return expenseDocumentQueryService.getDocumentNavigation(userId, documentCode, approvalViewer);
    }

    @Override
    public ExpenseDocumentEditContextVO getDocumentEditContext(Long userId, String documentCode) {
        return expenseDocumentQueryService.getDocumentEditContext(userId, documentCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentSubmitResultVO resubmitDocument(Long userId, String username, String documentCode, ExpenseDocumentUpdateDTO dto) {
        return expenseDocumentSubmissionService.resubmitDocument(userId, username, documentCode, dto);
    }

    @Override
    public List<ExpenseApprovalPendingItemVO> listPendingApprovals(Long userId) {
        return expenseApprovalWorkflowService.listPendingApprovals(userId);
    }

    @Override
    public List<ExpensePaymentOrderVO> listPaymentOrders(Long userId, String status) {
        return expensePaymentWorkflowService.listPaymentOrders(userId, status);
    }

    @Override
    public List<ExpenseBankLinkSummaryVO> listBankLinks() {
        return expensePaymentWorkflowService.listBankLinks();
    }

    @Override
    public ExpenseBankLinkConfigVO getBankLink(Long companyBankAccountId) {
        return expensePaymentWorkflowService.getBankLink(companyBankAccountId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseBankLinkConfigVO updateBankLink(Long companyBankAccountId, ExpenseBankLinkSaveDTO dto) {
        return expensePaymentWorkflowService.updateBankLink(companyBankAccountId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO handleCmbCloudCallback(ExpenseBankCallbackDTO dto) {
        return expensePaymentWorkflowService.handleCmbCloudCallback(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void runBankReceiptPolling() {
        expensePaymentWorkflowService.runBankReceiptPolling();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO approveTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expenseApprovalWorkflowService.approveTask(userId, username, taskId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO rejectTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expenseApprovalWorkflowService.rejectTask(userId, username, taskId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO startPaymentTask(Long userId, String username, Long taskId) {
        return expensePaymentWorkflowService.startPaymentTask(userId, username, taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO completePaymentTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expensePaymentWorkflowService.completePaymentTask(userId, username, taskId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO markPaymentTaskException(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expensePaymentWorkflowService.markPaymentTaskException(userId, username, taskId, dto);
    }

    @Override
    public ExpenseDocumentEditContextVO getTaskModifyContext(Long userId, Long taskId) {
        return expenseApprovalWorkflowService.getTaskModifyContext(userId, taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO modifyTaskDocument(Long userId, String username, Long taskId, ExpenseDocumentUpdateDTO dto) {
        return expenseApprovalWorkflowService.modifyTaskDocument(userId, username, taskId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO transferTask(Long userId, String username, Long taskId, ExpenseTaskTransferDTO dto) {
        return expenseApprovalWorkflowService.transferTask(userId, username, taskId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO addSignTask(Long userId, String username, Long taskId, ExpenseTaskAddSignDTO dto) {
        return expenseApprovalWorkflowService.addSignTask(userId, username, taskId, dto);
    }

    @Override
    public List<ExpenseActionUserOptionVO> searchActionUsers(Long userId, String keyword) {
        return expenseApprovalWorkflowService.searchActionUsers(userId, keyword);
    }
}
