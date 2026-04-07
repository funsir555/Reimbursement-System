package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
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
        summary.setCCusName("广州客户");
        summary.setCompanyId("COMPANY_A");
        summary.setActive(true);

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:customers:view");
        when(financeCustomerService.listCustomers("COMPANY_A", "广州", false)).thenReturn(List.of(summary));

        mockMvc.perform(get("/auth/finance/archives/customers")
                        .param("companyId", "COMPANY_A")
                        .param("keyword", "广州")
                        .param("includeDisabled", "false")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].companyId").value("COMPANY_A"))
                .andExpect(jsonPath("$.data[0].ccusName").value("广州客户"));

        verify(financeCustomerService).listCustomers("COMPANY_A", "广州", false);
    }
}
