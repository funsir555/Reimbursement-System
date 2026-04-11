package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.SystemPermissionMapper;
import com.finex.auth.mapper.SystemRolePermissionMapper;
import com.finex.auth.mapper.SystemUserRoleMapper;
import com.finex.auth.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseWorkflowRuntimeSupport {

    private final ExpenseWorkflowContextSupport contextSupport;
    private final ExpenseWorkflowExecutionSupport executionSupport;
    private final ExpenseWorkflowRepairSupport repairSupport;

    public ExpenseWorkflowRuntimeSupport(
            ProcessDocumentInstanceMapper processDocumentInstanceMapper,
            ProcessDocumentTaskMapper processDocumentTaskMapper,
            ProcessDocumentActionLogMapper processDocumentActionLogMapper,
            ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper,
            SystemPermissionMapper systemPermissionMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            SystemRolePermissionMapper systemRolePermissionMapper,
            SystemUserRoleMapper systemUserRoleMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper
    ) {
        AbstractExpenseWorkflowSupport support = new AbstractExpenseWorkflowSupport(
                processDocumentInstanceMapper,
                processDocumentTaskMapper,
                processDocumentActionLogMapper,
                processDocumentExpenseDetailMapper,
                systemPermissionMapper,
                systemDepartmentMapper,
                systemRolePermissionMapper,
                systemUserRoleMapper,
                userMapper,
                objectMapper
        );
        this.contextSupport = new ExpenseWorkflowContextSupport(support);
        this.executionSupport = new ExpenseWorkflowExecutionSupport(support);
        this.repairSupport = new ExpenseWorkflowRepairSupport(support, executionSupport);
    }

    public Map<String, Object> buildRuntimeFlowContext(
            User currentUser,
            ProcessDocumentTemplate template,
            ProcessFormDesign formDesign,
            Map<String, Object> formData,
            ProcessExpenseDetailDesign expenseDetailDesign,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        return contextSupport.buildRuntimeFlowContext(currentUser, template, formDesign, formData, expenseDetailDesign, expenseDetails);
    }

    public Map<String, Object> buildRuntimeContextForInstance(ProcessDocumentInstance instance) {
        return contextSupport.buildRuntimeContextForInstance(instance);
    }

    public void initializeRuntime(ProcessDocumentInstance instance, Map<String, Object> context) {
        executionSupport.initializeRuntime(instance, context);
    }

    public void validateFlowSnapshot(String snapshotJson) {
        contextSupport.validateFlowSnapshot(snapshotJson);
    }

    public void approvePendingTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment
    ) {
        executionSupport.approvePendingTask(instance, task, userId, username, comment);
    }

    public void rejectPendingTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment
    ) {
        executionSupport.rejectPendingTask(instance, task, userId, username, comment);
    }

    public void createAddSignTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            User targetUser,
            Long userId,
            String username,
            String remark
    ) {
        executionSupport.createAddSignTask(instance, task, targetUser, userId, username, remark);
    }

    public void approveAddSignTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment
    ) {
        executionSupport.approveAddSignTask(instance, task, userId, username, comment);
    }

    public boolean paymentTaskAllowsRetry(ProcessDocumentTask task) {
        return executionSupport.paymentTaskAllowsRetry(task);
    }

    public boolean paymentTaskAllowsRetry(ProcessDocumentInstance instance, ProcessDocumentTask task) {
        return executionSupport.paymentTaskAllowsRetry(instance, task);
    }

    public void markPaymentStarted(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            boolean retrying,
            Long companyBankAccountId,
            String companyBankAccountName,
            String pushRequestNo
    ) {
        executionSupport.markPaymentStarted(instance, task, userId, username, retrying, companyBankAccountId, companyBankAccountName, pushRequestNo);
    }

    public void completePaymentRuntime(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment,
            boolean manualPaid,
            LocalDateTime paidAt
    ) {
        executionSupport.completePaymentRuntime(instance, task, userId, username, comment, manualPaid, paidAt);
    }

    public void markPaymentException(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment,
            boolean allowRetry
    ) {
        executionSupport.markPaymentException(instance, task, userId, username, comment, allowRetry);
    }

    public RawFlowSnapshotSignature inspectRawFlowSnapshot(String snapshotJson) {
        return contextSupport.inspectRawFlowSnapshot(snapshotJson);
    }

    boolean isMisapprovedByBlankRootBug(String documentCode) {
        return repairSupport.isMisapprovedByBlankRootBug(documentCode);
    }

    void rebuildMisapprovedRuntime(ProcessDocumentInstance instance) {
        repairSupport.rebuildMisapprovedRuntime(instance);
    }
}
