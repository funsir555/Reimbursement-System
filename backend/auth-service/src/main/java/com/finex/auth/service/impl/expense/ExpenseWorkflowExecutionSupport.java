package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.User;

import java.time.LocalDateTime;
import java.util.Map;

class ExpenseWorkflowExecutionSupport {

    private final AbstractExpenseWorkflowSupport support;

    ExpenseWorkflowExecutionSupport(AbstractExpenseWorkflowSupport support) {
        this.support = support;
    }

    void initializeRuntime(ProcessDocumentInstance instance, Map<String, Object> context) {
        support.initializeRuntime(instance, context);
    }

    void approvePendingTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment
    ) {
        support.approvePendingTask(instance, task, userId, username, comment);
    }

    void rejectPendingTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment
    ) {
        support.rejectPendingTask(instance, task, userId, username, comment);
    }

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
