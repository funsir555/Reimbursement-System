package com.finex.auth.agent.workflow;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Parses and validates the workflow graph saved by the Agent canvas.
 */
public final class WorkflowGraphPlan {

    private final List<Map<String, Object>> orderedNodes;
    private final Map<String, List<String>> outgoing;
    private final Map<String, Integer> incomingCount;

    private WorkflowGraphPlan(
            List<Map<String, Object>> orderedNodes,
            Map<String, List<String>> outgoing,
            Map<String, Integer> incomingCount
    ) {
        this.orderedNodes = List.copyOf(orderedNodes);
        this.outgoing = outgoing;
        this.incomingCount = incomingCount;
    }

    public static WorkflowGraphPlan from(Map<String, Object> workflow) {
        List<Map<String, Object>> nodes = readList(workflow == null ? null : workflow.get("nodes"));
        List<Map<String, Object>> edges = readList(workflow == null ? null : workflow.get("edges"));
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("工作流至少需要一个节点");
        }

        Map<String, Map<String, Object>> nodeByKey = new LinkedHashMap<>();
        Map<String, Integer> nodeOrder = new HashMap<>();
        String startKey = null;
        int startCount = 0;
        int endCount = 0;
        for (int index = 0; index < nodes.size(); index++) {
            Map<String, Object> node = nodes.get(index);
            String nodeKey = text(node.get("nodeKey"));
            String nodeType = text(node.get("nodeType")).toLowerCase();
            if (nodeKey.isBlank()) {
                throw new IllegalArgumentException("工作流节点缺少 nodeKey");
            }
            if (nodeType.isBlank()) {
                throw new IllegalArgumentException("节点 " + nodeKey + " 缺少 nodeType");
            }
            if (nodeByKey.putIfAbsent(nodeKey, node) != null) {
                throw new IllegalArgumentException("工作流存在重复节点: " + nodeKey);
            }
            nodeOrder.put(nodeKey, index);
            if ("start".equals(nodeType)) {
                startCount++;
                startKey = nodeKey;
            }
            if ("end".equals(nodeType)) {
                endCount++;
            }
        }
        if (startCount != 1) {
            throw new IllegalArgumentException("工作流必须有且只有一个开始节点");
        }
        if (endCount == 0) {
            throw new IllegalArgumentException("工作流至少需要一个结束节点");
        }

        Map<String, List<String>> outgoing = new LinkedHashMap<>();
        Map<String, Integer> incoming = new LinkedHashMap<>();
        nodeByKey.keySet().forEach(key -> {
            outgoing.put(key, new ArrayList<>());
            incoming.put(key, 0);
        });

        Set<String> uniqueEdges = new HashSet<>();
        for (Map<String, Object> edge : edges) {
            String source = text(edge.get("source"));
            String target = text(edge.get("target"));
            if (!nodeByKey.containsKey(source) || !nodeByKey.containsKey(target)) {
                throw new IllegalArgumentException("工作流连线存在无效节点引用: " + source + " -> " + target);
            }
            if (source.equals(target)) {
                throw new IllegalArgumentException("工作流不允许节点连接自身: " + source);
            }
            if (uniqueEdges.add(source + "\u0000" + target)) {
                outgoing.get(source).add(target);
                incoming.put(target, incoming.get(target) + 1);
            }
        }
        if (incoming.get(startKey) != 0) {
            throw new IllegalArgumentException("开始节点不能有入线");
        }

        ensureReachable(startKey, outgoing, nodeByKey.size());
        List<Map<String, Object>> ordered = topologicalSort(nodeByKey, outgoing, incoming, nodeOrder);
        return new WorkflowGraphPlan(ordered, outgoing, incoming);
    }

    public List<Map<String, Object>> orderedNodes() {
        return orderedNodes;
    }

    public List<Map<String, Object>> requireLinearNodes() {
        for (Map<String, Object> node : orderedNodes) {
            String nodeKey = text(node.get("nodeKey"));
            String nodeType = text(node.get("nodeType")).toLowerCase();
            int incoming = incomingCount.getOrDefault(nodeKey, 0);
            int outgoingCount = outgoing.getOrDefault(nodeKey, List.of()).size();
            if ("start".equals(nodeType)) {
                if (outgoingCount != 1) {
                    throw new IllegalArgumentException("开始节点必须且只能连接一个后续节点");
                }
            } else if ("end".equals(nodeType)) {
                if (incoming != 1 || outgoingCount != 0) {
                    throw new IllegalArgumentException("结束节点必须只有一条入线且不能有出线");
                }
            } else if (incoming != 1 || outgoingCount != 1) {
                throw new IllegalArgumentException("当前受控查询流程只支持单链路，节点 " + nodeKey + " 必须各有一条入线和出线");
            }
        }
        long endCount = orderedNodes.stream()
                .filter(node -> "end".equalsIgnoreCase(text(node.get("nodeType"))))
                .count();
        if (endCount != 1) {
            throw new IllegalArgumentException("当前受控查询流程必须有且只有一个结束节点");
        }
        return orderedNodes;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> nodeConfig(Map<String, Object> node) {
        Object value = node == null ? null : node.get("config");
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return new LinkedHashMap<>();
    }

    private static void ensureReachable(String startKey, Map<String, List<String>> outgoing, int nodeCount) {
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(startKey);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            queue.addAll(outgoing.getOrDefault(current, List.of()));
        }
        if (visited.size() != nodeCount) {
            throw new IllegalArgumentException("工作流存在未从开始节点连通的节点");
        }
    }

    private static List<Map<String, Object>> topologicalSort(
            Map<String, Map<String, Object>> nodeByKey,
            Map<String, List<String>> outgoing,
            Map<String, Integer> incoming,
            Map<String, Integer> nodeOrder
    ) {
        Map<String, Integer> remainingIncoming = new HashMap<>(incoming);
        PriorityQueue<String> ready = new PriorityQueue<>(Comparator.comparingInt(nodeOrder::get));
        remainingIncoming.forEach((key, count) -> {
            if (count == 0) {
                ready.add(key);
            }
        });

        List<Map<String, Object>> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            String key = ready.remove();
            ordered.add(nodeByKey.get(key));
            for (String target : outgoing.getOrDefault(key, List.of())) {
                int nextCount = remainingIncoming.computeIfPresent(target, (ignored, count) -> count - 1);
                if (nextCount == 0) {
                    ready.add(target);
                }
            }
        }
        if (ordered.size() != nodeByKey.size()) {
            throw new IllegalArgumentException("工作流存在循环连线");
        }
        return ordered;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> readList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> copy = new LinkedHashMap<>();
                map.forEach((key, mapValue) -> copy.put(String.valueOf(key), mapValue));
                result.add(copy);
            }
        }
        return result;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
