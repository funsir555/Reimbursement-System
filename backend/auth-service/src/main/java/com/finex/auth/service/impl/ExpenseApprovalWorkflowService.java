// 业务域：报销单录入、流转与查询
// 文件角色：流程编排类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl;

import com.finex.auth.dto.ExpenseActionUserOptionVO;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.dto.ExpenseTaskAddSignDTO;
import com.finex.auth.dto.ExpenseTaskTransferDTO;
import com.finex.auth.service.impl.expense.ExpenseApprovalDomainSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ExpenseApprovalWorkflowService：流程编排类。
 * 封装 报销单审批这块可复用的业务能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
@RequiredArgsConstructor
public class ExpenseApprovalWorkflowService {

    private final ExpenseApprovalDomainSupport expenseApprovalDomainSupport;

    /**
     * 查询Pending审批列表。
     */
    public List<ExpenseApprovalPendingItemVO> listPendingApprovals(Long userId) {
        return expenseApprovalDomainSupport.listPendingApprovals(userId);
    }

    /**
     * 审批通过任务。
     */
    public ExpenseDocumentDetailVO approveTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expenseApprovalDomainSupport.approveTask(userId, username, taskId, dto);
    }

    /**
     * 审批驳回任务。
     */
    public ExpenseDocumentDetailVO rejectTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expenseApprovalDomainSupport.rejectTask(userId, username, taskId, dto);
    }

    /**
     * 获取任务Modify上下文。
     */
    public ExpenseDocumentEditContextVO getTaskModifyContext(Long userId, Long taskId) {
        return expenseApprovalDomainSupport.getTaskModifyContext(userId, taskId);
    }

    /**
     * 处理报销单审批中的这一步。
     */
    public ExpenseDocumentDetailVO modifyTaskDocument(Long userId, String username, Long taskId, ExpenseDocumentUpdateDTO dto) {
        return expenseApprovalDomainSupport.modifyTaskDocument(userId, username, taskId, dto);
    }

    /**
     * 处理报销单审批中的这一步。
     */
    public ExpenseDocumentDetailVO transferTask(Long userId, String username, Long taskId, ExpenseTaskTransferDTO dto) {
        return expenseApprovalDomainSupport.transferTask(userId, username, taskId, dto);
    }

    /**
     * 处理报销单审批中的这一步。
     */
    public ExpenseDocumentDetailVO addSignTask(Long userId, String username, Long taskId, ExpenseTaskAddSignDTO dto) {
        return expenseApprovalDomainSupport.addSignTask(userId, username, taskId, dto);
    }

    /**
     * 查询Action用户。
     */
    public List<ExpenseActionUserOptionVO> searchActionUsers(Long userId, String keyword) {
        return expenseApprovalDomainSupport.searchActionUsers(userId, keyword);
    }
}
