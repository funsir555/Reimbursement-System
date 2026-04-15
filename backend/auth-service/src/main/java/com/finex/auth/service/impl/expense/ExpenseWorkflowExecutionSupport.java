// 业务域：报销单录入、流转与查询
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.User;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ExpenseWorkflowExecutionSupport：通用支撑类。
 * 封装 报销单这块可复用的业务能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
class ExpenseWorkflowExecutionSupport {

    private final AbstractExpenseWorkflowSupport support;

    /**
     * 初始化这个类所需的依赖组件。
     */
    ExpenseWorkflowExecutionSupport(AbstractExpenseWorkflowSupport support) {
        this.support = support;
    }

    void initializeRuntime(ProcessDocumentInstance instance, Map<String, Object> context) {
        support.initializeRuntime(instance, context);
    }

    /**
     * 审批通过Pending任务。
     */
    void approvePendingTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment
    ) {
        support.approvePendingTask(instance, task, userId, username, comment);
    }

    /**
     * 审批驳回Pending任务。
     */
    void rejectPendingTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment
    ) {
        support.rejectPendingTask(instance, task, userId, username, comment);
    }

    /**
     * 创建AddSign任务。
     */
    void createAddSignTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            User targetUser,
            Long userId,
            String username,
            String remark
    ) {
        support.createAddSignTask(instance, task, targetUser, userId, username, remark);
    }

    /**
     * 审批通过AddSign任务。
     */
    void approveAddSignTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment
    ) {
        support.approveAddSignTask(instance, task, userId, username, comment);
    }

    boolean paymentTaskAllowsRetry(ProcessDocumentTask task) {
        return support.paymentTaskAllowsRetry(task);
    }

    boolean paymentTaskAllowsRetry(ProcessDocumentInstance instance, ProcessDocumentTask task) {
        return support.paymentTaskAllowsRetry(instance, task);
    }

    void markPaymentStarted(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            boolean retrying,
            Long companyBankAccountId,
            String companyBankAccountName,
            String pushRequestNo
    ) {
        support.markPaymentStarted(instance, task, userId, username, retrying, companyBankAccountId, companyBankAccountName, pushRequestNo);
    }

    void completePaymentRuntime(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment,
            boolean manualPaid,
            LocalDateTime paidAt
    ) {
        support.completePaymentRuntime(instance, task, userId, username, comment, manualPaid, paidAt);
    }

    void markPaymentException(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment,
            boolean allowRetry
    ) {
        support.markPaymentException(instance, task, userId, username, comment, allowRetry);
    }
}
