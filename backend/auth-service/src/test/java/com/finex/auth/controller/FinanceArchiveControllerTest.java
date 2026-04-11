package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.dto.FinanceVendorSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceVendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
        summary.setCVenName("Guangzhou Supplier");
        summary.setCompanyId("COMPANY_A");
        summary.setActive(true);

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:suppliers:view");
        when(financeVendorService.listVendors("COMPANY_A", "Guangzhou", false)).thenReturn(List.of(summary));

        mockMvc.perform(get("/auth/finance/archives/suppliers")
                        .param("companyId", "COMPANY_A")
                        .param("keyword", "Guangzhou")
                        .param("includeDisabled", "false")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].companyId").value("COMPANY_A"))
                .andExpect(jsonPath("$.data[0].cvenName").value("Guangzhou Supplier"));

        verify(financeVendorService).listVendors("COMPANY_A", "Guangzhou", false);
    }

    @Test
    void createVendorRequiresAnyPermissionAndForwardsOperator() throws Exception {
        FinanceVendorDetailVO detail = new FinanceVendorDetailVO();
        detail.setCVenCode("VEN202604110001");
        detail.setCompanyId("COMPANY_A");

        doNothing().when(accessControlService)
                .requireAnyPermission(1L, "finance:archives:suppliers:create", "finance:archives:suppliers:edit");
        when(financeVendorService.createVendor(eq("COMPANY_A"), any(FinanceVendorSaveDTO.class), eq("tester"), eq(false)))
                .thenReturn(detail);

        mockMvc.perform(post("/auth/finance/archives/suppliers")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .param("companyId", "COMPANY_A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"cVenName\": \"Quick Vendor\",
                                  \"companyId\": \"COMPANY_B\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.companyId").value("COMPANY_A"));

        verify(accessControlService)
                .requireAnyPermission(1L, "finance:archives:suppliers:create", "finance:archives:suppliers:edit");
        verify(financeVendorService)
                .createVendor(eq("COMPANY_A"), any(FinanceVendorSaveDTO.class), eq("tester"), eq(false));
    }
}