package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceVendorSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceVendorService;
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
class FinanceArchiveControllerTest {

    @Mock
    private FinanceVendorService financeVendorService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceArchiveController(financeVendorService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listVendorsForwardsCompanyIdToService() throws Exception {
        FinanceVendorSummaryVO summary = new FinanceVendorSummaryVO();
        summary.setCVenCode("VEN202604050001");
        summary.setCVenName("广州供应商");
        summary.setCompanyId("COMPANY_A");
        summary.setActive(true);

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:suppliers:view");
        when(financeVendorService.listVendors("COMPANY_A", "广州", false)).thenReturn(List.of(summary));

        mockMvc.perform(get("/auth/finance/archives/suppliers")
                        .param("companyId", "COMPANY_A")
                        .param("keyword", "广州")
                        .param("includeDisabled", "false")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].companyId").value("COMPANY_A"))
                .andExpect(jsonPath("$.data[0].cvenName").value("广州供应商"));

        verify(financeVendorService).listVendors("COMPANY_A", "广州", false);
    }
}
