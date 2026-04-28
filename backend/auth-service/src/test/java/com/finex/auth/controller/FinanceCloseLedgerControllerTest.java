package com.finex.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceCloseLedgerMetaVO;
import com.finex.auth.dto.FinanceCloseLedgerRequestDTO;
import com.finex.auth.dto.FinanceCloseLedgerReconcileResultVO;
import com.finex.auth.dto.FinanceCloseLedgerValidationResultVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceCloseLedgerService;
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
class FinanceCloseLedgerControllerTest {

    @Mock
    private FinanceCloseLedgerService financeCloseLedgerService;

    @Mock
    private AccessControlService accessControlService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceCloseLedgerController(financeCloseLedgerService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .build();
    }

    @Test
    void metaUsesViewPermission() throws Exception {
        FinanceCloseLedgerMetaVO meta = new FinanceCloseLedgerMetaVO();
        meta.setCompanyId("COMP-001");
        meta.setCompanyName("\u5e7f\u5dde\u5206\u516c\u53f8");
        meta.setIyear(2026);
        meta.setIperiod(4);
        meta.setStatus("OPEN");
        meta.setStatusLabel("\u672a\u7ed3\u8d26");

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:close_ledger:view");
        when(financeCloseLedgerService.getMeta(1L, "COMP-001", 2026, 4)).thenReturn(meta);

        mockMvc.perform(get("/auth/finance/close-ledger/meta")
                        .param("companyId", "COMP-001")
                        .param("iyear", "2026")
                        .param("iperiod", "4")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.companyId").value("COMP-001"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        verify(accessControlService).requirePermission(1L, "finance:general_ledger:close_ledger:view");
        verify(financeCloseLedgerService).getMeta(1L, "COMP-001", 2026, 4);
    }

    @Test
    void reconcileUsesClosePermissionAndPassesPayload() throws Exception {
        FinanceCloseLedgerRequestDTO dto = new FinanceCloseLedgerRequestDTO();
        dto.setCompanyId("COMP-001");
        dto.setIyear(2026);
        dto.setIperiod(4);

        FinanceCloseLedgerReconcileResultVO result = new FinanceCloseLedgerReconcileResultVO();
        result.setPassed(true);
        result.setSummaryMessage("\u603b\u8d26\u5bf9\u8d26\u901a\u8fc7");

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:close_ledger:close");
        when(financeCloseLedgerService.reconcile(eq(1L), eq("\u8d22\u52a1\u5f20\u4e09"), any(FinanceCloseLedgerRequestDTO.class)))
                .thenReturn(result);

        mockMvc.perform(post("/auth/finance/close-ledger/reconcile")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "\u8d22\u52a1\u5f20\u4e09")
                        .characterEncoding("UTF-8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.passed").value(true));

        ArgumentCaptor<FinanceCloseLedgerRequestDTO> captor = ArgumentCaptor.forClass(FinanceCloseLedgerRequestDTO.class);
        verify(financeCloseLedgerService).reconcile(eq(1L), eq("\u8d22\u52a1\u5f20\u4e09"), captor.capture());
        assertEquals("COMP-001", captor.getValue().getCompanyId());
        assertEquals(2026, captor.getValue().getIyear());
        assertEquals(4, captor.getValue().getIperiod());
    }

    @Test
    void validateUsesClosePermission() throws Exception {
        FinanceCloseLedgerRequestDTO dto = new FinanceCloseLedgerRequestDTO();
        dto.setCompanyId("COMP-001");
        dto.setIyear(2026);
        dto.setIperiod(4);

        FinanceCloseLedgerValidationResultVO result = new FinanceCloseLedgerValidationResultVO();
        result.setPassed(false);
        result.setGeneralPassed(false);
        result.setExternalPassed(true);

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:close_ledger:close");
        when(financeCloseLedgerService.validate(eq(1L), eq("\u8d22\u52a1\u5f20\u4e09"), any(FinanceCloseLedgerRequestDTO.class)))
                .thenReturn(result);

        mockMvc.perform(post("/auth/finance/close-ledger/validate")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "\u8d22\u52a1\u5f20\u4e09")
                        .characterEncoding("UTF-8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passed").value(false))
                .andExpect(jsonPath("$.data.generalPassed").value(false));
    }

    @Test
    void closeReturnsSuccessMessage() throws Exception {
        FinanceCloseLedgerRequestDTO dto = new FinanceCloseLedgerRequestDTO();
        dto.setCompanyId("COMP-001");
        dto.setIyear(2026);
        dto.setIperiod(4);
        dto.setCloseNote("\u6708\u7ed3\u5b8c\u6210");

        FinanceCloseLedgerMetaVO result = new FinanceCloseLedgerMetaVO();
        result.setCompanyId("COMP-001");
        result.setStatus("CLOSED");
        result.setStatusLabel("\u5df2\u7ed3\u8d26");
        result.setCloseNote("\u6708\u7ed3\u5b8c\u6210");

        doNothing().when(accessControlService).requirePermission(1L, "finance:general_ledger:close_ledger:close");
        when(financeCloseLedgerService.close(eq(1L), eq("\u8d22\u52a1\u5f20\u4e09"), any(FinanceCloseLedgerRequestDTO.class)))
                .thenReturn(result);

        mockMvc.perform(post("/auth/finance/close-ledger/close")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "\u8d22\u52a1\u5f20\u4e09")
                        .characterEncoding("UTF-8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("\u7ed3\u8d26\u6210\u529f"))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }
}
