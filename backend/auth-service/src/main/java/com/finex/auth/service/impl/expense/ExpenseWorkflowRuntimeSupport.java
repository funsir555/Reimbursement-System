// 业务域：报销单录入、流转与查询
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

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

/**
 * ExpenseWorkflowRuntimeSupport：通用支撑类。
 * 封装 报销单这块可复用的业务能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
public class ExpenseWorkflowRuntimeSupport {

    private final ExpenseWorkflowContextSupport contextSupport;
    private final ExpenseWorkflowExecutionSupport executionSupport;
    private final ExpenseWorkflowRepairSupport repairSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
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

    /**
     * 组装运行时流程上下文。
     */
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

    /**
     * 组装运行时上下文ForInstance。
     */
    public Map<String, Object> buildRuntimeContextForInstance(ProcessDocumentInstance instance) {
        return contextSupport.buildRuntimeContextForInstance(instance);
    }

    /**
     * 处理报销单中的这一步。
     */
    public void initializeRuntime(ProcessDocumentInstance instance, Map<String, Object> context) {
        executionSupport.initializeRuntime(instance, context);
    }

    /**
     * 校验流程Snapshot。
     */
    public void validateFlowSnapshot(String snapshotJson) {
        contextSupport.validateFlowSnapshot(snapshotJson);
    }

    /**
     * 审批通过Pending任务。
     */
    public void approvePendingTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment
    ) {
        executionSupport.approvePendingTask(instance, task, userId, username, comment);
    }

    /**
     * 审批驳回Pending任务。
     */
    public void rejectPendingTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment
    ) {
        executionSupport.rejectPendingTask(instance, task, userId, username, comment);
    }

    /**
     * 创建AddSign任务。
     */
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

    /**
     * 审批通过AddSign任务。
     */
    public void approveAddSignTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment
    ) {
        executionSupport.approveAddSignTask(instance, task, userId, username, comment);
    }

    /**
     * 处理报销单中的这一步。
     */
    public boolean paymentTaskAllowsRetry(ProcessDocumentTask task) {
        return executionSupport.paymentTaskAllowsRetry(task);
    }

    /**
     * 处理报销单中的这一步。
     */
    public boolean paymentTaskAllowsRetry(ProcessDocumentInstance instance, ProcessDocumentTask task) {
        return executionSupport.paymentTaskAllowsRetry(instance, task);
    }

    /**
     * 处理报销单中的这一步。
     */
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

    /**
     * 处理报销单中的这一步。
     */
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

    /**
     * 处理报销单中的这一步。
     */
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

    /**
     * 处理报销单中的这一步。
     */
    public RawFlowSnapshotSignature inspectRawFlowSnapshot(String snapshotJson) {
        return contextSupport.inspectRawFlowSnapshot(snapshotJson);
    }

    /**
     * 判断Misapproved按BlankRootBug是否成立。
     */
    boolean isMisapprovedByBlankRootBug(String documentCode) {
        return repairSupport.isMisapprovedByBlankRootBug(documentCode);
    }

    void rebuildMisapprovedRuntime(ProcessDocumentInstance instance) {
        repairSupport.rebuildMisapprovedRuntime(instance);
    }
}
