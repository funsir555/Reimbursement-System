package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseBankLinkConfigVO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExpensePaymentControllerTest {

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
                .standaloneSetup(new ExpensePaymentController(expenseDocumentService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void listOrdersRequiresViewPermission() throws Exception {
        ExpensePaymentOrderVO order = new ExpensePaymentOrderVO();
        order.setTaskId(20L);
        doNothing().when(accessControlService).requirePermission(1L, "expense:payment:payment_order:view");
        when(expenseDocumentService.listPaymentOrders(1L, "PENDING")).thenReturn(List.of(order));

        mockMvc.perform(get("/auth/expense-payment/orders")
                        .requestAttr("currentUserId", 1L)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].taskId").value(20));

        verify(expenseDocumentService).listPaymentOrders(1L, "PENDING");
    }

    @Test
    void completeTaskUsesCurrentUserIdentity() throws Exception {
        doNothing().when(accessControlService).requirePermission(1L, "expense:payment:payment_order:execute");
        when(expenseDocumentService.completePaymentTask(eq(1L), eq("tester"), eq(7L), any(ExpenseApprovalActionDTO.class))).thenReturn(null);

        mockMvc.perform(post("/auth/expense-payment/tasks/7/complete")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(accessControlService).requirePermission(1L, "expense:payment:payment_order:execute");
        verify(expenseDocumentService).completePaymentTask(eq(1L), eq("tester"), eq(7L), any(ExpenseApprovalActionDTO.class));
    }

    @Test
    void updateBankLinkRequiresEditPermission() throws Exception {
        ExpenseBankLinkConfigVO config = new ExpenseBankLinkConfigVO();
        config.setCompanyBankAccountId(5L);
        doNothing().when(accessControlService).requirePermission(1L, "expense:payment:bank_link:edit");
        when(expenseDocumentService.updateBankLink(eq(5L), any()))
                .thenReturn(config);

        mockMvc.perform(put("/auth/expense-payment/bank-links/5")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"directConnectProvider\":\"CMB\",\"directConnectChannel\":\"CMB_CLOUD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.companyBankAccountId").value(5));

        verify(accessControlService).requirePermission(1L, "expense:payment:bank_link:edit");
        verify(expenseDocumentService).updateBankLink(eq(5L), any());
    }
}
