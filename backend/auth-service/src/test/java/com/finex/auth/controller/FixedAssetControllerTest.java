package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FixedAssetCardSaveDTO;
import com.finex.auth.dto.FixedAssetChangeBillSaveDTO;
import com.finex.auth.dto.FixedAssetChangeLineDTO;
import com.finex.auth.dto.FixedAssetCategoryVO;
import com.finex.auth.dto.FixedAssetCategorySaveDTO;
import com.finex.auth.dto.FixedAssetDisposalBillSaveDTO;
import com.finex.auth.dto.FixedAssetDisposalLineDTO;
import com.finex.auth.dto.FixedAssetMetaVO;
import com.finex.auth.dto.FixedAssetOpeningImportDTO;
import com.finex.auth.dto.FixedAssetOpeningImportRowDTO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FixedAssetService;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FixedAssetController(fixedAssetService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
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

    @Test
    void categorySaveDtoRejectsOverlongCategoryCode() {
        FixedAssetCategorySaveDTO dto = new FixedAssetCategorySaveDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setCategoryCode("123456789012345678901234567890123");
        dto.setCategoryName("Office Equipment");
        dto.setShareScope("COMPANY");
        dto.setDepreciationMethod("STRAIGHT_LINE");
        dto.setUsefulLifeMonths(36);
        dto.setResidualRate(new BigDecimal("0.05"));
        dto.setAssetAccount("1601");
        dto.setAccumDeprAccount("1602");
        dto.setDeprExpenseAccount("6602");
        dto.setDisposalAccount("1701");
        dto.setGainAccount("6301");
        dto.setLossAccount("6711");
        dto.setOffsetAccount("1002");

        Set<ConstraintViolation<FixedAssetCategorySaveDTO>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(item -> "类别编码长度不能超过32个字符".equals(item.getMessage())));
    }

    @Test
    void cardSaveDtoRejectsOverlongAssetName() {
        FixedAssetCardSaveDTO dto = new FixedAssetCardSaveDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setAssetCode("FA-CARD-001");
        dto.setAssetName("ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLM");
        dto.setCategoryId(1L);
        dto.setInServiceDate("2026-04-14");
        dto.setOriginalAmount(new BigDecimal("1000"));
        dto.setAccumDeprAmount(BigDecimal.ZERO);
        dto.setSalvageAmount(BigDecimal.ZERO);
        dto.setUsefulLifeMonths(36);
        dto.setDepreciatedMonths(0);
        dto.setRemainingMonths(36);

        Set<ConstraintViolation<FixedAssetCardSaveDTO>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(item -> "资产名称长度不能超过64个字符".equals(item.getMessage())));
    }

    @Test
    void openingImportDtoRejectsOverlongAssetCode() {
        FixedAssetOpeningImportRowDTO row = new FixedAssetOpeningImportRowDTO();
        row.setRowNo(1);
        row.setAssetCode("123456789012345678901234567890123");
        row.setAssetName("测试固定资产");
        row.setCategoryCode("CAT001");
        row.setInServiceDate("2026-04-14");
        row.setOriginalAmount(new BigDecimal("1000"));
        row.setAccumDeprAmount(BigDecimal.ZERO);
        row.setSalvageAmount(BigDecimal.ZERO);
        row.setUsefulLifeMonths(36);
        row.setDepreciatedMonths(0);
        row.setRemainingMonths(36);

        FixedAssetOpeningImportDTO dto = new FixedAssetOpeningImportDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setRows(List.of(row));

        Set<ConstraintViolation<FixedAssetOpeningImportDTO>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(item -> "资产编码长度不能超过32个字符".equals(item.getMessage())));
    }

    @Test
    void changeBillSaveDtoRejectsOverlongAssetCode() {
        FixedAssetChangeLineDTO line = new FixedAssetChangeLineDTO();
        line.setAssetCode("123456789012345678901234567890123");

        FixedAssetChangeBillSaveDTO dto = new FixedAssetChangeBillSaveDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setBillType("ADD");
        dto.setLines(List.of(line));

        Set<ConstraintViolation<FixedAssetChangeBillSaveDTO>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(item -> "资产编码长度不能超过32个字符".equals(item.getMessage())));
    }

    @Test
    void disposalBillSaveDtoRejectsOverlongAssetCode() {
        FixedAssetDisposalLineDTO line = new FixedAssetDisposalLineDTO();
        line.setAssetCode("123456789012345678901234567890123");

        FixedAssetDisposalBillSaveDTO dto = new FixedAssetDisposalBillSaveDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setLines(List.of(line));

        Set<ConstraintViolation<FixedAssetDisposalBillSaveDTO>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(item -> "资产编码长度不能超过32个字符".equals(item.getMessage())));
    }
}
