package com.finex.auth.agent.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowGraphPlanTest {

    @Test
    void ordersNodesByEdgesInsteadOfSavedArrayOrder() {
        Map<String, Object> workflow = Map.of(
                "nodes", List.of(
                        node("start", "start"),
                        node("end", "end"),
                        node("tool", "tool"),
                        node("llm", "llm")
                ),
                "edges", List.of(
                        edge("start", "tool"),
                        edge("tool", "llm"),
                        edge("llm", "end")
                )
        );

        List<String> orderedKeys = WorkflowGraphPlan.from(workflow).requireControlledQueryPlan().orderedNodes().stream()
                .map(node -> String.valueOf(node.get("nodeKey")))
                .toList();

        assertEquals(List.of("start", "tool", "llm", "end"), orderedKeys);
    }

    @Test
    void rejectsDisconnectedCanvasNodes() {
        Map<String, Object> workflow = Map.of(
                "nodes", List.of(node("start", "start"), node("tool", "tool"), node("end", "end")),
                "edges", List.of(edge("start", "end"))
        );

        assertThrows(IllegalArgumentException.class, () -> WorkflowGraphPlan.from(workflow));
    }

    @Test
    void rejectsCycles() {
        Map<String, Object> workflow = Map.of(
                "nodes", List.of(node("start", "start"), node("tool", "tool"), node("end", "end")),
                "edges", List.of(edge("start", "tool"), edge("tool", "end"), edge("end", "tool"))
        );

        assertThrows(IllegalArgumentException.class, () -> WorkflowGraphPlan.from(workflow));
    }

    @Test
    void acceptsOneFanOutAndKeepsCanvasOrder() {
        Map<String, Object> workflow = Map.of(
                "nodes", List.of(node("start", "start"), node("prefix", "tool"),
                        node("third", "tool"), node("first", "tool"),
                        Map.of("nodeKey", "second", "nodeType", "tool", "config",
                                Map.of("variableMapping", "{\"companyId\":\"{{steps.prefix.record.id}}\"}")),
                        node("llm", "llm"), node("end", "end")),
                "edges", List.of(edge("start", "prefix"), edge("prefix", "first"),
                        edge("prefix", "second"), edge("prefix", "third"),
                        edge("first", "llm"), edge("second", "llm"), edge("third", "llm"),
                        edge("llm", "end")));
        WorkflowGraphPlan.ControlledQueryPlan plan = WorkflowGraphPlan.from(workflow).requireControlledQueryPlan();
        assertEquals(List.of("prefix"), plan.sequential().stream().map(node -> node.get("nodeKey")).toList());
        assertEquals(List.of("third", "first", "second"),
                plan.parallel().stream().map(node -> node.get("nodeKey")).toList());
    }

    @Test
    void acceptsPublishedThreeBranchAndNewerLinearCanvas() {
        List<Map<String, Object>> nodes = List.of(node("start", "start"), node("end", "end"),
                node("first", "tool"), node("second", "tool"), node("third", "tool"), node("llm", "llm"));
        Map<String, Object> published = Map.of("nodes", nodes, "edges", List.of(
                edge("start", "first"), edge("start", "second"), edge("start", "third"),
                edge("first", "llm"), edge("second", "llm"), edge("third", "llm"), edge("llm", "end")));
        Map<String, Object> newerDraft = Map.of("nodes", nodes, "edges", List.of(
                edge("start", "first"), edge("first", "second"), edge("second", "third"),
                edge("third", "llm"), edge("llm", "end")));

        assertEquals(3, WorkflowGraphPlan.from(published).requireControlledQueryPlan().parallel().size());
        assertEquals(3, WorkflowGraphPlan.from(newerDraft).requireControlledQueryPlan().sequential().size());
    }

    @Test
    void rejectsSiblingReferenceAndFifthBranch() {
        Map<String, Object> sibling = Map.of("nodes", List.of(node("start", "start"),
                node("a", "tool"), Map.of("nodeKey", "b", "nodeType", "tool",
                        "config", Map.of("variableMapping", "{\"id\":\"{{steps.a.record.id}}\"}")),
                node("llm", "llm"), node("end", "end")),
                "edges", List.of(edge("start", "a"), edge("start", "b"),
                        edge("a", "llm"), edge("b", "llm"), edge("llm", "end")));
        assertThrows(IllegalArgumentException.class,
                () -> WorkflowGraphPlan.from(sibling).requireControlledQueryPlan());
        List<Map<String, Object>> nodes = new java.util.ArrayList<>();
        List<Map<String, Object>> edges = new java.util.ArrayList<>();
        nodes.add(node("start", "start"));
        for (int i = 0; i < 5; i++) {
            nodes.add(node("branch" + i, "tool"));
            edges.add(edge("start", "branch" + i));
            edges.add(edge("branch" + i, "llm"));
        }
        nodes.add(node("llm", "llm"));
        nodes.add(node("end", "end"));
        edges.add(edge("llm", "end"));
        assertThrows(IllegalArgumentException.class,
                () -> WorkflowGraphPlan.from(Map.of("nodes", nodes, "edges", edges)).requireControlledQueryPlan());
    }

    @Test
    void rejectsChainedParallelBranch() {
        Map<String, Object> workflow = Map.of("nodes", List.of(node("start", "start"),
                node("a", "tool"), node("a2", "tool"), node("b", "tool"),
                node("llm", "llm"), node("end", "end")),
                "edges", List.of(edge("start", "a"), edge("start", "b"),
                        edge("a", "a2"), edge("a2", "llm"), edge("b", "llm"), edge("llm", "end")));
        assertThrows(IllegalArgumentException.class,
                () -> WorkflowGraphPlan.from(workflow).requireControlledQueryPlan());
    }

    private Map<String, Object> node(String key, String type) {
        return Map.of("nodeKey", key, "nodeType", type, "label", key);
    }

    private Map<String, Object> edge(String source, String target) {
        return Map.of("source", source, "target", target);
    }
}
