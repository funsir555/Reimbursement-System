package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FixedAssetCategoryVO;
import com.finex.auth.dto.FixedAssetMetaVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FixedAssetService;
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
class FixedAssetControllerTest {

    @Mock
    private FixedAssetService fixedAssetService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FixedAssetController(fixedAssetService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void metaRequiresViewPermissionAndForwardsContext() throws Exception {
        FixedAssetMetaVO metaVO = new FixedAssetMetaVO();
        metaVO.setDefaultCompanyId("COMPANY_A");
        metaVO.setDefaultFiscalYear(2026);
        metaVO.setDefaultFiscalPeriod(4);

        doNothing().when(accessControlService).requirePermission(1L, "finance:fixed_assets:view");
        when(fixedAssetService.getMeta(1L, "tester", "COMPANY_A", 2026, 4)).thenReturn(metaVO);

        mockMvc.perform(get("/auth/finance/fixed-assets/meta")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .param("companyId", "COMPANY_A")
                        .param("fiscalYear", "2026")
                        .param("fiscalPeriod", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.defaultCompanyId").value("COMPANY_A"))
                .andExpect(jsonPath("$.data.defaultFiscalYear").value(2026));

        verify(accessControlService).requirePermission(1L, "finance:fixed_assets:view");
        verify(fixedAssetService).getMeta(1L, "tester", "COMPANY_A", 2026, 4);
    }

    @Test
    void createCategoryRequiresPermissionAndForwardsOperator() throws Exception {
        FixedAssetCategoryVO categoryVO = new FixedAssetCategoryVO();
        categoryVO.setCompanyId("COMPANY_A");
        categoryVO.setCategoryCode("FA01");
        categoryVO.setCategoryName("Office Equipment");

        doNothing().when(accessControlService)
                .requireAnyPermission(1L, "finance:fixed_assets:create", "finance:fixed_assets:edit");
        when(fixedAssetService.createCategory(any(), eq("tester"))).thenReturn(categoryVO);

        mockMvc.perform(post("/auth/finance/fixed-assets/categories")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": "COMPANY_A",
                                  "categoryCode": "FA01",
                                  "categoryName": "Office Equipment",
                                  "shareScope": "COMPANY",
                                  "depreciationMethod": "STRAIGHT_LINE",
                                  "usefulLifeMonths": 36,
                                  "residualRate": 0.05,
                                  "assetAccount": "1601",
                                  "accumDeprAccount": "1602",
                                  "deprExpenseAccount": "6602",
                                  "disposalAccount": "1701",
                                  "gainAccount": "6301",
                                  "lossAccount": "6711",
                                  "offsetAccount": "1002"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.companyId").value("COMPANY_A"))
                .andExpect(jsonPath("$.data.categoryCode").value("FA01"));

        verify(accessControlService)
                .requireAnyPermission(1L, "finance:fixed_assets:create", "finance:fixed_assets:edit");
        verify(fixedAssetService).createCategory(any(), eq("tester"));
    }
}
