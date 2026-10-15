package com.finex.auth.agent.ontology;

import com.finex.auth.agent.workflow.SelectedWorkflow;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OntologyResultContextServiceTest {
    @Test
    @SuppressWarnings("unchecked")
    void projectsOnlyPublishedResultFieldsWithoutPhysicalIndexEvidence() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        BusinessOntologyService terms = mock(BusinessOntologyService.class);
        when(terms.modelContext(any(), any())).thenReturn(List.of());
        when(jdbc.query(any(String.class), any(RowMapper.class))).thenReturn(List.of(
                new OntologyResultContextService.ResultLink(
                        "finance.query_sequence_entries", "ccode", "分录科目编码",
                        "finance.query_account_subjects", "subject_code", "科目编码",
                        "REFERENCE", "同一账套内对应会计科目", "同一账套", 1),
                new OntologyResultContextService.ResultLink(
                        "finance.query_sequence_entries", "csup_id", "供应商编码",
                        "finance.query_vendors", "cVenCode", "供应商编码",
                        "REFERENCE", "供应商关联", "同一账套", 1)));
        OntologyResultContextService service = new OntologyResultContextService(jdbc, terms);
        SelectedWorkflow workflow = SelectedWorkflow.builder()
                .toolCode("finance.query_sequence_entries").build();

        Map<String, Object> context = service.forAnalysis("查科目", workflow, List.of(
                Map.of("toolCode", "finance.query_sequence_entries",
                        "records", List.of(Map.of("ccode", "1001", "cdigest", "摘要")))));
        List<Map<String, Object>> links = (List<Map<String, Object>>) context.get("fieldRelations");
        assertThat(links).hasSize(1);
        assertThat(links.get(0)).containsEntry("sourceField", "ccode")
                .containsEntry("targetInResult", false).containsEntry("comparableScope", false);
        assertThat(context.toString()).doesNotContain("gl_accvouch", "idx_gl_accvouch", "csup_id");

        Map<String, Object> joined = service.forAnalysis("查科目", workflow, List.of(
                Map.of("toolCode", "finance.query_sequence_entries", "records", List.of(Map.of("ccode", "1001")),
                        "queryDescriptor", Map.of("companyId", "A")),
                Map.of("toolCode", "finance.query_account_subjects", "records", List.of(Map.of("subject_code", "1001")),
                        "queryDescriptor", Map.of("companyId", "A"))));
        assertThat(((List<Map<String, Object>>) joined.get("fieldRelations")).get(0))
                .containsEntry("targetInResult", true).containsEntry("comparableScope", true);

        Map<String, Object> otherCompany = service.forAnalysis("查科目", workflow, List.of(
                Map.of("toolCode", "finance.query_sequence_entries", "records", List.of(Map.of("ccode", "1001")),
                        "queryDescriptor", Map.of("companyId", "A")),
                Map.of("toolCode", "finance.query_account_subjects", "records", List.of(Map.of("subject_code", "1001")),
                        "queryDescriptor", Map.of("companyId", "B"))));
        assertThat(((List<Map<String, Object>>) otherCompany.get("fieldRelations")).get(0))
                .containsEntry("targetInResult", true).containsEntry("comparableScope", false);
    }
}
