package com.finex.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.OpeningBalanceMetaVO;
import com.finex.auth.dto.OpeningBalanceTaskRequestDTO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceOpeningBalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

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
class FinanceOpeningBalanceControllerTest {

    @Mock
    private FinanceOpeningBalanceService financeOpeningBalanceService;

    @Mock
    private AccessControlService accessControlService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceOpeningBalanceController(financeOpeningBalanceService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void metaUsesOpeningBalanceViewPermission() throws Exception {
        OpeningBalanceMetaVO meta = new OpeningBalanceMetaVO();
        meta.setDefaultCompanyId("COMP-001");
        meta.setDefaultYear(2026);
        meta.setDefaultPeriod(4);
        meta.setStatus("OPENED");
        meta.setStatusLabel("已开账");
        meta.setOpened(true);

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:opening_balance:view");
        when(financeOpeningBalanceService.getMeta(1L, "alice", "COMP-001", 2026, 4)).thenReturn(meta);

        mockMvc.perform(get("/auth/finance/opening-balance/meta")
                        .param("companyId", "COMP-001")
                        .param("iyear", "2026")
                        .param("iperiod", "4")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultCompanyId").value("COMP-001"))
                .andExpect(jsonPath("$.data.status").value("OPENED"))
                .andExpect(jsonPath("$.data.statusLabel").value("已开账"));

        verify(accessControlService).requirePermission(1L, "finance:general_ledger:opening_balance:view");
        verify(financeOpeningBalanceService).getMeta(1L, "alice", "COMP-001", 2026, 4);
    }

    @Test
    void openBookSubmitsAsyncTask() throws Exception {
        AsyncTaskSubmitResultVO result = new AsyncTaskSubmitResultVO();
        result.setTaskNo("FOB20260427120000001");
        result.setTaskType("FINANCE_OPENING_BALANCE_OPEN_BOOK");
        result.setStatus("PENDING");
        result.setMessage("开账任务已提交");

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:opening_balance:view");
        when(financeOpeningBalanceService.openBook(eq(1L), eq("alice"), any(OpeningBalanceTaskRequestDTO.class)))
                .thenReturn(result);

        mockMvc.perform(post("/auth/finance/opening-balance/open-book")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(buildTaskRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("开账任务已提交"))
                .andExpect(jsonPath("$.data.taskNo").value("FOB20260427120000001"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    private OpeningBalanceTaskRequestDTO buildTaskRequest() {
        OpeningBalanceTaskRequestDTO dto = new OpeningBalanceTaskRequestDTO();
        dto.setCompanyId("COMP-001");
        dto.setIyear(2026);
        dto.setIperiod(4);
        return dto;
    }
}
