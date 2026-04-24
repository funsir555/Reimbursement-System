// 业务域：报销单录入、流转与查询
// 文件角色：通用支撑类
// 上下游关系：上游通常来自报销单页面、审批页面、付款页面对应的控制器，下游继续协调报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ProcessFlowConditionDTO;
import com.finex.auth.dto.ProcessFlowConditionGroupDTO;
import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.SystemPermission;
import com.finex.auth.entity.SystemRolePermission;
import com.finex.auth.entity.SystemUserRole;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 报销流程运行态通用支持类。
 * 封装报销单运行态可复用的业务能力。
 * 修改这里时，要特别关注单据状态、审批链路、金额结果和重复提交保护。
 */
@Slf4j
@RequiredArgsConstructor
class AbstractExpenseWorkflowSupport {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String UNDERTAKE_DEPARTMENT_COMPONENT_CODE = "undertake-department";
    private static final String NODE_TYPE_APPROVAL = "APPROVAL";
    private static final String NODE_TYPE_CC = "CC";
    private static final String NODE_TYPE_PAYMENT = "PAYMENT";
    private static final String NODE_TYPE_BRANCH = "BRANCH";

    private static final String APPROVER_TYPE_MANAGER = "MANAGER";
    private static final String APPROVER_TYPE_DESIGNATED_MEMBER = "DESIGNATED_MEMBER";
    private static final String APPROVER_TYPE_MANUAL_SELECT = "MANUAL_SELECT";
    private static final String PAYMENT_EXECUTOR_TYPE_DESIGNATED_MEMBER = "DESIGNATED_MEMBER";
    private static final String PAYMENT_EXECUTOR_TYPE_FINANCE_ROLE = "FINANCE_ROLE";
    private static final String PAYMENT_EXECUTOR_TYPE_SUBMITTER = "SUBMITTER";
    private static final String CC_RECEIVER_TYPE_DESIGNATED_MEMBER = "DESIGNATED_MEMBER";
    private static final String CC_RECEIVER_TYPE_SUBMITTER = "SUBMITTER";
    private static final String CC_RECEIVER_TYPE_DEPT_MANAGER = "DEPT_MANAGER";
    private static final String CC_TIMING_ON_ENTER = "ON_ENTER";
    private static final String CC_TIMING_ON_APPROVED = "ON_APPROVED";
    private static final String DEPT_SOURCE_UNDERTAKE = "UNDERTAKE_DEPT";
    private static final String DEPT_SOURCE_SUBMITTER = "SUBMITTER_DEPT";
    private static final String MISSING_HANDLER_AUTO_SKIP = "AUTO_SKIP";
    private static final String MISSING_HANDLER_EXCEPTION = "EXCEPTION";
    private static final String MISSING_HANDLER_AUTO_TRANSFER = "AUTO_TRANSFER";
    private static final String MISSING_HANDLER_BLOCK_SUBMIT = "BLOCK_SUBMIT";
    private static final String APPROVAL_MODE_OR_SIGN = "OR_SIGN";
    private static final String APPROVAL_MODE_AND_SIGN = "AND_SIGN";
    private static final String APPROVAL_SPECIAL_AUTO_PASS_IF_APPROVER_IS_SUBMITTER = "AUTO_PASS_IF_APPOVER_IS_SUBMITTER";
    private static final String APPROVAL_SPECIAL_AUTO_PASS_IF_APPROVED_BEFORE = "AUTO_PASS_IF_APPROVED_BEFORE";
    private static final String APPROVAL_SPECIAL_DIRECT_REACH_AFTER_RESUBMIT = "DIRECT_REACH_AFTER_RESUBMIT";
    private static final String APPROVAL_SPECIAL_REJECT_TO_ANY_NODE = "REJECT_TO_ANY_NODE";
    private static final String APPROVAL_SPECIAL_DIRECT_REACH_AFTER_ANY_REJECT = "DIRECT_REACH_AFTER_ANY_REJECT";
    private static final String PAYMENT_SPECIAL_ALLOW_RETRY = "ALLOW_RETRY";
    private static final String CC_SPECIAL_SEND_ONCE = "SEND_ONCE";
    private static final String CC_SPECIAL_INCLUDE_SUBMITTER = "INCLUDE_SUBMITTER";
    private static final String PAYMENT_EXECUTE_PERMISSION = "expense:payment:payment_order:execute";
    private static final String CONDITION_FIELD_SUBMITTER_DEPT_ID = "submitterDeptId";

    private static final String DOCUMENT_STATUS_PENDING = "PENDING_APPROVAL";
    private static final String DOCUMENT_STATUS_COMPLETED = "COMPLETED";
    private static final String DOCUMENT_STATUS_REJECTED = "REJECTED";
    private static final String DOCUMENT_STATUS_EXCEPTION = "EXCEPTION";
    private static final String DOCUMENT_STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String DOCUMENT_STATUS_PAYING = "PAYING";
    private static final String DOCUMENT_STATUS_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    private static final String DOCUMENT_STATUS_PAYMENT_FINISHED = "PAYMENT_FINISHED";
    private static final String DOCUMENT_STATUS_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";

    private static final String TASK_STATUS_PENDING = "PENDING";
    private static final String TASK_STATUS_PAUSED = "PAUSED";
    private static final String TASK_STATUS_APPROVED = "APPROVED";
    private static final String TASK_STATUS_REJECTED = "REJECTED";
    private static final String TASK_STATUS_CANCELLED = "CANCELLED";
    private static final String TASK_KIND_NORMAL = "NORMAL";
    private static final String TASK_KIND_ADD_SIGN = "ADD_SIGN";

    private static final String LOG_ROUTE_HIT = "ROUTE_HIT";
    private static final String LOG_APPROVAL_PENDING = "APPROVAL_PENDING";
    private static final String LOG_APPROVE = "APPROVE";
    private static final String LOG_REJECT = "REJECT";
    private static final String LOG_ADD_SIGN = "ADD_SIGN";
    private static final String LOG_AUTO_SKIP = "AUTO_SKIP";
    private static final String LOG_CC_REACHED = "CC_REACHED";
    private static final String LOG_PAYMENT_REACHED = "PAYMENT_REACHED";
    private static final String LOG_PAYMENT_PENDING = "PAYMENT_PENDING";
    private static final String LOG_PAYMENT_START = "PAYMENT_START";
    private static final String LOG_PAYMENT_COMPLETE = "PAYMENT_COMPLETE";
    private static final String LOG_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";
    private static final String LOG_FINISH = "FINISH";
    private static final String LOG_EXCEPTION = "EXCEPTION";
    private static final String LOG_SUBMIT = "SUBMIT";
    private static final String LOG_RESUBMIT = "RESUBMIT";
    private static final String FLOW_FINISH_COMMENT = "审批流程结束";
    private static final String CURRENT_TASK_TYPE_MANUAL_SELECT = "MANUAL_SELECT";

    private final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    private final ProcessDocumentTaskMapper processDocumentTaskMapper;
    private final ProcessDocumentActionLogMapper processDocumentActionLogMapper;
    private final ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    private final SystemPermissionMapper systemPermissionMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final SystemRolePermissionMapper systemRolePermissionMapper;
    private final SystemUserRoleMapper systemUserRoleMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

            /**
     * 组装运行态流程上下文。
     */
public Map<String, Object> buildRuntimeFlowContext(
            User currentUser,
            ProcessDocumentTemplate template,
            ProcessFormDesign formDesign,
            Map<String, Object> formData,
            ProcessExpenseDetailDesign expenseDetailDesign,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        Map<String, Object> context = mergeRuntimeFormData(formData, expenseDetails);
        if (currentUser != null && currentUser.getId() != null) {
            context.put("submitterUserId", currentUser.getId());
        }
        if (currentUser != null && currentUser.getDeptId() != null) {
            context.put("submitterDeptId", currentUser.getDeptId());
        }
        BigDecimal amount = resolveTotalAmount(
                formData == null ? Collections.emptyMap() : formData,
                expenseDetails,
                template == null ? null : template.getExpenseDetailModeDefault()
        );
        if (amount != null) {
            context.put("amount", amount);
        }
        String documentType = trimToNull(template == null ? null : template.getTemplateType());
        if (documentType != null) {
            context.put("documentType", documentType);
        }
        String expenseTypeCode = firstNonBlank(
                stringValue(formData == null ? null : formData.get("expenseTypeCode")),
                template == null ? null : trimToNull(template.getCategoryCode())
        );
        if (expenseTypeCode != null) {
            context.put("expenseTypeCode", expenseTypeCode);
        }
        List<String> undertakeDeptIds = resolveUndertakeDeptIds(formDesign, formData, expenseDetailDesign, expenseDetails);
        if (!undertakeDeptIds.isEmpty()) {
            context.put("undertakeDeptIds", undertakeDeptIds);
        }
        return context;
    }

            /**
     * 基于单据实例组装运行态上下文。
     */
public Map<String, Object> buildRuntimeContextForInstance(ProcessDocumentInstance instance) {
        Map<String, Object> formData = readMap(instance.getFormDataJson());
        List<ProcessDocumentExpenseDetail> expenseDetails = loadExpenseDetails(instance.getDocumentCode());
        Map<String, Object> context = mergeRuntimeFormData(
                formData,
                expenseDetails.stream().map(this::toRuntimeExpenseDetailDTO).toList()
        );
        if (instance.getSubmitterUserId() != null) {
            context.put("submitterUserId", instance.getSubmitterUserId());
        }
        User submitter = instance.getSubmitterUserId() == null ? null : userMapper.selectById(instance.getSubmitterUserId());
        if (submitter != null && submitter.getDeptId() != null) {
            context.put("submitterDeptId", submitter.getDeptId());
        }
        if (instance.getTotalAmount() != null) {
            context.put("amount", instance.getTotalAmount());
        }
        if (trimToNull(instance.getTemplateType()) != null) {
            context.put("documentType", instance.getTemplateType());
        }
        Map<String, Object> templateSnapshot = readMap(instance.getTemplateSnapshotJson());
        String expenseTypeCode = firstNonBlank(
                stringValue(formData.get("expenseTypeCode")),
                templateSnapshot.get("categoryCode") == null ? null : String.valueOf(templateSnapshot.get("categoryCode"))
        );
        if (expenseTypeCode != null) {
            context.put("expenseTypeCode", expenseTypeCode);
        }
        List<String> undertakeDeptIds = resolveUndertakeDeptIdsFromSnapshots(
                readMap(instance.getFormSchemaSnapshotJson()),
                formData,
                expenseDetails
        );
        if (!undertakeDeptIds.isEmpty()) {
            context.put("undertakeDeptIds", undertakeDeptIds);
        }
        return context;
    }

            /**
     * 初始化单据运行态并推进流程。
     */
public void initializeRuntime(ProcessDocumentInstance instance, Map<String, Object> context) {
        log.info(
                "Expense submit stage=initialize-runtime documentCode={} approvalFlowCode={} status={}",
                instance.getDocumentCode(),
                instance.getApprovalFlowCode(),
                instance.getStatus()
        );
        try {
            FlowRuntimeSnapshot snapshot = readFlowSnapshot(instance.getFlowSnapshotJson());
            if (snapshot.nodes().isEmpty()) {
                markDocumentApproved(instance, DOCUMENT_STATUS_COMPLETED);
            appendLog(instance.getDocumentCode(), null, null, LOG_FINISH, null, "SYSTEM", "未配置审批节点", Collections.emptyMap());
                return;
            }
            ProcessFlowNodeDTO resumeNode = resolveResumeNode(snapshot, context);
            if (resumeNode != null) {
                advanceFromPosition(
                        instance,
                        snapshot,
                        context,
                        resumeNode.getParentNodeKey(),
                        snapshot.indexInContainer(resumeNode.getParentNodeKey(), resumeNode.getNodeKey()),
                        DOCUMENT_STATUS_COMPLETED
                );
                return;
            }
            advanceFromPosition(instance, snapshot, context, null, 0, DOCUMENT_STATUS_COMPLETED);
        } catch (RuntimeException ex) {
            log.error(
                    "Expense submit runtime initialization failed documentCode={} approvalFlowCode={} status={}",
                    instance.getDocumentCode(),
                    instance.getApprovalFlowCode(),
                    instance.getStatus(),
                    ex
            );
            throw ex;
        }
    }

            /**
     * 校验流程快照是否合法。
     */
public void validateFlowSnapshot(String snapshotJson) {
        readFlowSnapshot(snapshotJson);
    }

    ProcessFlowRouteDTO previewMatchedRoute(List<ProcessFlowRouteDTO> routes, Map<String, Object> context) {
        return matchRoute(routes, context);
    }

    List<User> previewResolvedApprovers(ProcessFlowNodeDTO node, Map<String, Object> context) {
        return resolveApprovers(node, context);
    }

    List<User> previewResolvedCcRecipients(ProcessDocumentInstance instance, ProcessFlowNodeDTO node, Map<String, Object> context) {
        return resolveCcRecipients(instance, node, context);
    }

    List<User> previewResolvedPaymentExecutors(ProcessFlowNodeDTO node, Map<String, Object> context) {
        return resolvePaymentExecutors(node, context);
    }

            /**
     * 审批通过待处理任务。
     */
public void approvePendingTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment
    ) {
        FlowRuntimeSnapshot snapshot = readFlowSnapshot(instance.getFlowSnapshotJson());
        ProcessFlowNodeDTO node = snapshot.node(task.getNodeKey());
        if (node == null) {
            throw new IllegalStateException("当前任务找不到对应流程节点");
        }

        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TASK_STATUS_APPROVED);
        task.setHandledAt(now);
        task.setActionComment(trimToNull(comment));
        processDocumentTaskMapper.updateById(task);
        appendLog(instance.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_APPROVE, userId, resolveActorDisplayName(userId, username), task.getActionComment(), Map.of(
                "taskId", task.getId(),
                "approvalMode", defaultText(task.getApprovalMode(), APPROVAL_MODE_OR_SIGN)
        ));

        List<ProcessDocumentTask> openTasks = loadNodeOpenTasks(task.getDocumentCode(), task.getNodeKey());
        String approvalMode = defaultText(task.getApprovalMode(), APPROVAL_MODE_OR_SIGN);
        boolean nodeCompleted;
        if (APPROVAL_MODE_AND_SIGN.equals(approvalMode)) {
            nodeCompleted = openTasks.isEmpty();
        } else {
            cancelOpenTasks(openTasks, task.getId(), now);
            nodeCompleted = true;
        }

        if (nodeCompleted) {
            Map<String, Object> context = buildRuntimeContextForInstance(instance);
            clearCurrentNode(instance);
            advanceFromPosition(instance, snapshot, context, node.getParentNodeKey(), nextIndex(snapshot, node), DOCUMENT_STATUS_COMPLETED);
        } else {
            instance.setUpdatedAt(now);
            processDocumentInstanceMapper.updateById(instance);
        }
    }

            /**
     * 驳回待处理任务。
     */
public void rejectPendingTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment,
            String targetNodeKey
    ) {
        FlowRuntimeSnapshot snapshot = readFlowSnapshot(instance.getFlowSnapshotJson());
        ProcessFlowNodeDTO currentNode = snapshot.node(task.getNodeKey());
        String normalizedTargetNodeKey = normalizeRejectTargetNodeKey(instance.getDocumentCode(), snapshot, currentNode, targetNodeKey);
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TASK_STATUS_REJECTED);
        task.setHandledAt(now);
        task.setActionComment(trimToNull(comment));
        processDocumentTaskMapper.updateById(task);
        cancelOpenTasks(loadNodeOpenTasks(task.getDocumentCode(), task.getNodeKey()), task.getId(), now);

        instance.setStatus(DOCUMENT_STATUS_REJECTED);
        instance.setCurrentNodeKey(task.getNodeKey());
        instance.setCurrentNodeName(task.getNodeName());
        instance.setCurrentTaskType("REJECTED");
        instance.setFinishedAt(now);
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);

        appendLog(instance.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_REJECT, userId, resolveActorDisplayName(userId, username), task.getActionComment(), Map.of(
                "taskId", task.getId(),
                "rejectedByNodeKey", task.getNodeKey(),
                "targetNodeKey", defaultText(normalizedTargetNodeKey, "")
        ));
    }

    public void submitManualApproverSelection(
            ProcessDocumentInstance instance,
            Long userId,
            String username,
            String nodeKey,
            List<Long> userIds
    ) {
        FlowRuntimeSnapshot snapshot = readFlowSnapshot(instance.getFlowSnapshotJson());
        String normalizedNodeKey = trimToNull(nodeKey);
        if (!Objects.equals(trimToNull(instance.getCurrentTaskType()), CURRENT_TASK_TYPE_MANUAL_SELECT)) {
            throw new IllegalStateException("\u5f53\u524d\u5355\u636e\u4e0d\u5904\u4e8e\u5f85\u624b\u52a8\u9009\u62e9\u5ba1\u6279\u4eba\u7684\u72b6\u6001");
        }
        if (!Objects.equals(trimToNull(instance.getCurrentNodeKey()), normalizedNodeKey)) {
            throw new IllegalStateException("\u5f53\u524d\u5355\u636e\u7b49\u5f85\u9009\u4eba\u7684\u8282\u70b9\u4e0e\u63d0\u4ea4\u8282\u70b9\u4e0d\u4e00\u81f4");
        }
        ProcessFlowNodeDTO node = snapshot.node(normalizedNodeKey);
        if (!isManualSelectApprovalNode(node)) {
            throw new IllegalStateException("\u5f53\u524d\u8282\u70b9\u4e0d\u662f\u624b\u52a8\u9009\u62e9\u5ba1\u6279\u4eba\u8282\u70b9");
        }
        List<Long> normalizedUserIds = userIds == null
                ? Collections.emptyList()
                : userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (normalizedUserIds.isEmpty()) {
            throw new IllegalStateException("\u8bf7\u81f3\u5c11\u9009\u62e9\u4e00\u4f4d\u5ba1\u6279\u4eba");
        }
        List<User> selectedUsers = loadActiveUsers(normalizedUserIds);
        if (selectedUsers.size() != normalizedUserIds.size()) {
            throw new IllegalStateException("\u6240\u9009\u5ba1\u6279\u4eba\u4e0d\u5b58\u5728\u6216\u5df2\u505c\u7528");
        }
        Map<String, Object> runtimeContext = buildRuntimeContextForInstance(instance);
        runtimeContext.put("manualApproverSelections", Map.of(normalizedNodeKey, normalizedUserIds));
        runtimeContext.put("manualSelectedUserIds", normalizedUserIds);
        clearCurrentNode(instance);
        advanceFromPosition(
                instance,
                snapshot,
                runtimeContext,
                node.getParentNodeKey(),
                snapshot.indexInContainer(node.getParentNodeKey(), normalizedNodeKey),
                DOCUMENT_STATUS_COMPLETED
        );
    }

            /**
     * 为当前审批节点创建加签任务。
     */
public void createAddSignTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            User targetUser,
            Long userId,
            String username,
            String remark
    ) {
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TASK_STATUS_PAUSED);
        processDocumentTaskMapper.updateById(task);

        ProcessDocumentTask addSignTask = new ProcessDocumentTask();
        addSignTask.setDocumentCode(task.getDocumentCode());
        addSignTask.setNodeKey(task.getNodeKey());
        addSignTask.setNodeName(task.getNodeName());
        addSignTask.setNodeType(task.getNodeType());
        addSignTask.setAssigneeUserId(targetUser.getId());
        addSignTask.setAssigneeName(normalizeUserName(targetUser));
        addSignTask.setStatus(TASK_STATUS_PENDING);
        addSignTask.setTaskBatchNo(buildTaskBatchNo(task.getDocumentCode(), task.getNodeKey()));
        addSignTask.setApprovalMode(APPROVAL_MODE_OR_SIGN);
        addSignTask.setTaskKind(TASK_KIND_ADD_SIGN);
        addSignTask.setSourceTaskId(task.getId());
        addSignTask.setCreatedAt(now);
        processDocumentTaskMapper.insert(addSignTask);

        instance.setStatus(DOCUMENT_STATUS_PENDING);
        instance.setCurrentNodeKey(task.getNodeKey());
        instance.setCurrentNodeName(task.getNodeName());
        instance.setCurrentTaskType(TASK_KIND_ADD_SIGN);
        instance.setFinishedAt(null);
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);

        appendLog(task.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_ADD_SIGN, userId, resolveActorDisplayName(userId, username), trimToNull(remark), Map.of(
                "taskId", task.getId(),
                "targetUserId", targetUser.getId(),
                "targetUserName", normalizeUserName(targetUser)
        ));
    }

            /**
     * 审批通过加签任务。
     */
public void approveAddSignTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment
    ) {
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TASK_STATUS_APPROVED);
        task.setHandledAt(now);
        task.setActionComment(trimToNull(comment));
        processDocumentTaskMapper.updateById(task);
        appendLog(instance.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_APPROVE, userId, resolveActorDisplayName(userId, username), task.getActionComment(), Map.of(
                "taskId", task.getId(),
                "taskKind", TASK_KIND_ADD_SIGN,
                "sourceTaskId", task.getSourceTaskId()
        ));
        resumeSourceTask(task.getSourceTaskId(), now);
        instance.setStatus(DOCUMENT_STATUS_PENDING);
        instance.setCurrentNodeKey(task.getNodeKey());
        instance.setCurrentNodeName(task.getNodeName());
        instance.setCurrentTaskType(NODE_TYPE_APPROVAL);
        instance.setFinishedAt(null);
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);
    }

            /**
     * 判断付款任务是否允许重试。
     */
public boolean paymentTaskAllowsRetry(ProcessDocumentTask task) {
        ProcessDocumentInstance instance = requireDocument(task.getDocumentCode());
        return paymentTaskAllowsRetry(instance, task);
    }

            /**
     * 根据流程快照判断付款任务是否允许重试。
     */
public boolean paymentTaskAllowsRetry(ProcessDocumentInstance instance, ProcessDocumentTask task) {
        FlowRuntimeSnapshot snapshot = readFlowSnapshot(instance.getFlowSnapshotJson());
        ProcessFlowNodeDTO node = snapshot.node(task.getNodeKey());
        return node != null && paymentNodeAllowsRetry(node);
    }

            /**
     * 标记付款任务开始执行。
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
        LocalDateTime now = LocalDateTime.now();
        persistDocumentRuntimeState(
                instance,
                DOCUMENT_STATUS_PAYING,
                task.getNodeKey(),
                task.getNodeName(),
                NODE_TYPE_PAYMENT,
                null,
                now
        );
                appendLog(instance.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_PAYMENT_START, userId, resolveActorDisplayName(userId, username), retrying ? "重试支付" : null, Map.of(
                "taskId", task.getId(),
                "pushRequestNo", defaultText(trimToNull(pushRequestNo), ""),
                "companyBankAccountId", companyBankAccountId,
                "companyBankAccountName", defaultText(trimToNull(companyBankAccountName), ""),
                "retry", retrying
        ));
    }

            /**
     * 完成付款运行态并继续推进流程。
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
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TASK_STATUS_APPROVED);
        task.setHandledAt(now);
        task.setActionComment(trimToNull(comment));
        processDocumentTaskMapper.updateById(task);
        cancelOpenTasks(loadNodeOpenTasks(task.getDocumentCode(), task.getNodeKey()), task.getId(), now);

        appendLog(instance.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_PAYMENT_COMPLETE, userId, resolveActorDisplayName(userId, username), trimToNull(comment), Map.of(
                "taskId", task.getId(),
                "manualPaid", manualPaid,
                "paidAt", formatTime(paidAt == null ? now : paidAt)
        ));

        Map<String, Object> context = buildRuntimeContextForInstance(instance);
        persistDocumentRuntimeState(
                instance,
                DOCUMENT_STATUS_PAYMENT_COMPLETED,
                task.getNodeKey(),
                task.getNodeName(),
                NODE_TYPE_PAYMENT,
                null,
                now
        );

        FlowRuntimeSnapshot snapshot = readFlowSnapshot(instance.getFlowSnapshotJson());
        ProcessFlowNodeDTO node = snapshot.node(task.getNodeKey());
        if (node == null) {
            throw new IllegalStateException("当前支付任务找不到对应流程节点");
        }
        clearCurrentNode(instance);
        advanceFromPosition(instance, snapshot, context, node.getParentNodeKey(), nextIndex(snapshot, node), DOCUMENT_STATUS_PAYMENT_COMPLETED);
    }

            /**
     * 标记付款异常。
     */
public void markPaymentException(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment,
            boolean allowRetry
    ) {
        LocalDateTime now = LocalDateTime.now();
        persistDocumentRuntimeState(
                instance,
                DOCUMENT_STATUS_PAYMENT_EXCEPTION,
                task.getNodeKey(),
                task.getNodeName(),
                NODE_TYPE_PAYMENT,
                null,
                now
        );
        appendLog(instance.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_PAYMENT_EXCEPTION, userId, resolveActorDisplayName(userId, username), trimToNull(comment), Map.of(
                "taskId", task.getId(),
                "allowRetry", allowRetry
        ));
    }

            /**
     * 检查原始流程快照的根节点签名。
     */
public RawFlowSnapshotSignature inspectRawFlowSnapshot(String snapshotJson) {
        if (trimToNull(snapshotJson) == null) {
            return new RawFlowSnapshotSignature(false, false, false);
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(snapshotJson, new TypeReference<LinkedHashMap<String, Object>>() {});
            Object rawNodes = raw.get("nodes");
            if (!(rawNodes instanceof List<?> nodes)) {
                return new RawFlowSnapshotSignature(false, false, false);
            }
            boolean hasApprovalNode = false;
            boolean hasBlankRootNode = false;
            boolean hasNullRootNode = false;
            for (Object rawNode : nodes) {
                if (!(rawNode instanceof Map<?, ?> nodeMap)) {
                    continue;
                }
                String nodeType = trimToNull(stringValue(nodeMap.get("nodeType")));
                if (Objects.equals(nodeType, NODE_TYPE_APPROVAL)) {
                    hasApprovalNode = true;
                }
                if (!nodeMap.containsKey("parentNodeKey") || nodeMap.get("parentNodeKey") == null) {
                    hasNullRootNode = true;
                    continue;
                }
                String parentNodeKey = trimToNull(String.valueOf(nodeMap.get("parentNodeKey")));
                if (parentNodeKey == null) {
                    hasBlankRootNode = true;
                }
            }
            return new RawFlowSnapshotSignature(hasApprovalNode, hasBlankRootNode, hasNullRootNode);
        } catch (Exception ex) {
            log.warn("筛查流程快照修复信息时解析原始数据失败", ex);
            return new RawFlowSnapshotSignature(false, false, false);
        }
    }

    private FlowAdvanceState advanceFromPosition(
            ProcessDocumentInstance instance,
            FlowRuntimeSnapshot snapshot,
            Map<String, Object> context,
            String containerKey,
            int startIndex,
            String terminalStatus
    ) {
        FlowAdvanceState state = processContainer(instance, snapshot, context, containerKey, startIndex, terminalStatus);
        if (state == FlowAdvanceState.PAUSED) {
            return state;
        }

        ProcessFlowRouteDTO route = snapshot.routeByKey(containerKey);
        if (route != null) {
            ProcessFlowNodeDTO branchNode = snapshot.node(route.getSourceNodeKey());
            if (branchNode != null) {
                boolean branchHasAttachedTail = snapshot.routes(route.getSourceNodeKey()).stream()
                        .anyMatch(item -> Boolean.TRUE.equals(item.getAttachBelowNodes()));
                if (!branchHasAttachedTail || Boolean.TRUE.equals(route.getAttachBelowNodes())) {
                    return advanceFromPosition(instance, snapshot, context, branchNode.getParentNodeKey(), nextIndex(snapshot, branchNode), terminalStatus);
                }
                String parentContainerKey = branchNode.getParentNodeKey();
                return advanceFromPosition(
                        instance,
                        snapshot,
                        context,
                        parentContainerKey,
                        snapshot.children(parentContainerKey).size(),
                        terminalStatus
                );
            }
        }

        markDocumentApproved(instance, terminalStatus);
        appendLog(instance.getDocumentCode(), null, null, LOG_FINISH, null, "SYSTEM", FLOW_FINISH_COMMENT, Collections.emptyMap());
        return FlowAdvanceState.COMPLETED;
    }

    private FlowAdvanceState processContainer(
            ProcessDocumentInstance instance,
            FlowRuntimeSnapshot snapshot,
            Map<String, Object> context,
            String containerKey,
            int startIndex,
            String terminalStatus
    ) {
        List<ProcessFlowNodeDTO> nodes = snapshot.children(containerKey);
        for (int index = startIndex; index < nodes.size(); index++) {
            ProcessFlowNodeDTO node = nodes.get(index);
            switch (defaultText(asText(node.getNodeType()), "")) {
                case NODE_TYPE_BRANCH -> {
                    ProcessFlowRouteDTO matchedRoute = matchRoute(snapshot.routes(node.getNodeKey()), context);
                    if (matchedRoute == null) {
            markDocumentException(instance, node, "未命中任何分支条件");
                        return FlowAdvanceState.PAUSED;
                    }
                    appendLog(instance.getDocumentCode(), node.getNodeKey(), node.getNodeName(), LOG_ROUTE_HIT, null, "SYSTEM", null, Map.of(
                            "routeKey", matchedRoute.getRouteKey(),
                            "routeName", defaultText(matchedRoute.getRouteName(), matchedRoute.getRouteKey())
                    ));
                    return advanceFromPosition(instance, snapshot, context, matchedRoute.getRouteKey(), 0, terminalStatus);
                }
                case NODE_TYPE_APPROVAL -> {
                    if (isManualSelectApprovalNode(node)) {
                        List<User> manualApprovers = resolveManualMembers(node, context);
                        if (manualApprovers.isEmpty()) {
                            pauseForManualApproverSelection(instance, node);
                            return FlowAdvanceState.PAUSED;
                        }
                        if (dispatchApprovalNode(instance, snapshot, node, manualApprovers, context, resolveMissingHandler(node.getConfig()))) {
                            continue;
                        }
                        return FlowAdvanceState.PAUSED;
                    }
                    String missingHandler = resolveMissingHandler(node.getConfig());
                    List<User> approvers = resolveApprovers(node, context);
                    if (approvers.isEmpty()) {
                        approvers = resolveAutoTransferApprovers(node, context, missingHandler);
                    }
                    if (approvers.isEmpty()) {
                        FlowAdvanceState state = handleMissingUsers(
                                instance,
                                node,
                                missingHandler,
                                "\u5ba1\u6279\u4eba"
                        );
                        if (state == FlowAdvanceState.COMPLETED) {
                            continue;
                        }
                        return state;
                    }
                    if (dispatchApprovalNode(instance, snapshot, node, approvers, context, missingHandler)) {
                        continue;
                    }
                    return FlowAdvanceState.PAUSED;
                }
                case NODE_TYPE_CC -> {
                    FlowAdvanceState state = handleCcNode(instance, node, context);
                    if (state == FlowAdvanceState.COMPLETED) {
                        continue;
                    }
                    return state;
                }
                case NODE_TYPE_PAYMENT -> {
            appendLog(instance.getDocumentCode(), node.getNodeKey(), node.getNodeName(), LOG_PAYMENT_REACHED, null, "SYSTEM", "到达支付节点", Collections.emptyMap());
                    String missingHandler = resolveMissingHandler(node.getConfig());
                    List<User> executors = resolvePaymentExecutors(node, context);
                    if (executors.isEmpty()) {
                        executors = resolveAutoTransferPaymentExecutors(context, missingHandler);
                    }
                    if (executors.isEmpty()) {
                        FlowAdvanceState state = handleMissingUsers(
                                instance,
                                node,
                                missingHandler,
                                "\u652f\u4ed8\u6267\u884c\u4eba"
                        );
                        if (state == FlowAdvanceState.COMPLETED) {
                            continue;
                        }
                        return state;
                    }
                    createPaymentTasks(instance, node, executors);
                    return FlowAdvanceState.PAUSED;
                }
                default -> {
                }
            }
        }
        return FlowAdvanceState.COMPLETED;
    }

            /**
     * 为审批节点创建待办任务。
     */
private void createApprovalTasks(ProcessDocumentInstance instance, ProcessFlowNodeDTO node, List<User> approvers) {
        LocalDateTime now = LocalDateTime.now();
        String approvalMode = resolveApprovalMode(node);
        String batchNo = buildTaskBatchNo(instance.getDocumentCode(), node.getNodeKey());
        List<User> distinctApprovers = approvers.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new),
                        item -> new ArrayList<>(item.values())
                ));
        for (User approver : distinctApprovers) {
            ProcessDocumentTask task = new ProcessDocumentTask();
            task.setDocumentCode(instance.getDocumentCode());
            task.setNodeKey(node.getNodeKey());
            task.setNodeName(node.getNodeName());
            task.setNodeType(node.getNodeType());
            task.setAssigneeUserId(approver.getId());
            task.setAssigneeName(normalizeUserName(approver));
            task.setStatus(TASK_STATUS_PENDING);
            task.setTaskBatchNo(batchNo);
            task.setApprovalMode(approvalMode);
            task.setTaskKind(TASK_KIND_NORMAL);
            task.setCreatedAt(now);
            processDocumentTaskMapper.insert(task);
        }
        instance.setStatus(DOCUMENT_STATUS_PENDING);
        instance.setCurrentNodeKey(node.getNodeKey());
        instance.setCurrentNodeName(node.getNodeName());
        instance.setCurrentTaskType(NODE_TYPE_APPROVAL);
        instance.setFinishedAt(null);
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);

        appendLog(instance.getDocumentCode(), node.getNodeKey(), node.getNodeName(), LOG_APPROVAL_PENDING, null, "SYSTEM", null, Map.of(
                "approvalMode", approvalMode,
                "approverUserIds", distinctApprovers.stream().map(User::getId).toList(),
                "approverNames", distinctApprovers.stream().map(this::normalizeUserName).toList()
        ));
    }

            /**
     * 为付款节点创建待办任务。
     */
private void createPaymentTasks(ProcessDocumentInstance instance, ProcessFlowNodeDTO node, List<User> executors) {
        LocalDateTime now = LocalDateTime.now();
        String batchNo = buildTaskBatchNo(instance.getDocumentCode(), node.getNodeKey());
        List<User> distinctExecutors = executors.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new),
                        item -> new ArrayList<>(item.values())
                ));
        for (User executor : distinctExecutors) {
            ProcessDocumentTask task = new ProcessDocumentTask();
            task.setDocumentCode(instance.getDocumentCode());
            task.setNodeKey(node.getNodeKey());
            task.setNodeName(node.getNodeName());
            task.setNodeType(node.getNodeType());
            task.setAssigneeUserId(executor.getId());
            task.setAssigneeName(normalizeUserName(executor));
            task.setStatus(TASK_STATUS_PENDING);
            task.setTaskBatchNo(batchNo);
            task.setApprovalMode(APPROVAL_MODE_OR_SIGN);
            task.setTaskKind(TASK_KIND_NORMAL);
            task.setCreatedAt(now);
            processDocumentTaskMapper.insert(task);
        }
        persistDocumentRuntimeState(
                instance,
                DOCUMENT_STATUS_PENDING_PAYMENT,
                node.getNodeKey(),
                node.getNodeName(),
                NODE_TYPE_PAYMENT,
                null,
                now
        );
        appendLog(instance.getDocumentCode(), node.getNodeKey(), node.getNodeName(), LOG_PAYMENT_PENDING, null, "SYSTEM", null, Map.of(
                "executorUserIds", distinctExecutors.stream().map(User::getId).toList(),
                "executorNames", distinctExecutors.stream().map(this::normalizeUserName).toList(),
                "allowRetry", paymentNodeAllowsRetry(node)
        ));
    }

    private boolean dispatchApprovalNode(
            ProcessDocumentInstance instance,
            FlowRuntimeSnapshot snapshot,
            ProcessFlowNodeDTO node,
            List<User> approvers,
            Map<String, Object> context,
            String missingHandler
    ) {
        Map<Long, String> autoApprovedReasons = resolveAutoApprovedApproverReasons(
                instance.getDocumentCode(),
                snapshot,
                node,
                approvers,
                context
        );
        autoApprovedReasons.forEach((userId, reason) -> {
            User approver = approvers.stream()
                    .filter(item -> Objects.equals(item.getId(), userId))
                    .findFirst()
                    .orElse(null);
            if (approver == null) {
                return;
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("autoPass", true);
            payload.put("reason", reason);
            appendLog(
                    instance.getDocumentCode(),
                    node.getNodeKey(),
                    node.getNodeName(),
                    LOG_APPROVE,
                    approver.getId(),
                    normalizeUserName(approver),
                    reason,
                    payload
            );
        });
        String approvalMode = resolveApprovalMode(node);
        if (APPROVAL_MODE_OR_SIGN.equals(approvalMode) && !autoApprovedReasons.isEmpty()) {
            return true;
        }
        List<User> pendingApprovers = approvers.stream()
                .filter(item -> !autoApprovedReasons.containsKey(item.getId()))
                .toList();
        if (pendingApprovers.isEmpty()) {
            return true;
        }
        createApprovalTasks(instance, node, pendingApprovers);
        return false;
    }

    private Map<Long, String> resolveAutoApprovedApproverReasons(
            String documentCode,
            FlowRuntimeSnapshot snapshot,
            ProcessFlowNodeDTO node,
            List<User> approvers,
            Map<String, Object> context
    ) {
        Set<String> specialSettings = approvalSpecialSettings(node);
        if (specialSettings.isEmpty() || approvers == null || approvers.isEmpty()) {
            return Collections.emptyMap();
        }
        Long submitterUserId = asLong(context == null ? null : context.get("submitterUserId"));
        Set<Long> approvedBeforeUserIds = specialSettings.contains(APPROVAL_SPECIAL_AUTO_PASS_IF_APPROVED_BEFORE)
                ? loadApprovedUserIds(documentCode, snapshot, node)
                : Collections.emptySet();
        Map<Long, String> result = new LinkedHashMap<>();
        for (User approver : approvers) {
            if (approver == null || approver.getId() == null) {
                continue;
            }
            if (specialSettings.contains(APPROVAL_SPECIAL_AUTO_PASS_IF_APPROVER_IS_SUBMITTER)
                    && submitterUserId != null
                    && Objects.equals(approver.getId(), submitterUserId)) {
                result.put(approver.getId(), "\u5ba1\u6279\u4eba\u4e0e\u63d0\u5355\u4eba\u91cd\u590d\uff0c\u7cfb\u7edf\u81ea\u52a8\u901a\u8fc7");
                continue;
            }
            if (specialSettings.contains(APPROVAL_SPECIAL_AUTO_PASS_IF_APPROVED_BEFORE)
                    && approvedBeforeUserIds.contains(approver.getId())) {
                result.put(approver.getId(), "\u5ba1\u6279\u4eba\u5df2\u5728\u672c\u8f6e\u4e0a\u6e38\u8282\u70b9\u5ba1\u6279\uff0c\u7cfb\u7edf\u81ea\u52a8\u901a\u8fc7");
            }
        }
        return result;
    }

    private FlowAdvanceState handleCcNode(ProcessDocumentInstance instance, ProcessFlowNodeDTO node, Map<String, Object> context) {
        String missingHandler = resolveMissingHandler(node.getConfig());
        List<User> receivers = resolveCcRecipients(instance, node, context);
        if (receivers.isEmpty()) {
            receivers = resolveAutoTransferCcRecipients(context, missingHandler);
        }
        if (receivers.isEmpty()) {
            return handleMissingUsers(instance, node, missingHandler, "\u6284\u9001\u63a5\u6536\u4eba");
        }
        String timing = defaultText(asText(node.getConfig() == null ? null : node.getConfig().get("timing")), CC_TIMING_ON_ENTER);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("receiverUserIds", receivers.stream().map(User::getId).toList());
        payload.put("receiverNames", receivers.stream().map(this::normalizeUserName).toList());
        payload.put("timing", timing);
        appendLog(
                instance.getDocumentCode(),
                node.getNodeKey(),
                node.getNodeName(),
                LOG_CC_REACHED,
                null,
                "SYSTEM",
                CC_TIMING_ON_APPROVED.equals(timing) ? "审批通过后抄送" : "进入节点时抄送",
                payload
        );
        return FlowAdvanceState.COMPLETED;
    }

    private List<User> resolveCcRecipients(ProcessDocumentInstance instance, ProcessFlowNodeDTO node, Map<String, Object> context) {
        Map<String, Object> config = node.getConfig() == null ? new LinkedHashMap<>() : node.getConfig();
        String receiverType = defaultText(asText(config.get("receiverType")), CC_RECEIVER_TYPE_DESIGNATED_MEMBER);
        List<User> receivers;
        if (CC_RECEIVER_TYPE_SUBMITTER.equals(receiverType)) {
            receivers = resolveSubmitterUser(context);
        } else if (CC_RECEIVER_TYPE_DEPT_MANAGER.equals(receiverType)) {
            Map<String, Object> managerConfig = new LinkedHashMap<>();
            managerConfig.put("managerConfig", Map.of(
                    "deptSource", DEPT_SOURCE_SUBMITTER,
                    "managerLevel", 1,
                    "orgTreeLookupEnabled", true,
                    "orgTreeLookupLevel", 1
            ));
            receivers = resolveManagerMembers(managerConfig, context);
        } else {
            receivers = loadActiveUsers(toLongList(config.get("receiverUserIds")));
        }
        if (ccSpecialSettings(node).contains(CC_SPECIAL_INCLUDE_SUBMITTER)) {
            receivers = new ArrayList<>(receivers);
            receivers.addAll(resolveSubmitterUser(context));
        }
        List<User> distinctReceivers = receivers.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new),
                        item -> new ArrayList<>(item.values())
                ));
        if (!ccSpecialSettings(node).contains(CC_SPECIAL_SEND_ONCE)) {
            return distinctReceivers;
        }
        Set<Long> alreadySentUserIds = loadCcUserIds(instance.getDocumentCode());
        return distinctReceivers.stream()
                .filter(item -> !alreadySentUserIds.contains(item.getId()))
                .toList();
    }

    private FlowAdvanceState handleMissingUsers(
            ProcessDocumentInstance instance,
            ProcessFlowNodeDTO node,
            String missingHandler,
            String subjectLabel
    ) {
        if (MISSING_HANDLER_AUTO_SKIP.equals(missingHandler)) {
            appendLog(instance.getDocumentCode(), node.getNodeKey(), node.getNodeName(), LOG_AUTO_SKIP, null, "SYSTEM", "No " + subjectLabel + " resolved, auto skipped", Collections.emptyMap());
            return FlowAdvanceState.COMPLETED;
        }
        if (MISSING_HANDLER_BLOCK_SUBMIT.equals(missingHandler)) {
            throw new IllegalStateException("\u8282\u70b9\u3010" + defaultText(node == null ? null : node.getNodeName(), "\u672a\u547d\u540d\u8282\u70b9") + "\u3011\u627e\u4e0d\u5230" + subjectLabel + "\uff0c\u5f53\u524d\u914d\u7f6e\u4e0d\u5141\u8bb8\u63d0\u4ea4");
        }
        markDocumentException(instance, node, "No " + subjectLabel + " resolved");
        return FlowAdvanceState.PAUSED;
    }

    private void pauseForManualApproverSelection(ProcessDocumentInstance instance, ProcessFlowNodeDTO node) {
        LocalDateTime now = LocalDateTime.now();
        persistDocumentRuntimeState(
                instance,
                DOCUMENT_STATUS_PENDING,
                node == null ? null : node.getNodeKey(),
                node == null ? null : node.getNodeName(),
                CURRENT_TASK_TYPE_MANUAL_SELECT,
                null,
                now
        );
    }

    private List<User> resolveAutoTransferApprovers(ProcessFlowNodeDTO node, Map<String, Object> context, String missingHandler) {
        if (!MISSING_HANDLER_AUTO_TRANSFER.equals(missingHandler)) {
            return Collections.emptyList();
        }
        return resolveManagerMembers(new LinkedHashMap<>(), context);
    }

    private List<User> resolveAutoTransferCcRecipients(Map<String, Object> context, String missingHandler) {
        if (!MISSING_HANDLER_AUTO_TRANSFER.equals(missingHandler)) {
            return Collections.emptyList();
        }
        return resolveSubmitterUser(context);
    }

    private List<User> resolveAutoTransferPaymentExecutors(Map<String, Object> context, String missingHandler) {
        if (!MISSING_HANDLER_AUTO_TRANSFER.equals(missingHandler)) {
            return Collections.emptyList();
        }
        List<User> financeRoleUsers = resolvePaymentFinanceRoleMembers();
        if (!financeRoleUsers.isEmpty()) {
            return financeRoleUsers;
        }
        return resolveSubmitterUser(context);
    }

    private Set<Long> loadApprovedUserIds(String documentCode, FlowRuntimeSnapshot snapshot, ProcessFlowNodeDTO currentNode) {
        if (snapshot == null || currentNode == null) {
            return Collections.emptySet();
        }
        Set<String> upstreamNodeKeys = resolveRejectableTargetNodeKeys(documentCode, snapshot, currentNode);
        if (upstreamNodeKeys.isEmpty()) {
            return Collections.emptySet();
        }
        List<ProcessDocumentActionLog> logs = loadActionLogs(documentCode);
        int startIndex = resolveCurrentRoundStartIndex(logs);
        return logs.stream()
                .skip(startIndex)
                .filter(item -> Objects.equals(item.getActionType(), LOG_APPROVE))
                .filter(item -> upstreamNodeKeys.contains(trimToNull(item.getNodeKey())))
                .map(ProcessDocumentActionLog::getActorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int resolveCurrentRoundStartIndex(List<ProcessDocumentActionLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return 0;
        }
        for (int index = logs.size() - 1; index >= 0; index--) {
            String actionType = trimToNull(logs.get(index).getActionType());
            if (Objects.equals(actionType, LOG_SUBMIT) || Objects.equals(actionType, LOG_RESUBMIT)) {
                return index;
            }
        }
        return 0;
    }

    private Set<Long> loadCcUserIds(String documentCode) {
        Set<Long> result = new LinkedHashSet<>();
        loadActionLogs(documentCode).stream()
                .filter(item -> Objects.equals(item.getActionType(), LOG_CC_REACHED))
                .forEach(item -> result.addAll(toLongList(readMap(item.getPayloadJson()).get("receiverUserIds"))));
        return result;
    }

    private ProcessFlowNodeDTO resolveResumeNode(FlowRuntimeSnapshot snapshot, Map<String, Object> context) {
        String resumeNodeKey = trimToNull(asText(context == null ? null : context.get("resumeNodeKey")));
        return resumeNodeKey == null ? null : snapshot.node(resumeNodeKey);
    }

    private String normalizeRejectTargetNodeKey(
            String documentCode,
            FlowRuntimeSnapshot snapshot,
            ProcessFlowNodeDTO currentNode,
            String targetNodeKey
    ) {
        String normalizedTargetNodeKey = trimToNull(targetNodeKey);
        if (normalizedTargetNodeKey == null) {
            return null;
        }
        if (currentNode == null || !approvalSpecialSettings(currentNode).contains(APPROVAL_SPECIAL_REJECT_TO_ANY_NODE)) {
            throw new IllegalStateException("\u5f53\u524d\u5ba1\u6279\u8282\u70b9\u672a\u5f00\u542f\u9a73\u56de\u81f3\u4efb\u610f\u8282\u70b9");
        }
        ProcessFlowNodeDTO targetNode = snapshot.node(normalizedTargetNodeKey);
        if (targetNode == null || !NODE_TYPE_APPROVAL.equals(targetNode.getNodeType())) {
            throw new IllegalStateException("\u9a73\u56de\u76ee\u6807\u8282\u70b9\u4e0d\u5b58\u5728\u6216\u4e0d\u662f\u5ba1\u6279\u8282\u70b9");
        }
        if (Objects.equals(targetNode.getNodeKey(), currentNode.getNodeKey())) {
            throw new IllegalStateException("\u9a73\u56de\u76ee\u6807\u8282\u70b9\u4e0d\u80fd\u662f\u5f53\u524d\u5ba1\u6279\u8282\u70b9");
        }
        if (!resolveRejectableTargetNodeKeys(documentCode, snapshot, currentNode).contains(targetNode.getNodeKey())) {
            throw new IllegalStateException("\u9a73\u56de\u76ee\u6807\u53ea\u80fd\u9009\u62e9\u5f53\u524d\u8282\u70b9\u4e4b\u524d\u5df2\u5230\u8fbe\u7684\u5ba1\u6279\u8282\u70b9");
        }
        return targetNode.getNodeKey();
    }

    private Set<String> resolveRejectableTargetNodeKeys(
            String documentCode,
            FlowRuntimeSnapshot snapshot,
            ProcessFlowNodeDTO currentNode
    ) {
        if (trimToNull(documentCode) == null) {
            return Collections.emptySet();
        }
        List<ProcessDocumentActionLog> logs = loadActionLogs(documentCode);
        int startIndex = resolveCurrentRoundStartIndex(logs);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (int index = startIndex; index < logs.size(); index++) {
            ProcessDocumentActionLog logItem = logs.get(index);
            String nodeKey = trimToNull(logItem.getNodeKey());
            if (nodeKey == null || Objects.equals(nodeKey, currentNode == null ? null : currentNode.getNodeKey())) {
                continue;
            }
            ProcessFlowNodeDTO logNode = snapshot.node(nodeKey);
            if (logNode == null || !NODE_TYPE_APPROVAL.equals(logNode.getNodeType())) {
                continue;
            }
            String actionType = trimToNull(logItem.getActionType());
            if (Objects.equals(actionType, LOG_APPROVAL_PENDING)
                    || Objects.equals(actionType, LOG_APPROVE)
                    || Objects.equals(actionType, LOG_AUTO_SKIP)
                    || Objects.equals(actionType, LOG_REJECT)) {
                result.add(nodeKey);
            }
        }
        return result;
    }

    private Set<String> approvalSpecialSettings(ProcessFlowNodeDTO node) {
        if (node == null || node.getConfig() == null) {
            return Collections.emptySet();
        }
        Object raw = node.getConfig().get("specialSettings");
        if (!(raw instanceof Collection<?> collection)) {
            return Collections.emptySet();
        }
        return collection.stream()
                .map(this::asText)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> ccSpecialSettings(ProcessFlowNodeDTO node) {
        if (node == null || node.getConfig() == null) {
            return Collections.emptySet();
        }
        Object raw = node.getConfig().get("specialSettings");
        if (!(raw instanceof Collection<?> collection)) {
            return Collections.emptySet();
        }
        return collection.stream()
                .map(this::asText)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<User> resolveSubmitterUser(Map<String, Object> context) {
        Long submitterUserId = asLong(context == null ? null : context.get("submitterUserId"));
        User submitter = loadActiveUser(submitterUserId);
        return submitter == null ? Collections.emptyList() : List.of(submitter);
    }

    private ProcessFlowRouteDTO matchRoute(List<ProcessFlowRouteDTO> routes, Map<String, Object> context) {
        if (routes == null || routes.isEmpty()) {
            return null;
        }
        return routes.stream()
                .sorted(Comparator.comparing(item -> item.getPriority() == null ? Integer.MAX_VALUE : item.getPriority()))
                .filter(route -> routeMatches(route, context))
                .findFirst()
                .orElse(null);
    }

    private boolean routeMatches(ProcessFlowRouteDTO route, Map<String, Object> context) {
        if (route.getConditionGroups() == null || route.getConditionGroups().isEmpty()) {
            return true;
        }
        return route.getConditionGroups().stream().anyMatch(group -> groupMatches(group, context));
    }

    private boolean groupMatches(ProcessFlowConditionGroupDTO group, Map<String, Object> context) {
        if (group.getConditions() == null || group.getConditions().isEmpty()) {
            return true;
        }
        return group.getConditions().stream().allMatch(condition -> conditionMatches(condition, context));
    }

    private boolean conditionMatches(ProcessFlowConditionDTO condition, Map<String, Object> context) {
        Object actual = resolveConditionActualValue(condition, context);
        Object compare = condition.getCompareValue();
        String operator = defaultText(condition.getOperator(), "EQ");
        return switch (operator) {
            case "NE" -> !valuesEqual(actual, compare);
            case "IN" -> anyIn(actual, compare, false);
            case "NOT_IN" -> !anyIn(actual, compare, false);
            case "GT" -> compareNumbers(actual, compare) > 0;
            case "GE" -> compareNumbers(actual, compare) >= 0;
            case "LT" -> compareNumbers(actual, compare) < 0;
            case "LE" -> compareNumbers(actual, compare) <= 0;
            case "BETWEEN" -> between(actual, compare);
            case "CONTAINS" -> containsValue(actual, compare);
            default -> valuesEqual(actual, compare);
        };
    }

    private Object resolveConditionActualValue(ProcessFlowConditionDTO condition, Map<String, Object> context) {
        if (condition == null || context == null) {
            return null;
        }
        String fieldKey = trimToNull(condition.getFieldKey());
        if (!CONDITION_FIELD_SUBMITTER_DEPT_ID.equals(fieldKey)) {
            return context.get(fieldKey);
        }
        Long submitterDeptId = asLong(context.get(fieldKey));
        if (submitterDeptId == null) {
            return Collections.emptyList();
        }
        return resolveDepartmentLineageIds(submitterDeptId);
    }

    private List<Long> resolveDepartmentLineageIds(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        Map<Long, SystemDepartment> departmentMap = loadAllDepartmentMap();
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        Long currentDeptId = deptId;
        while (currentDeptId != null && result.add(currentDeptId)) {
            SystemDepartment current = departmentMap.get(currentDeptId);
            currentDeptId = current == null ? null : current.getParentId();
        }
        return new ArrayList<>(result);
    }

            /**
     * 解析审批模式。
     */
private String resolveApprovalMode(ProcessFlowNodeDTO node) {
        if (shouldForceManagerAndSign(node)) {
            return APPROVAL_MODE_AND_SIGN;
        }
        Map<String, Object> config = node == null || node.getConfig() == null ? new LinkedHashMap<>() : node.getConfig();
        return defaultText(asText(config.get("approvalMode")), APPROVAL_MODE_OR_SIGN);
    }

    private boolean shouldForceManagerAndSign(ProcessFlowNodeDTO node) {
        Map<String, Object> config = node == null || node.getConfig() == null ? new LinkedHashMap<>() : node.getConfig();
        if (!APPROVER_TYPE_MANAGER.equals(defaultText(asText(config.get("approverType")), APPROVER_TYPE_MANAGER))) {
            return false;
        }
        Map<String, Object> managerConfig = toObjectMap(config.get("managerConfig"));
        return clampLevel(asInteger(managerConfig.get("managerLevel"), 1)) > 1;
    }

    private List<User> resolveApprovers(ProcessFlowNodeDTO node, Map<String, Object> context) {
        Map<String, Object> config = node.getConfig() == null ? new LinkedHashMap<>() : node.getConfig();
        String approverType = defaultText(asText(config.get("approverType")), APPROVER_TYPE_MANAGER);
        List<User> users;
        if (APPROVER_TYPE_DESIGNATED_MEMBER.equals(approverType)) {
            users = resolveDesignatedMembers(config);
        } else if (APPROVER_TYPE_MANUAL_SELECT.equals(approverType)) {
            users = resolveManualMembers(node, context);
        } else {
            users = resolveManagerMembers(config, context);
        }
        return users.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new),
                        item -> new ArrayList<>(item.values())
                ));
    }

            /**
     * 按主管规则解析审批人。
     */
private List<User> resolveManagerMembers(Map<String, Object> config, Map<String, Object> context) {
        Map<String, Object> managerConfig = toObjectMap(config.get("managerConfig"));
        String deptSource = defaultText(asText(managerConfig.get("deptSource")), DEPT_SOURCE_UNDERTAKE);
        int managerLevel = clampLevel(asInteger(managerConfig.get("managerLevel"), 1));
        boolean orgTreeLookupEnabled = asBoolean(managerConfig.get("orgTreeLookupEnabled"), true);
        int lookupLevel = clampLevel(asInteger(managerConfig.get("orgTreeLookupLevel"), 1));
        Map<Long, SystemDepartment> departmentMap = loadAllDepartmentMap();
        List<Long> startDeptIds = resolveStartDeptIds(deptSource, context);
        if (startDeptIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, User> result = new LinkedHashMap<>();
        for (Long deptId : startDeptIds) {
            SystemDepartment sourceDept = departmentMap.get(deptId);
            if (sourceDept == null) {
                return Collections.emptyList();
            }
            for (int level = 1; level <= managerLevel; level++) {
                SystemDepartment targetDept = climbDepartment(sourceDept, departmentMap, Math.max(level - 1, 0));
                if (targetDept == null) {
                    return Collections.emptyList();
                }
                LeaderResolution leader = resolveLeader(targetDept, departmentMap, orgTreeLookupEnabled, lookupLevel);
                if (leader == null) {
                    return Collections.emptyList();
                }
                User user = loadActiveUser(leader.userId());
                if (user == null || user.getId() == null) {
                    return Collections.emptyList();
                }
                result.putIfAbsent(user.getId(), user);
            }
        }
        return new ArrayList<>(result.values());
    }

            /**
     * 解析指定成员。
     */
private List<User> resolveDesignatedMembers(Map<String, Object> config) {
        return loadActiveUsers(toLongList(toObjectMap(config.get("designatedMemberConfig")).get("userIds")));
    }

            /**
     * 解析手动选人节点的审批人。
     */
private List<User> resolveManualMembers(ProcessFlowNodeDTO node, Map<String, Object> context) {
        Map<String, Object> selections = toObjectMap(context == null ? null : context.get("manualApproverSelections"));
        List<Long> userIds = toLongList(selections.get(node == null ? null : node.getNodeKey()));
        if (userIds.isEmpty()) {
            userIds = toLongList(context == null ? null : context.get("manualSelectedUserIds"));
        }
        return loadActiveUsers(userIds);
    }

    private boolean isManualSelectApprovalNode(ProcessFlowNodeDTO node) {
        if (node == null || !NODE_TYPE_APPROVAL.equals(node.getNodeType())) {
            return false;
        }
        Map<String, Object> config = node.getConfig() == null ? Collections.emptyMap() : node.getConfig();
        return APPROVER_TYPE_MANUAL_SELECT.equals(defaultText(asText(config.get("approverType")), APPROVER_TYPE_MANAGER));
    }

            /**
     * 解析付款执行人。
     */
private List<User> resolvePaymentExecutors(ProcessFlowNodeDTO node, Map<String, Object> context) {
        Map<String, Object> config = node.getConfig() == null ? new LinkedHashMap<>() : node.getConfig();
        String executorType = defaultText(asText(config.get("executorType")), PAYMENT_EXECUTOR_TYPE_DESIGNATED_MEMBER);
        List<User> users;
        if (PAYMENT_EXECUTOR_TYPE_FINANCE_ROLE.equals(executorType)) {
            users = resolvePaymentFinanceRoleMembers();
        } else if (PAYMENT_EXECUTOR_TYPE_SUBMITTER.equals(executorType)) {
            users = resolveSubmitterUser(context);
        } else {
            users = resolvePaymentDesignatedMembers(config);
        }
        return users.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new),
                        item -> new ArrayList<>(item.values())
                ));
    }

            /**
     * 解析指定付款执行人。
     */
private List<User> resolvePaymentDesignatedMembers(Map<String, Object> config) {
        return loadActiveUsers(toLongList(config.get("executorUserIds")));
    }

            /**
     * 解析财务角色付款执行人。
     */
private List<User> resolvePaymentFinanceRoleMembers() {
        SystemPermission permission = systemPermissionMapper.selectOne(
                Wrappers.<SystemPermission>lambdaQuery()
                        .eq(SystemPermission::getPermissionCode, PAYMENT_EXECUTE_PERMISSION)
                        .eq(SystemPermission::getStatus, 1)
                        .last("limit 1")
        );
        if (permission == null || permission.getId() == null) {
            return Collections.emptyList();
        }
        List<Long> roleIds = systemRolePermissionMapper.selectList(
                Wrappers.<SystemRolePermission>lambdaQuery()
                        .eq(SystemRolePermission::getPermissionId, permission.getId())
        ).stream().map(SystemRolePermission::getRoleId).distinct().toList();
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = systemUserRoleMapper.selectList(
                Wrappers.<SystemUserRole>lambdaQuery()
                        .in(SystemUserRole::getRoleId, roleIds)
        ).stream().map(SystemUserRole::getUserId).distinct().toList();
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return loadActiveUsers(userIds);
    }

    private boolean paymentNodeAllowsRetry(ProcessFlowNodeDTO node) {
        return paymentSpecialSettings(node).contains(PAYMENT_SPECIAL_ALLOW_RETRY);
    }

    private Set<String> paymentSpecialSettings(ProcessFlowNodeDTO node) {
        if (node == null || node.getConfig() == null) {
            return Collections.emptySet();
        }
        Object raw = node.getConfig().get("specialSettings");
        if (!(raw instanceof Collection<?> collection)) {
            return Collections.emptySet();
        }
        return collection.stream()
                .map(this::asText)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

            /**
     * 解析主管规则的起始部门。
     */
private List<Long> resolveStartDeptIds(String deptSource, Map<String, Object> context) {
        if (DEPT_SOURCE_SUBMITTER.equals(deptSource)) {
            Long submitterDeptId = asLong(context.get("submitterDeptId"));
            return submitterDeptId == null ? Collections.emptyList() : List.of(submitterDeptId);
        }
        List<Long> undertakeDeptIds = toLongList(context.get("undertakeDeptIds"));
        if (!undertakeDeptIds.isEmpty()) {
            return List.of(undertakeDeptIds.get(0));
        }
        Long submitterDeptId = asLong(context.get("submitterDeptId"));
        return submitterDeptId == null ? Collections.emptyList() : List.of(submitterDeptId);
    }

            /**
     * 判断是否命中过去的空根节点误审批问题。
     */
boolean isMisapprovedByBlankRootBug(String documentCode) {
        if (trimToNull(documentCode) == null) {
            return false;
        }
        List<ProcessDocumentActionLog> logs = loadActionLogs(documentCode);
        boolean hasFinish = logs.stream().anyMatch(logItem ->
                Objects.equals(logItem.getActionType(), LOG_FINISH)
                        && Objects.equals(trimToNull(logItem.getActionComment()), FLOW_FINISH_COMMENT)
        );
        boolean hasApprovalPending = logs.stream().anyMatch(logItem -> Objects.equals(logItem.getActionType(), LOG_APPROVAL_PENDING));
        long taskCount = processDocumentTaskMapper.selectCount(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, documentCode)
        );
        return hasFinish && !hasApprovalPending && taskCount == 0L;
    }

    void rebuildMisapprovedRuntime(ProcessDocumentInstance instance) {
        String documentCode = instance.getDocumentCode();
        log.info("Repairing misapproved expense document documentCode={}", documentCode);

        List<ProcessDocumentActionLog> logs = loadActionLogs(documentCode);
        logs.stream()
                .filter(logItem -> Objects.equals(logItem.getActionType(), LOG_FINISH))
                .filter(logItem -> Objects.equals(trimToNull(logItem.getActionComment()), FLOW_FINISH_COMMENT))
                .map(ProcessDocumentActionLog::getId)
                .filter(Objects::nonNull)
                .forEach(processDocumentActionLogMapper::deleteById);

        persistDocumentRuntimeState(instance, DOCUMENT_STATUS_PENDING, null, null, null, null, LocalDateTime.now());
        initializeRuntime(instance, buildRuntimeContextForInstance(instance));
    }

            /**
     * 加载单据费用明细。
     */
private List<ProcessDocumentExpenseDetail> loadExpenseDetails(String documentCode) {
        return processDocumentExpenseDetailMapper.selectList(
                Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                        .eq(ProcessDocumentExpenseDetail::getDocumentCode, documentCode)
                        .orderByAsc(ProcessDocumentExpenseDetail::getSortOrder, ProcessDocumentExpenseDetail::getId)
        );
    }

    private ExpenseDetailInstanceDTO toRuntimeExpenseDetailDTO(ProcessDocumentExpenseDetail detail) {
        ExpenseDetailInstanceDTO dto = new ExpenseDetailInstanceDTO();
        dto.setDetailNo(detail.getDetailNo());
        dto.setDetailDesignCode(detail.getDetailDesignCode());
        dto.setDetailType(detail.getDetailType());
        dto.setEnterpriseMode(detail.getEnterpriseMode());
        dto.setExpenseTypeCode(detail.getExpenseTypeCode());
        dto.setBusinessSceneMode(detail.getBusinessSceneMode());
        dto.setDetailTitle(detail.getDetailTitle());
        dto.setSortOrder(detail.getSortOrder());
        dto.setFormData(readMap(detail.getFormDataJson()));
        return dto;
    }

    private void markDocumentApproved(ProcessDocumentInstance instance, String terminalStatus) {
        LocalDateTime now = LocalDateTime.now();
        instance.setStatus(defaultText(trimToNull(terminalStatus), DOCUMENT_STATUS_COMPLETED));
        instance.setCurrentNodeKey(null);
        instance.setCurrentNodeName(null);
        instance.setCurrentTaskType(null);
        instance.setFinishedAt(now);
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);
    }

    private void markDocumentException(ProcessDocumentInstance instance, ProcessFlowNodeDTO node, String reason) {
        LocalDateTime now = LocalDateTime.now();
        instance.setStatus(DOCUMENT_STATUS_EXCEPTION);
        instance.setCurrentNodeKey(node == null ? null : node.getNodeKey());
        instance.setCurrentNodeName(node == null ? null : node.getNodeName());
        instance.setCurrentTaskType("EXCEPTION");
        instance.setFinishedAt(now);
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);
        appendLog(instance.getDocumentCode(), node == null ? null : node.getNodeKey(), node == null ? null : node.getNodeName(), LOG_EXCEPTION, null, "SYSTEM", reason, Collections.emptyMap());
    }

            /**
     * 清空单据当前节点信息。
     */
private void clearCurrentNode(ProcessDocumentInstance instance) {
        instance.setCurrentNodeKey(null);
        instance.setCurrentNodeName(null);
        instance.setCurrentTaskType(null);
        instance.setUpdatedAt(LocalDateTime.now());
        processDocumentInstanceMapper.updateById(instance);
    }

    private void persistDocumentRuntimeState(
            ProcessDocumentInstance instance,
            String status,
            String currentNodeKey,
            String currentNodeName,
            String currentTaskType,
            LocalDateTime finishedAt,
            LocalDateTime updatedAt
    ) {
        instance.setStatus(status);
        instance.setCurrentNodeKey(currentNodeKey);
        instance.setCurrentNodeName(currentNodeName);
        instance.setCurrentTaskType(currentTaskType);
        instance.setFinishedAt(finishedAt);
        instance.setUpdatedAt(updatedAt);
        processDocumentInstanceMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ProcessDocumentInstance>()
                        .eq("id", instance.getId())
                        .set("status", status)
                        .set("current_node_key", currentNodeKey)
                        .set("current_node_name", currentNodeName)
                        .set("current_task_type", currentTaskType)
                        .set("finished_at", finishedAt)
                        .set("updated_at", updatedAt)
        );
    }

    private void appendLog(
            String documentCode,
            String nodeKey,
            String nodeName,
            String actionType,
            Long actorUserId,
            String actorName,
            String actionComment,
            Map<String, Object> payload
    ) {
        ProcessDocumentActionLog logItem = new ProcessDocumentActionLog();
        logItem.setDocumentCode(documentCode);
        logItem.setNodeKey(nodeKey);
        logItem.setNodeName(nodeName);
        logItem.setActionType(actionType);
        logItem.setActorUserId(actorUserId);
        logItem.setActorName(actorName);
        logItem.setActionComment(trimToNull(actionComment));
        logItem.setPayloadJson(payload == null || payload.isEmpty() ? null : writeJson(payload));
        logItem.setCreatedAt(LocalDateTime.now());
        processDocumentActionLogMapper.insert(logItem);
    }

    private void resumeSourceTask(Long sourceTaskId, LocalDateTime now) {
        if (sourceTaskId == null) {
            return;
        }
        ProcessDocumentTask sourceTask = processDocumentTaskMapper.selectById(sourceTaskId);
        if (sourceTask == null || !Objects.equals(sourceTask.getStatus(), TASK_STATUS_PAUSED)) {
            return;
        }
        sourceTask.setStatus(TASK_STATUS_PENDING);
        sourceTask.setCreatedAt(now);
        sourceTask.setHandledAt(null);
        processDocumentTaskMapper.updateById(sourceTask);
    }

    private ProcessDocumentInstance requireDocument(String documentCode) {
        String normalizedCode = trimToNull(documentCode);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("单据编码不能为空");
        }
        ProcessDocumentInstance instance = processDocumentInstanceMapper.selectOne(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getDocumentCode, normalizedCode)
                        .last("limit 1")
        );
        if (instance == null) {
            throw new IllegalStateException("未找到对应单据");
        }
        return instance;
    }

    private FlowRuntimeSnapshot readFlowSnapshot(String snapshotJson) {
        if (trimToNull(snapshotJson) == null) {
            return new FlowRuntimeSnapshot(Collections.emptyList(), Collections.emptyList());
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(snapshotJson, new TypeReference<LinkedHashMap<String, Object>>() {});
            List<ProcessFlowNodeDTO> nodes = objectMapper.convertValue(raw.getOrDefault("nodes", Collections.emptyList()), new TypeReference<List<ProcessFlowNodeDTO>>() {});
            List<ProcessFlowRouteDTO> routes = objectMapper.convertValue(raw.getOrDefault("routes", Collections.emptyList()), new TypeReference<List<ProcessFlowRouteDTO>>() {});
            return new FlowRuntimeSnapshot(nodes, routes);
        } catch (Exception ex) {
            throw new IllegalStateException("流程快照解析失败", ex);
        }
    }

            /**
     * 加载单据未完成任务。
     */
private List<ProcessDocumentTask> loadOpenTasks(String documentCode) {
        return processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                        .in(ProcessDocumentTask::getStatus, List.of(TASK_STATUS_PENDING, TASK_STATUS_PAUSED))
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
    }

            /**
     * 加载节点下未完成任务。
     */
private List<ProcessDocumentTask> loadNodeOpenTasks(String documentCode, String nodeKey) {
        return processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                        .eq(ProcessDocumentTask::getNodeKey, nodeKey)
                        .in(ProcessDocumentTask::getStatus, List.of(TASK_STATUS_PENDING, TASK_STATUS_PAUSED))
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
    }

            /**
     * 加载单据动作日志。
     */
private List<ProcessDocumentActionLog> loadActionLogs(String documentCode) {
        return processDocumentActionLogMapper.selectList(
                Wrappers.<ProcessDocumentActionLog>lambdaQuery()
                        .eq(ProcessDocumentActionLog::getDocumentCode, documentCode)
                        .orderByAsc(ProcessDocumentActionLog::getCreatedAt, ProcessDocumentActionLog::getId)
        );
    }

    private void cancelOpenTasks(List<ProcessDocumentTask> tasks, Long keepTaskId, LocalDateTime handledAt) {
        for (ProcessDocumentTask task : tasks) {
            if (Objects.equals(task.getId(), keepTaskId)
                    || (!TASK_STATUS_PENDING.equals(task.getStatus()) && !TASK_STATUS_PAUSED.equals(task.getStatus()))) {
                continue;
            }
            task.setStatus(TASK_STATUS_CANCELLED);
            task.setHandledAt(handledAt);
            processDocumentTaskMapper.updateById(task);
        }
    }

            /**
     * 解析单据总金额。
     */
private BigDecimal resolveTotalAmount(
            Map<String, Object> formData,
            List<ExpenseDetailInstanceDTO> expenseDetails,
            String defaultBusinessSceneMode
    ) {
        return ExpenseAmountResolver.resolveDocumentTotalAmount(formData, expenseDetails, defaultBusinessSceneMode);
    }

    private void collectUndertakeDeptIdsFromSchema(Set<String> result, Map<String, Object> schema, Map<String, Object> formData) {
        if (schema == null || formData == null || formData.isEmpty()) {
            return;
        }
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks)) {
            return;
        }
        for (Object rawBlock : blocks) {
            if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(blockMap.get("kind")), "BUSINESS_COMPONENT")) {
                continue;
            }
            Object rawProps = blockMap.get("props");
            if (!(rawProps instanceof Map<?, ?> props)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(props.get("componentCode")), UNDERTAKE_DEPARTMENT_COMPONENT_CODE)) {
                continue;
            }
            String fieldKey = trimToNull(String.valueOf(blockMap.get("fieldKey")));
            if (fieldKey != null) {
                collectDeptIds(result, formData.get(fieldKey));
            }
        }
    }

            /**
     * 解析承担部门标识。
     */
private List<String> resolveUndertakeDeptIds(
            ProcessFormDesign formDesign,
            Map<String, Object> formData,
            ProcessExpenseDetailDesign expenseDetailDesign,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        Set<String> deptIds = new LinkedHashSet<>();
        collectUndertakeDeptIdsFromSchema(deptIds, formDesign == null ? null : readSchema(formDesign.getSchemaJson()), formData == null ? Collections.emptyMap() : formData);
        if (expenseDetailDesign != null && expenseDetails != null) {
            Map<String, Object> schema = readSchema(expenseDetailDesign.getSchemaJson());
            for (ExpenseDetailInstanceDTO expenseDetail : expenseDetails) {
                collectUndertakeDeptIdsFromSchema(
                        deptIds,
                        schema,
                        expenseDetail == null || expenseDetail.getFormData() == null ? Collections.emptyMap() : expenseDetail.getFormData()
                );
            }
        }
        return new ArrayList<>(deptIds);
    }

            /**
     * 从快照中解析承担部门标识。
     */
private List<String> resolveUndertakeDeptIdsFromSnapshots(
            Map<String, Object> mainSchema,
            Map<String, Object> mainFormData,
            List<ProcessDocumentExpenseDetail> expenseDetails
    ) {
        Set<String> deptIds = new LinkedHashSet<>();
        collectUndertakeDeptIdsFromSchema(deptIds, mainSchema, mainFormData);
        if (expenseDetails != null) {
            for (ProcessDocumentExpenseDetail expenseDetail : expenseDetails) {
                collectUndertakeDeptIdsFromSchema(
                        deptIds,
                        readMap(expenseDetail.getSchemaSnapshotJson()),
                        readMap(expenseDetail.getFormDataJson())
                );
            }
        }
        return new ArrayList<>(deptIds);
    }

            /**
     * 合并主表单与费用明细运行态数据。
     */
private Map<String, Object> mergeRuntimeFormData(Map<String, Object> formData, List<ExpenseDetailInstanceDTO> expenseDetails) {
        Map<String, Object> merged = formData == null ? new LinkedHashMap<>() : new LinkedHashMap<>(formData);
        if (expenseDetails == null || expenseDetails.isEmpty()) {
            return merged;
        }
        List<Map<String, Object>> detailFormDataList = expenseDetails.stream()
                .<Map<String, Object>>map(item -> item == null || item.getFormData() == null
                        ? new LinkedHashMap<String, Object>()
                        : new LinkedHashMap<>(item.getFormData()))
                .toList();
        merged.put("expenseDetails", detailFormDataList);
        merged.put("__expenseDetailCount", detailFormDataList.size());
        return merged;
    }

    private void collectDeptIds(Set<String> result, Object value) {
        if (value instanceof List<?> items) {
            for (Object item : items) {
                String normalized = trimToNull(String.valueOf(item));
                if (normalized != null) {
                    result.add(normalized);
                }
            }
            return;
        }
        String normalized = trimToNull(value == null ? null : String.valueOf(value));
        if (normalized != null) {
            result.add(normalized);
        }
    }

    private Map<String, Object> readSchema(String schemaJson) {
        if (trimToNull(schemaJson) == null) {
            return defaultSchema();
        }
        try {
            return objectMapper.readValue(schemaJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("表单结构解析失败", ex);
        }
    }

    private Map<String, Object> defaultSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("layoutMode", "TWO_COLUMN");
        schema.put("blocks", Collections.emptyList());
        return schema;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("数据序列化失败", ex);
        }
    }

    private Map<String, Object> readMap(String json) {
        if (trimToNull(json) == null) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("JSON 映射解析失败", ex);
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(String.valueOf(number));
        }
        try {
            String normalized = trimToNull(String.valueOf(value));
            return normalized == null ? null : new BigDecimal(normalized);
        } catch (Exception ex) {
            return null;
        }
    }

    private int compareNumbers(Object actual, Object compare) {
        BigDecimal left = toBigDecimal(actual);
        BigDecimal right = toBigDecimal(compare);
        if (left == null || right == null) {
            return 0;
        }
        return left.compareTo(right);
    }

    private boolean between(Object actual, Object compare) {
        BigDecimal current = toBigDecimal(actual);
        if (current == null) {
            return false;
        }
        List<Object> range = toObjectList(compare);
        if (range.size() < 2) {
            return false;
        }
        BigDecimal start = toBigDecimal(range.get(0));
        BigDecimal end = toBigDecimal(range.get(1));
        if (start == null || end == null) {
            return false;
        }
        return current.compareTo(start) >= 0 && current.compareTo(end) <= 0;
    }

    private boolean containsValue(Object actual, Object compare) {
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> valuesEqual(item, compare));
        }
        String actualText = trimToNull(String.valueOf(actual));
        String compareText = trimToNull(String.valueOf(compare));
        return actualText != null && compareText != null && actualText.contains(compareText);
    }

    private boolean anyIn(Object actual, Object compare, boolean defaultResult) {
        List<Object> compareList = toObjectList(compare);
        if (compareList.isEmpty()) {
            return defaultResult;
        }
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> compareList.stream().anyMatch(candidate -> valuesEqual(item, candidate)));
        }
        return compareList.stream().anyMatch(candidate -> valuesEqual(actual, candidate));
    }

    private boolean valuesEqual(Object actual, Object compare) {
        BigDecimal leftNumber = toBigDecimal(actual);
        BigDecimal rightNumber = toBigDecimal(compare);
        if (leftNumber != null && rightNumber != null) {
            return leftNumber.compareTo(rightNumber) == 0;
        }
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> valuesEqual(item, compare));
        }
        return Objects.equals(defaultText(asText(actual), ""), defaultText(asText(compare), ""));
    }

    private List<Object> toObjectList(Object value) {
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (value == null) {
            return new ArrayList<>();
        }
        return List.of(value);
    }

            /**
     * 生成任务批次号。
     */
private String buildTaskBatchNo(String documentCode, String nodeKey) {
        return documentCode + "-" + nodeKey + "-" + System.currentTimeMillis();
    }

            /**
     * 解析人员缺失时的处理策略。
     */
private String resolveMissingHandler(Map<String, Object> config) {
        return defaultText(asText(config == null ? null : config.get("missingHandler")), MISSING_HANDLER_AUTO_SKIP);
    }

            /**
     * 加载启用中的部门映射。
     */
private Map<Long, SystemDepartment> loadAllDepartmentMap() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getStatus, 1)
        ).stream().collect(Collectors.toMap(
                SystemDepartment::getId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

            /**
     * 加载启用中的用户。
     */
private User loadActiveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user != null && Objects.equals(user.getStatus(), 1) ? user : null;
    }

            /**
     * 批量加载启用中的用户。
     */
private List<User> loadActiveUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(item.getStatus(), 1))
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    private SystemDepartment climbDepartment(SystemDepartment start, Map<Long, SystemDepartment> departmentMap, int steps) {
        SystemDepartment current = start;
        for (int index = 0; index < steps && current != null; index++) {
            current = current.getParentId() == null ? null : departmentMap.get(current.getParentId());
        }
        return current;
    }

            /**
     * 按组织树向上查找负责人。
     */
private LeaderResolution resolveLeader(SystemDepartment startDept, Map<Long, SystemDepartment> departmentMap, boolean allowLookup, int lookupLevel) {
        SystemDepartment current = startDept;
        int remaining = lookupLevel;
        while (current != null) {
            if (current.getLeaderUserId() != null && current.getLeaderUserId() > 0) {
                return new LeaderResolution(current.getId(), current.getLeaderUserId());
            }
            if (!allowLookup || remaining <= 0 || current.getParentId() == null) {
                break;
            }
            current = departmentMap.get(current.getParentId());
            remaining--;
        }
        return null;
    }

    private int nextIndex(FlowRuntimeSnapshot snapshot, ProcessFlowNodeDTO node) {
        return snapshot.indexInContainer(node.getParentNodeKey(), node.getNodeKey()) + 1;
    }

    private String normalizeUserName(User user) {
        String name = trimToNull(user.getName());
        return name != null ? name : defaultText(asText(user.getUsername()), "未知用户");
    }

    private String resolveActorDisplayName(Long userId, String username) {
        User user = loadActiveUser(userId);
        return user != null ? normalizeUserName(user) : defaultUsername(username);
    }

    private Map<String, Object> toObjectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return new LinkedHashMap<>();
    }

    private List<Long> toLongList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        List<Long> result = new ArrayList<>();
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                Long parsed = asLong(item);
                if (parsed != null) {
                    result.add(parsed);
                }
            }
            return result;
        }
        Long parsed = asLong(value);
        if (parsed != null) {
            result.add(parsed);
        }
        return result;
    }

    private Integer asInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean asBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private int clampLevel(Integer value) {
        int level = value == null ? 1 : value;
        if (level < 1) {
            return 1;
        }
        return Math.min(level, 10);
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return firstNonBlank(
                    stringValue(map.get("value")),
                    stringValue(map.get("label")),
                    stringValue(map.get("text"))
            );
        }
        return trimToNull(String.valueOf(value));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String defaultUsername(String username) {
        String normalized = trimToNull(username);
        return normalized == null ? "SYSTEM" : normalized;
    }
}




