package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

final class FlowRuntimeSnapshot {
    private static final String ROOT_CONTAINER_KEY = "__ROOT__";

    private final List<ProcessFlowNodeDTO> nodes;
    private final Map<String, ProcessFlowNodeDTO> nodeByKey;
    private final Map<String, List<ProcessFlowNodeDTO>> childrenByContainer;
    private final Map<String, List<ProcessFlowRouteDTO>> routesBySourceNode;
    private final Map<String, ProcessFlowRouteDTO> routeByKey;

    FlowRuntimeSnapshot(List<ProcessFlowNodeDTO> rawNodes, List<ProcessFlowRouteDTO> rawRoutes) {
        this.nodes = rawNodes == null ? Collections.emptyList() : rawNodes.stream()
                .sorted(Comparator.comparing(item -> item.getDisplayOrder() == null ? Integer.MAX_VALUE : item.getDisplayOrder()))
                .toList();
        this.nodeByKey = this.nodes.stream().collect(Collectors.toMap(
                ProcessFlowNodeDTO::getNodeKey,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        this.childrenByContainer = this.nodes.stream().collect(Collectors.groupingBy(
                item -> normalizeContainerKey(item.getParentNodeKey()),
                LinkedHashMap::new,
                Collectors.collectingAndThen(Collectors.toList(), items -> items.stream()
                        .sorted(Comparator.comparing(node -> node.getDisplayOrder() == null ? Integer.MAX_VALUE : node.getDisplayOrder()))
                        .toList())
        ));
        List<ProcessFlowRouteDTO> routes = rawRoutes == null ? Collections.emptyList() : rawRoutes;
        this.routesBySourceNode = routes.stream().collect(Collectors.groupingBy(
                ProcessFlowRouteDTO::getSourceNodeKey,
                LinkedHashMap::new,
                Collectors.collectingAndThen(Collectors.toList(), items -> items.stream()
                        .sorted(Comparator.comparing(route -> route.getPriority() == null ? Integer.MAX_VALUE : route.getPriority()))
                        .toList())
        ));
        this.routeByKey = routes.stream().collect(Collectors.toMap(
                ProcessFlowRouteDTO::getRouteKey,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    List<ProcessFlowNodeDTO> nodes() {
        return nodes;
    }

    ProcessFlowNodeDTO node(String nodeKey) {
        return nodeByKey.get(nodeKey);
    }

    List<ProcessFlowNodeDTO> children(String containerKey) {
        return childrenByContainer.getOrDefault(normalizeContainerKey(containerKey), Collections.emptyList());
    }

    List<ProcessFlowRouteDTO> routes(String sourceNodeKey) {
        return routesBySourceNode.getOrDefault(sourceNodeKey, Collections.emptyList());
    }

    ProcessFlowRouteDTO routeByKey(String routeKey) {
        if (routeKey == null) {
            return null;
        }
        return routeByKey.get(routeKey);
    }

    int indexInContainer(String containerKey, String nodeKey) {
        List<ProcessFlowNodeDTO> children = children(containerKey);
        for (int index = 0; index < children.size(); index++) {
            if (Objects.equals(children.get(index).getNodeKey(), nodeKey)) {
                return index;
            }
        }
        return children.size();
    }

    static String normalizeContainerKey(String containerKey) {
        return containerKey == null || containerKey.trim().isEmpty() ? ROOT_CONTAINER_KEY : containerKey.trim();
    }
}
