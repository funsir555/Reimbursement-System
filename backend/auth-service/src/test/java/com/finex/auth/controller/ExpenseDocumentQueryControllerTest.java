package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.ExpenseDocumentNavigationVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.impl.expense.ExpenseDocumentPrintOrientation;
import com.finex.auth.service.impl.expense.ExpenseDocumentPrintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentQueryControllerTest {

    @Mock
    private ExpenseDocumentService expenseDocumentService;

    @Mock
    private ExpenseDocumentPrintService expenseDocumentPrintService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ExpenseDocumentQueryController(expenseDocumentService, expenseDocumentPrintService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void navigationAllowsApprovalViewerAndReturnsAdjacentDocumentCodes() throws Exception {
        ExpenseDocumentNavigationVO navigation = new ExpenseDocumentNavigationVO();
        navigation.setPrevDocumentCode("DOC-001");
        navigation.setNextDocumentCode("DOC-003");

        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "expense:list:view",
                "expense:approval:view",
                "expense:documents:view"
        );
        when(accessControlService.getPermissionCodes(1L)).thenReturn(List.of("expense:approval:view"));
        when(expenseDocumentService.getDocumentNavigation(1L, "DOC-002", true)).thenReturn(navigation);

        mockMvc.perform(get("/auth/expenses/DOC-002/navigation").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.prevDocumentCode").value("DOC-001"))
                .andExpect(jsonPath("$.data.nextDocumentCode").value("DOC-003"));

        verify(accessControlService).requireAnyPermission(
                1L,
                "expense:list:view",
                "expense:approval:view",
                "expense:documents:view"
        );
        verify(expenseDocumentService).getDocumentNavigation(1L, "DOC-002", true);
    }

    @Test
    void detailReturnsStructuredErrorWhenServiceThrows() throws Exception {
        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "expense:list:view",
                "expense:approval:view",
                "expense:documents:view"
        );
        when(accessControlService.getPermissionCodes(1L)).thenReturn(List.of("expense:list:view"));
        when(expenseDocumentService.getDocumentDetail(1L, "DOC-404", false))
                .thenThrow(new IllegalStateException("Document not found"));

        mockMvc.perform(get("/auth/expenses/DOC-404").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("当前单据不存在"));

        verify(expenseDocumentService).getDocumentDetail(1L, "DOC-404", false);
    }

    @Test
    void printPdfReturnsInlinePdfResponse() throws Exception {
        byte[] pdfBytes = new byte[] {1, 2, 3, 4};

        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "expense:list:view",
                "expense:approval:view",
                "expense:documents:view"
        );
        when(accessControlService.getPermissionCodes(1L)).thenReturn(List.of("expense:documents:view"));
        when(expenseDocumentPrintService.generateSinglePdf(
                1L,
                "DOC-001",
                true,
                ExpenseDocumentPrintOrientation.LANDSCAPE
        )).thenReturn(new ExpenseDocumentPrintService.ExpensePrintPdfResult(pdfBytes, "expense-document-DOC-001.pdf"));

        mockMvc.perform(
                        get("/auth/expenses/DOC-001/print-pdf")
                                .param("orientation", "landscape")
                                .requestAttr("currentUserId", 1L)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("expense-document-DOC-001.pdf")))
                .andExpect(content().bytes(pdfBytes));

        verify(expenseDocumentPrintService).generateSinglePdf(
                1L,
                "DOC-001",
                true,
                ExpenseDocumentPrintOrientation.LANDSCAPE
        );
    }

    @Test
    void batchPrintPdfReturnsInlinePdfResponse() throws Exception {
        byte[] pdfBytes = new byte[] {9, 8, 7};

        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "expense:list:view",
                "expense:approval:view",
                "expense:documents:view",
                "expense:payment:payment_order:view"
        );
        when(accessControlService.getPermissionCodes(1L)).thenReturn(List.of("expense:payment:payment_order:view"));
        when(expenseDocumentPrintService.generateBatchPdf(
                1L,
                List.of("DOC-001", "DOC-002"),
                true,
                ExpenseDocumentPrintOrientation.PORTRAIT
        )).thenReturn(new ExpenseDocumentPrintService.ExpensePrintPdfResult(pdfBytes, "expense-documents-batch-2.pdf"));

        mockMvc.perform(
                        get("/auth/expenses/print-pdf/batch")
                                .param("documentCodes", "DOC-001,DOC-002")
                                .requestAttr("currentUserId", 1L)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("expense-documents-batch-2.pdf")))
                .andExpect(content().bytes(pdfBytes));

        verify(expenseDocumentPrintService).generateBatchPdf(
                1L,
                List.of("DOC-001", "DOC-002"),
                true,
                ExpenseDocumentPrintOrientation.PORTRAIT
        );
    }
}
