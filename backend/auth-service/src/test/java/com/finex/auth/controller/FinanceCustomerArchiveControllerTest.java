package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceCustomerDetailVO;
import com.finex.auth.dto.FinanceCustomerSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceCustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceCustomerArchiveControllerTest {

    @Mock
    private FinanceCustomerService financeCustomerService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceCustomerArchiveController(financeCustomerService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listCustomersForwardsCompanyIdToService() throws Exception {
        FinanceCustomerSummaryVO summary = new FinanceCustomerSummaryVO();
        summary.setCCusCode("CUS202604050001");
        summary.setCCusName("??????");
        summary.setCompanyId("COMPANY_A");
        summary.setActive(true);

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:customers:view");
        when(financeCustomerService.listCustomers("COMPANY_A", "???", false)).thenReturn(List.of(summary));

        mockMvc.perform(get("/auth/finance/archives/customers")
                        .param("companyId", "COMPANY_A")
                        .param("keyword", "???")
                        .param("includeDisabled", "false")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].companyId").value("COMPANY_A"))
                .andExpect(jsonPath("$.data[0].ccusName").value("??????"));

        verify(financeCustomerService).listCustomers("COMPANY_A", "???", false);
    }

    @Test
    void createCustomerRequiresAnyPermissionAndDelegates() throws Exception {
        FinanceCustomerDetailVO detail = new FinanceCustomerDetailVO();
        detail.setCCusCode("CUS001");
        detail.setCCusName("????");
        detail.setCompanyId("COMPANY_A");

        doNothing().when(accessControlService).requireAnyPermission(1L, "finance:archives:customers:create", "finance:archives:customers:edit");
        when(financeCustomerService.createCustomer(org.mockito.ArgumentMatchers.eq("COMPANY_A"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("tester")))
                .thenReturn(detail);

        mockMvc.perform(post("/auth/finance/archives/customers")
                        .param("companyId", "COMPANY_A")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .contentType("application/json")
                        .content("{\"cCusName\":\"广州客户\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(accessControlService).requireAnyPermission(1L, "finance:archives:customers:create", "finance:archives:customers:edit");
        verify(financeCustomerService).createCustomer(org.mockito.ArgumentMatchers.eq("COMPANY_A"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("tester"));
    }
}
