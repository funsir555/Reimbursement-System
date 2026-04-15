// 业务域：报销单录入、流转与查询
// 文件角色：流程编排类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl;

import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseBankLinkConfigVO;
import com.finex.auth.dto.ExpenseBankLinkSaveDTO;
import com.finex.auth.dto.ExpenseBankLinkSummaryVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.service.impl.expense.ExpensePaymentDomainSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ExpensePaymentWorkflowService：流程编排类。
 * 封装 报销单付款这块可复用的业务能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
@RequiredArgsConstructor
public class ExpensePaymentWorkflowService {

    private final ExpensePaymentDomainSupport expensePaymentDomainSupport;

    /**
     * 查询付款Orders列表。
     */
    public List<ExpensePaymentOrderVO> listPaymentOrders(Long userId, String status) {
        return expensePaymentDomainSupport.listPaymentOrders(userId, status);
    }

    /**
     * 查询银行Links列表。
     */
    public List<ExpenseBankLinkSummaryVO> listBankLinks() {
        return expensePaymentDomainSupport.listBankLinks();
    }

    /**
     * 获取银行Link。
     */
    public ExpenseBankLinkConfigVO getBankLink(Long companyBankAccountId) {
        return expensePaymentDomainSupport.getBankLink(companyBankAccountId);
    }

    /**
     * 更新银行Link。
     */
    public ExpenseBankLinkConfigVO updateBankLink(Long companyBankAccountId, ExpenseBankLinkSaveDTO dto) {
        return expensePaymentDomainSupport.updateBankLink(companyBankAccountId, dto);
    }

    /**
     * 处理报销单付款中的这一步。
     */
    public ExpenseDocumentDetailVO handleCmbCloudCallback(ExpenseBankCallbackDTO dto) {
        return expensePaymentDomainSupport.handleCmbCloudCallback(dto);
    }

    /**
     * 执行银行回执Polling。
     */
    public void runBankReceiptPolling() {
        expensePaymentDomainSupport.runBankReceiptPolling();
    }

    /**
     * 处理报销单付款中的这一步。
     */
    public ExpenseDocumentDetailVO startPaymentTask(Long userId, String username, Long taskId) {
        return expensePaymentDomainSupport.startPaymentTask(userId, username, taskId);
    }

    /**
     * 处理报销单付款中的这一步。
     */
    public ExpenseDocumentDetailVO completePaymentTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expensePaymentDomainSupport.completePaymentTask(userId, username, taskId, dto);
    }

    /**
     * 处理报销单付款中的这一步。
     */
    public ExpenseDocumentDetailVO markPaymentTaskException(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expensePaymentDomainSupport.markPaymentTaskException(userId, username, taskId, dto);
    }
}
