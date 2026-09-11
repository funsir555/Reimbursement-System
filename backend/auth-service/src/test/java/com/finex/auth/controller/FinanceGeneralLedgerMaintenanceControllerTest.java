package com.finex.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceGeneralLedgerRollbackPeriodDTO;
import com.finex.auth.dto.FinanceGeneralLedgerRollbackPeriodResultVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceGeneralLedgerMaintenanceService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceGeneralLedgerMaintenanceControllerTest {

    @Mock
    private FinanceGeneralLedgerMaintenanceService financeGeneralLedgerMaintenanceService;

    @Mock
    private AccessControlService accessControlService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceGeneralLedgerMaintenanceController(
                        financeGeneralLedgerMaintenanceService,
                        accessControlService
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .build();
    }

    @Test
    void rollbackPeriodUsesRepairPermissionAndPassesPayload() throws Exception {
        FinanceGeneralLedgerRollbackPeriodDTO dto = new FinanceGeneralLedgerRollbackPeriodDTO();
        dto.setCompanyId("COMP-001");
        dto.setIyear(2022);
        dto.setIperiod(12);

        FinanceGeneralLedgerRollbackPeriodResultVO result = new FinanceGeneralLedgerRollbackPeriodResultVO();
        result.setCompanyId("COMP-001");
        result.setIyperiod(202212);
        result.setPostStatus("NOT_POSTED");

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:repair:run");
        when(financeGeneralLedgerMaintenanceService.rollbackPeriod(eq(1L), eq("财务张三"), any(FinanceGeneralLedgerRollbackPeriodDTO.class)))
                .thenReturn(result);

        mockMvc.perform(post("/auth/finance/system-maintenance/general-ledger/rollback-period")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "财务张三")
                        .characterEncoding("UTF-8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("总账期间专项回退完成"))
                .andExpect(jsonPath("$.data.iyperiod").value(202212));

        ArgumentCaptor<FinanceGeneralLedgerRollbackPeriodDTO> captor =
                ArgumentCaptor.forClass(FinanceGeneralLedgerRollbackPeriodDTO.class);
        verify(financeGeneralLedgerMaintenanceService).rollbackPeriod(eq(1L), eq("财务张三"), captor.capture());
        assertEquals("COMP-001", captor.getValue().getCompanyId());
        assertEquals(2022, captor.getValue().getIyear());
        assertEquals(12, captor.getValue().getIperiod());
    }
}
