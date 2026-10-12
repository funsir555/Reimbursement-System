package com.finex.auth.agent.workflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Whitelisted scalar mappings from caller input or one unambiguous prior record. */
@Component
@RequiredArgsConstructor
public class ControlledToolParameterMapper {
    private static final Pattern INPUT = Pattern.compile("\\{\\{input\\.([A-Za-z][A-Za-z0-9_]*)}}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRIOR = Pattern.compile("\\{\\{steps\\.([A-Za-z0-9_-]+)\\.record\\.([A-Za-z0-9_]+)}}");
    private final ObjectMapper json;

    public Map<String, Object> map(String nodeKey, String toolCode, Object rawMapping,
                                   Map<String, Object> input, Map<String, ControlledToolResult> prior) {
        ControlledQueryCatalog.Definition definition = ControlledQueryCatalog.require(toolCode);
        Set<String> allowed = definition.fields().stream().map(ControlledQueryCatalog.Field::key)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Map<String, Object> values = new LinkedHashMap<>();
        Set<String> sourceCompanies = new java.util.HashSet<>();
        Object nodeParameters = input.get("parameters");
        if (nodeParameters instanceof Map<?, ?> all && all.get(nodeKey) instanceof Map<?, ?> node) {
            node.forEach((key, value) -> values.put(String.valueOf(key), value));
        }
        Map<String, Object> mapping = read(rawMapping);
        if (!allowed.containsAll(values.keySet()) || !allowed.containsAll(mapping.keySet()))
            throw new IllegalArgumentException("工具参数不在固定模板中");
        for (Map.Entry<String, Object> entry : mapping.entrySet()) {
            Object raw = entry.getValue();
            if (!(raw instanceof String expression)) {
                values.put(entry.getKey(), raw);
                continue;
            }
            Matcher inputMatch = INPUT.matcher(expression);
            Matcher priorMatch = PRIOR.matcher(expression);
            if (inputMatch.matches()) {
                Object value = input.get(inputMatch.group(1));
                if (value != null) values.put(entry.getKey(), value);
            } else if (priorMatch.matches()) {
                ControlledToolResult source = prior.get(priorMatch.group(1));
                if (source == null || source.getRecords() == null || source.getRecords().size() != 1
                        || Boolean.TRUE.equals(source.getHasMore()))
                    throw new IllegalArgumentException("前序结果不能唯一确定，请明确选择后重试");
                String field = priorMatch.group(2);
                String sourceCode = String.valueOf(((Map<?, ?>) source.getQueryDescriptor()).get("toolCode"));
                Set<String> sourceFields = ControlledQueryCatalog.require(sourceCode).kind().equals("PEOPLE")
                        ? Set.of("id", "username")
                        : Set.of("id", "subject_code", "project_class_code", "citemcode", "citemccode",
                                "cCusCode", "cVenCode", "dept_code");
                if (!sourceFields.contains(field) || !source.getRecords().get(0).containsKey(field))
                    throw new IllegalArgumentException("前序字段不可用于参数映射");
                Object value = source.getRecords().get(0).get(field);
                if (value == null) throw new IllegalArgumentException("前序映射字段为空");
                Object sourceCompany = ((Map<?, ?>) source.getQueryDescriptor()).get("companyId");
                if (sourceCompany != null) sourceCompanies.add(String.valueOf(sourceCompany));
                values.put(entry.getKey(), value);
            } else if (expression.contains("{{") || expression.contains("}}")) {
                throw new IllegalArgumentException("不支持的变量映射表达式");
            } else {
                values.put(entry.getKey(), expression);
            }
        }
        values.values().removeIf(java.util.Objects::isNull);
        Object targetCompany = values.get("companyId");
        if (!sourceCompanies.isEmpty() && (targetCompany == null
                || sourceCompanies.stream().anyMatch(company -> !company.equals(String.valueOf(targetCompany)))))
            throw new SecurityException("前序数据与目标账套不一致");
        for (Object value : values.values()) {
            if (!(value instanceof String || value instanceof Number || value instanceof Boolean)
                    || String.valueOf(value).length() > 256)
                throw new IllegalArgumentException("工具参数必须是有效的标量值");
        }
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private Map<String, Object> read(Object raw) {
        if (raw == null) return Map.of();
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        if (raw instanceof String text && text.isBlank()) return Map.of();
        try { return json.readValue(String.valueOf(raw), new TypeReference<LinkedHashMap<String, Object>>() { }); }
        catch (Exception exception) { throw new IllegalArgumentException("变量映射不是合法 JSON", exception); }
    }
}
