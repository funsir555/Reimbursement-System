package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseApprovalNodeStatusVO;
import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseApprovalProjectionSupportTest {

    @Mock
    private ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;

    @Mock
    private UserMapper userMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildUsesRouteHitLogsAndIncludesAttachedSharedTail() throws Exception {
        ExpenseApprovalProjectionSupport support = new ExpenseApprovalProjectionSupport(expenseWorkflowRuntimeSupport, objectMapper, userMapper);
        FlowRuntimeSnapshot snapshot = new FlowRuntimeSnapshot(
                List.of(
                        node("approval-start", "APPROVAL", null, 1),
                        node("branch-1", "BRANCH", null, 2),
                        node("approval-tail", "APPROVAL", null, 3),
                        node("approval-lane-a", "APPROVAL", "route-a", 1),
                        node("approval-lane-b", "APPROVAL", "route-b", 1)
                ),
                List.of(
                        route("route-a", "branch-1", 1, true),
                        route("route-b", "branch-1", 2, false)
                )
        );
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setSubmitterName("张三");
        User tailApprover = user(300L, "尾部审批人");
        when(expenseWorkflowRuntimeSupport.previewResolvedApprovers(eq(snapshot.node("approval-tail")), eq(Map.of("amount", 18))))
                .thenReturn(List.of(tailApprover));

        ExpenseApprovalProjectionSupport.ApprovalProjectionResult result = support.build(
                instance,
                snapshot,
                Map.of("amount", 18),
                List.of(pendingTask("approval-lane-a", "经理审批", "审批人A")),
                List.of(
                        actionLog(1L, "approval-start", "发起审批", "APPROVE", "审批人Z", "同意", null),
                        actionLog(2L, "branch-1", "条件分支", "ROUTE_HIT", "SYSTEM", null, Map.of("routeKey", "route-a"))
                )
        );

        List<ExpenseApprovalNodeStatusVO> statuses = result.approvalNodeStatuses();
        assertEquals(List.of("approval-start", "approval-lane-a", "approval-tail"), statuses.stream().map(ExpenseApprovalNodeStatusVO::getNodeKey).toList());
        assertEquals(List.of("APPROVED", "PENDING", "NOT_REACHED"), statuses.stream().map(ExpenseApprovalNodeStatusVO::getStatus).toList());
        assertEquals(List.of("尾部审批人"), statuses.get(2).getAssigneeNames());
        assertFalse(statuses.stream().anyMatch(item -> "approval-lane-b".equals(item.getNodeKey())));
        assertTrue(result.approvalTimeline().stream().anyMatch(item -> item.isPending() && "approval-lane-a".equals(item.getNodeKey())));
        assertTrue(result.approvalTimeline().stream().anyMatch(item -> item.isFuture() && "approval-tail".equals(item.getNodeKey())));
    }

    @Test
    void buildPredictsFutureRouteWhenRouteHitLogMissing() {
        ExpenseApprovalProjectionSupport support = new ExpenseApprovalProjectionSupport(expenseWorkflowRuntimeSupport, objectMapper, userMapper);
        FlowRuntimeSnapshot snapshot = new FlowRuntimeSnapshot(
                List.of(
                        node("branch-1", "BRANCH", null, 1),
                        node("approval-lane-a", "APPROVAL", "route-a", 1),
                        node("approval-lane-b", "APPROVAL", "route-b", 1),
                        node("approval-tail", "APPROVAL", null, 2)
                ),
                List.of(
                        route("route-a", "branch-1", 1, true),
                        route("route-b", "branch-1", 2, false)
                )
        );
        ProcessFlowRouteDTO matchedRoute = snapshot.routeByKey("route-b");
        User futureApprover = user(100L, "未来审批人");
        when(expenseWorkflowRuntimeSupport.previewMatchedRoute(anyList(), eq(Map.of("documentType", "B")))).thenReturn(matchedRoute);
        when(expenseWorkflowRuntimeSupport.previewResolvedApprovers(eq(snapshot.node("approval-lane-b")), eq(Map.of("documentType", "B"))))
                .thenReturn(List.of(futureApprover));

        ExpenseApprovalProjectionSupport.ApprovalProjectionResult result = support.build(
                new ProcessDocumentInstance(),
                snapshot,
                Map.of("documentType", "B"),
                List.of(),
                List.of()
        );

        assertEquals(List.of("approval-lane-b"), result.approvalNodeStatuses().stream().map(ExpenseApprovalNodeStatusVO::getNodeKey).toList());
        assertEquals("NOT_REACHED", result.approvalNodeStatuses().get(0).getStatus());
        assertEquals(List.of("未来审批人"), result.approvalNodeStatuses().get(0).getAssigneeNames());
        verify(expenseWorkflowRuntimeSupport).previewMatchedRoute(anyList(), eq(Map.of("documentType", "B")));
    }

    @Test
    void buildStopsAtLastReachablePaymentTailForNestedMatchedBranches() throws Exception {
        ExpenseApprovalProjectionSupport support = new ExpenseApprovalProjectionSupport(expenseWorkflowRuntimeSupport, objectMapper, userMapper);
        FlowRuntimeSnapshot snapshot = new FlowRuntimeSnapshot(
                List.of(
                        node("approval-start", "发起审批", "APPROVAL", null, 1),
                        node("branch-1", "流程分支 1", "BRANCH", null, 2),
                        node("branch-2", "流程分支 2", "BRANCH", null, 3),
                        node("cc-tail", "抄送节点 3", "CC", null, 4),
                        node("payment-tail", "支付节点 3", "PAYMENT", null, 5),
                        node("approval-level-1", "一级审批", "APPROVAL", "route-1-a", 1),
                        node("branch-1-1", "嵌套分支 1", "BRANCH", "route-1-a", 2),
                        node("approval-level-2", "二级审批", "APPROVAL", "route-1-1-a", 1),
                        node("branch-1-1-1", "嵌套分支 2", "BRANCH", "route-1-1-a", 2),
                        node("approval-current", "当前审批节点", "APPROVAL", "route-1-1-1-a", 1),
                        node("approval-follow-up", "后续审批节点", "APPROVAL", "route-2-a", 1)
                ),
                List.of(
                        route("route-1-a", "branch-1", 1, true),
                        route("route-1-1-a", "branch-1-1", 1, true),
                        route("route-1-1-1-a", "branch-1-1-1", 1, true),
                        route("route-2-a", "branch-2", 1, true)
                )
        );
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setSubmitterName("张三");
        when(expenseWorkflowRuntimeSupport.previewMatchedRoute(eq(snapshot.routes("branch-2")), eq(Map.of("amount", 88))))
                .thenReturn(snapshot.routeByKey("route-2-a"));
        when(expenseWorkflowRuntimeSupport.previewResolvedApprovers(eq(snapshot.node("approval-follow-up")), eq(Map.of("amount", 88))))
                .thenReturn(List.of(user(410L, "后续审批人")));
        when(expenseWorkflowRuntimeSupport.previewResolvedCcRecipients(eq(instance), eq(snapshot.node("cc-tail")), eq(Map.of("amount", 88))))
                .thenReturn(List.of(user(510L, "抄送人A")));
        when(expenseWorkflowRuntimeSupport.previewResolvedPaymentExecutors(eq(snapshot.node("payment-tail")), eq(Map.of("amount", 88))))
                .thenReturn(List.of(user(610L, "出纳B")));

        ExpenseApprovalProjectionSupport.ApprovalProjectionResult result = support.build(
                instance,
                snapshot,
                Map.of("amount", 88),
                List.of(pendingTask("approval-current", "当前审批节点", "审批人C")),
                List.of(
                        actionLog(1L, "approval-start", "发起审批", "APPROVE", "审批人A", "通过", null),
                        actionLog(2L, "branch-1", "流程分支 1", "ROUTE_HIT", "SYSTEM", null, Map.of("routeKey", "route-1-a")),
                        actionLog(3L, "approval-level-1", "一级审批", "APPROVE", "审批人B", "通过", null),
                        actionLog(4L, "branch-1-1", "嵌套分支 1", "ROUTE_HIT", "SYSTEM", null, Map.of("routeKey", "route-1-1-a")),
                        actionLog(5L, "approval-level-2", "二级审批", "APPROVE", "审批人D", "通过", null),
                        actionLog(6L, "branch-1-1-1", "嵌套分支 2", "ROUTE_HIT", "SYSTEM", null, Map.of("routeKey", "route-1-1-1-a"))
                )
        );

        List<ExpenseApprovalNodeStatusVO> statuses = result.approvalNodeStatuses();
        assertEquals(
                List.of(
                        "approval-start",
                        "approval-level-1",
                        "approval-level-2",
                        "approval-current",
                        "approval-follow-up",
                        "cc-tail",
                        "payment-tail"
                ),
                statuses.stream().map(ExpenseApprovalNodeStatusVO::getNodeKey).toList()
        );
        assertEquals(
                List.of("APPROVED", "APPROVED", "APPROVED", "PENDING", "NOT_REACHED", "NOT_REACHED", "NOT_REACHED"),
                statuses.stream().map(ExpenseApprovalNodeStatusVO::getStatus).toList()
        );
        assertEquals(List.of("后续审批人"), statuses.get(4).getAssigneeNames());
        assertEquals(List.of("抄送人A"), statuses.get(5).getAssigneeNames());
        assertEquals(List.of("出纳B"), statuses.get(6).getAssigneeNames());
        assertEquals("payment-tail", statuses.get(statuses.size() - 1).getNodeKey());

        assertEquals("payment-tail", result.approvalTimeline().get(result.approvalTimeline().size() - 1).getNodeKey());
        assertTrue(result.approvalTimeline().get(result.approvalTimeline().size() - 1).isFuture());
        assertEquals("支付节点 3 未到达", result.approvalTimeline().get(result.approvalTimeline().size() - 1).getTitle());
    }

    @Test
    void buildMarksCurrentManualSelectNodeAsPendingSelection() {
        ExpenseApprovalProjectionSupport support = new ExpenseApprovalProjectionSupport(expenseWorkflowRuntimeSupport, objectMapper, userMapper);
        FlowRuntimeSnapshot snapshot = new FlowRuntimeSnapshot(
                List.of(node("approval-manual", "APPROVAL", null, 1)),
                List.of()
        );
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setCurrentNodeKey("approval-manual");
        instance.setCurrentTaskType("MANUAL_SELECT");

        ExpenseApprovalProjectionSupport.ApprovalProjectionResult result = support.build(
                instance,
                snapshot,
                Map.of(),
                List.of(),
                List.of()
        );

        assertEquals("MANUAL_SELECTION_PENDING", result.approvalNodeStatuses().get(0).getStatus());
        assertEquals("待手动选择审批人", result.approvalNodeStatuses().get(0).getStatusLabel());
        assertTrue(result.approvalTimeline().stream().anyMatch(item -> item.isPending() && "approval-manual".equals(item.getNodeKey())));
    }

    @Test
    void buildPrefersRealNameForApprovedStatusAndTimeline() throws Exception {
        ExpenseApprovalProjectionSupport support = new ExpenseApprovalProjectionSupport(expenseWorkflowRuntimeSupport, objectMapper, userMapper);
        FlowRuntimeSnapshot snapshot = new FlowRuntimeSnapshot(
                List.of(node("approval-node", "APPROVAL", null, 1)),
                List.of()
        );
        when(userMapper.selectById(10L)).thenReturn(user(10L, "Real Name", "legacy-user"));

        ProcessDocumentActionLog approveLog = actionLog(
                1L,
                "approval-node",
                "Approval Node",
                "APPROVE",
                "legacy-user",
                "ok",
                null
        );
        approveLog.setActorUserId(10L);

        ExpenseApprovalProjectionSupport.ApprovalProjectionResult result = support.build(
                new ProcessDocumentInstance(),
                snapshot,
                Map.of(),
                List.of(),
                List.of(approveLog)
        );

        assertEquals(List.of("Real Name"), result.approvalNodeStatuses().get(0).getAssigneeNames());
        assertTrue(result.approvalTimeline().stream().anyMatch(item -> item.getTitle() != null && item.getTitle().contains("Real Name")));
        assertFalse(result.approvalTimeline().stream().anyMatch(item -> item.getTitle() != null && item.getTitle().contains("legacy-user")));
    }

    @Test
    void buildFallsBackToUsernameWhenNameMissing() {
        ExpenseApprovalProjectionSupport support = new ExpenseApprovalProjectionSupport(expenseWorkflowRuntimeSupport, objectMapper, userMapper);
        FlowRuntimeSnapshot snapshot = new FlowRuntimeSnapshot(
                List.of(node("approval-node", "APPROVAL", null, 1)),
                List.of()
        );
        when(userMapper.selectById(20L)).thenReturn(user(20L, null, "legacy-user"));

        ExpenseApprovalProjectionSupport.ApprovalProjectionResult result = support.build(
                new ProcessDocumentInstance(),
                snapshot,
                Map.of(),
                List.of(pendingTask("approval-node", "Approval Node", "masked-user", 20L)),
                List.of()
        );

        assertEquals(List.of("legacy-user"), result.approvalNodeStatuses().get(0).getAssigneeNames());
    }

    private ProcessFlowNodeDTO node(String nodeKey, String nodeType, String parentNodeKey, int displayOrder) {
        return node(nodeKey, nodeKey, nodeType, parentNodeKey, displayOrder);
    }

    private ProcessFlowNodeDTO node(String nodeKey, String nodeName, String nodeType, String parentNodeKey, int displayOrder) {
        ProcessFlowNodeDTO node = new ProcessFlowNodeDTO();
        node.setNodeKey(nodeKey);
        node.setNodeType(nodeType);
        node.setNodeName(nodeName);
        node.setParentNodeKey(parentNodeKey);
        node.setDisplayOrder(displayOrder);
        return node;
    }

    private ProcessFlowRouteDTO route(String routeKey, String sourceNodeKey, int priority, boolean attachBelowNodes) {
        ProcessFlowRouteDTO route = new ProcessFlowRouteDTO();
        route.setRouteKey(routeKey);
        route.setSourceNodeKey(sourceNodeKey);
        route.setPriority(priority);
        route.setAttachBelowNodes(attachBelowNodes);
        route.setRouteName(routeKey);
        return route;
    }

    private ProcessDocumentTask pendingTask(String nodeKey, String nodeName, String assigneeName) {
        return pendingTask(nodeKey, nodeName, assigneeName, null);
    }

    private ProcessDocumentTask pendingTask(String nodeKey, String nodeName, String assigneeName, Long assigneeUserId) {
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setNodeKey(nodeKey);
        task.setNodeName(nodeName);
        task.setNodeType("APPROVAL");
        task.setAssigneeUserId(assigneeUserId);
        task.setAssigneeName(assigneeName);
        task.setStatus("PENDING");
        task.setCreatedAt(LocalDateTime.of(2026, 4, 21, 10, 0));
        return task;
    }

    private ProcessDocumentActionLog actionLog(
            Long id,
            String nodeKey,
            String nodeName,
            String actionType,
            String actorName,
            String comment,
            Map<String, Object> payload
    ) throws Exception {
        ProcessDocumentActionLog log = new ProcessDocumentActionLog();
        log.setId(id);
        log.setNodeKey(nodeKey);
        log.setNodeName(nodeName);
        log.setActionType(actionType);
        log.setActorName(actorName);
        log.setActionComment(comment);
        log.setPayloadJson(payload == null ? null : objectMapper.writeValueAsString(payload));
        log.setCreatedAt(LocalDateTime.of(2026, 4, 21, 9, 0).plusMinutes(id));
        return log;
    }

    private User user(Long id, String name) {
        return user(id, name, name);
    }

    private User user(Long id, String name, String username) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setUsername(username);
        user.setStatus(1);
        return user;
    }
}
