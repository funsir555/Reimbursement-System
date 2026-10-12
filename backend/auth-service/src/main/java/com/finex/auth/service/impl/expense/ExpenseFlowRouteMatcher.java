package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ProcessFlowConditionDTO;
import com.finex.auth.dto.ProcessFlowConditionGroupDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 流程分支的纯路由规则：只比较调用方提供的实际值，不读取用户或部门数据。
 */
final class ExpenseFlowRouteMatcher {

    private ExpenseFlowRouteMatcher() {
    }

    static ProcessFlowRouteDTO match(
            List<ProcessFlowRouteDTO> routes,
            Function<ProcessFlowConditionDTO, Object> actualValueResolver
    ) {
        if (routes == null || routes.isEmpty()) {
            return null;
        }
        List<ProcessFlowRouteDTO> orderedRoutes = routes.stream()
                .sorted(Comparator.comparing(item -> item.getPriority() == null ? Integer.MAX_VALUE : item.getPriority()))
                .toList();

        ProcessFlowRouteDTO matchedRoute = orderedRoutes.stream()
                .filter(route -> !Boolean.TRUE.equals(route.getDefaultRoute()))
                .filter(route -> routeMatches(route, actualValueResolver))
                .findFirst()
                .orElse(null);
        if (matchedRoute != null) {
            return matchedRoute;
        }

        return orderedRoutes.stream()
                .filter(route -> Boolean.TRUE.equals(route.getDefaultRoute()))
                .findFirst()
                .orElseGet(() -> orderedRoutes.stream()
                        .filter(route -> routeMatches(route, actualValueResolver))
                        .findFirst()
                        .orElse(null));
    }

    private static boolean routeMatches(
            ProcessFlowRouteDTO route,
            Function<ProcessFlowConditionDTO, Object> actualValueResolver
    ) {
        if (route.getConditionGroups() == null || route.getConditionGroups().isEmpty()) {
            return true;
        }
        return route.getConditionGroups().stream()
                .anyMatch(group -> groupMatches(group, actualValueResolver));
    }

    private static boolean groupMatches(
            ProcessFlowConditionGroupDTO group,
            Function<ProcessFlowConditionDTO, Object> actualValueResolver
    ) {
        if (group.getConditions() == null || group.getConditions().isEmpty()) {
            return true;
        }
        return group.getConditions().stream()
                .allMatch(condition -> conditionMatches(condition, actualValueResolver));
    }

    private static boolean conditionMatches(
            ProcessFlowConditionDTO condition,
            Function<ProcessFlowConditionDTO, Object> actualValueResolver
    ) {
        Object actual = actualValueResolver.apply(condition);
        Object compare = condition.getCompareValue();
        String operator = defaultText(condition.getOperator(), "EQ");

        return switch (operator) {
            case "NE" -> !valuesEqual(actual, compare);
            case "IN" -> anyIn(actual, compare, false);
            case "NOT_IN" -> !anyIn(actual, compare, false);
            case "GT" -> compareNumbers(actual, compare) > 0;
            case "GE" -> compareNumbers(actual, compare) >= 0;
            case "LT" -> compareNumbers(actual, compare) < 0;
            case "LE" -> compareNumbers(actual, compare) <= 0;
            case "BETWEEN" -> between(actual, compare);
            case "CONTAINS" -> containsValue(actual, compare);
            default -> valuesEqual(actual, compare);
        };
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(String.valueOf(number));
        }
        try {
            String normalized = trimToNull(String.valueOf(value));
            return normalized == null ? null : new BigDecimal(normalized);
        } catch (Exception ex) {
            return null;
        }
    }

    private static int compareNumbers(Object actual, Object compare) {
        BigDecimal left = toBigDecimal(actual);
        BigDecimal right = toBigDecimal(compare);
        if (left == null || right == null) {
            return 0;
        }
        return left.compareTo(right);
    }

    private static boolean between(Object actual, Object compare) {
        BigDecimal current = toBigDecimal(actual);
        if (current == null) {
            return false;
        }
        List<Object> range = toObjectList(compare);
        if (range.size() < 2) {
            return false;
        }
        BigDecimal start = toBigDecimal(range.get(0));
        BigDecimal end = toBigDecimal(range.get(1));
        if (start == null || end == null) {
            return false;
        }
        return current.compareTo(start) >= 0 && current.compareTo(end) <= 0;
    }

    private static boolean containsValue(Object actual, Object compare) {
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> valuesEqual(item, compare));
        }
        String actualText = trimToNull(String.valueOf(actual));
        String compareText = trimToNull(String.valueOf(compare));
        return actualText != null && compareText != null && actualText.contains(compareText);
    }

    private static boolean anyIn(Object actual, Object compare, boolean defaultResult) {
        List<Object> compareList = toObjectList(compare);
        if (compareList.isEmpty()) {
            return defaultResult;
        }
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> compareList.stream()
                    .anyMatch(candidate -> valuesEqual(item, candidate)));
        }
        return compareList.stream().anyMatch(candidate -> valuesEqual(actual, candidate));
    }

    private static boolean valuesEqual(Object actual, Object compare) {
        BigDecimal leftNumber = toBigDecimal(actual);
        BigDecimal rightNumber = toBigDecimal(compare);
        if (leftNumber != null && rightNumber != null) {
            return leftNumber.compareTo(rightNumber) == 0;
        }
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> valuesEqual(item, compare));
        }
        return Objects.equals(defaultText(asText(actual), ""), defaultText(asText(compare), ""));
    }

    private static List<Object> toObjectList(Object value) {
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (value == null) {
            return new ArrayList<>();
        }
        return List.of(value);
    }

    private static String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
