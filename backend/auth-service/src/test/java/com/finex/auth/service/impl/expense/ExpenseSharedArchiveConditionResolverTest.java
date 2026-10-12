package com.finex.auth.service.impl.expense;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExpenseSharedArchiveConditionResolverTest {

    @Test
    void declaredArchiveWithoutFormValueStillProducesEmptyConditionList() {
        Map<String, Object> context = new LinkedHashMap<>();
        Map<String, Object> schema = Map.of("blocks", List.of(
                "invalid block",
                block("SHARED_FIELD", "cost", "COST_CENTER")));

        ExpenseSharedArchiveConditionResolver.putForDesign(
                context, schema, Map.of(), null, null);

        assertEquals(List.of(), context.get("COST_CENTER"));
    }

    @Test
    void onlySharedFieldsDeclareArchiveCodesWhileMatchingPropsPreserveLegacyValueCollection() {
        Map<String, Object> context = new LinkedHashMap<>();
        Map<String, Object> schema = Map.of("blocks", List.of(
                block("SHARED_FIELD", "cost", "COST_CENTER"),
                block("OTHER", "legacyCost", "COST_CENTER"),
                block("OTHER", "ignored", "NOT_DECLARED")));

        ExpenseSharedArchiveConditionResolver.putForDesign(
                context, schema, Map.of("cost", " A ", "legacyCost", List.of("A", "B"), "ignored", "X"),
                null, null);

        assertEquals(List.of("A", "B"), context.get("COST_CENTER"));
        assertFalse(context.containsKey("NOT_DECLARED"));
    }

    private static Map<String, Object> block(String kind, String fieldKey, String archiveCode) {
        return Map.of("kind", kind, "fieldKey", fieldKey, "props", Map.of("archiveCode", archiveCode));
    }
}
