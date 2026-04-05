package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.ExpenseDocumentNavigationVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseDocumentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentQueryControllerTest {

    @Mock
    private ExpenseDocumentService expenseDocumentService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ExpenseDocumentQueryController(expenseDocumentService, accessControlService))
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
}
