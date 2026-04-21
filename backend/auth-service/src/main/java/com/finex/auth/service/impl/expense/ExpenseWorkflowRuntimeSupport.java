// 业务域：报销单录入、流转与查询
// 文件角色：通用支撑类
// 上下游关系：上游通常来自报销单页面、审批页面、付款页面对应的 Controller，下游继续协调报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;
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
 * 封装报销单运行态可复用的业务能力。
 * 修改这里时，要特别关注单据状态、审批链、金额结果和重复提交。
 */
@Service
public class ExpenseWorkflowRuntimeSupport {

    private final ExpenseWorkflowContextSupport contextSupport;
    private final ExpenseWorkflowExecutionSupport executionSupport;
    private final ExpenseWorkflowRepairSupport repairSupport;

    /**
     * 鍒濆鍖栬繖涓被鎵€闇€鐨勪緷璧栫粍浠躲€?
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
     * 缁勮杩愯鏃舵祦绋嬩笂涓嬫枃銆?
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
     * 缁勮杩愯鏃朵笂涓嬫枃ForInstance銆?
     */
    public Map<String, Object> buildRuntimeContextForInstance(ProcessDocumentInstance instance) {
        return contextSupport.buildRuntimeContextForInstance(instance);
    }

    /**
     * 澶勭悊鎶ラ攢鍗曚腑鐨勮繖涓€姝ャ€?
     */
    public void initializeRuntime(ProcessDocumentInstance instance, Map<String, Object> context) {
        executionSupport.initializeRuntime(instance, context);
    }

    /**
     * 鏍￠獙娴佺▼Snapshot銆?
     */
    public void validateFlowSnapshot(String snapshotJson) {
        contextSupport.validateFlowSnapshot(snapshotJson);
    }

    public ProcessFlowRouteDTO previewMatchedRoute(List<ProcessFlowRouteDTO> routes, Map<String, Object> context) {
        return contextSupport.previewMatchedRoute(routes, context);
    }

    public List<User> previewResolvedApprovers(ProcessFlowNodeDTO node, Map<String, Object> context) {
        return contextSupport.previewResolvedApprovers(node, context);
    }

    public List<User> previewResolvedCcRecipients(ProcessDocumentInstance instance, ProcessFlowNodeDTO node, Map<String, Object> context) {
        return contextSupport.previewResolvedCcRecipients(instance, node, context);
    }

    public List<User> previewResolvedPaymentExecutors(ProcessFlowNodeDTO node, Map<String, Object> context) {
        return contextSupport.previewResolvedPaymentExecutors(node, context);
    }

    /**
     * 瀹℃壒閫氳繃Pending浠诲姟銆?
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
     * 瀹℃壒椹冲洖Pending浠诲姟銆?
     */
    public void rejectPendingTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment,
            String targetNodeKey
    ) {
        executionSupport.rejectPendingTask(instance, task, userId, username, comment, targetNodeKey);
    }

    public void submitManualApproverSelection(
            ProcessDocumentInstance instance,
            Long userId,
            String username,
            String nodeKey,
            List<Long> userIds
    ) {
        executionSupport.submitManualApproverSelection(instance, userId, username, nodeKey, userIds);
    }

    /**
     * 鍒涘缓AddSign浠诲姟銆?
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
     * 瀹℃壒閫氳繃AddSign浠诲姟銆?
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
     * 澶勭悊鎶ラ攢鍗曚腑鐨勮繖涓€姝ャ€?
     */
    public boolean paymentTaskAllowsRetry(ProcessDocumentTask task) {
        return executionSupport.paymentTaskAllowsRetry(task);
    }

    /**
     * 澶勭悊鎶ラ攢鍗曚腑鐨勮繖涓€姝ャ€?
     */
    public boolean paymentTaskAllowsRetry(ProcessDocumentInstance instance, ProcessDocumentTask task) {
        return executionSupport.paymentTaskAllowsRetry(instance, task);
    }

    /**
     * 澶勭悊鎶ラ攢鍗曚腑鐨勮繖涓€姝ャ€?
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
     * 澶勭悊鎶ラ攢鍗曚腑鐨勮繖涓€姝ャ€?
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
     * 澶勭悊鎶ラ攢鍗曚腑鐨勮繖涓€姝ャ€?
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
     * 澶勭悊鎶ラ攢鍗曚腑鐨勮繖涓€姝ャ€?
     */
    public RawFlowSnapshotSignature inspectRawFlowSnapshot(String snapshotJson) {
        return contextSupport.inspectRawFlowSnapshot(snapshotJson);
    }

    /**
     * 鍒ゆ柇Misapproved鎸塀lankRootBug鏄惁鎴愮珛銆?
     */
    boolean isMisapprovedByBlankRootBug(String documentCode) {
        return repairSupport.isMisapprovedByBlankRootBug(documentCode);
    }

    void rebuildMisapprovedRuntime(ProcessDocumentInstance instance) {
        repairSupport.rebuildMisapprovedRuntime(instance);
    }
}

