package com.finex.auth.support;

import com.finex.auth.dto.ProcessFlowConditionDTO;
import com.finex.auth.dto.ProcessFlowConditionGroupDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class ProcessConditionMatchSupport {

    public boolean matches(List<ProcessFlowConditionGroupDTO> groups, Function<String, Object> actualValueResolver) {
        if (groups == null || groups.isEmpty()) {
            return true;
        }
        return groups.stream().anyMatch(group -> groupMatches(group, actualValueResolver));
    }

    private boolean groupMatches(ProcessFlowConditionGroupDTO group, Function<String, Object> actualValueResolver) {
        if (group == null || group.getConditions() == null || group.getConditions().isEmpty()) {
            return true;
        }
        return group.getConditions().stream().allMatch(condition -> conditionMatches(condition, actualValueResolver));
    }

    private boolean conditionMatches(ProcessFlowConditionDTO condition, Function<String, Object> actualValueResolver) {
        if (condition == null) {
            return true;
        }
        String fieldKey = trimToNull(condition.getFieldKey());
        Object actual = fieldKey == null ? null : actualValueResolver.apply(fieldKey);
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

    private int compareNumbers(Object actual, Object compare) {
        BigDecimal left = toBigDecimal(actual);
        BigDecimal right = toBigDecimal(compare);
        if (left == null || right == null) {
            return 0;
        }
        return left.compareTo(right);
    }

    private boolean between(Object actual, Object compare) {
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

    private boolean containsValue(Object actual, Object compare) {
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> valuesEqual(item, compare));
        }
        String actualText = trimToNull(String.valueOf(actual));
        String compareText = trimToNull(String.valueOf(compare));
        return actualText != null && compareText != null && actualText.contains(compareText);
    }

    private boolean anyIn(Object actual, Object compare, boolean defaultResult) {
        List<Object> compareList = toObjectList(compare);
        if (compareList.isEmpty()) {
            return defaultResult;
        }
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> compareList.stream().anyMatch(candidate -> valuesEqual(item, candidate)));
        }
        return compareList.stream().anyMatch(candidate -> valuesEqual(actual, candidate));
    }

    private boolean valuesEqual(Object actual, Object compare) {
        BigDecimal leftNumber = toBigDecimal(actual);
        BigDecimal rightNumber = toBigDecimal(compare);
        if (leftNumber != null && rightNumber != null) {
            return leftNumber.compareTo(rightNumber) == 0;
        }
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> valuesEqual(item, compare));
        }
        return Objects.equals(defaultText(actual == null ? null : String.valueOf(actual), ""), defaultText(compare == null ? null : String.valueOf(compare), ""));
    }

    private List<Object> toObjectList(Object value) {
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (value == null) {
            return new ArrayList<>();
        }
        return List.of(value);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(String.valueOf(number));
        }
        String normalized = trimToNull(String.valueOf(value));
        if (normalized == null) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String defaultText(String value, String defaultValue) {
        String normalized = trimToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
