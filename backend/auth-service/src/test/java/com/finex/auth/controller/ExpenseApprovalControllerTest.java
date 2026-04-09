package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExpenseApprovalControllerTest {

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
                .standaloneSetup(new ExpenseApprovalController(expenseDocumentService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void pendingRequiresViewPermissionAndReturnsItems() throws Exception {
        ExpenseApprovalPendingItemVO item = new ExpenseApprovalPendingItemVO();
        item.setTaskId(10L);
        doNothing().when(accessControlService).requirePermission(1L, "expense:approval:view");
        when(expenseDocumentService.listPendingApprovals(1L)).thenReturn(List.of(item));

        mockMvc.perform(get("/auth/expense-approval/pending").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].taskId").value(10));

        verify(accessControlService).requirePermission(1L, "expense:approval:view");
        verify(expenseDocumentService).listPendingApprovals(1L);
    }

    @Test
    void approveUsesCurrentUserIdentity() throws Exception {
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        detail.setDocumentCode("DOC-1");
        doNothing().when(accessControlService).requirePermission(1L, "expense:approval:approve");
        when(expenseDocumentService.approveTask(eq(1L), eq("tester"), eq(99L), any(ExpenseApprovalActionDTO.class))).thenReturn(detail);

        mockMvc.perform(post("/auth/expense-approval/tasks/99/approve")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.documentCode").value("DOC-1"));

        verify(accessControlService).requirePermission(1L, "expense:approval:approve");
        verify(expenseDocumentService).approveTask(eq(1L), eq("tester"), eq(99L), any(ExpenseApprovalActionDTO.class));
    }
}
