package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseApprovalNodeStatusVO;
import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.User;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildUsesRouteHitLogsAndIncludesAttachedSharedTail() throws Exception {
        ExpenseApprovalProjectionSupport support = new ExpenseApprovalProjectionSupport(expenseWorkflowRuntimeSupport, objectMapper);
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
        ExpenseApprovalProjectionSupport support = new ExpenseApprovalProjectionSupport(expenseWorkflowRuntimeSupport, objectMapper);
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

    private ProcessFlowNodeDTO node(String nodeKey, String nodeType, String parentNodeKey, int displayOrder) {
        ProcessFlowNodeDTO node = new ProcessFlowNodeDTO();
        node.setNodeKey(nodeKey);
        node.setNodeType(nodeType);
        node.setNodeName(nodeKey);
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
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setNodeKey(nodeKey);
        task.setNodeName(nodeName);
        task.setNodeType("APPROVAL");
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
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setUsername(name);
        user.setStatus(1);
        return user;
    }
}
