package com.finex.auth.agent.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ControlledToolParameterMapperTest {
    private final ControlledToolParameterMapper mapper = new ControlledToolParameterMapper(new ObjectMapper());

    @Test
    void rejectsPriorCompanyEvenWhenTargetCompanyIsMappedLater() {
        ControlledToolResult prior = ControlledToolResult.builder()
                .records(List.of(Map.of("subject_code", "1001")))
                .queryDescriptor(Map.of("toolCode", "finance.query_account_subjects", "companyId", "A"))
                .hasMore(false).build();
        Map<String, Object> mapping = new java.util.LinkedHashMap<>();
        mapping.put("accountCodeFrom", "{{steps.subject.record.subject_code}}");
        mapping.put("companyId", "{{input.companyId}}");

        assertThatThrownBy(() -> mapper.map("balance", "finance.query_balance_sheet", mapping,
                Map.of("companyId", "B"), Map.of("subject", prior)))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void mapsUniqueCodeWithinSameCompany() {
        ControlledToolResult prior = ControlledToolResult.builder()
                .records(List.of(Map.of("subject_code", "1001")))
                .queryDescriptor(Map.of("toolCode", "finance.query_account_subjects", "companyId", "A"))
                .hasMore(false).build();
        Map<String, Object> input = Map.of("parameters", Map.of("balance", Map.of("companyId", "A")));

        assertThat(mapper.map("balance", "finance.query_balance_sheet",
                Map.of("accountCodeFrom", "{{steps.subject.record.subject_code}}"),
                input, Map.of("subject", prior)))
                .containsEntry("companyId", "A").containsEntry("accountCodeFrom", "1001");
    }
}
