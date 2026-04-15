// 业务域：报销单录入、流转与查询
// 文件角色：查询支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl;

import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseDocumentCommentDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentNavigationVO;
import com.finex.auth.dto.ExpenseDocumentReminderDTO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.service.impl.expense.ExpenseQueryDomainSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ExpenseDocumentQueryService：查询支撑类。
 * 封装 报销单单据这块可复用的业务能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
@RequiredArgsConstructor
public class ExpenseDocumentQueryService {

    private final ExpenseQueryDomainSupport expenseQueryDomainSupport;

    /**
     * 查询报销单Summaries列表。
     */
    public List<ExpenseSummaryVO> listExpenseSummaries(Long userId) {
        return expenseQueryDomainSupport.listExpenseSummaries(userId);
    }

    /**
     * 查询查询单据Summaries列表。
     */
    public List<ExpenseSummaryVO> listQueryDocumentSummaries(Long userId) {
        return expenseQueryDomainSupport.listQueryDocumentSummaries(userId);
    }

    /**
     * 查询Outstanding单据列表。
     */
    public List<ExpenseSummaryVO> listOutstandingDocuments(Long userId, String kind) {
        return expenseQueryDomainSupport.listOutstandingDocuments(userId, kind);
    }

    /**
     * 获取单据明细。
     */
    public ExpenseDocumentDetailVO getDocumentDetail(Long userId, String documentCode, boolean allowCrossView) {
        return expenseQueryDomainSupport.getDocumentDetail(userId, documentCode, allowCrossView);
    }

    /**
     * 获取报销单明细。
     */
    public ExpenseDetailInstanceDetailVO getExpenseDetail(Long userId, String documentCode, String detailNo, boolean allowCrossView) {
        return expenseQueryDomainSupport.getExpenseDetail(userId, documentCode, detailNo, allowCrossView);
    }

    /**
     * 处理报销单单据中的这一步。
     */
    public ExpenseDocumentDetailVO recallDocument(Long userId, String username, String documentCode) {
        return expenseQueryDomainSupport.recallDocument(userId, username, documentCode);
    }

    /**
     * 处理报销单单据中的这一步。
     */
    public ExpenseDocumentDetailVO commentOnDocument(Long userId, String username, String documentCode, ExpenseDocumentCommentDTO dto, boolean allowCrossView) {
        return expenseQueryDomainSupport.commentOnDocument(userId, username, documentCode, dto, allowCrossView);
    }

    /**
     * 处理报销单单据中的这一步。
     */
    public ExpenseDocumentDetailVO remindDocument(Long userId, String username, String documentCode, ExpenseDocumentReminderDTO dto) {
        return expenseQueryDomainSupport.remindDocument(userId, username, documentCode, dto);
    }

    /**
     * 获取单据Navigation。
     */
    public ExpenseDocumentNavigationVO getDocumentNavigation(Long userId, String documentCode, boolean approvalViewer) {
        return expenseQueryDomainSupport.getDocumentNavigation(userId, documentCode, approvalViewer);
    }

    /**
     * 获取单据Edit上下文。
     */
    public ExpenseDocumentEditContextVO getDocumentEditContext(Long userId, String documentCode) {
        return expenseQueryDomainSupport.getDocumentEditContext(userId, documentCode);
    }
}
