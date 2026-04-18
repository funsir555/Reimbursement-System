// 业务域：报销单录入、流转与查询
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

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
 * AbstractExpenseWorkflowSupport：通用支撑类。
 * 封装 报销单这块可复用的业务能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
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
    private static final String DEPT_SOURCE_UNDERTAKE = "UNDERTAKE_DEPT";
    private static final String DEPT_SOURCE_SUBMITTER = "SUBMITTER_DEPT";
    private static final String MISSING_HANDLER_AUTO_SKIP = "AUTO_SKIP";
    private static final String APPROVAL_MODE_OR_SIGN = "OR_SIGN";
    private static final String APPROVAL_MODE_AND_SIGN = "AND_SIGN";
    private static final String PAYMENT_SPECIAL_ALLOW_RETRY = "ALLOW_RETRY";
    private static final String PAYMENT_EXECUTE_PERMISSION = "expense:payment:payment_order:execute";

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
    private static final String FLOW_FINISH_COMMENT = "Approval flow finished";

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
        Map<String, Object> context = mergeRuntimeFormData(formData, expenseDetails);
        if (currentUser != null && currentUser.getId() != null) {
            context.put("submitterUserId", currentUser.getId());
        }
        if (currentUser != null && currentUser.getDeptId() != null) {
            context.put("submitterDeptId", currentUser.getDeptId());
        }
        BigDecimal amount = resolveTotalAmount(formData == null ? Collections.emptyMap() : formData);
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
     * 组装运行时上下文ForInstance。
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
     * 处理报销单中的这一步。
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
                appendLog(instance.getDocumentCode(), null, null, LOG_FINISH, null, "SYSTEM", "No approval nodes configured", Collections.emptyMap());
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
     * 校验流程Snapshot。
     */
    public void validateFlowSnapshot(String snapshotJson) {
        readFlowSnapshot(snapshotJson);
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
        FlowRuntimeSnapshot snapshot = readFlowSnapshot(instance.getFlowSnapshotJson());
        ProcessFlowNodeDTO node = snapshot.node(task.getNodeKey());
        if (node == null) {
            throw new IllegalStateException("Flow node not found for current task");
        }

        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TASK_STATUS_APPROVED);
        task.setHandledAt(now);
        task.setActionComment(trimToNull(comment));
        processDocumentTaskMapper.updateById(task);
        appendLog(instance.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_APPROVE, userId, defaultUsername(username), task.getActionComment(), Map.of(
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
     * 审批驳回Pending任务。
     */
    public void rejectPendingTask(
            ProcessDocumentInstance instance,
            ProcessDocumentTask task,
            Long userId,
            String username,
            String comment
    ) {
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

        appendLog(instance.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_REJECT, userId, defaultUsername(username), task.getActionComment(), Map.of(
                "taskId", task.getId()
        ));
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

        appendLog(task.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_ADD_SIGN, userId, defaultUsername(username), trimToNull(remark), Map.of(
                "taskId", task.getId(),
                "targetUserId", targetUser.getId(),
                "targetUserName", normalizeUserName(targetUser)
        ));
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
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TASK_STATUS_APPROVED);
        task.setHandledAt(now);
        task.setActionComment(trimToNull(comment));
        processDocumentTaskMapper.updateById(task);
        appendLog(instance.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_APPROVE, userId, defaultUsername(username), task.getActionComment(), Map.of(
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
     * 处理报销单中的这一步。
     */
    public boolean paymentTaskAllowsRetry(ProcessDocumentTask task) {
        ProcessDocumentInstance instance = requireDocument(task.getDocumentCode());
        return paymentTaskAllowsRetry(instance, task);
    }

    /**
     * 处理报销单中的这一步。
     */
    public boolean paymentTaskAllowsRetry(ProcessDocumentInstance instance, ProcessDocumentTask task) {
        FlowRuntimeSnapshot snapshot = readFlowSnapshot(instance.getFlowSnapshotJson());
        ProcessFlowNodeDTO node = snapshot.node(task.getNodeKey());
        return node != null && paymentNodeAllowsRetry(node);
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
        appendLog(instance.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_PAYMENT_START, userId, defaultUsername(username), retrying ? "Retry payment" : null, Map.of(
                "taskId", task.getId(),
                "pushRequestNo", defaultText(trimToNull(pushRequestNo), ""),
                "companyBankAccountId", companyBankAccountId,
                "companyBankAccountName", defaultText(trimToNull(companyBankAccountName), ""),
                "retry", retrying
        ));
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
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TASK_STATUS_APPROVED);
        task.setHandledAt(now);
        task.setActionComment(trimToNull(comment));
        processDocumentTaskMapper.updateById(task);
        cancelOpenTasks(loadNodeOpenTasks(task.getDocumentCode(), task.getNodeKey()), task.getId(), now);

        appendLog(instance.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_PAYMENT_COMPLETE, userId, defaultUsername(username), trimToNull(comment), Map.of(
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
            throw new IllegalStateException("Flow node not found for current payment task");
        }
        clearCurrentNode(instance);
        advanceFromPosition(instance, snapshot, context, node.getParentNodeKey(), nextIndex(snapshot, node), DOCUMENT_STATUS_PAYMENT_COMPLETED);
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
        appendLog(instance.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_PAYMENT_EXCEPTION, userId, defaultUsername(username), trimToNull(comment), Map.of(
                "taskId", task.getId(),
                "allowRetry", allowRetry
        ));
    }

    /**
     * 处理报销单中的这一步。
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
            log.warn("Failed to inspect raw flow snapshot for repair screening", ex);
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
                        markDocumentException(instance, node, "No branch route matched");
                        return FlowAdvanceState.PAUSED;
                    }
                    appendLog(instance.getDocumentCode(), node.getNodeKey(), node.getNodeName(), LOG_ROUTE_HIT, null, "SYSTEM", null, Map.of(
                            "routeKey", matchedRoute.getRouteKey(),
                            "routeName", defaultText(matchedRoute.getRouteName(), matchedRoute.getRouteKey())
                    ));
                    return advanceFromPosition(instance, snapshot, context, matchedRoute.getRouteKey(), 0, terminalStatus);
                }
                case NODE_TYPE_APPROVAL -> {
                    List<User> approvers = resolveApprovers(node, context);
                    if (approvers.isEmpty()) {
                        String missingHandler = resolveMissingHandler(node.getConfig());
                        if (MISSING_HANDLER_AUTO_SKIP.equals(missingHandler)) {
                            appendLog(instance.getDocumentCode(), node.getNodeKey(), node.getNodeName(), LOG_AUTO_SKIP, null, "SYSTEM", "No approver resolved, auto skipped", Collections.emptyMap());
                            continue;
                        }
                        markDocumentException(instance, node, "No approver resolved");
                        return FlowAdvanceState.PAUSED;
                    }
                    createApprovalTasks(instance, node, approvers);
                    return FlowAdvanceState.PAUSED;
                }
                case NODE_TYPE_CC -> appendLog(instance.getDocumentCode(), node.getNodeKey(), node.getNodeName(), LOG_CC_REACHED, null, "SYSTEM", "CC node reached", Collections.emptyMap());
                case NODE_TYPE_PAYMENT -> {
                    appendLog(instance.getDocumentCode(), node.getNodeKey(), node.getNodeName(), LOG_PAYMENT_REACHED, null, "SYSTEM", "Payment node reached", Collections.emptyMap());
                    List<User> executors = resolvePaymentExecutors(node);
                    if (executors.isEmpty()) {
                        String missingHandler = resolveMissingHandler(node.getConfig());
                        if (MISSING_HANDLER_AUTO_SKIP.equals(missingHandler)) {
                            appendLog(instance.getDocumentCode(), node.getNodeKey(), node.getNodeName(), LOG_AUTO_SKIP, null, "SYSTEM", "No payment executor resolved, auto skipped", Collections.emptyMap());
                            continue;
                        }
                        markDocumentException(instance, node, "No payment executor resolved");
                        return FlowAdvanceState.PAUSED;
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
     * 创建审批任务。
     */
    private void createApprovalTasks(ProcessDocumentInstance instance, ProcessFlowNodeDTO node, List<User> approvers) {
        LocalDateTime now = LocalDateTime.now();
        String approvalMode = defaultText(asText(node.getConfig().get("approvalMode")), APPROVAL_MODE_OR_SIGN);
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
     * 创建付款任务。
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
        Object actual = context.get(condition.getFieldKey());
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

    /**
     * 解析Approvers。
     */
    private List<User> resolveApprovers(ProcessFlowNodeDTO node, Map<String, Object> context) {
        Map<String, Object> config = node.getConfig() == null ? new LinkedHashMap<>() : node.getConfig();
        String approverType = defaultText(asText(config.get("approverType")), APPROVER_TYPE_MANAGER);
        List<User> users;
        if (APPROVER_TYPE_DESIGNATED_MEMBER.equals(approverType)) {
            users = resolveDesignatedMembers(config);
        } else if (APPROVER_TYPE_MANUAL_SELECT.equals(approverType)) {
            users = resolveManualMembers(context);
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
     * 解析ManagerMembers。
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
        List<User> result = new ArrayList<>();
        for (Long deptId : startDeptIds) {
            SystemDepartment sourceDept = departmentMap.get(deptId);
            if (sourceDept == null) {
                continue;
            }
            SystemDepartment targetDept = climbDepartment(sourceDept, departmentMap, Math.max(managerLevel - 1, 0));
            LeaderResolution leader = resolveLeader(targetDept, departmentMap, orgTreeLookupEnabled, lookupLevel);
            if (leader == null) {
                continue;
            }
            User user = loadActiveUser(leader.userId());
            if (user != null) {
                result.add(user);
            }
        }
        return result;
    }

    /**
     * 解析DesignatedMembers。
     */
    private List<User> resolveDesignatedMembers(Map<String, Object> config) {
        return loadActiveUsers(toLongList(toObjectMap(config.get("designatedMemberConfig")).get("userIds")));
    }

    /**
     * 解析ManualMembers。
     */
    private List<User> resolveManualMembers(Map<String, Object> context) {
        return loadActiveUsers(toLongList(context.get("manualSelectedUserIds")));
    }

    /**
     * 解析付款Executors。
     */
    private List<User> resolvePaymentExecutors(ProcessFlowNodeDTO node) {
        Map<String, Object> config = node.getConfig() == null ? new LinkedHashMap<>() : node.getConfig();
        String executorType = defaultText(asText(config.get("executorType")), PAYMENT_EXECUTOR_TYPE_DESIGNATED_MEMBER);
        List<User> users;
        if (PAYMENT_EXECUTOR_TYPE_FINANCE_ROLE.equals(executorType)) {
            users = resolvePaymentFinanceRoleMembers();
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
     * 解析付款DesignatedMembers。
     */
    private List<User> resolvePaymentDesignatedMembers(Map<String, Object> config) {
        return loadActiveUsers(toLongList(config.get("executorUserIds")));
    }

    /**
     * 解析付款财务角色Members。
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
     * 解析StartDeptIds。
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
     * 判断Misapproved按BlankRootBug是否成立。
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
     * 加载报销单明细。
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
     * 清理当前Node。
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
            throw new IllegalArgumentException("Document code is required");
        }
        ProcessDocumentInstance instance = processDocumentInstanceMapper.selectOne(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getDocumentCode, normalizedCode)
                        .last("limit 1")
        );
        if (instance == null) {
            throw new IllegalStateException("Document not found");
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
            throw new IllegalStateException("Failed to parse flow snapshot", ex);
        }
    }

    /**
     * 加载开立任务。
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
     * 加载Node开立任务。
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
     * 加载ActionLogs。
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
     * 解析TotalAmount。
     */
    private BigDecimal resolveTotalAmount(Map<String, Object> formData) {
        BigDecimal directAmount = toBigDecimal(formData.get("__totalAmount"));
        if (directAmount != null) {
            return directAmount;
        }
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase();
            if (key.contains("amount") || key.contains("money")) {
                BigDecimal amount = toBigDecimal(entry.getValue());
                if (amount != null) {
                    return amount;
                }
            }
        }
        return null;
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
     * 解析UndertakeDeptIds。
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
     * 解析UndertakeDeptIdsFromSnapshots。
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
     * 合并运行时表单数据。
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
            throw new IllegalStateException("Failed to parse form schema", ex);
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
            throw new IllegalStateException("Failed to serialize data", ex);
        }
    }

    private Map<String, Object> readMap(String json) {
        if (trimToNull(json) == null) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse json map", ex);
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
     * 组装任务BatchNo。
     */
    private String buildTaskBatchNo(String documentCode, String nodeKey) {
        return documentCode + "-" + nodeKey + "-" + System.currentTimeMillis();
    }

    /**
     * 解析MissingHandler。
     */
    private String resolveMissingHandler(Map<String, Object> config) {
        return defaultText(asText(config == null ? null : config.get("missingHandler")), MISSING_HANDLER_AUTO_SKIP);
    }

    /**
     * 加载全部Department映射。
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
     * 加载Active用户。
     */
    private User loadActiveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user != null && Objects.equals(user.getStatus(), 1) ? user : null;
    }

    /**
     * 加载Active用户。
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
     * 解析上级。
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
        return name != null ? name : defaultText(asText(user.getUsername()), "Unknown User");
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
