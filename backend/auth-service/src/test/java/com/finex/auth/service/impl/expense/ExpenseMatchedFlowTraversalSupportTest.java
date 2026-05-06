package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpenseMatchedFlowTraversalSupportTest {

    private final ExpenseMatchedFlowTraversalSupport support = new ExpenseMatchedFlowTraversalSupport();

    @Test
    void collectMatchedPathStopsAtReachableTailForNestedManualSelectBranch() {
        FlowRuntimeSnapshot snapshot = new FlowRuntimeSnapshot(
                List.of(
                        node("approval-start", "APPROVAL", null, 1),
                        node("branch-1", "BRANCH", null, 2),
                        node("branch-2", "BRANCH", null, 3),
                        node("cc-tail", "CC", null, 4),
                        node("payment-tail", "PAYMENT", null, 5),
                        node("approval-level-1", "APPROVAL", "route-1-a", 1),
                        node("branch-1-1", "BRANCH", "route-1-a", 2),
                        node("approval-manual", "APPROVAL", "route-1-1-a", 1),
                        node("approval-follow-up", "APPROVAL", "route-2-a", 1)
                ),
                List.of(
                        route("route-1-a", "branch-1", 1, true),
                        route("route-1-1-a", "branch-1-1", 1, true),
                        route("route-2-a", "branch-2", 1, true)
                )
        );

        List<ExpenseMatchedFlowTraversalSupport.MatchedPathStep> steps = support.collectMatchedPath(
                snapshot,
                null,
                0,
                branchNode -> switch (branchNode.getNodeKey()) {
                    case "branch-1" -> snapshot.routeByKey("route-1-a");
                    case "branch-1-1" -> snapshot.routeByKey("route-1-1-a");
                    case "branch-2" -> snapshot.routeByKey("route-2-a");
                    default -> null;
                }
        );

        assertEquals(
                List.of(
                        "approval-start",
                        "branch-1",
                        "approval-level-1",
                        "branch-1-1",
                        "approval-manual",
                        "branch-2",
                        "approval-follow-up",
                        "cc-tail",
                        "payment-tail"
                ),
                steps.stream().map(step -> step.node().getNodeKey()).toList()
        );
        assertEquals(
                List.of(false, true, false, true, false, true, false, false, false),
                steps.stream().map(ExpenseMatchedFlowTraversalSupport.MatchedPathStep::branch).toList()
        );
        assertEquals("payment-tail", steps.get(steps.size() - 1).node().getNodeKey());
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
}
