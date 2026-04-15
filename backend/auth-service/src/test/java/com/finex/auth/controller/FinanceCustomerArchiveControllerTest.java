package com.finex.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceCustomerDetailVO;
import com.finex.auth.dto.FinanceCustomerSaveDTO;
import com.finex.auth.dto.FinanceCustomerSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceCustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceCustomerArchiveControllerTest {

    private static final String CUSTOMER_CREATED = "\u5ba2\u6237\u6863\u6848\u5df2\u521b\u5efa";
    private static final String CUSTOMER_UPDATED = "\u5ba2\u6237\u6863\u6848\u5df2\u66f4\u65b0";
    private static final String CUSTOMER_CODE_TOO_LONG = "\u5ba2\u6237\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private FinanceCustomerService financeCustomerService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceCustomerArchiveController(financeCustomerService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void listCustomersReturnsFrontendContractKeys() throws Exception {
        FinanceCustomerSummaryVO summary = new FinanceCustomerSummaryVO();
        summary.setCCusCode("CUS202604050001");
        summary.setCCusName("\u6d4b\u8bd5\u5ba2\u6237");
        summary.setCCusAbbName("\u6d4b\u8bd5\u7b80\u79f0");
        summary.setCompanyId("COMPANY_A");
        summary.setActive(true);

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:customers:view");
        when(financeCustomerService.listCustomers("COMPANY_A", "\u6d4b\u8bd5", false)).thenReturn(List.of(summary));

        mockMvc.perform(get("/auth/finance/archives/customers")
                        .param("companyId", "COMPANY_A")
                        .param("keyword", "\u6d4b\u8bd5")
                        .param("includeDisabled", "false")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].companyId").value("COMPANY_A"))
                .andExpect(jsonPath("$.data[0].cCusCode").value("CUS202604050001"))
                .andExpect(jsonPath("$.data[0].cCusName").value("\u6d4b\u8bd5\u5ba2\u6237"))
                .andExpect(jsonPath("$.data[0].cCusAbbName").value("\u6d4b\u8bd5\u7b80\u79f0"));

        verify(financeCustomerService).listCustomers("COMPANY_A", "\u6d4b\u8bd5", false);
    }

    @Test
    void getCustomerDetailReturnsFrontendContractKeys() throws Exception {
        FinanceCustomerDetailVO detail = new FinanceCustomerDetailVO();
        detail.setCCusCode("CUS001");
        detail.setCCusName("\u5e7f\u5dde\u5ba2\u6237");
        detail.setCCusAbbName("\u5e7f\u5dde\u7b80\u79f0");
        detail.setCompanyId("COMPANY_A");
        detail.setActive(true);

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:customers:view");
        when(financeCustomerService.getCustomerDetail("COMPANY_A", "CUS001")).thenReturn(detail);

        mockMvc.perform(get("/auth/finance/archives/customers/CUS001")
                        .param("companyId", "COMPANY_A")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.cCusCode").value("CUS001"))
                .andExpect(jsonPath("$.data.cCusName").value("\u5e7f\u5dde\u5ba2\u6237"))
                .andExpect(jsonPath("$.data.cCusAbbName").value("\u5e7f\u5dde\u7b80\u79f0"))
                .andExpect(jsonPath("$.data.companyId").value("COMPANY_A"));

        verify(financeCustomerService).getCustomerDetail("COMPANY_A", "CUS001");
    }

    @Test
    void createCustomerBindsCamelCasePayloadAndReturnsChineseMessage() throws Exception {
        FinanceCustomerDetailVO detail = new FinanceCustomerDetailVO();
        detail.setCCusCode("CUS001");
        detail.setCCusName("\u6d4b\u8bd5\u5ba2\u6237");
        detail.setCompanyId("COMPANY_A");

        doNothing().when(accessControlService)
                .requireAnyPermission(1L, "finance:archives:customers:create", "finance:archives:customers:edit");
        when(financeCustomerService.createCustomer(eq("COMPANY_A"), any(), eq("tester"))).thenReturn(detail);

        mockMvc.perform(post("/auth/finance/archives/customers")
                        .param("companyId", "COMPANY_A")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .contentType("application/json")
                        .content("""
                                {
                                  "cCusName": "\u6d4b\u8bd5\u5ba2\u6237",
                                  "cCusAbbName": "\u6d4b\u8bd5\u7b80\u79f0"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(CUSTOMER_CREATED))
                .andExpect(jsonPath("$.data.cCusCode").value("CUS001"))
                .andExpect(jsonPath("$.data.cCusName").value("\u6d4b\u8bd5\u5ba2\u6237"));

        ArgumentCaptor<FinanceCustomerSaveDTO> captor = ArgumentCaptor.forClass(FinanceCustomerSaveDTO.class);
        verify(financeCustomerService).createCustomer(eq("COMPANY_A"), captor.capture(), eq("tester"));
        assertEquals("\u6d4b\u8bd5\u5ba2\u6237", captor.getValue().getCCusName());
        assertEquals("\u6d4b\u8bd5\u7b80\u79f0", captor.getValue().getCCusAbbName());
    }

    @Test
    void updateCustomerBindsCamelCasePayloadAndReturnsChineseMessage() throws Exception {
        FinanceCustomerDetailVO detail = new FinanceCustomerDetailVO();
        detail.setCCusCode("CUS001");
        detail.setCCusName("\u66f4\u65b0\u540e\u5ba2\u6237");
        detail.setCCusAbbName("\u66f4\u65b0\u540e\u7b80\u79f0");
        detail.setCompanyId("COMPANY_A");

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:customers:edit");
        when(financeCustomerService.updateCustomer(eq("COMPANY_A"), eq("CUS001"), any(), eq("tester"))).thenReturn(detail);

        mockMvc.perform(put("/auth/finance/archives/customers/CUS001")
                        .param("companyId", "COMPANY_A")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .contentType("application/json")
                        .content("""
                                {
                                  "cCusName": "\u66f4\u65b0\u540e\u5ba2\u6237",
                                  "cCusAbbName": "\u66f4\u65b0\u540e\u7b80\u79f0"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(CUSTOMER_UPDATED))
                .andExpect(jsonPath("$.data.cCusCode").value("CUS001"))
                .andExpect(jsonPath("$.data.cCusName").value("\u66f4\u65b0\u540e\u5ba2\u6237"));

        ArgumentCaptor<FinanceCustomerSaveDTO> captor = ArgumentCaptor.forClass(FinanceCustomerSaveDTO.class);
        verify(financeCustomerService).updateCustomer(eq("COMPANY_A"), eq("CUS001"), captor.capture(), eq("tester"));
        assertEquals("\u66f4\u65b0\u540e\u5ba2\u6237", captor.getValue().getCCusName());
        assertEquals("\u66f4\u65b0\u540e\u7b80\u79f0", captor.getValue().getCCusAbbName());
    }

    @Test
    void createCustomerRejectsOverlongTightenedField() throws Exception {
        FinanceCustomerSaveDTO dto = new FinanceCustomerSaveDTO();
        dto.setCCusName("\u6d4b\u8bd5\u5ba2\u6237");
        dto.setCCusCode("C".repeat(65));

        mockMvc.perform(post("/auth/finance/archives/customers")
                        .param("companyId", "COMPANY_A")
                        .requestAttr("currentUserId", 1L)
                        .contentType("application/json")
                        .content(OBJECT_MAPPER.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(CUSTOMER_CODE_TOO_LONG));

        verifyNoInteractions(financeCustomerService, accessControlService);
    }
}
