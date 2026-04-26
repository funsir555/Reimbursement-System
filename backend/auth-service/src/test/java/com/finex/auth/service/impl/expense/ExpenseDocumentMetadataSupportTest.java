package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessDocumentTemplate;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpenseDocumentMetadataSupportTest {

    private final ExpenseDocumentMetadataSupport support = new ExpenseDocumentMetadataSupport();

    @Test
    void buildSubmitPayloadUsesTemplateCodeAndResolvedName() {
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateCode("TPL-1");

        Map<String, Object> payload = support.buildSubmitPayload(template);

        assertEquals("TPL-1", payload.get("templateCode"));
        assertEquals("TPL-1", payload.get("templateName"));
    }

    @Test
    void resolveDocumentTitleFallsBackToTemplateUserAndDate() {
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateName("差旅报销");

        String title = support.resolveDocumentTitle(template, Map.of(), "tester");

        assertTrue(title.startsWith("差旅报销-tester-"));
    }

    @Test
    void resolveDocumentReasonFallsBackToTemplateName() {
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateName("差旅报销");

        assertEquals("差旅报销", support.resolveDocumentReason(template, Map.of()));
    }
}
