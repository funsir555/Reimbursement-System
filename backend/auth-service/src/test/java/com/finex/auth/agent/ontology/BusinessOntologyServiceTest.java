package com.finex.auth.agent.ontology;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.workflow.SelectedWorkflow;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class BusinessOntologyServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final BusinessOntologyService service = spy(new BusinessOntologyService(jdbc, new ObjectMapper()));

    @Test
    void modelContextContainsOnlyPublishedEffectiveSemanticsAndAuthorizedCapability() {
        var entity = item("expense_document", "ENTITY", null, "报销单", "PUBLISHED", null);
        var field = item("expense_document.status", "FIELD", "expense_document", "单据状态", "PUBLISHED", null);
        var draft = item("expense_document.secret", "FIELD", "expense_document", "内部草稿", "DRAFT", null);
        var capability = item("expense_document.query", "CAPABILITY", "expense_document", "查询报销单",
                "PUBLISHED", "expense.query_my_expenses");
        var retired = item("expense_document.old", "RULE", "expense_document", "旧规则", "RETIRED", null);
        doReturn(List.of(entity, field, draft, capability, retired)).when(service).list(null);
        SelectedWorkflow allowed = mock(SelectedWorkflow.class);
        when(allowed.getToolCodes()).thenReturn(List.of("expense.query_my_expenses"));

        var context = service.modelContext("我的报销单有哪些", List.of(allowed));

        assertThat(context).extracting(row -> row.get("name"))
                .contains("报销单", "单据状态", "查询报销单")
                .doesNotContain("内部草稿", "旧规则");
        assertThat(context.toString()).doesNotContain("toolCode", "sourceRef", "tableName");
        assertThat(service.modelContext("我的报销单有哪些", List.of()))
                .extracting(row -> row.get("name")).doesNotContain("查询报销单");
    }

    @Test
    void rejectsObviousPrivateValuesBeforeWritingDraft() {
        var draft = new BusinessOntologyService.Draft("expense_document.contact", "FIELD", "expense_document",
                "联系人", "联系邮箱为 someone@example.com", List.of(), null, null, null, null, null, null,
                "EXPENSE", "Expense 领域", LocalDate.now(), null);

        assertThatThrownBy(() -> service.saveDraft(draft, 9L))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("敏感值");
    }

    private BusinessOntologyService.Item item(String key, String kind, String entityKey,
                                              String name, String status, String toolCode) {
        return new BusinessOntologyService.Item(1L, key, kind, entityKey, name, "通用语义说明", List.of(),
                null, null, null, null, null, toolCode, "EXPENSE", "内部来源", 1,
                status, LocalDate.now().minusDays(1), null, 0L, 0L);
    }
}
