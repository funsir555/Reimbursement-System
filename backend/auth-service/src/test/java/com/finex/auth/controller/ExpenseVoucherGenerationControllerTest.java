package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.ExpenseVoucherGeneratedRecordDetailVO;
import com.finex.auth.dto.ExpenseVoucherGenerationMetaVO;
import com.finex.auth.dto.ExpenseVoucherPageVO;
import com.finex.auth.dto.ExpenseVoucherPushBatchResultVO;
import com.finex.auth.dto.ExpenseVoucherPushDTO;
import com.finex.auth.dto.ExpenseVoucherTemplatePolicyVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseVoucherGenerationService;
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
class ExpenseVoucherGenerationControllerTest {

    @Mock
    private ExpenseVoucherGenerationService expenseVoucherGenerationService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ExpenseVoucherGenerationController(expenseVoucherGenerationService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void metaRequiresAnyPermissionAndForwardsUserId() throws Exception {
        ExpenseVoucherGenerationMetaVO meta = new ExpenseVoucherGenerationMetaVO();
        meta.setDefaultCompanyId("COMPANY_A");
        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "expense:voucher_generation:view",
                "expense:voucher_generation:mapping:view",
                "expense:voucher_generation:push:view",
                "expense:voucher_generation:query:view"
        );
        when(expenseVoucherGenerationService.getMeta(1L)).thenReturn(meta);

        mockMvc.perform(get("/auth/expenses/voucher-generation/meta")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.defaultCompanyId").value("COMPANY_A"));

        verify(accessControlService).requireAnyPermission(
                1L,
                "expense:voucher_generation:view",
                "expense:voucher_generation:mapping:view",
                "expense:voucher_generation:push:view",
                "expense:voucher_generation:query:view"
        );
        verify(expenseVoucherGenerationService).getMeta(1L);
    }

    @Test
    void mappingsTemplateRouteDelegatesToTemplatePolicies() throws Exception {
        ExpenseVoucherPageVO<ExpenseVoucherTemplatePolicyVO> page = new ExpenseVoucherPageVO<>();
        page.setTotal(1);
        page.setPage(1);
        page.setPageSize(10);
        page.setItems(List.of(new ExpenseVoucherTemplatePolicyVO()));
        doNothing().when(accessControlService).requirePermission(1L, "expense:voucher_generation:mapping:view");
        when(expenseVoucherGenerationService.getTemplatePolicies("COMPANY_A", "TMP001", 1, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/auth/expenses/voucher-generation/mappings")
                        .requestAttr("currentUserId", 1L)
                        .param("type", "template")
                        .param("companyId", "COMPANY_A")
                        .param("templateCode", "TMP001")
                        .param("enabled", "1")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(accessControlService).requirePermission(1L, "expense:voucher_generation:mapping:view");
        verify(expenseVoucherGenerationService).getTemplatePolicies("COMPANY_A", "TMP001", 1, 1, 10);
    }

    @Test
    void pushRequiresPermissionAndForwardsOperator() throws Exception {
        ExpenseVoucherPushBatchResultVO result = new ExpenseVoucherPushBatchResultVO();
        result.setLatestBatchNo("VG001");
        doNothing().when(accessControlService).requirePermission(1L, "expense:voucher_generation:push:execute");
        when(expenseVoucherGenerationService.pushDocuments(any(ExpenseVoucherPushDTO.class), eq(1L), eq("tester"))).thenReturn(result);

        mockMvc.perform(post("/auth/expenses/voucher-generation/push")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentCodes":["DOC-001"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.latestBatchNo").value("VG001"));

        verify(accessControlService).requirePermission(1L, "expense:voucher_generation:push:execute");
        verify(expenseVoucherGenerationService).pushDocuments(any(ExpenseVoucherPushDTO.class), eq(1L), eq("tester"));
    }

    @Test
    void voucherDetailRequiresQueryPermission() throws Exception {
        ExpenseVoucherGeneratedRecordDetailVO detail = new ExpenseVoucherGeneratedRecordDetailVO();
        doNothing().when(accessControlService).requirePermission(1L, "expense:voucher_generation:query:view");
        when(expenseVoucherGenerationService.getGeneratedVoucherDetail(9L)).thenReturn(detail);

        mockMvc.perform(get("/auth/expenses/voucher-generation/vouchers/9")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(accessControlService).requirePermission(1L, "expense:voucher_generation:query:view");
        verify(expenseVoucherGenerationService).getGeneratedVoucherDetail(9L);
    }
}
