package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseWorkflowExecutionSupportTest {

    @Mock
    private AbstractExpenseWorkflowSupport support;

    @Test
    void initializeAndApprovalOperationsDelegateToSharedSupport() {
        ExpenseWorkflowExecutionSupport executionSupport = new ExpenseWorkflowExecutionSupport(support);
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        ProcessDocumentTask task = new ProcessDocumentTask();
        User targetUser = new User();
        Map<String, Object> context = Map.of("amount", 1);

        executionSupport.initializeRuntime(instance, context);
        executionSupport.approvePendingTask(instance, task, 1L, "tester", "ok");
        executionSupport.rejectPendingTask(instance, task, 1L, "tester", "no");
        executionSupport.createAddSignTask(instance, task, targetUser, 1L, "tester", "add");
        executionSupport.approveAddSignTask(instance, task, 1L, "tester", "done");

        verify(support).initializeRuntime(instance, context);
        verify(support).approvePendingTask(instance, task, 1L, "tester", "ok");
        verify(support).rejectPendingTask(instance, task, 1L, "tester", "no");
        verify(support).createAddSignTask(instance, task, targetUser, 1L, "tester", "add");
        verify(support).approveAddSignTask(instance, task, 1L, "tester", "done");
    }

    @Test
    void paymentRetryQueriesDelegateToSharedSupport() {
        ExpenseWorkflowExecutionSupport executionSupport = new ExpenseWorkflowExecutionSupport(support);
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        ProcessDocumentTask task = new ProcessDocumentTask();
        when(support.paymentTaskAllowsRetry(task)).thenReturn(true);
        when(support.paymentTaskAllowsRetry(instance, task)).thenReturn(true);

        assertTrue(executionSupport.paymentTaskAllowsRetry(task));
        assertTrue(executionSupport.paymentTaskAllowsRetry(instance, task));
    }

    @Test
    void paymentTransitionsDelegateToSharedSupport() {
        ExpenseWorkflowExecutionSupport executionSupport = new ExpenseWorkflowExecutionSupport(support);
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        ProcessDocumentTask task = new ProcessDocumentTask();
        LocalDateTime paidAt = LocalDateTime.now();

        executionSupport.markPaymentStarted(instance, task, 1L, "tester", true, 2L, "Main", "REQ-1");
        executionSupport.completePaymentRuntime(instance, task, 1L, "tester", "done", true, paidAt);
        executionSupport.markPaymentException(instance, task, 1L, "tester", "fail", true);

        verify(support).markPaymentStarted(instance, task, 1L, "tester", true, 2L, "Main", "REQ-1");
        verify(support).completePaymentRuntime(instance, task, 1L, "tester", "done", true, paidAt);
        verify(support).markPaymentException(instance, task, 1L, "tester", "fail", true);
    }
}