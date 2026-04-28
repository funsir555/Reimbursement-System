package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceContextCompanyOptionVO;
import com.finex.auth.dto.FinanceContextMetaVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceContextControllerTest {

    @Mock
    private FinanceContextService financeContextService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceContextController(financeContextService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void metaReturnsFinanceCompanyContext() throws Exception {
        FinanceContextCompanyOptionVO option = new FinanceContextCompanyOptionVO();
        option.setCompanyId("COMPANY_A");
        option.setCompanyCode("COMP_A");
        option.setCompanyName("\u5e7f\u5dde\u8fdc\u667a\u6559\u80b2\u79d1\u6280\u6709\u9650\u516c\u53f8");
        option.setHasActiveAccountSet(true);
        option.setEnabledYear(2026);
        option.setEnabledPeriod(4);
        option.setPeriodStartYear(2026);
        option.setPeriodStartMonth(4);
        option.setPeriodEndYear(2026);
        option.setPeriodEndMonth(4);
        option.setValue("COMPANY_A");
        option.setLabel("COMP_A - \u5e7f\u5dde\u8fdc\u667a\u6559\u80b2\u79d1\u6280\u6709\u9650\u516c\u53f8");

        FinanceContextMetaVO meta = new FinanceContextMetaVO();
        meta.getCompanyOptions().add(option);
        meta.setCurrentUserCompanyId("COMPANY_A");
        meta.setDefaultCompanyId("COMPANY_A");

        when(financeContextService.getMeta(1L)).thenReturn(meta);

        mockMvc.perform(get("/auth/finance/context/meta")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.defaultCompanyId").value("COMPANY_A"))
                .andExpect(jsonPath("$.data.companyOptions[0].companyName").value("\u5e7f\u5dde\u8fdc\u667a\u6559\u80b2\u79d1\u6280\u6709\u9650\u516c\u53f8"))
                .andExpect(jsonPath("$.data.companyOptions[0].hasActiveAccountSet").value(true))
                .andExpect(jsonPath("$.data.companyOptions[0].enabledYear").value(2026))
                .andExpect(jsonPath("$.data.companyOptions[0].enabledPeriod").value(4))
                .andExpect(jsonPath("$.data.companyOptions[0].periodStartYear").value(2026))
                .andExpect(jsonPath("$.data.companyOptions[0].periodStartMonth").value(4))
                .andExpect(jsonPath("$.data.companyOptions[0].periodEndYear").value(2026))
                .andExpect(jsonPath("$.data.companyOptions[0].periodEndMonth").value(4));

        verify(financeContextService).getMeta(1L);
        List<String> requiredPermissions = mockingDetails(accessControlService).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("requireAnyPermission"))
                .findFirst()
                .map(invocation -> Arrays.stream(invocation.getArguments())
                        .skip(1)
                        .map(String.class::cast)
                        .toList())
                .orElseThrow();

        assertTrue(requiredPermissions.contains("finance:general_ledger:post_voucher:view"));
        assertTrue(requiredPermissions.contains("finance:general_ledger:close_ledger:view"));
        assertTrue(requiredPermissions.contains("finance:general_ledger:opening_balance:view"));
        assertTrue(requiredPermissions.contains("finance:general_ledger:balance_sheet:view"));
    }
}
