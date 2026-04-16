package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceBankBranchVO;
import com.finex.auth.dto.FinanceBankOptionVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceBankCatalogService;
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
class FinanceBankCatalogControllerTest {

    @Mock
    private FinanceBankCatalogService financeBankCatalogService;
    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceBankCatalogController(financeBankCatalogService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listBanksSupportsBusinessScopeQuery() throws Exception {
        FinanceBankOptionVO option = new FinanceBankOptionVO();
        option.setBankCode("CMB");
        option.setBankName("招商银行");
        option.setBusinessScope("PUBLIC");

        when(financeBankCatalogService.listBanks("招商", "PUBLIC")).thenReturn(List.of(option));
        doNothing().when(accessControlService).requireAnyPermission(1L,
                "expense:create:view",
                "expense:create:create",
                "expense:create:submit",
                "profile:view",
                "finance:archives:suppliers:view",
                "finance:archives:suppliers:create",
                "finance:archives:suppliers:edit",
                "settings:company_accounts:view",
                "settings:company_accounts:create",
                "settings:company_accounts:edit");

        mockMvc.perform(get("/auth/finance/banks")
                        .requestAttr("currentUserId", 1L)
                        .param("keyword", "招商")
                        .param("businessScope", "PUBLIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].bankCode").value("CMB"))
                .andExpect(jsonPath("$.data[0].businessScope").value("PUBLIC"));

        verify(financeBankCatalogService).listBanks("招商", "PUBLIC");
    }

    @Test
    void listBankCitiesUsesBankScopeFilters() throws Exception {
        when(financeBankCatalogService.listCities("CMB", "广东省", "PUBLIC"))
                .thenReturn(List.of("深圳市", "广州市"));
        doNothing().when(accessControlService).requireAnyPermission(1L,
                "expense:create:view",
                "expense:create:create",
                "expense:create:submit",
                "profile:view",
                "finance:archives:suppliers:view",
                "finance:archives:suppliers:create",
                "finance:archives:suppliers:edit",
                "settings:company_accounts:view",
                "settings:company_accounts:create",
                "settings:company_accounts:edit");

        mockMvc.perform(get("/auth/finance/banks/cities")
                        .requestAttr("currentUserId", 1L)
                        .param("bankCode", "CMB")
                        .param("province", "广东省")
                        .param("businessScope", "PUBLIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("深圳市"));

        verify(financeBankCatalogService).listCities("CMB", "广东省", "PUBLIC");
    }

    @Test
    void listBranchesKeepsExistingResponseShape() throws Exception {
        FinanceBankBranchVO branch = new FinanceBankBranchVO();
        branch.setBranchCode("CMB-SZ-FH");
        branch.setBranchName("招商银行深圳福华支行");
        branch.setCnapsCode("308584000013");

        when(financeBankCatalogService.listBankBranches("CMB", "广东省", "深圳市", "福华", "PUBLIC"))
                .thenReturn(List.of(branch));
        doNothing().when(accessControlService).requireAnyPermission(1L,
                "expense:create:view",
                "expense:create:create",
                "expense:create:submit",
                "profile:view",
                "finance:archives:suppliers:view",
                "finance:archives:suppliers:create",
                "finance:archives:suppliers:edit",
                "settings:company_accounts:view",
                "settings:company_accounts:create",
                "settings:company_accounts:edit");

        mockMvc.perform(get("/auth/finance/bank-branches")
                        .requestAttr("currentUserId", 1L)
                        .param("bankCode", "CMB")
                        .param("province", "广东省")
                        .param("city", "深圳市")
                        .param("keyword", "福华")
                        .param("businessScope", "PUBLIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].branchCode").value("CMB-SZ-FH"))
                .andExpect(jsonPath("$.data[0].cnapsCode").value("308584000013"));

        verify(financeBankCatalogService).listBankBranches("CMB", "广东省", "深圳市", "福华", "PUBLIC");
    }
}
