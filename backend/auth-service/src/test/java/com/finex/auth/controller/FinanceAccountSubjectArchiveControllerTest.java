package com.finex.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceAccountSubjectDerivedDefaultsVO;
import com.finex.auth.dto.FinanceAccountSubjectDetailVO;
import com.finex.auth.dto.FinanceAccountSubjectMetaVO;
import com.finex.auth.dto.FinanceAccountSubjectOptionVO;
import com.finex.auth.dto.FinanceAccountSubjectSaveDTO;
import com.finex.auth.dto.FinanceAccountSubjectSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceAccountSubjectArchiveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class FinanceAccountSubjectArchiveControllerTest {

    @Mock
    private FinanceAccountSubjectArchiveService financeAccountSubjectArchiveService;

    @Mock
    private AccessControlService accessControlService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceAccountSubjectArchiveController(financeAccountSubjectArchiveService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .build();
    }

    @Test
    void metaRequiresPermissionAndReturnsPayload() throws Exception {
        FinanceAccountSubjectOptionVO option = new FinanceAccountSubjectOptionVO();
        option.setValue("ASSET");
        option.setLabel("\u8d44\u4ea7");
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
        summary.setSubjectName("\u5e93\u5b58\u73b0\u91d1");
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
    void getDerivedDefaultsRequiresPermissionAndReturnsPayload() throws Exception {
        FinanceAccountSubjectDerivedDefaultsVO defaults = new FinanceAccountSubjectDerivedDefaultsVO();
        defaults.setSubjectLevel(1);
        defaults.setSubjectCategory("PROFIT");
        defaults.setBalanceDirection("DEBIT");
        defaults.setLeafFlag(1);
        defaults.setMatchedBy("TEMPLATE_EXACT");

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:account_subjects:view");
        when(financeAccountSubjectArchiveService.getDerivedDefaults("COMPANY_A", "6602")).thenReturn(defaults);

        mockMvc.perform(get("/auth/finance/archives/account-subjects/derived-defaults")
                        .requestAttr("currentUserId", 1L)
                        .param("companyId", "COMPANY_A")
                        .param("subjectCode", "6602"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subject_level").value(1))
                .andExpect(jsonPath("$.data.subject_category").value("PROFIT"))
                .andExpect(jsonPath("$.data.matched_by").value("TEMPLATE_EXACT"));

        verify(financeAccountSubjectArchiveService).getDerivedDefaults("COMPANY_A", "6602");
    }

    @Test
    void getDerivedDefaultsReturnsChildParentMatchPayload() throws Exception {
        FinanceAccountSubjectDerivedDefaultsVO defaults = new FinanceAccountSubjectDerivedDefaultsVO();
        defaults.setParentSubjectCode("1122");
        defaults.setSubjectLevel(2);
        defaults.setSubjectCategory("ASSET");
        defaults.setBalanceDirection("DEBIT");
        defaults.setLeafFlag(1);
        defaults.setMatchedBy("EXISTING_PARENT");

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:account_subjects:view");
        when(financeAccountSubjectArchiveService.getDerivedDefaults("COMPANY_A", "112203")).thenReturn(defaults);

        mockMvc.perform(get("/auth/finance/archives/account-subjects/derived-defaults")
                        .requestAttr("currentUserId", 1L)
                        .param("companyId", "COMPANY_A")
                        .param("subjectCode", "112203"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parent_subject_code").value("1122"))
                .andExpect(jsonPath("$.data.subject_level").value(2))
                .andExpect(jsonPath("$.data.subject_category").value("ASSET"))
                .andExpect(jsonPath("$.data.balance_direction").value("DEBIT"))
                .andExpect(jsonPath("$.data.matched_by").value("EXISTING_PARENT"));

        verify(financeAccountSubjectArchiveService).getDerivedDefaults("COMPANY_A", "112203");
    }

    @Test
    void createSubjectRequiresAnyPermissionAndDelegates() throws Exception {
        FinanceAccountSubjectDetailVO detail = new FinanceAccountSubjectDetailVO();
        detail.setSubjectCode("1001");
        detail.setSubjectName("\u5e93\u5b58\u73b0\u91d1");

        doNothing().when(accessControlService).requireAnyPermission(1L, "finance:archives:account_subjects:create", "finance:archives:account_subjects:edit");
        when(financeAccountSubjectArchiveService.createSubject(eq("COMPANY_A"), any(FinanceAccountSubjectSaveDTO.class), eq("tester")))
                .thenReturn(detail);

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("1001");
        dto.setSubjectName("\u5e93\u5b58\u73b0\u91d1");
        dto.setSubjectCategory("ASSET");

        mockMvc.perform(post("/auth/finance/archives/account-subjects")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .param("companyId", "COMPANY_A")
                        .characterEncoding("UTF-8")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(accessControlService).requireAnyPermission(1L, "finance:archives:account_subjects:create", "finance:archives:account_subjects:edit");
        verify(financeAccountSubjectArchiveService).createSubject(eq("COMPANY_A"), any(FinanceAccountSubjectSaveDTO.class), eq("tester"));
    }

    @Test
    void updateSubjectKeepsUtf8ChinesePayloadIntact() throws Exception {
        FinanceAccountSubjectDetailVO detail = new FinanceAccountSubjectDetailVO();
        detail.setSubjectCode("560101");
        detail.setSubjectName("\u5e7f\u544a\u5ba3\u4f20\u8d39");

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:account_subjects:edit");
        when(financeAccountSubjectArchiveService.updateSubject(eq("COMPANY202604050001"), eq("560101"), any(FinanceAccountSubjectSaveDTO.class), eq("tester")))
                .thenReturn(detail);

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("560101");
        dto.setSubjectName("\u5e7f\u544a\u5ba3\u4f20\u8d39");
        dto.setChelp("\u5e7f\u544a\u5ba3\u4f20\u8d39");
        dto.setSubjectCategory("PROFIT");

        mockMvc.perform(put("/auth/finance/archives/account-subjects/560101")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .param("companyId", "COMPANY202604050001")
                        .characterEncoding("UTF-8")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.subject_name").value("\u5e7f\u544a\u5ba3\u4f20\u8d39"));

        ArgumentCaptor<FinanceAccountSubjectSaveDTO> dtoCaptor = ArgumentCaptor.forClass(FinanceAccountSubjectSaveDTO.class);
        verify(financeAccountSubjectArchiveService).updateSubject(eq("COMPANY202604050001"), eq("560101"), dtoCaptor.capture(), eq("tester"));
        assertEquals("\u5e7f\u544a\u5ba3\u4f20\u8d39", dtoCaptor.getValue().getSubjectName());
        assertEquals("\u5e7f\u544a\u5ba3\u4f20\u8d39", dtoCaptor.getValue().getChelp());
    }
}
