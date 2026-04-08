package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceProjectArchiveMetaVO;
import com.finex.auth.dto.FinanceProjectArchiveOptionVO;
import com.finex.auth.dto.FinanceProjectSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceProjectArchiveService;
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
class FinanceProjectArchiveControllerTest {

    @Mock
    private FinanceProjectArchiveService financeProjectArchiveService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceProjectArchiveController(financeProjectArchiveService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void metaRequiresPermissionAndReturnsPayload() throws Exception {
        FinanceProjectArchiveOptionVO option = new FinanceProjectArchiveOptionVO();
        option.setValue("1");
        option.setLabel("启用");
        FinanceProjectArchiveMetaVO meta = new FinanceProjectArchiveMetaVO();
        meta.setStatusOptions(List.of(option));

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:projects:view");
        when(financeProjectArchiveService.getMeta("COMPANY_A")).thenReturn(meta);

        mockMvc.perform(get("/auth/finance/archives/projects/meta")
                        .requestAttr("currentUserId", 1L)
                        .param("companyId", "COMPANY_A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.statusOptions[0].value").value("1"));

        verify(accessControlService).requirePermission(1L, "finance:archives:projects:view");
        verify(financeProjectArchiveService).getMeta("COMPANY_A");
    }

    @Test
    void listProjectsForwardsFilters() throws Exception {
        FinanceProjectSummaryVO summary = new FinanceProjectSummaryVO();
        summary.setCitemcode("PROJ001");
        summary.setCitemname("项目一");
        summary.setStatus(1);
        summary.setBclose(0);
        summary.setCitemccode("CLASS001");

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:projects:view");
        when(financeProjectArchiveService.listProjects("COMPANY_A", "PROJ", "CLASS001", 1, 0)).thenReturn(List.of(summary));

        mockMvc.perform(get("/auth/finance/archives/projects")
                        .requestAttr("currentUserId", 1L)
                        .param("companyId", "COMPANY_A")
                        .param("keyword", "PROJ")
                        .param("projectClassCode", "CLASS001")
                        .param("status", "1")
                        .param("bclose", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].citemcode").value("PROJ001"));

        verify(financeProjectArchiveService).listProjects("COMPANY_A", "PROJ", "CLASS001", 1, 0);
    }
}
