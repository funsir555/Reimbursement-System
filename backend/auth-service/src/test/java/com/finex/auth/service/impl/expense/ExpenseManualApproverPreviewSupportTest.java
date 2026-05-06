package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseManualApproverPreviewVO;
import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseManualApproverPreviewSupportTest {

    @Mock
    private AbstractExpenseDocumentSupport support;

    @Mock
    private ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;

    @Mock
    private ExpenseDocumentMutationApplySupport mutationApplySupport;

    @Test
    void buildPreviewDoesNotRenderBranchStepsInTimeline() {
        ExpenseManualApproverPreviewSupport previewSupport = new ExpenseManualApproverPreviewSupport(
                support,
                expenseWorkflowRuntimeSupport,
                mutationApplySupport
        );
        String flowSnapshotJson = "{\"nodes\":[],\"routes\":[]}";
        FlowRuntimeSnapshot snapshot = new FlowRuntimeSnapshot(
                List.of(
                        node("approval-start", "起始审批", "APPROVAL", null, 1),
                        node("branch-1", "流程分支 1", "BRANCH", null, 2),
                        node("payment-tail", "支付节点 3", "PAYMENT", null, 3),
                        node("approval-manual", "财务复核", "APPROVAL", "route-1-a", 1)
                ),
                List.of(route("route-1-a", "branch-1", 1, true))
        );
        Map<String, Object> runtimeContext = new LinkedHashMap<>();
        runtimeContext.put("manualApproverSelections", Collections.emptyMap());
        ProcessFormOptionVO candidate = option("2", "李四");
        User approver = user(11L, "审批人A");

        when(support.readFlowRuntimeSnapshot(flowSnapshotJson)).thenReturn(snapshot);
        when(support.readMap(flowSnapshotJson)).thenReturn(Map.of());
        when(support.normalizeManualApproverSelections(Collections.emptyMap())).thenReturn(Collections.emptyMap());
        when(support.loadUserOptions(Map.of())).thenReturn(List.of(candidate));
        when(expenseWorkflowRuntimeSupport.previewMatchedRoute(eq(snapshot.routes("branch-1")), eq(runtimeContext)))
                .thenReturn(snapshot.routeByKey("route-1-a"));
        when(expenseWorkflowRuntimeSupport.previewResolvedApprovers(eq(snapshot.node("approval-start")), eq(runtimeContext)))
                .thenReturn(List.of(approver));
        when(support.resolveUserDisplayName(eq(approver), eq(null))).thenReturn("审批人A");

        ExpenseManualApproverPreviewVO preview = previewSupport.buildPreview(flowSnapshotJson, runtimeContext);

        assertEquals(
                List.of("approval-start", "approval-manual", "payment-tail"),
                preview.getApprovalTimeline().stream().map(item -> item.getNodeKey()).toList()
        );
        assertFalse(preview.getApprovalTimeline().stream().anyMatch(item -> "branch-1".equals(item.getNodeKey())));
        assertEquals(List.of("approval-manual"), preview.getManualNodes().stream().map(item -> item.getNodeKey()).toList());
    }

    private ProcessFlowNodeDTO node(String nodeKey, String nodeName, String nodeType, String parentNodeKey, int displayOrder) {
        ProcessFlowNodeDTO node = new ProcessFlowNodeDTO();
        node.setNodeKey(nodeKey);
        node.setNodeName(nodeName);
        node.setNodeType(nodeType);
        node.setParentNodeKey(parentNodeKey);
        node.setDisplayOrder(displayOrder);
        if ("APPROVAL".equals(nodeType) && "approval-manual".equals(nodeKey)) {
            node.setConfig(Map.of("approverType", "MANUAL_SELECT"));
        }
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

    private ProcessFormOptionVO option(String value, String label) {
        ProcessFormOptionVO option = new ProcessFormOptionVO();
        option.setValue(value);
        option.setLabel(label);
        return option;
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
