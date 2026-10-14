package com.finex.auth.agent.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryTemplateVariableMapperTest {

    private final QueryTemplateVariableMapper mapper = new QueryTemplateVariableMapper(new ObjectMapper());

    @Test
    void mapsWorkbenchInputIntoWhitelistedQueryVariables() {
        QueryTemplateVariables extracted = QueryTemplateVariables.builder()
                .rawKeyword("原关键词")
                .normalizedKeyword("原关键词")
                .scope(QueryTemplateVariables.SCOPE_SUBMITTED_BY_ME)
                .status("PENDING_APPROVAL")
                .limit(10)
                .build();
        Map<String, Object> nodeConfig = Map.of(
                "variableMapping", "{\"scope\":\"{{input.scope}}\",\"keyword\":\"{{input.keyword}}\",\"startDate\":\"{{input.startDate}}\",\"endDate\":\"{{input.endDate}}\",\"limit\":\"{{input.limit}}\"}"
        );

        QueryTemplateVariables mapped = mapper.map(extracted, nodeConfig, Map.of(
                "scope", QueryTemplateVariables.SCOPE_ASSIGNED_TO_ME,
                "keyword", "远智",
                "startDate", "2026-09-01",
                "endDate", "2026-09-30",
                "limit", 5
        ));

        assertEquals("远智", mapped.getRawKeyword());
        assertEquals("远智", mapped.getNormalizedKeyword());
        assertEquals(QueryTemplateVariables.SCOPE_ASSIGNED_TO_ME, mapped.getScope());
        assertEquals("PENDING_APPROVAL", mapped.getStatus());
        assertEquals(LocalDate.of(2026, 9, 1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant(), mapped.getStartDate());
        assertEquals(LocalDate.of(2026, 10, 1).atStartOfDay(ZoneId.of("Asia/Shanghai")).minusNanos(1).toInstant(), mapped.getEndDate());
        assertEquals(5, mapped.getLimit());
    }

    @Test
    void keepsExtractorValuesForUnknownMappingTargets() {
        QueryTemplateVariables extracted = QueryTemplateVariables.builder()
                .normalizedKeyword("差旅")
                .status("APPROVED")
                .limit(10)
                .build();

        QueryTemplateVariables mapped = mapper.map(
                extracted,
                Map.of("variableMapping", "{\"dateRange\":\"{{user.dateRange}}\"}"),
                Map.of()
        );

        assertEquals("差旅", mapped.getNormalizedKeyword());
        assertEquals("APPROVED", mapped.getStatus());
        assertEquals(10, mapped.getLimit());
    }

    @Test
    void validatedFinWorkExpenseInputsOverrideTemplateMapping() {
        QueryTemplateVariables defaults = QueryTemplateVariables.builder()
                .scope(QueryTemplateVariables.SCOPE_SUBMITTED_BY_ME).limit(10).build();
        QueryTemplateVariables mapped = mapper.withNodeParameters(defaults,
                Map.of("parameters", Map.of("expense", Map.of(
                        "scope", QueryTemplateVariables.SCOPE_ASSIGNED_TO_ME,
                        "startDate", "2026-09-01", "endDate", "2026-09-30",
                        "minAmount", 100, "limit", 20))), "expense");

        assertEquals(QueryTemplateVariables.SCOPE_ASSIGNED_TO_ME, mapped.getScope());
        assertEquals(LocalDate.of(2026, 9, 1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant(),
                mapped.getStartDate());
        assertEquals(LocalDate.of(2026, 10, 1).atStartOfDay(ZoneId.of("Asia/Shanghai"))
                .minusNanos(1).toInstant(), mapped.getEndDate());
        assertEquals(100d, mapped.getMinAmount());
        assertEquals(20, mapped.getLimit());
    }

    @Test
    void legacyExpenseScopeAliasesRemainAccepted() {
        QueryTemplateVariables mapped = mapper.withNodeParameters(
                QueryTemplateVariables.builder().limit(10).build(),
                Map.of("parameters", Map.of("expense", Map.of("scope", "MY"))), "expense");
        assertEquals(QueryTemplateVariables.SCOPE_SUBMITTED_BY_ME, mapped.getScope());
    }
}
