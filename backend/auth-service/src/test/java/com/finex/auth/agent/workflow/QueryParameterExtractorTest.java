package com.finex.auth.agent.workflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryParameterExtractorTest {

    private final QueryParameterExtractor extractor = new QueryParameterExtractor();

    @Test
    void extractsStatusAmountAndKeyword() {
        QueryTemplateVariables variables = extractor.extract("查询本月待审批的差旅报销单，金额超过500");

        assertThat(variables.getScope()).isEqualTo(QueryTemplateVariables.SCOPE_SUBMITTED_BY_ME);
        assertThat(variables.getStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(variables.getMinAmount()).isEqualTo(500.0);
        assertThat(variables.getStartDate()).isNotNull();
        assertThat(variables.getRawKeyword()).contains("差旅");
    }

    @Test
    void extractsDocumentCodeAsKeyword() {
        QueryTemplateVariables variables = extractor.extract("帮我查询 DOC202609160001 报销单");

        assertThat(variables.getNormalizedKeyword()).isEqualTo("DOC202609160001");
    }

    @Test
    void mapsPendingWorkIntentWithoutCreatingGenericKeyword() {
        QueryTemplateVariables variables = extractor.extract("总结我当前待处理的报销事项");

        assertThat(variables.getScope()).isEqualTo(QueryTemplateVariables.SCOPE_ASSIGNED_TO_ME);
        assertThat(variables.getStatus()).isNull();
        assertThat(variables.getNormalizedKeyword()).isNull();
    }

    @Test
    void preservesBusinessEntityAsKeyword() {
        QueryTemplateVariables variables = extractor.extract("查询远智相关的报销单");

        assertThat(variables.getNormalizedKeyword()).isEqualTo("远智相关");
    }
}
