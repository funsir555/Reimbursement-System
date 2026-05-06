package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseApprovalNodeStatusVO;
import com.finex.auth.dto.ExpenseApprovalTimelineItemVO;
import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class ExpenseApprovalProjectionSupport {

    private static final String ROOT_CONTAINER_KEY = "__ROOT__";
    private static final String NODE_TYPE_APPROVAL = "APPROVAL";
    private static final String NODE_TYPE_CC = "CC";
    private static final String NODE_TYPE_PAYMENT = "PAYMENT";
    private static final String NODE_TYPE_BRANCH = "BRANCH";

    private static final String TASK_STATUS_PENDING = "PENDING";
    private static final String TASK_STATUS_PAUSED = "PAUSED";

    private static final String LOG_SUBMIT = "SUBMIT";
    private static final String LOG_RECALL = "RECALL";
    private static final String LOG_RESUBMIT = "RESUBMIT";
    private static final String LOG_ROUTE_HIT = "ROUTE_HIT";
    private static final String LOG_APPROVE = "APPROVE";
    private static final String LOG_REJECT = "REJECT";
    private static final String LOG_MODIFY = "MODIFY";
    private static final String LOG_COMMENT = "COMMENT";
    private static final String LOG_REMIND = "REMIND";
    private static final String LOG_TRANSFER = "TRANSFER";
    private static final String LOG_ADD_SIGN = "ADD_SIGN";
    private static final String LOG_AUTO_SKIP = "AUTO_SKIP";
    private static final String LOG_CC_REACHED = "CC_REACHED";
    private static final String LOG_PAYMENT_COMPLETE = "PAYMENT_COMPLETE";
    private static final String LOG_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";
    private static final String LOG_PAYMENT_START = "PAYMENT_START";
    private static final String LOG_FINISH = "FINISH";
    private static final String LOG_EXCEPTION = "EXCEPTION";

    private static final String STATUS_NOT_REACHED = "NOT_REACHED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_MANUAL_SELECTION_PENDING = "MANUAL_SELECTION_PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_AUTO_SKIPPED = "AUTO_SKIPPED";
    private static final String STATUS_EXCEPTION = "EXCEPTION";
    private static final String STATUS_PAYMENT_PENDING = "PAYMENT_PENDING";
    private static final String STATUS_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    private static final String STATUS_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ExpenseMatchedFlowTraversalSupport matchedFlowTraversalSupport = new ExpenseMatchedFlowTraversalSupport();

    private final ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;

    ApprovalProjectionResult build(
            ProcessDocumentInstance instance,
            FlowRuntimeSnapshot snapshot,
            Map<String, Object> runtimeContext,
            List<ProcessDocumentTask> allTasks,
            List<ProcessDocumentActionLog> actionLogs
    ) {
        List<LogEntry> logEntries = actionLogs == null
                ? Collections.emptyList()
                : actionLogs.stream().map(this::toLogEntry).toList();
        Map<Long, String> userDisplayNames = new HashMap<>();
        Map<String, String> routeHitsByBranchNode = buildRouteHitIndex(logEntries);
        List<ProcessFlowNodeDTO> matchedBusinessNodes = collectFromPosition(
                instance,
                snapshot,
                runtimeContext == null ? Collections.emptyMap() : runtimeContext,
                routeHitsByBranchNode,
                ROOT_CONTAINER_KEY,
                0
        );
        Map<String, List<ProcessDocumentTask>> tasksByNode = groupTasksByNode(allTasks);
        Map<String, List<LogEntry>> logsByNode = logEntries.stream()
                .filter(item -> hasText(item.log().getNodeKey()))
                .collect(Collectors.groupingBy(
                        item -> item.log().getNodeKey(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ExpenseApprovalNodeStatusVO> nodeStatuses = matchedBusinessNodes.stream()
                .map(node -> buildNodeStatus(
                        instance,
                        node,
                        runtimeContext,
                        tasksByNode.get(node.getNodeKey()),
                        logsByNode.get(node.getNodeKey()),
                        userDisplayNames
                ))
                .toList();
        List<ExpenseApprovalTimelineItemVO> timeline = buildTimeline(instance, logEntries, nodeStatuses, userDisplayNames);
        return new ApprovalProjectionResult(nodeStatuses, timeline);
    }

    private Map<String, List<ProcessDocumentTask>> groupTasksByNode(List<ProcessDocumentTask> allTasks) {
        if (allTasks == null || allTasks.isEmpty()) {
            return Collections.emptyMap();
        }
        return allTasks.stream()
                .filter(item -> hasText(item.getNodeKey()))
                .collect(Collectors.groupingBy(ProcessDocumentTask::getNodeKey, LinkedHashMap::new, Collectors.toList()));
    }

    private List<ProcessFlowNodeDTO> collectFromPosition(
            ProcessDocumentInstance instance,
            FlowRuntimeSnapshot snapshot,
            Map<String, Object> runtimeContext,
            Map<String, String> routeHitsByBranchNode,
            String containerKey,
            int startIndex
    ) {
        return matchedFlowTraversalSupport.collectMatchedPath(
                        snapshot,
                        containerKey,
                        startIndex,
                        branchNode -> resolveMatchedRoute(instance, snapshot, runtimeContext, routeHitsByBranchNode, branchNode)
                ).stream()
                .filter(step -> !step.branch())
                .map(ExpenseMatchedFlowTraversalSupport.MatchedPathStep::node)
                .toList();
    }

    private ProcessFlowRouteDTO resolveMatchedRoute(
            ProcessDocumentInstance instance,
            FlowRuntimeSnapshot snapshot,
            Map<String, Object> runtimeContext,
            Map<String, String> routeHitsByBranchNode,
            ProcessFlowNodeDTO branchNode
    ) {
        String routeKey = routeHitsByBranchNode.get(branchNode.getNodeKey());
        if (hasText(routeKey)) {
            ProcessFlowRouteDTO route = snapshot.routeByKey(routeKey);
            if (route != null) {
                return route;
            }
        }
        return expenseWorkflowRuntimeSupport.previewMatchedRoute(snapshot.routes(branchNode.getNodeKey()), runtimeContext);
    }

    private ExpenseApprovalNodeStatusVO buildNodeStatus(
            ProcessDocumentInstance instance,
            ProcessFlowNodeDTO node,
            Map<String, Object> runtimeContext,
            List<ProcessDocumentTask> nodeTasks,
            List<LogEntry> nodeLogs,
            Map<Long, String> userDisplayNames
    ) {
        List<ProcessDocumentTask> tasks = nodeTasks == null ? Collections.emptyList() : nodeTasks;
        List<LogEntry> logs = nodeLogs == null ? Collections.emptyList() : nodeLogs;
        List<ProcessDocumentTask> openTasks = tasks.stream()
                .filter(item -> TASK_STATUS_PENDING.equals(item.getStatus()) || TASK_STATUS_PAUSED.equals(item.getStatus()))
                .toList();

        ExpenseApprovalNodeStatusVO status = new ExpenseApprovalNodeStatusVO();
        status.setNodeKey(node.getNodeKey());
        status.setNodeName(node.getNodeName());
        status.setNodeType(node.getNodeType());

        String nodeType = defaultText(node.getNodeType(), "");
        if (NODE_TYPE_PAYMENT.equals(nodeType)) {
            applyPaymentStatus(status, node, runtimeContext, openTasks, logs, userDisplayNames);
        } else if (NODE_TYPE_CC.equals(nodeType)) {
            applyCcStatus(instance, status, node, runtimeContext, logs, userDisplayNames);
        } else {
            applyApprovalStatus(instance, status, node, runtimeContext, openTasks, logs, userDisplayNames);
        }
        return status;
    }

    private void applyApprovalStatus(
            ProcessDocumentInstance instance,
            ExpenseApprovalNodeStatusVO status,
            ProcessFlowNodeDTO node,
            Map<String, Object> runtimeContext,
            List<ProcessDocumentTask> openTasks,
            List<LogEntry> logs,
            Map<Long, String> userDisplayNames
    ) {
        if (!openTasks.isEmpty()) {
            status.setStatus(STATUS_PENDING);
            status.setStatusLabel("审批中");
            status.setAssigneeNames(distinctNonBlank(openTasks.stream()
                    .map(item -> resolveTaskAssigneeDisplayName(item, userDisplayNames))
                    .toList()));
            status.setOccurredAt(formatTime(openTasks.get(0).getCreatedAt()));
            status.setDescription(joinNames("当前处理人", status.getAssigneeNames()));
            return;
        }
        if (Objects.equals(trimToNull(instance.getCurrentTaskType()), "MANUAL_SELECT")
                && Objects.equals(trimToNull(instance.getCurrentNodeKey()), trimToNull(node.getNodeKey()))) {
            status.setStatus(STATUS_MANUAL_SELECTION_PENDING);
            status.setStatusLabel("待手动选择审批人");
            status.setDescription("等待提单人指定当前节点审批人");
            return;
        }

        LogEntry rejectLog = lastLog(logs, LOG_REJECT);
        if (rejectLog != null) {
            status.setStatus(STATUS_REJECTED);
            status.setStatusLabel("已驳回");
            status.setAssigneeNames(distinctNonBlank(List.of(resolveActorDisplayName(rejectLog.log(), userDisplayNames))));
            status.setOccurredAt(formatTime(rejectLog.log().getCreatedAt()));
            status.setDescription(joinCommentOrNames(rejectLog.log().getActionComment(), "处理人", status.getAssigneeNames()));
            return;
        }

        LogEntry autoSkipLog = lastLog(logs, LOG_AUTO_SKIP);
        if (autoSkipLog != null) {
            status.setStatus(STATUS_AUTO_SKIPPED);
            status.setStatusLabel("已自动跳过");
            status.setOccurredAt(formatTime(autoSkipLog.log().getCreatedAt()));
            status.setDescription(trimToNull(autoSkipLog.log().getActionComment()));
            return;
        }

        LogEntry approveLog = lastLog(logs, LOG_APPROVE);
        if (approveLog != null) {
            status.setStatus(STATUS_APPROVED);
            status.setStatusLabel("已通过");
            status.setAssigneeNames(distinctNonBlank(logsOfType(logs, LOG_APPROVE).stream()
                    .map(item -> resolveActorDisplayName(item.log(), userDisplayNames))
                    .toList()));
            status.setOccurredAt(formatTime(approveLog.log().getCreatedAt()));
            status.setDescription(joinCommentOrNames(trimToNull(approveLog.log().getActionComment()), "处理人", status.getAssigneeNames()));
            return;
        }

        LogEntry exceptionLog = lastLog(logs, LOG_EXCEPTION);
        if (exceptionLog != null) {
            status.setStatus(STATUS_EXCEPTION);
            status.setStatusLabel("异常");
            status.setOccurredAt(formatTime(exceptionLog.log().getCreatedAt()));
            status.setDescription(trimToNull(exceptionLog.log().getActionComment()));
            return;
        }

        status.setStatus(STATUS_NOT_REACHED);
        status.setStatusLabel("未到达");
        status.setAssigneeNames(resolveFutureApprovers(node, runtimeContext));
        status.setDescription(joinNames("预计处理人", status.getAssigneeNames()));
    }

    private void applyCcStatus(
            ProcessDocumentInstance instance,
            ExpenseApprovalNodeStatusVO status,
            ProcessFlowNodeDTO node,
            Map<String, Object> runtimeContext,
            List<LogEntry> logs,
            Map<Long, String> userDisplayNames
    ) {
        LogEntry ccReachedLog = lastLog(logs, LOG_CC_REACHED);
        if (ccReachedLog != null) {
            status.setStatus(STATUS_APPROVED);
            status.setStatusLabel("已抄送");
            status.setAssigneeNames(readStringList(ccReachedLog.payload().get("receiverNames")));
            status.setOccurredAt(formatTime(ccReachedLog.log().getCreatedAt()));
            status.setDescription(joinNames("抄送对象", status.getAssigneeNames()));
            return;
        }

        LogEntry autoSkipLog = lastLog(logs, LOG_AUTO_SKIP);
        if (autoSkipLog != null) {
            status.setStatus(STATUS_AUTO_SKIPPED);
            status.setStatusLabel("已自动跳过");
            status.setOccurredAt(formatTime(autoSkipLog.log().getCreatedAt()));
            status.setDescription(trimToNull(autoSkipLog.log().getActionComment()));
            return;
        }

        LogEntry exceptionLog = lastLog(logs, LOG_EXCEPTION);
        if (exceptionLog != null) {
            status.setStatus(STATUS_EXCEPTION);
            status.setStatusLabel("异常");
            status.setOccurredAt(formatTime(exceptionLog.log().getCreatedAt()));
            status.setDescription(trimToNull(exceptionLog.log().getActionComment()));
            return;
        }

        status.setStatus(STATUS_NOT_REACHED);
        status.setStatusLabel("未到达");
        status.setAssigneeNames(distinctNonBlank(expenseWorkflowRuntimeSupport.previewResolvedCcRecipients(instance, node, runtimeContext).stream()
                .map(this::normalizeUserDisplayName)
                .toList()));
        status.setDescription(joinNames("预计抄送", status.getAssigneeNames()));
    }

    private void applyPaymentStatus(
            ExpenseApprovalNodeStatusVO status,
            ProcessFlowNodeDTO node,
            Map<String, Object> runtimeContext,
            List<ProcessDocumentTask> openTasks,
            List<LogEntry> logs,
            Map<Long, String> userDisplayNames
    ) {
        if (!openTasks.isEmpty()) {
            status.setStatus(STATUS_PAYMENT_PENDING);
            status.setStatusLabel("待支付");
            status.setAssigneeNames(distinctNonBlank(openTasks.stream()
                    .map(item -> resolveTaskAssigneeDisplayName(item, userDisplayNames))
                    .toList()));
            status.setOccurredAt(formatTime(openTasks.get(0).getCreatedAt()));
            status.setDescription(joinNames("当前处理人", status.getAssigneeNames()));
            return;
        }

        LogEntry paymentExceptionLog = lastLog(logs, LOG_PAYMENT_EXCEPTION);
        if (paymentExceptionLog != null) {
            status.setStatus(STATUS_PAYMENT_EXCEPTION);
            status.setStatusLabel("支付异常");
            status.setAssigneeNames(distinctNonBlank(List.of(resolveActorDisplayName(paymentExceptionLog.log(), userDisplayNames))));
            status.setOccurredAt(formatTime(paymentExceptionLog.log().getCreatedAt()));
            status.setDescription(joinCommentOrNames(paymentExceptionLog.log().getActionComment(), "处理人", status.getAssigneeNames()));
            return;
        }

        LogEntry paymentCompleteLog = lastLog(logs, LOG_PAYMENT_COMPLETE);
        if (paymentCompleteLog != null) {
            status.setStatus(STATUS_PAYMENT_COMPLETED);
            status.setStatusLabel("已支付");
            status.setAssigneeNames(distinctNonBlank(List.of(resolveActorDisplayName(paymentCompleteLog.log(), userDisplayNames))));
            status.setOccurredAt(formatTime(paymentCompleteLog.log().getCreatedAt()));
            status.setDescription(joinCommentOrNames(paymentCompleteLog.log().getActionComment(), "处理人", status.getAssigneeNames()));
            return;
        }

        LogEntry exceptionLog = lastLog(logs, LOG_EXCEPTION);
        if (exceptionLog != null) {
            status.setStatus(STATUS_EXCEPTION);
            status.setStatusLabel("异常");
            status.setOccurredAt(formatTime(exceptionLog.log().getCreatedAt()));
            status.setDescription(trimToNull(exceptionLog.log().getActionComment()));
            return;
        }

        status.setStatus(STATUS_NOT_REACHED);
        status.setStatusLabel("未到达");
        status.setAssigneeNames(distinctNonBlank(expenseWorkflowRuntimeSupport.previewResolvedPaymentExecutors(node, runtimeContext).stream()
                .map(this::normalizeUserDisplayName)
                .toList()));
        status.setDescription(joinNames("预计处理人", status.getAssigneeNames()));
    }

    private List<String> resolveFutureApprovers(ProcessFlowNodeDTO node, Map<String, Object> runtimeContext) {
        return distinctNonBlank(expenseWorkflowRuntimeSupport.previewResolvedApprovers(node, runtimeContext).stream()
                .map(this::normalizeUserDisplayName)
                .toList());
    }

    private List<ExpenseApprovalTimelineItemVO> buildTimeline(
            ProcessDocumentInstance instance,
            List<LogEntry> logEntries,
            List<ExpenseApprovalNodeStatusVO> nodeStatuses,
            Map<Long, String> userDisplayNames
    ) {
        List<ExpenseApprovalTimelineItemVO> items = new ArrayList<>();
        for (LogEntry entry : logEntries) {
            if (!shouldDisplayTimelineLog(entry.log().getActionType())) {
                continue;
            }
            items.add(toTimelineItem(instance, entry, userDisplayNames));
        }
        for (ExpenseApprovalNodeStatusVO nodeStatus : nodeStatuses) {
            if (STATUS_PENDING.equals(nodeStatus.getStatus())
                    || STATUS_PAYMENT_PENDING.equals(nodeStatus.getStatus())
                    || STATUS_MANUAL_SELECTION_PENDING.equals(nodeStatus.getStatus())) {
                items.add(buildPendingTimelineItem(nodeStatus));
            } else if (STATUS_NOT_REACHED.equals(nodeStatus.getStatus())) {
                items.add(buildFutureTimelineItem(nodeStatus));
            }
        }
        return items;
    }

    private ExpenseApprovalTimelineItemVO toTimelineItem(
            ProcessDocumentInstance instance,
            LogEntry entry,
            Map<Long, String> userDisplayNames
    ) {
        ProcessDocumentActionLog log = entry.log();
        ExpenseApprovalTimelineItemVO item = new ExpenseApprovalTimelineItemVO();
        item.setKey("log-" + defaultText(String.valueOf(log.getId()), String.valueOf(item.hashCode())));
        item.setNodeKey(log.getNodeKey());
        item.setNodeName(log.getNodeName());
        item.setStatus(resolveTimelineStatus(log.getActionType()));
        item.setStatusLabel(resolveTimelineStatusLabel(log.getActionType()));
        item.setTitle(resolveTimelineTitle(instance, entry, userDisplayNames));
        item.setDescription(resolveTimelineDescription(entry, userDisplayNames));
        item.setTimestamp(formatTime(log.getCreatedAt()));
        item.setAttachmentNames(readStringList(entry.payload().get("attachmentFileNames")));
        return item;
    }

    private ExpenseApprovalTimelineItemVO buildPendingTimelineItem(ExpenseApprovalNodeStatusVO nodeStatus) {
        ExpenseApprovalTimelineItemVO item = new ExpenseApprovalTimelineItemVO();
        item.setKey("pending-" + defaultText(nodeStatus.getNodeKey(), String.valueOf(item.hashCode())));
        item.setNodeKey(nodeStatus.getNodeKey());
        item.setNodeName(nodeStatus.getNodeName());
        item.setNodeType(nodeStatus.getNodeType());
        item.setStatus(nodeStatus.getStatus());
        item.setStatusLabel(nodeStatus.getStatusLabel());
        if (STATUS_MANUAL_SELECTION_PENDING.equals(nodeStatus.getStatus())) {
            item.setTitle(defaultText(nodeStatus.getNodeName(), "审批节点") + " 待手动选择审批人");
            item.setDescription(trimToNull(nodeStatus.getDescription()));
        } else {
            item.setTitle(NODE_TYPE_PAYMENT.equals(nodeStatus.getNodeType())
                    ? defaultText(nodeStatus.getNodeName(), "支付节点") + " 待支付"
                    : defaultText(nodeStatus.getNodeName(), "审批节点") + " 审批中");
            item.setDescription(joinNames("当前处理人", nodeStatus.getAssigneeNames()));
        }
        item.setTimestamp(defaultText(nodeStatus.getOccurredAt(), ""));
        item.setPending(true);
        return item;
    }

    private ExpenseApprovalTimelineItemVO buildFutureTimelineItem(ExpenseApprovalNodeStatusVO nodeStatus) {
        ExpenseApprovalTimelineItemVO item = new ExpenseApprovalTimelineItemVO();
        item.setKey("future-" + defaultText(nodeStatus.getNodeKey(), String.valueOf(item.hashCode())));
        item.setNodeKey(nodeStatus.getNodeKey());
        item.setNodeName(nodeStatus.getNodeName());
        item.setNodeType(nodeStatus.getNodeType());
        item.setStatus(nodeStatus.getStatus());
        item.setStatusLabel(nodeStatus.getStatusLabel());
        item.setTitle(defaultText(nodeStatus.getNodeName(), "流程节点") + " 未到达");
        item.setDescription(trimToNull(nodeStatus.getDescription()));
        item.setTimestamp("");
        item.setFuture(true);
        return item;
    }

    private boolean shouldDisplayTimelineLog(String actionType) {
        return Set.of(
                LOG_SUBMIT,
                LOG_RECALL,
                LOG_RESUBMIT,
                LOG_APPROVE,
                LOG_REJECT,
                LOG_MODIFY,
                LOG_COMMENT,
                LOG_REMIND,
                LOG_TRANSFER,
                LOG_ADD_SIGN,
                LOG_AUTO_SKIP,
                LOG_CC_REACHED,
                LOG_PAYMENT_START,
                LOG_PAYMENT_COMPLETE,
                LOG_PAYMENT_EXCEPTION,
                LOG_FINISH,
                LOG_EXCEPTION
        ).contains(defaultText(actionType, ""));
    }

    private String resolveTimelineTitle(
            ProcessDocumentInstance instance,
            LogEntry entry,
            Map<Long, String> userDisplayNames
    ) {
        ProcessDocumentActionLog log = entry.log();
        String actorName = resolveActorDisplayName(log, userDisplayNames);
        String nodeName = defaultText(trimToNull(log.getNodeName()), "节点");
        return switch (defaultText(log.getActionType(), "")) {
            case LOG_SUBMIT -> defaultText(trimToNull(instance.getSubmitterName()), actorName) + " 提交单据";
            case LOG_RECALL -> actorName + " 召回单据";
            case LOG_RESUBMIT -> actorName + " 重新提交";
            case LOG_APPROVE -> nodeName + " " + actorName + " 审批通过";
            case LOG_REJECT -> nodeName + " " + actorName + " 审批驳回";
            case LOG_MODIFY -> actorName + " 修改单据";
            case LOG_COMMENT -> actorName + " 发表评论";
            case LOG_REMIND -> actorName + " 发起催办";
            case LOG_TRANSFER -> actorName + " 转交审批";
            case LOG_ADD_SIGN -> actorName + " 发起加签";
            case LOG_AUTO_SKIP -> nodeName + " 自动跳过";
            case LOG_CC_REACHED -> nodeName + " 已抄送";
            case LOG_PAYMENT_START -> actorName + " 发起支付";
            case LOG_PAYMENT_COMPLETE -> actorName + " 确认已支付";
            case LOG_PAYMENT_EXCEPTION -> actorName + " 标记支付异常";
            case LOG_FINISH -> "审批完成";
            case LOG_EXCEPTION -> "流程异常";
            default -> defaultText(log.getActionType(), "审批轨迹");
        };
    }

    private String resolveTimelineDescription(LogEntry entry, Map<Long, String> userDisplayNames) {
        ProcessDocumentActionLog log = entry.log();
        String actionType = defaultText(log.getActionType(), "");
        if (LOG_COMMENT.equals(actionType)) {
            return firstNonBlank(stringValue(entry.payload().get("comment")), trimToNull(log.getActionComment()));
        }
        if (LOG_TRANSFER.equals(actionType)) {
            return joinParts(
                    trimToNull(log.getActionComment()),
                    valueLabel("\u8f6c\u4ea4\u7ed9", resolvePayloadUserDisplayName(entry.payload(), "targetUserId", "targetUserName", userDisplayNames))
            );
        }
        if (LOG_ADD_SIGN.equals(actionType)) {
            return joinParts(
                    trimToNull(log.getActionComment()),
                    valueLabel("\u52a0\u7b7e\u7ed9", resolvePayloadUserDisplayName(entry.payload(), "targetUserId", "targetUserName", userDisplayNames))
            );
        }
        if (LOG_CC_REACHED.equals(actionType)) {
            return joinNames("抄送对象", readStringList(entry.payload().get("receiverNames")));
        }
        String comment = trimToNull(log.getActionComment());
        if (comment != null) {
            return comment;
        }
        return "";
    }

    private String resolveTimelineStatus(String actionType) {
        return switch (defaultText(actionType, "")) {
            case LOG_REJECT -> STATUS_REJECTED;
            case LOG_AUTO_SKIP -> STATUS_AUTO_SKIPPED;
            case LOG_EXCEPTION, LOG_PAYMENT_EXCEPTION -> STATUS_EXCEPTION;
            case LOG_PAYMENT_COMPLETE -> STATUS_PAYMENT_COMPLETED;
            default -> STATUS_APPROVED;
        };
    }

    private String resolveTimelineStatusLabel(String actionType) {
        return switch (defaultText(actionType, "")) {
            case LOG_REJECT -> "已驳回";
            case LOG_AUTO_SKIP -> "已自动跳过";
            case LOG_EXCEPTION -> "异常";
            case LOG_PAYMENT_EXCEPTION -> "支付异常";
            case LOG_PAYMENT_COMPLETE -> "已支付";
            case LOG_CC_REACHED -> "已抄送";
            default -> "已完成";
        };
    }

    private Map<String, String> buildRouteHitIndex(List<LogEntry> logEntries) {
        Map<String, String> result = new LinkedHashMap<>();
        for (LogEntry entry : logEntries) {
            if (!LOG_ROUTE_HIT.equals(entry.log().getActionType()) || !hasText(entry.log().getNodeKey())) {
                continue;
            }
            String routeKey = stringValue(entry.payload().get("routeKey"));
            if (hasText(routeKey)) {
                result.put(entry.log().getNodeKey(), routeKey);
            }
        }
        return result;
    }

    private LogEntry lastLog(List<LogEntry> logs, String actionType) {
        List<LogEntry> matched = logsOfType(logs, actionType);
        return matched.isEmpty() ? null : matched.get(matched.size() - 1);
    }

    private List<LogEntry> logsOfType(List<LogEntry> logs, String actionType) {
        if (logs == null || logs.isEmpty()) {
            return Collections.emptyList();
        }
        return logs.stream()
                .filter(item -> Objects.equals(defaultText(item.log().getActionType(), ""), actionType))
                .toList();
    }

    private LogEntry toLogEntry(ProcessDocumentActionLog log) {
        return new LogEntry(log, readMap(log.getPayloadJson()));
    }

    private String resolveActorDisplayName(ProcessDocumentActionLog log, Map<Long, String> userDisplayNames) {
        return defaultText(resolveUserDisplayName(log.getActorUserId(), log.getActorName(), userDisplayNames), "系统");
    }

    private String resolveTaskAssigneeDisplayName(ProcessDocumentTask task, Map<Long, String> userDisplayNames) {
        return resolveUserDisplayName(task.getAssigneeUserId(), task.getAssigneeName(), userDisplayNames);
    }

    private String resolvePayloadUserDisplayName(
            Map<String, Object> payload,
            String userIdKey,
            String nameKey,
            Map<Long, String> userDisplayNames
    ) {
        return resolveUserDisplayName(asLong(payload.get(userIdKey)), stringValue(payload.get(nameKey)), userDisplayNames);
    }

    private String resolveUserDisplayName(Long userId, String fallbackName, Map<Long, String> userDisplayNames) {
        if (userId != null) {
            if (userDisplayNames.containsKey(userId)) {
                return trimToNull(userDisplayNames.get(userId));
            }
            User user = userMapper.selectById(userId);
            String resolved = normalizeUserDisplayName(user);
            userDisplayNames.put(userId, resolved);
            if (resolved != null) {
                return resolved;
            }
        }
        return trimToNull(fallbackName);
    }

    private String normalizeUserDisplayName(User user) {
        if (user == null) {
            return null;
        }
        return trimToNull(firstNonBlank(user.getName(), user.getUsername()));
    }

    private Map<String, Object> readMap(String payloadJson) {
        if (!hasText(payloadJson)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }

    private boolean isBusinessNode(String nodeType) {
        return NODE_TYPE_APPROVAL.equals(nodeType) || NODE_TYPE_CC.equals(nodeType) || NODE_TYPE_PAYMENT.equals(nodeType);
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "" : value.format(TIME_FORMATTER);
    }

    private List<String> readStringList(Object raw) {
        if (!(raw instanceof Collection<?> collection) || collection.isEmpty()) {
            return Collections.emptyList();
        }
        return distinctNonBlank(collection.stream().map(this::stringValue).toList());
    }

    private List<String> distinctNonBlank(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
    }

    private String joinNames(String label, List<String> names) {
        if (names == null || names.isEmpty()) {
            return "";
        }
        return defaultText(label, "处理人") + "：" + String.join("、", names);
    }

    private String joinCommentOrNames(String comment, String label, List<String> names) {
        String normalized = trimToNull(comment);
        if (normalized != null) {
            return normalized;
        }
        return joinNames(label, names);
    }

    private String joinParts(String left, String right) {
        List<String> values = new ArrayList<>();
        if (trimToNull(left) != null) {
            values.add(trimToNull(left));
        }
        if (trimToNull(right) != null) {
            values.add(trimToNull(right));
        }
        return String.join(" / ", values);
    }

    private String valueLabel(String label, String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "";
        }
        return defaultText(label, "信息") + "：" + normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return "";
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
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
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return trimToNull(value) != null;
    }

    record ApprovalProjectionResult(
            List<ExpenseApprovalNodeStatusVO> approvalNodeStatuses,
            List<ExpenseApprovalTimelineItemVO> approvalTimeline
    ) {}

    private record LogEntry(ProcessDocumentActionLog log, Map<String, Object> payload) {}
}
