package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.ExpenseAttachmentOcrResultVO;
import com.finex.auth.dto.ExpenseAttachmentVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseAttachmentOcrService;
import com.finex.auth.service.ExpenseAttachmentService;
import com.finex.common.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExpenseAttachmentControllerTest {

    @Mock
    private ExpenseAttachmentService expenseAttachmentService;

    @Mock
    private ExpenseAttachmentOcrService expenseAttachmentOcrService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ExpenseAttachmentController(expenseAttachmentService, expenseAttachmentOcrService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void uploadAttachmentReturnsAttachmentMetadata() throws Exception {
        ExpenseAttachmentVO attachment = new ExpenseAttachmentVO();
        attachment.setAttachmentId("abc123def456ghi7");
        attachment.setFileName("invoice.pdf");
        attachment.setContentType("application/pdf");
        attachment.setFileSize(123L);
        attachment.setPreviewUrl("/api/auth/expenses/attachments/abc123def456ghi7/content");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                "pdf-body".getBytes()
        );

        doNothing().when(accessControlService).requireAnyPermission(
                eq(1L),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        when(expenseAttachmentService.uploadAttachment(any())).thenReturn(attachment);

        mockMvc.perform(multipart("/auth/expenses/attachments")
                        .file(file)
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.attachmentId").value("abc123def456ghi7"))
                .andExpect(jsonPath("$.data.previewUrl").value("/api/auth/expenses/attachments/abc123def456ghi7/content"));

        verify(expenseAttachmentService).uploadAttachment(any());
    }

    @Test
    void uploadAttachmentReturnsReadableMessageWhenFileSizeExceedsLimit() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                "pdf-body".getBytes()
        );

        doNothing().when(accessControlService).requireAnyPermission(
                eq(1L),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        when(expenseAttachmentService.uploadAttachment(any()))
                .thenThrow(new IllegalArgumentException("文件大小超出 50MB"));

        mockMvc.perform(multipart("/auth/expenses/attachments")
                        .file(file)
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("文件大小超出 50MB"));

        verify(expenseAttachmentService).uploadAttachment(any());
    }

    @Test
    void recognizeAttachmentReturnsStructuredOcrResult() throws Exception {
        ExpenseAttachmentOcrResultVO result = new ExpenseAttachmentOcrResultVO();
        result.setStatus("SUCCESS");
        result.setProviderCode("ALIYUN");
        result.setInvoiceCode("1234567890");

        doNothing().when(accessControlService).requireAnyPermission(
                eq(1L),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        when(expenseAttachmentOcrService.recognizeAttachment("abc123def456ghi7")).thenReturn(result);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/expenses/attachments/abc123def456ghi7/ocr")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.providerCode").value("ALIYUN"))
                .andExpect(jsonPath("$.data.invoiceCode").value("1234567890"));

        verify(expenseAttachmentOcrService).recognizeAttachment("abc123def456ghi7");
    }

    @Test
    void previewAttachmentStreamsBinaryContentWhenTokenIsValid() throws Exception {
        String token = JwtUtil.generateToken(2L, "tester");
        ByteArrayResource resource = new ByteArrayResource("image-bytes".getBytes());

        doNothing().when(accessControlService).requireAnyPermission(
                eq(2L),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        when(expenseAttachmentService.loadAttachment("abc123def456ghi7"))
                .thenReturn(new ExpenseAttachmentService.StoredExpenseAttachment(
                        resource,
                        "invoice.png",
                        "image/png",
                        "image-bytes".getBytes().length
                ));

        mockMvc.perform(get("/auth/expenses/attachments/abc123def456ghi7/content")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes("image-bytes".getBytes()));

        verify(expenseAttachmentService).loadAttachment("abc123def456ghi7");
    }

    @Test
    void previewAttachmentRejectsInvalidToken() throws Exception {
        mockMvc.perform(get("/auth/expenses/attachments/abc123def456ghi7/content")
                        .param("token", "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verify(expenseAttachmentService, never()).loadAttachment(any());
    }
}
