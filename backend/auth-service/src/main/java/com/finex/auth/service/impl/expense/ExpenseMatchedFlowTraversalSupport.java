package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;

import java.util.ArrayList;
import java.util.List;

final class ExpenseMatchedFlowTraversalSupport {

    private static final String NODE_TYPE_APPROVAL = "APPROVAL";
    private static final String NODE_TYPE_CC = "CC";
    private static final String NODE_TYPE_PAYMENT = "PAYMENT";
    private static final String NODE_TYPE_BRANCH = "BRANCH";

    List<MatchedPathStep> collectMatchedPath(
            FlowRuntimeSnapshot snapshot,
            String containerKey,
            int startIndex,
            BranchRouteResolver branchRouteResolver
    ) {
        List<MatchedPathStep> result = new ArrayList<>();
        if (snapshot == null || branchRouteResolver == null) {
            return result;
        }
        List<TraversalFrame> frames = new ArrayList<>();
        frames.add(new TraversalFrame(
                FlowRuntimeSnapshot.normalizeContainerKey(containerKey),
                Math.max(startIndex, 0)
        ));
        while (!frames.isEmpty()) {
            TraversalFrame frame = frames.remove(frames.size() - 1);
            List<ProcessFlowNodeDTO> children = snapshot.children(frame.containerKey());
            boolean descended = false;
            for (int index = Math.max(frame.startIndex(), 0); index < children.size(); index++) {
                ProcessFlowNodeDTO node = children.get(index);
                String nodeType = defaultText(node == null ? null : node.getNodeType(), "");
                if (NODE_TYPE_BRANCH.equals(nodeType)) {
                    ProcessFlowRouteDTO matchedRoute = branchRouteResolver.resolve(node);
                    if (matchedRoute == null) {
                        return result;
                    }
                    result.add(MatchedPathStep.branch(node, matchedRoute));
                    int resumeIndex = resolveContinuationIndex(snapshot, frame.containerKey(), index, node, matchedRoute);
                    if (resumeIndex < children.size()) {
                        frames.add(new TraversalFrame(frame.containerKey(), resumeIndex));
                    }
                    frames.add(new TraversalFrame(matchedRoute.getRouteKey(), 0));
                    descended = true;
                    break;
                }
                if (isBusinessNode(nodeType)) {
                    result.add(MatchedPathStep.businessNode(node));
                }
            }
            if (!descended) {
                continue;
            }
        }
        return result;
    }

    private int resolveContinuationIndex(
            FlowRuntimeSnapshot snapshot,
            String containerKey,
            int branchIndex,
            ProcessFlowNodeDTO branchNode,
            ProcessFlowRouteDTO matchedRoute
    ) {
        List<ProcessFlowNodeDTO> siblings = snapshot.children(containerKey);
        boolean hasAttachedRoute = snapshot.routes(branchNode.getNodeKey()).stream()
                .anyMatch(item -> Boolean.TRUE.equals(item.getAttachBelowNodes()));
        if (hasAttachedRoute && !Boolean.TRUE.equals(matchedRoute.getAttachBelowNodes())) {
            return siblings.size();
        }
        return branchIndex + 1;
    }

    private boolean isBusinessNode(String nodeType) {
        return NODE_TYPE_APPROVAL.equals(nodeType)
                || NODE_TYPE_CC.equals(nodeType)
                || NODE_TYPE_PAYMENT.equals(nodeType);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @FunctionalInterface
    interface BranchRouteResolver {
        ProcessFlowRouteDTO resolve(ProcessFlowNodeDTO branchNode);
    }

    record MatchedPathStep(ProcessFlowNodeDTO node, ProcessFlowRouteDTO matchedRoute, boolean branch) {
        static MatchedPathStep branch(ProcessFlowNodeDTO node, ProcessFlowRouteDTO matchedRoute) {
            return new MatchedPathStep(node, matchedRoute, true);
        }

        static MatchedPathStep businessNode(ProcessFlowNodeDTO node) {
            return new MatchedPathStep(node, null, false);
        }
    }

    private record TraversalFrame(String containerKey, int startIndex) {}
}
