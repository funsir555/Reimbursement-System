package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ProcessFlowConditionDTO;
import com.finex.auth.dto.ProcessFlowConditionGroupDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExpenseFlowRouteMatcherTest {

    @Test
    void firstMatchingNonDefaultRouteWinsEvenWhenDefaultHasHigherPriority() {
        ProcessFlowRouteDTO fallback = route("default", 1, true);
        ProcessFlowRouteDTO lowPriority = route("low", 20, false, condition("company", "EQ", "A"));
        ProcessFlowRouteDTO highPriority = route("high", 10, false, condition("company", "EQ", "A"));

        assertEquals("high", match(List.of(fallback, lowPriority, highPriority), Map.of("company", "A")));
        assertEquals("default", match(List.of(fallback, lowPriority, highPriority), Map.of("company", "B")));
    }

    @Test
    void groupsAreOrAndConditionsWithinGroupAreAnd() {
        ProcessFlowRouteDTO route = route("matched", 1, false);
        ProcessFlowConditionGroupDTO first = group(
                condition("company", "EQ", "A"), condition("amount", "GT", 100));
        ProcessFlowConditionGroupDTO second = group(condition("company", "EQ", "B"));
        route.setConditionGroups(List.of(first, second));

        assertEquals("matched", match(List.of(route), Map.of("company", "A", "amount", 101)));
        assertEquals("matched", match(List.of(route), Map.of("company", "B", "amount", 1)));
        assertNull(match(List.of(route), Map.of("company", "A", "amount", 99)));
    }

    @Test
    void comparisonOperatorsKeepLegacyNumberCollectionAndTextSemantics() {
        Map<String, Object> values = Map.of(
                "amount", "10.00",
                "departments", List.of(3L, 7L),
                "memo", "travel expense"
        );
        assertMatches(values, "amount", "EQ", 10, true);
        assertMatches(values, "amount", "NE", 11, true);
        assertMatches(values, "amount", "GE", 10, true);
        assertMatches(values, "amount", "GT", 10, false);
        assertMatches(values, "amount", "LT", 11, true);
        assertMatches(values, "amount", "LE", 10, true);
        assertMatches(values, "amount", "BETWEEN", List.of(9, 11), true);
        assertMatches(values, "departments", "IN", List.of("7"), true);
        assertMatches(values, "departments", "NOT_IN", List.of("7"), false);
        assertMatches(values, "departments", "CONTAINS", "3", true);
        assertMatches(values, "memo", "CONTAINS", "expense", true);
        assertMatches(values, "memo", "UNKNOWN", "travel expense", true);
    }

    @Test
    void emptyRoutesAndNullComparisonRetainLegacyBehavior() {
        assertNull(ExpenseFlowRouteMatcher.match(null, condition -> null));
        assertNull(ExpenseFlowRouteMatcher.match(List.of(), condition -> null));
        assertMatches(Map.of(), "missing", "IN", List.of(), false);
        assertMatches(Map.of(), "missing", "NOT_IN", List.of(), true);
        assertMatches(Map.of(), "missing", "GE", 1, true);
    }

    private static void assertMatches(
            Map<String, Object> values, String field, String operator, Object compare, boolean expected
    ) {
        String actual = match(List.of(route("matched", 1, false, condition(field, operator, compare))), values);
        assertEquals(expected ? "matched" : null, actual, operator + " " + field);
    }

    private static String match(List<ProcessFlowRouteDTO> routes, Map<String, Object> values) {
        ProcessFlowRouteDTO matched = ExpenseFlowRouteMatcher.match(
                routes, condition -> values.get(condition.getFieldKey()));
        return matched == null ? null : matched.getRouteKey();
    }

    private static ProcessFlowRouteDTO route(
            String key, int priority, boolean defaultRoute, ProcessFlowConditionDTO... conditions
    ) {
        ProcessFlowRouteDTO route = new ProcessFlowRouteDTO();
        route.setRouteKey(key);
        route.setPriority(priority);
        route.setDefaultRoute(defaultRoute);
        if (conditions.length > 0) {
            route.setConditionGroups(List.of(group(conditions)));
        }
        return route;
    }

    private static ProcessFlowConditionGroupDTO group(ProcessFlowConditionDTO... conditions) {
        ProcessFlowConditionGroupDTO group = new ProcessFlowConditionGroupDTO();
        group.setConditions(List.of(conditions));
        return group;
    }

    private static ProcessFlowConditionDTO condition(String field, String operator, Object compare) {
        ProcessFlowConditionDTO condition = new ProcessFlowConditionDTO();
        condition.setFieldKey(field);
        condition.setOperator(operator);
        condition.setCompareValue(compare);
        return condition;
    }
}
