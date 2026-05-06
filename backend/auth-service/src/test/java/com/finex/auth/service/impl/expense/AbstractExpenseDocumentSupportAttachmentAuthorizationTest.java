package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.service.ExpenseAttachmentService;
import com.finex.auth.service.impl.ExpenseDetailSystemFieldSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractExpenseDocumentSupportAttachmentAuthorizationTest {

    private static final String DOCUMENT_CODE = "DOC-001";
    private static final String CURRENT_ATTACHMENT_ID = "A1234567890BCDEF";
    private static final String LEGACY_MAIN_ATTACHMENT_ID = "B1234567890CDEFG";
    private static final String LEGACY_DETAIL_ATTACHMENT_ID = "C1234567890DEFGH";
    private static final String INVOICE_ATTACHMENT_ID = "D1234567890EFGHI";
    private static final String OTHER_ATTACHMENT_ID = "Z1234567890JKLMN";

    @Mock
    private ExpenseAttachmentService expenseAttachmentService;

    private AbstractExpenseDocumentSupport support;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        support = mock(AbstractExpenseDocumentSupport.class, Answers.CALLS_REAL_METHODS);
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(support, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(support, "expenseAttachmentService", expenseAttachmentService);
    }

    @Test
    void loadDocumentAttachmentAllowsCurrentAttachmentShapeInMainForm() throws Exception {
        ProcessDocumentInstance instance = createDocument(
                schemaJson("attachmentFiles"),
                formDataJson("attachmentFiles", List.of(Map.of(
                        "attachmentId", CURRENT_ATTACHMENT_ID,
                        "fileName", "hotel.pdf"
                )))
        );
        ExpenseAttachmentService.StoredExpenseAttachment storedAttachment = storedAttachment("hotel.pdf");
        doReturn(instance).when(support).requireDocument(DOCUMENT_CODE);
        when(expenseAttachmentService.loadAttachment(CURRENT_ATTACHMENT_ID)).thenReturn(storedAttachment);

        ExpenseAttachmentService.StoredExpenseAttachment actual =
                support.loadDocumentAttachment(1L, DOCUMENT_CODE, CURRENT_ATTACHMENT_ID, false);

        assertSame(storedAttachment, actual);
        verify(expenseAttachmentService).loadAttachment(CURRENT_ATTACHMENT_ID);
    }

    @Test
    void loadDocumentAttachmentAllowsLegacyPreviewUrlShapeInMainForm() throws Exception {
        ProcessDocumentInstance instance = createDocument(
                schemaJson("attachmentFiles"),
                formDataJson("attachmentFiles", List.of(Map.of(
                        "id", "legacy-row",
                        "fileName", "legacy-main.pdf",
                        "previewUrl", "/api/auth/expenses/attachments/" + LEGACY_MAIN_ATTACHMENT_ID + "/content"
                )))
        );
        ExpenseAttachmentService.StoredExpenseAttachment storedAttachment = storedAttachment("legacy-main.pdf");
        doReturn(instance).when(support).requireDocument(DOCUMENT_CODE);
        when(expenseAttachmentService.loadAttachment(LEGACY_MAIN_ATTACHMENT_ID)).thenReturn(storedAttachment);

        ExpenseAttachmentService.StoredExpenseAttachment actual =
                support.loadDocumentAttachment(1L, DOCUMENT_CODE, LEGACY_MAIN_ATTACHMENT_ID, false);

        assertSame(storedAttachment, actual);
        verify(expenseAttachmentService).loadAttachment(LEGACY_MAIN_ATTACHMENT_ID);
    }

    @Test
    void loadDocumentAttachmentAllowsLegacyFileUrlShapeInExpenseDetail() throws Exception {
        ProcessDocumentInstance instance = createDocument(schemaJson("attachmentFiles"), "{}");
        ProcessDocumentExpenseDetail detail = new ProcessDocumentExpenseDetail();
        detail.setSchemaSnapshotJson(schemaJson("detailAttachments"));
        detail.setFormDataJson(formDataJson("detailAttachments", List.of(Map.of(
                "fileName", "legacy-detail.pdf",
                "fileUrl", "/auth/expenses/attachments/" + LEGACY_DETAIL_ATTACHMENT_ID + "/content"
        ))));
        ExpenseAttachmentService.StoredExpenseAttachment storedAttachment = storedAttachment("legacy-detail.pdf");
        doReturn(instance).when(support).requireDocument(DOCUMENT_CODE);
        doReturn(List.of(detail)).when(support).loadExpenseDetails(DOCUMENT_CODE);
        when(expenseAttachmentService.loadAttachment(LEGACY_DETAIL_ATTACHMENT_ID)).thenReturn(storedAttachment);

        ExpenseAttachmentService.StoredExpenseAttachment actual =
                support.loadDocumentAttachment(1L, DOCUMENT_CODE, LEGACY_DETAIL_ATTACHMENT_ID, false);

        assertSame(storedAttachment, actual);
        verify(expenseAttachmentService).loadAttachment(LEGACY_DETAIL_ATTACHMENT_ID);
    }

    @Test
    void loadDocumentAttachmentKeepsInvoiceAttachmentsOutOfGenericAuthorization() throws Exception {
        ProcessDocumentInstance instance = createDocument(
                schemaJson(ExpenseDetailSystemFieldSupport.FIELD_INVOICE_ATTACHMENTS),
                formDataJson(ExpenseDetailSystemFieldSupport.FIELD_INVOICE_ATTACHMENTS, List.of(Map.of(
                        "attachmentId", INVOICE_ATTACHMENT_ID,
                        "fileName", "invoice.pdf"
                )))
        );
        doReturn(instance).when(support).requireDocument(DOCUMENT_CODE);
        doReturn(List.of()).when(support).loadExpenseDetails(DOCUMENT_CODE);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> support.loadDocumentAttachment(1L, DOCUMENT_CODE, INVOICE_ATTACHMENT_ID, false)
        );

        assertEquals("当前附件不属于该单据", error.getMessage());
    }

    @Test
    void loadDocumentAttachmentRejectsAttachmentsOutsideTheDocument() throws Exception {
        ProcessDocumentInstance instance = createDocument(
                schemaJson("attachmentFiles"),
                formDataJson("attachmentFiles", List.of(Map.of(
                        "attachmentId", CURRENT_ATTACHMENT_ID,
                        "fileName", "hotel.pdf"
                )))
        );
        doReturn(instance).when(support).requireDocument(DOCUMENT_CODE);
        doReturn(List.of()).when(support).loadExpenseDetails(DOCUMENT_CODE);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> support.loadDocumentAttachment(1L, DOCUMENT_CODE, OTHER_ATTACHMENT_ID, false)
        );

        assertEquals("当前附件不属于该单据", error.getMessage());
    }

    private ProcessDocumentInstance createDocument(String schemaJson, String formDataJson) {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode(DOCUMENT_CODE);
        instance.setSubmitterUserId(1L);
        instance.setStatus("APPROVED");
        instance.setFormSchemaSnapshotJson(schemaJson);
        instance.setFormDataJson(formDataJson);
        return instance;
    }

    private String schemaJson(String fieldKey) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "layoutMode", "TWO_COLUMN",
                "blocks", List.of(Map.of(
                        "kind", "CONTROL",
                        "fieldKey", fieldKey,
                        "label", "附件",
                        "required", false,
                        "props", Map.of("controlType", "ATTACHMENT")
                ))
        ));
    }

    private String formDataJson(String fieldKey, Object value) throws Exception {
        return objectMapper.writeValueAsString(Map.of(fieldKey, value));
    }

    private ExpenseAttachmentService.StoredExpenseAttachment storedAttachment(String fileName) {
        return new ExpenseAttachmentService.StoredExpenseAttachment(null, fileName, "application/pdf", 12L);
    }
}
