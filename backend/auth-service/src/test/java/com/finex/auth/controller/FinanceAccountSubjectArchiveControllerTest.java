package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceAccountSubjectDetailVO;
import com.finex.auth.dto.FinanceAccountSubjectMetaVO;
import com.finex.auth.dto.FinanceAccountSubjectOptionVO;
import com.finex.auth.dto.FinanceAccountSubjectSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceAccountSubjectArchiveService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceAccountSubjectArchiveControllerTest {

    @Mock
    private FinanceAccountSubjectArchiveService financeAccountSubjectArchiveService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceAccountSubjectArchiveController(financeAccountSubjectArchiveService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void metaRequiresPermissionAndReturnsPayload() throws Exception {
        FinanceAccountSubjectOptionVO option = new FinanceAccountSubjectOptionVO();
        option.setValue("ASSET");
        option.setLabel("??");
        FinanceAccountSubjectMetaVO meta = new FinanceAccountSubjectMetaVO();
        meta.setSubjectCategoryOptions(List.of(option));

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:account_subjects:view");
        when(financeAccountSubjectArchiveService.getMeta()).thenReturn(meta);

        mockMvc.perform(get("/auth/finance/archives/account-subjects/meta").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.subjectCategoryOptions[0].value").value("ASSET"));

        verify(accessControlService).requirePermission(1L, "finance:archives:account_subjects:view");
    }

    @Test
    void listSubjectsForwardsFilters() throws Exception {
        FinanceAccountSubjectSummaryVO summary = new FinanceAccountSubjectSummaryVO();
        summary.setSubjectCode("1001");
        summary.setSubjectName("????");
        summary.setSubjectLevel(1);
        summary.setStatus(1);
        summary.setBclose(0);
        summary.setLeafFlag(1);
        summary.setChildren(List.of());

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:account_subjects:view");
        when(financeAccountSubjectArchiveService.listSubjects("COMPANY_A", "1001", "ASSET", 1, 0)).thenReturn(List.of(summary));

        mockMvc.perform(get("/auth/finance/archives/account-subjects")
                        .requestAttr("currentUserId", 1L)
                        .param("companyId", "COMPANY_A")
                        .param("keyword", "1001")
                        .param("subjectCategory", "ASSET")
                        .param("status", "1")
                        .param("bclose", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].subject_code").value("1001"));

        verify(financeAccountSubjectArchiveService).listSubjects("COMPANY_A", "1001", "ASSET", 1, 0);
    }

    @Test
    void createSubjectRequiresAnyPermissionAndDelegates() throws Exception {
        FinanceAccountSubjectDetailVO detail = new FinanceAccountSubjectDetailVO();
        detail.setSubjectCode("1001");
        detail.setSubjectName("库存现金");

        doNothing().when(accessControlService).requireAnyPermission(1L, "finance:archives:account_subjects:create", "finance:archives:account_subjects:edit");
        when(financeAccountSubjectArchiveService.createSubject(org.mockito.ArgumentMatchers.eq("COMPANY_A"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("tester")))
                .thenReturn(detail);

        mockMvc.perform(post("/auth/finance/archives/account-subjects")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .param("companyId", "COMPANY_A")
                        .contentType("application/json")
                        .content("{\"subject_code\":\"1001\",\"subject_name\":\"库存现金\",\"subject_category\":\"ASSET\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(accessControlService).requireAnyPermission(1L, "finance:archives:account_subjects:create", "finance:archives:account_subjects:edit");
        verify(financeAccountSubjectArchiveService).createSubject(org.mockito.ArgumentMatchers.eq("COMPANY_A"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("tester"));
    }
}
