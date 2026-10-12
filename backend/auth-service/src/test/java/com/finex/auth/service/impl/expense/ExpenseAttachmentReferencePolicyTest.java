package com.finex.auth.service.impl.expense;

import com.finex.auth.service.impl.ExpenseDetailSystemFieldSupport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseAttachmentReferencePolicyTest {

    private static final String ID = "A1234567890BCDEF";

    @Test
    void acceptsNestedCurrentIdAndLegacyContentUrl() {
        Map<String, Object> schema = schema("attachments", "CONTROL", "ATTACHMENT");
        Map<String, Object> nested = Map.of("attachments", List.of(List.of(Map.of("attachmentId", " " + ID + " "))));
        assertThat(ExpenseAttachmentReferencePolicy.contains(schema, nested, ID)).isTrue();

        Map<String, Object> legacy = Map.of("attachments", List.of(Map.of(
                "previewUrl", "/API/auth/expenses/attachments/" + ID + "/content?download=1")));
        assertThat(ExpenseAttachmentReferencePolicy.contains(schema, legacy, ID)).isTrue();
    }

    @Test
    void ignoresInvoiceFieldsAndNonAttachmentControls() {
        Map<String, Object> data = Map.of("attachments", List.of(Map.of("id", ID)),
                ExpenseDetailSystemFieldSupport.FIELD_INVOICE_ATTACHMENTS, List.of(Map.of("id", ID)));
        assertThat(ExpenseAttachmentReferencePolicy.contains(schema("attachments", "TEXT", "ATTACHMENT"), data, ID)).isFalse();
        assertThat(ExpenseAttachmentReferencePolicy.contains(schema("attachments", "CONTROL", "TEXT"), data, ID)).isFalse();
        assertThat(ExpenseAttachmentReferencePolicy.contains(
                schema(ExpenseDetailSystemFieldSupport.FIELD_INVOICE_ATTACHMENTS, "CONTROL", "IMAGE"), data, ID)).isFalse();
    }

    @Test
    void rejectsMissingBlocksAndUnrelatedIds() {
        assertThat(ExpenseAttachmentReferencePolicy.contains(Map.of(), Map.of(), ID)).isFalse();
        assertThat(ExpenseAttachmentReferencePolicy.contains(
                schema("attachments", "CONTROL", "IMAGE"),
                Map.of("attachments", List.of(Map.of("id", "OTHER"))), ID)).isFalse();
    }

    private static Map<String, Object> schema(String fieldKey, String kind, String controlType) {
        return Map.of("blocks", List.of(Map.of("kind", kind, "fieldKey", fieldKey,
                "props", Map.of("controlType", controlType))));
    }
}
