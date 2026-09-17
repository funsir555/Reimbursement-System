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

        List<String> orderedKeys = WorkflowGraphPlan.from(workflow).requireLinearNodes().stream()
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

    private Map<String, Object> node(String key, String type) {
        return Map.of("nodeKey", key, "nodeType", type, "label", key);
    }

    private Map<String, Object> edge(String source, String target) {
        return Map.of("source", source, "target", target);
    }
}
