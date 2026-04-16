// 业务域：报销单录入、流转与查询
// 文件角色：service 接口
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service;

import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseActionUserOptionVO;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseBankLinkConfigVO;
import com.finex.auth.dto.ExpenseBankLinkSaveDTO;
import com.finex.auth.dto.ExpenseBankLinkSummaryVO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseDocumentCommentDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentNavigationVO;
import com.finex.auth.dto.ExpenseDocumentPickerVO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.dto.ExpenseDocumentReminderDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.ExpenseTaskAddSignDTO;
import com.finex.auth.dto.ExpenseTaskTransferDTO;

import java.util.List;

/**
 * ExpenseDocumentService：service 接口。
 * 定义报销单单据这块对外提供的业务入口能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
public interface ExpenseDocumentService {

    /**
     * 查询可用模板列表。
     */
    List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates();

    /**
     * 获取模板明细。
     */
    ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode);

    /**
     * 查询供应商选项。
     */
    List<ExpenseCreateVendorOptionVO> listVendorOptions(
            Long userId,
            String keyword,
            Boolean includeDisabled,
            String paymentCompanyId
    );

    /**
     * 查询收款方选项。
     */
    List<ExpenseCreatePayeeOptionVO> listPayeeOptions(Long userId, String keyword, Boolean personalOnly);

    /**
     * 查询收款方账户选项。
     */
    List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(
            Long userId,
            String keyword,
            String linkageMode,
            String payeeName,
            String counterpartyCode,
            String paymentCompanyId
    );

    /**
     * 提交单据。
     */
    ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto);

    /**
     * 查询报销单Summaries列表。
     */
    List<ExpenseSummaryVO> listExpenseSummaries(Long userId);

    /**
     * 查询查询单据Summaries列表。
     */
    List<ExpenseSummaryVO> listQueryDocumentSummaries(Long userId);

    /**
     * 查询Outstanding单据列表。
     */
    List<ExpenseSummaryVO> listOutstandingDocuments(Long userId, String kind);

    /**
     * 获取单据明细。
     */
    ExpenseDocumentDetailVO getDocumentDetail(Long userId, String documentCode, boolean allowCrossView);

    /**
     * 获取报销单明细。
     */
    ExpenseDetailInstanceDetailVO getExpenseDetail(Long userId, String documentCode, String detailNo, boolean allowCrossView);

    /**
     * 获取单据Picker。
     */
    ExpenseDocumentPickerVO getDocumentPicker(
            Long userId,
            String relationType,
            List<String> templateTypes,
            String keyword,
            Integer page,
            Integer pageSize,
            String excludeDocumentCode,
            boolean allowCrossView
    );

    /**
     * 获取首页看板写入OffSourceReportPicker。
     */
    ExpenseDocumentPickerVO getDashboardWriteOffSourceReportPicker(
            Long userId,
            String targetDocumentCode,
            String keyword,
            Integer page,
            Integer pageSize
    );

    /**
     * 查询Pending审批列表。
     */
    List<ExpenseApprovalPendingItemVO> listPendingApprovals(Long userId);

    /**
     * 查询付款Orders列表。
     */
    List<ExpensePaymentOrderVO> listPaymentOrders(Long userId, String status);

    /**
     * 查询银行Links列表。
     */
    List<ExpenseBankLinkSummaryVO> listBankLinks();

    /**
     * 获取银行Link。
     */
    ExpenseBankLinkConfigVO getBankLink(Long companyBankAccountId);

    /**
     * 更新银行Link。
     */
    ExpenseBankLinkConfigVO updateBankLink(Long companyBankAccountId, ExpenseBankLinkSaveDTO dto);

    ExpenseDocumentDetailVO handleCmbCloudCallback(ExpenseBankCallbackDTO dto);

    /**
     * 执行银行回执Polling。
     */
    void runBankReceiptPolling();

    boolean bindDashboardWriteOff(Long userId, String targetDocumentCode, String sourceReportDocumentCode);

    ExpenseDocumentDetailVO recallDocument(Long userId, String username, String documentCode);

    ExpenseDocumentDetailVO commentOnDocument(Long userId, String username, String documentCode, ExpenseDocumentCommentDTO dto, boolean allowCrossView);

    ExpenseDocumentDetailVO remindDocument(Long userId, String username, String documentCode, ExpenseDocumentReminderDTO dto);

    /**
     * 获取单据Navigation。
     */
    ExpenseDocumentNavigationVO getDocumentNavigation(Long userId, String documentCode, boolean approvalViewer);

    /**
     * 获取单据Edit上下文。
     */
    ExpenseDocumentEditContextVO getDocumentEditContext(Long userId, String documentCode);

    /**
     * 重新提交单据。
     */
    ExpenseDocumentSubmitResultVO resubmitDocument(Long userId, String username, String documentCode, ExpenseDocumentUpdateDTO dto);

    /**
     * 审批通过任务。
     */
    ExpenseDocumentDetailVO approveTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto);

    /**
     * 审批驳回任务。
     */
    ExpenseDocumentDetailVO rejectTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto);

    ExpenseDocumentDetailVO startPaymentTask(Long userId, String username, Long taskId);

    ExpenseDocumentDetailVO completePaymentTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto);

    ExpenseDocumentDetailVO markPaymentTaskException(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto);

    boolean rejectPaymentTasks(Long userId, String username, List<Long> taskIds, ExpenseApprovalActionDTO dto);

    /**
     * 获取任务Modify上下文。
     */
    ExpenseDocumentEditContextVO getTaskModifyContext(Long userId, Long taskId);

    ExpenseDocumentDetailVO modifyTaskDocument(Long userId, String username, Long taskId, ExpenseDocumentUpdateDTO dto);

    ExpenseDocumentDetailVO transferTask(Long userId, String username, Long taskId, ExpenseTaskTransferDTO dto);

    ExpenseDocumentDetailVO addSignTask(Long userId, String username, Long taskId, ExpenseTaskAddSignDTO dto);

    /**
     * 查询Action用户。
     */
    List<ExpenseActionUserOptionVO> searchActionUsers(Long userId, String keyword);
}
