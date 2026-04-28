package com.finex.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.FinancePostVoucherMetaVO;
import com.finex.auth.dto.FinancePostVoucherTaskRequestDTO;
import com.finex.auth.dto.FinancePostVoucherTaskStatusVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinancePostVoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.filter.CharacterEncodingFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class FinancePostVoucherControllerTest {

    @Mock
    private FinancePostVoucherService financePostVoucherService;

    @Mock
    private AccessControlService accessControlService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinancePostVoucherController(financePostVoucherService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .build();
    }

    @Test
    void metaUsesViewPermissionAndReturnsPostingSnapshot() throws Exception {
        FinancePostVoucherMetaVO meta = new FinancePostVoucherMetaVO();
        meta.setCompanyId("COMP-001");
        meta.setCompanyName("\u5e7f\u5dde\u5206\u516c\u53f8");
        meta.setIyear(2026);
        meta.setIperiod(4);
        meta.setStatus("PARTIALLY_POSTED");
        meta.setStatusLabel("\u90e8\u5206\u8bb0\u8d26");
        meta.setReviewableVoucherCount(3);
        meta.setPostedVoucherCount(5);
        meta.setCanPost(true);

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:post_voucher:view");
        when(financePostVoucherService.getMeta(1L, "COMP-001", 2026, 4)).thenReturn(meta);

        mockMvc.perform(get("/auth/finance/post-voucher/meta")
                        .param("companyId", "COMP-001")
                        .param("iyear", "2026")
                        .param("iperiod", "4")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.companyId").value("COMP-001"))
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_POSTED"))
                .andExpect(jsonPath("$.data.reviewableVoucherCount").value(3))
                .andExpect(jsonPath("$.data.postedVoucherCount").value(5));

        verify(accessControlService).requirePermission(1L, "finance:general_ledger:post_voucher:view");
        verify(financePostVoucherService).getMeta(1L, "COMP-001", 2026, 4);
    }

    @Test
    void runUsesRunPermissionAndPassesUtf8PayloadToService() throws Exception {
        FinancePostVoucherTaskRequestDTO dto = new FinancePostVoucherTaskRequestDTO();
        dto.setCompanyId("COMP-001");
        dto.setIyear(2026);
        dto.setIperiod(4);

        AsyncTaskSubmitResultVO result = new AsyncTaskSubmitResultVO();
        result.setTaskNo("FPV202604270001");
        result.setTaskType("finance_post_voucher_run");
        result.setBusinessType("finance_post_voucher");
        result.setStatus("PENDING");
        result.setMessage("\u8bb0\u8d26\u4efb\u52a1\u5df2\u63d0\u4ea4");

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:post_voucher:run");
        when(financePostVoucherService.runPosting(eq(1L), eq("\u8d22\u52a1\u5f20\u4e09"), any(FinancePostVoucherTaskRequestDTO.class))).thenReturn(result);

        mockMvc.perform(post("/auth/finance/post-voucher/run")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "\u8d22\u52a1\u5f20\u4e09")
                        .characterEncoding("UTF-8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskNo").value("FPV202604270001"));

        verify(accessControlService).requirePermission(1L, "finance:general_ledger:post_voucher:run");
        ArgumentCaptor<FinancePostVoucherTaskRequestDTO> captor = ArgumentCaptor.forClass(FinancePostVoucherTaskRequestDTO.class);
        verify(financePostVoucherService).runPosting(eq(1L), eq("\u8d22\u52a1\u5f20\u4e09"), captor.capture());
        assertEquals("COMP-001", captor.getValue().getCompanyId());
        assertEquals(2026, captor.getValue().getIyear());
        assertEquals(4, captor.getValue().getIperiod());
    }

    @Test
    void taskStatusAllowsTaskViewPermission() throws Exception {
        FinancePostVoucherTaskStatusVO statusVo = new FinancePostVoucherTaskStatusVO();
        statusVo.setTaskNo("FPV202604270001");
        statusVo.setStatus("SUCCESS");
        statusVo.setProgress(100);
        statusVo.setFinished(true);
        statusVo.setPeriodStatus("FULLY_POSTED");

        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "finance:general_ledger:post_voucher:task:view",
                "finance:general_ledger:post_voucher:view"
        );
        when(financePostVoucherService.getTaskStatus("FPV202604270001")).thenReturn(statusVo);

        mockMvc.perform(get("/auth/finance/post-voucher/tasks/{taskNo}", "FPV202604270001")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.progress").value(100))
                .andExpect(jsonPath("$.data.finished").value(true))
                .andExpect(jsonPath("$.data.periodStatus").value("FULLY_POSTED"));
    }
}
