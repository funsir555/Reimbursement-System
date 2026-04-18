package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceCashFlowItemSaveDTO;
import com.finex.auth.dto.FinanceCashFlowItemStatusDTO;
import com.finex.auth.dto.FinanceCashFlowItemSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceCashFlowArchiveService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceCashFlowArchiveControllerTest {

    @Mock
    private FinanceCashFlowArchiveService financeCashFlowArchiveService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceCashFlowArchiveController(financeCashFlowArchiveService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listRequiresPermissionAndReturnsItems() throws Exception {
        FinanceCashFlowItemSummaryVO item = buildSummary();
        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:projects:view");
        when(financeCashFlowArchiveService.listCashFlows("COMPANY_A", "1001", "INFLOW", 1)).thenReturn(List.of(item));

        mockMvc.perform(get("/auth/finance/archives/cash-flows")
                        .requestAttr("currentUserId", 1L)
                        .param("companyId", "COMPANY_A")
                        .param("keyword", "1001")
                        .param("direction", "INFLOW")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].cashFlowCode").value("1001"));

        verify(accessControlService).requirePermission(1L, "finance:archives:projects:view");
        verify(financeCashFlowArchiveService).listCashFlows("COMPANY_A", "1001", "INFLOW", 1);
    }

    @Test
    void createRequiresCreateOrEditPermission() throws Exception {
        FinanceCashFlowItemSummaryVO item = buildSummary();
        doNothing().when(accessControlService)
                .requireAnyPermission(1L, "finance:archives:projects:create", "finance:archives:projects:edit");
        when(financeCashFlowArchiveService.createCashFlow(eq("COMPANY_A"), any(FinanceCashFlowItemSaveDTO.class))).thenReturn(item);

        mockMvc.perform(post("/auth/finance/archives/cash-flows")
                        .requestAttr("currentUserId", 1L)
                        .param("companyId", "COMPANY_A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cashFlowCode":"1001","cashFlowName":"销售商品、提供劳务收到的现金","direction":"INFLOW","status":1,"sortOrder":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cashFlowName").value("销售商品、提供劳务收到的现金"));

        verify(accessControlService)
                .requireAnyPermission(1L, "finance:archives:projects:create", "finance:archives:projects:edit");
        verify(financeCashFlowArchiveService).createCashFlow(eq("COMPANY_A"), any(FinanceCashFlowItemSaveDTO.class));
    }

    @Test
    void updateRequiresEditPermission() throws Exception {
        FinanceCashFlowItemSummaryVO item = buildSummary();
        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:projects:edit");
        when(financeCashFlowArchiveService.updateCashFlow(eq("COMPANY_A"), eq(9L), any(FinanceCashFlowItemSaveDTO.class))).thenReturn(item);

        mockMvc.perform(put("/auth/finance/archives/cash-flows/9")
                        .requestAttr("currentUserId", 1L)
                        .param("companyId", "COMPANY_A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cashFlowCode":"1001","cashFlowName":"销售商品、提供劳务收到的现金","direction":"INFLOW","status":1,"sortOrder":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cashFlowCode").value("1001"));

        verify(accessControlService).requirePermission(1L, "finance:archives:projects:edit");
        verify(financeCashFlowArchiveService).updateCashFlow(eq("COMPANY_A"), eq(9L), any(FinanceCashFlowItemSaveDTO.class));
    }

    @Test
    void statusUpdateRequiresDisableOrEditPermission() throws Exception {
        doNothing().when(accessControlService)
                .requireAnyPermission(1L, "finance:archives:projects:disable", "finance:archives:projects:edit");
        when(financeCashFlowArchiveService.updateCashFlowStatus(eq("COMPANY_A"), eq(9L), any(FinanceCashFlowItemStatusDTO.class)))
                .thenReturn(Boolean.TRUE);

        mockMvc.perform(post("/auth/finance/archives/cash-flows/9/status")
                        .requestAttr("currentUserId", 1L)
                        .param("companyId", "COMPANY_A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(accessControlService)
                .requireAnyPermission(1L, "finance:archives:projects:disable", "finance:archives:projects:edit");
        verify(financeCashFlowArchiveService).updateCashFlowStatus(eq("COMPANY_A"), eq(9L), any(FinanceCashFlowItemStatusDTO.class));
    }

    private FinanceCashFlowItemSummaryVO buildSummary() {
        FinanceCashFlowItemSummaryVO item = new FinanceCashFlowItemSummaryVO();
        item.setId(9L);
        item.setCompanyId("COMPANY_A");
        item.setCashFlowCode("1001");
        item.setCashFlowName("销售商品、提供劳务收到的现金");
        item.setDirection("INFLOW");
        item.setStatus(1);
        item.setSortOrder(10);
        return item;
    }
}
