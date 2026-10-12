package com.finex.auth.agent.workflow;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 受控查询模板变量。
 */
@Data
@Builder
public class QueryTemplateVariables {

    public static final String SCOPE_SUBMITTED_BY_ME = "SUBMITTED_BY_ME";
    public static final String SCOPE_ASSIGNED_TO_ME = "ASSIGNED_TO_ME";
    public static final String SCOPE_VISIBLE = "VISIBLE";

    private String rawKeyword;
    private String normalizedKeyword;
    private String scope;
    private String status;
    private Instant startDate;
    private Instant endDate;
    private Double minAmount;
    private Double maxAmount;
    private Integer limit;

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("rawKeyword", rawKeyword);
        values.put("normalizedKeyword", normalizedKeyword);
        values.put("scope", scope);
        values.put("status", status);
        values.put("startDate", startDate == null ? null : startDate.toString());
        values.put("endDate", endDate == null ? null : endDate.toString());
        values.put("minAmount", minAmount);
        values.put("maxAmount", maxAmount);
        values.put("limit", limit);
        return values;
    }
}
