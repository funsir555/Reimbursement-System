package com.finex.auth.agent.workflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Applies the canvas variable mapping to the canonical controlled-query variables.
 */
@Component
@RequiredArgsConstructor
public class QueryTemplateVariableMapper {

    private final ObjectMapper objectMapper;

    public record PreparedQuery(QueryTemplateVariables variables, String message) { }

    public PreparedQuery prepare(String toolCode, QueryTemplateVariables extracted,
                                 Map<String, Object> config, Map<String, Object> input,
                                 String nodeKey, String message) {
        QueryTemplateVariables variables = map(extracted, config, input);
        if (!Boolean.TRUE.equals(input.get("finworkPrepared"))) return new PreparedQuery(variables, message);
        if (ExpenseDocumentControlledQueryTool.TOOL_CODE.equals(toolCode))
            variables = withNodeParameters(variables, input, nodeKey);
        if (FinanceUnclosedCompanyTool.TOOL_CODE.equals(toolCode)
                && input.get("parameters") instanceof Map<?, ?> nodes
                && nodes.get(nodeKey) instanceof Map<?, ?> period
                && period.get("year") != null && period.get("period") != null)
            message = period.get("year") + "年" + period.get("period") + "月";
        return new PreparedQuery(variables, message);
    }

    public QueryTemplateVariables map(
            QueryTemplateVariables extracted,
            Map<String, Object> nodeConfig,
            Map<String, Object> inputPayload
    ) {
        QueryTemplateVariables source = extracted == null
                ? QueryTemplateVariables.builder().limit(10).build()
                : extracted;
        Map<String, Object> mapping = readMapping(nodeConfig == null ? null : nodeConfig.get("variableMapping"));
        if (mapping.isEmpty()) {
            return source;
        }

        Map<String, Object> context = buildContext(source, inputPayload);
        MutableVariables target = new MutableVariables(source);
        mapping.forEach((targetName, expression) -> target.apply(targetName, resolve(expression, context)));
        return target.build();
    }

    /** Applies already validated FinWork node inputs after canvas defaults and mappings. */
    public QueryTemplateVariables withNodeParameters(QueryTemplateVariables source,
                                                     Map<String, Object> inputPayload, String nodeKey) {
        if (inputPayload == null || !(inputPayload.get("parameters") instanceof Map<?, ?> all)
                || !(all.get(nodeKey) instanceof Map<?, ?> node) || node.isEmpty()) return source;
        if (!Set.of("scope", "status", "keyword", "startDate", "endDate",
                "minAmount", "maxAmount", "limit").containsAll(node.keySet()))
            throw new IllegalArgumentException("报销工具参数不在固定模板中");
        MutableVariables target = new MutableVariables(source);
        for (Map.Entry<?, ?> entry : node.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if ("scope".equals(key)) value = switch (String.valueOf(value)) {
                case "MY", "SUBMITTED_BY_ME" -> QueryTemplateVariables.SCOPE_SUBMITTED_BY_ME;
                case "ASSIGNED", "ASSIGNED_TO_ME" -> QueryTemplateVariables.SCOPE_ASSIGNED_TO_ME;
                case "VISIBLE" -> QueryTemplateVariables.SCOPE_VISIBLE;
                default -> throw new IllegalArgumentException("报销查询范围参数无效");
            };
            if ("keyword".equals(key) && String.valueOf(value).trim().length() > 200)
                throw new IllegalArgumentException("报销关键词过长");
            if ("status".equals(key) && String.valueOf(value).trim().length() > 64)
                throw new IllegalArgumentException("单据状态参数无效");
            target.apply(key, value);
        }
        return target.build();
    }

    private Map<String, Object> buildContext(QueryTemplateVariables source, Map<String, Object> inputPayload) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("user.keyword", source.getNormalizedKeyword());
        context.put("user.rawKeyword", source.getRawKeyword());
        context.put("user.normalizedKeyword", source.getNormalizedKeyword());
        context.put("user.scope", source.getScope());
        context.put("user.status", source.getStatus());
        context.put("user.startDate", source.getStartDate());
        context.put("user.endDate", source.getEndDate());
        context.put("user.minAmount", source.getMinAmount());
        context.put("user.maxAmount", source.getMaxAmount());
        context.put("user.limit", source.getLimit());
        if (inputPayload != null) {
            inputPayload.forEach((key, value) -> context.put("input." + key, value));
        }
        return context;
    }

    private Object resolve(Object expression, Map<String, Object> context) {
        if (!(expression instanceof String text)) {
            return expression;
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("{{") && trimmed.endsWith("}}") && trimmed.indexOf("{{", 2) < 0) {
            return context.get(trimmed.substring(2, trimmed.length() - 2).trim());
        }
        String rendered = trimmed;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        return rendered;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMapping(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        if (!(raw instanceof String text) || text.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(text, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("工具节点的变量映射不是合法 JSON", ex);
        }
    }

    private static final class MutableVariables {
        private String rawKeyword;
        private String normalizedKeyword;
        private String scope;
        private String status;
        private Instant startDate;
        private Instant endDate;
        private Double minAmount;
        private Double maxAmount;
        private Integer limit;

        private MutableVariables(QueryTemplateVariables source) {
            rawKeyword = source.getRawKeyword();
            normalizedKeyword = source.getNormalizedKeyword();
            scope = source.getScope();
            status = source.getStatus();
            startDate = source.getStartDate();
            endDate = source.getEndDate();
            minAmount = source.getMinAmount();
            maxAmount = source.getMaxAmount();
            limit = source.getLimit();
        }

        private void apply(String targetName, Object value) {
            if (targetName == null || value == null) {
                return;
            }
            switch (targetName.trim()) {
                case "keyword", "rawKeyword" -> {
                    rawKeyword = text(value);
                    normalizedKeyword = text(value);
                }
                case "normalizedKeyword" -> normalizedKeyword = text(value);
                case "scope" -> scope = text(value);
                case "status" -> status = text(value);
                case "startDate" -> startDate = instant(value, startDate, false);
                case "endDate" -> endDate = instant(value, endDate, true);
                case "minAmount" -> minAmount = decimal(value, minAmount);
                case "maxAmount" -> maxAmount = decimal(value, maxAmount);
                case "limit" -> limit = integer(value, limit);
                default -> {
                    // Unknown targets are ignored so mappings can evolve without widening query authority.
                }
            }
        }

        private QueryTemplateVariables build() {
            return QueryTemplateVariables.builder()
                    .rawKeyword(rawKeyword)
                    .normalizedKeyword(normalizedKeyword)
                    .scope(scope)
                    .status(status)
                    .startDate(startDate)
                    .endDate(endDate)
                    .minAmount(minAmount)
                    .maxAmount(maxAmount)
                    .limit(limit == null ? 10 : Math.min(Math.max(limit, 1), 50))
                    .build();
        }

        private static String text(Object value) {
            String result = String.valueOf(value).trim();
            return result.isEmpty() ? null : result;
        }

        private static Instant instant(Object value, Instant fallback, boolean endOfDay) {
            if (value instanceof Instant instant) {
                return instant;
            }
            try {
                return Instant.parse(String.valueOf(value));
            } catch (Exception ex) {
                try {
                    LocalDate date = LocalDate.parse(String.valueOf(value));
                    return endOfDay
                            ? date.plusDays(1).atStartOfDay(ZoneId.of("Asia/Shanghai")).minusNanos(1).toInstant()
                            : date.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
                } catch (Exception ignored) {
                    return fallback;
                }
            }
        }

        private static Double decimal(Object value, Double fallback) {
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (Exception ex) {
                return fallback;
            }
        }

        private static Integer integer(Object value, Integer fallback) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (Exception ex) {
                return fallback;
            }
        }
    }
}
