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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and validates the workflow graph saved by the Agent canvas.
 */
public final class WorkflowGraphPlan {

    private static final Pattern STEP_REFERENCE = Pattern.compile("\\{\\{steps\\.([A-Za-z0-9_-]+)\\.record\\.[A-Za-z0-9_]+}}");

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

    /** The only supported controlled-query shapes are a chain or one fan-out into the LLM. */
    public ControlledQueryPlan requireControlledQueryPlan() {
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        orderedNodes.forEach(node -> byKey.put(text(node.get("nodeKey")), node));
        List<Map<String, Object>> starts = nodesOfType("start");
        List<Map<String, Object>> llms = nodesOfType("llm");
        List<Map<String, Object>> ends = nodesOfType("end");
        long tools = nodesOfType("tool").size();
        if (starts.size() != 1 || llms.size() != 1 || ends.size() != 1 || tools < 1 || tools > 9
                || orderedNodes.size() != tools + 3)
            throw new IllegalArgumentException("受控查询流程需要一个开始、1 至 9 个工具、一个 LLM 和一个结束节点");
        String start = text(starts.get(0).get("nodeKey"));
        String llm = text(llms.get(0).get("nodeKey"));
        String end = text(ends.get(0).get("nodeKey"));
        if (incomingCount.get(start) != 0 || !outgoing.get(llm).equals(List.of(end))
                || incomingCount.get(end) != 1 || !outgoing.get(end).isEmpty())
            throw new IllegalArgumentException("受控查询流程必须在 LLM 汇合后直接结束");
        List<Map<String, Object>> prefix = new ArrayList<>();
        List<Map<String, Object>> branches = new ArrayList<>();
        String cursor = start;
        while (true) {
            List<String> next = outgoing.get(cursor);
            if (next.size() > 1) {
                if (next.size() < 2 || next.size() > 4)
                    throw new IllegalArgumentException("并行区只允许 2 至 4 个查询分支");
                Set<String> prefixKeys = new HashSet<>();
                prefix.forEach(node -> prefixKeys.add(text(node.get("nodeKey"))));
                for (String key : next) {
                    Map<String, Object> node = byKey.get(key);
                    if (!"tool".equalsIgnoreCase(text(node.get("nodeType")))
                            || incomingCount.get(key) != 1 || !outgoing.get(key).equals(List.of(llm)))
                        throw new IllegalArgumentException("每条并行分支必须恰好一个查询节点，并直接汇合到 LLM");
                    validateStepReferences(node, prefixKeys);
                    branches.add(node);
                }
                if (incomingCount.get(llm) != branches.size())
                    throw new IllegalArgumentException("并行分支必须全部汇合到同一个 LLM 节点");
                break;
            }
            if (next.size() != 1)
                throw new IllegalArgumentException("受控查询流程存在悬空节点或不支持的分支");
            String key = next.get(0);
            if (key.equals(llm)) {
                if (incomingCount.get(llm) != 1)
                    throw new IllegalArgumentException("LLM 节点存在不支持的入线");
                break;
            }
            Map<String, Object> node = byKey.get(key);
            if (!"tool".equalsIgnoreCase(text(node.get("nodeType")))
                    || incomingCount.get(key) != 1)
                throw new IllegalArgumentException("顺序阶段只允许单链路查询节点");
            Set<String> available = new HashSet<>();
            prefix.forEach(prior -> available.add(text(prior.get("nodeKey"))));
            validateStepReferences(node, available);
            prefix.add(node);
            cursor = key;
        }
        if (prefix.size() + branches.size() != tools)
            throw new IllegalArgumentException("并行区之后不得继续查询，且所有工具节点必须在有效路径上");
        branches.sort(Comparator.comparingInt(orderedNodes::indexOf));
        return new ControlledQueryPlan(List.copyOf(prefix), List.copyOf(branches), llms.get(0),
                List.copyOf(orderedNodes));
    }

    private List<Map<String, Object>> nodesOfType(String type) {
        return orderedNodes.stream().filter(node -> type.equalsIgnoreCase(text(node.get("nodeType")))).toList();
    }

    private static void validateStepReferences(Map<String, Object> node, Set<String> available) {
        Object raw = nodeConfig(node).get("variableMapping");
        if (raw == null) return;
        Matcher matcher = STEP_REFERENCE.matcher(String.valueOf(raw));
        while (matcher.find()) {
            if (!available.contains(matcher.group(1)))
                throw new IllegalArgumentException("节点 " + text(node.get("nodeKey")) + " 只能引用已完成的上游查询结果");
        }
    }

    public record ControlledQueryPlan(List<Map<String, Object>> sequential,
                                      List<Map<String, Object>> parallel,
                                      Map<String, Object> llm,
                                      List<Map<String, Object>> orderedNodes) { }

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
