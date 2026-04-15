package com.finex.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.dto.FinanceVendorSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceVendorService;
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
class FinanceArchiveControllerTest {

    private static final String SUPPLIER_CREATED = "\u4f9b\u5e94\u5546\u6863\u6848\u5df2\u521b\u5efa";
    private static final String SUPPLIER_UPDATED = "\u4f9b\u5e94\u5546\u6863\u6848\u5df2\u66f4\u65b0";
    private static final String SUPPLIER_CODE_TOO_LONG = "\u4f9b\u5e94\u5546\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private FinanceVendorService financeVendorService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceArchiveController(financeVendorService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void listSuppliersReturnsFrontendContractKeys() throws Exception {
        FinanceVendorSummaryVO summary = new FinanceVendorSummaryVO();
        summary.setCVenCode("VEN001");
        summary.setCVenName("\u6838\u5fc3\u4f9b\u5e94\u5546");
        summary.setCVenAbbName("\u6838\u5fc3\u7b80\u79f0");
        summary.setCompanyId("COMPANY_A");
        summary.setActive(true);

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:suppliers:view");
        when(financeVendorService.listVendors("COMPANY_A", "\u6838\u5fc3", false)).thenReturn(List.of(summary));

        mockMvc.perform(get("/auth/finance/archives/suppliers")
                        .param("companyId", "COMPANY_A")
                        .param("keyword", "\u6838\u5fc3")
                        .param("includeDisabled", "false")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].companyId").value("COMPANY_A"))
                .andExpect(jsonPath("$.data[0].cVenCode").value("VEN001"))
                .andExpect(jsonPath("$.data[0].cVenName").value("\u6838\u5fc3\u4f9b\u5e94\u5546"))
                .andExpect(jsonPath("$.data[0].cVenAbbName").value("\u6838\u5fc3\u7b80\u79f0"));

        verify(financeVendorService).listVendors("COMPANY_A", "\u6838\u5fc3", false);
    }

    @Test
    void getSupplierDetailReturnsFrontendContractKeys() throws Exception {
        FinanceVendorDetailVO detail = new FinanceVendorDetailVO();
        detail.setCVenCode("VEN001");
        detail.setCVenName("\u6838\u5fc3\u4f9b\u5e94\u5546");
        detail.setCVenAbbName("\u6838\u5fc3\u7b80\u79f0");
        detail.setReceiptAccountName("\u6838\u5fc3\u4f9b\u5e94\u5546");
        detail.setCompanyId("COMPANY_A");
        detail.setActive(true);

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:suppliers:view");
        when(financeVendorService.getVendorDetail("COMPANY_A", "VEN001")).thenReturn(detail);

        mockMvc.perform(get("/auth/finance/archives/suppliers/VEN001")
                        .param("companyId", "COMPANY_A")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.cVenCode").value("VEN001"))
                .andExpect(jsonPath("$.data.cVenName").value("\u6838\u5fc3\u4f9b\u5e94\u5546"))
                .andExpect(jsonPath("$.data.cVenAbbName").value("\u6838\u5fc3\u7b80\u79f0"))
                .andExpect(jsonPath("$.data.receiptAccountName").value("\u6838\u5fc3\u4f9b\u5e94\u5546"));

        verify(financeVendorService).getVendorDetail("COMPANY_A", "VEN001");
    }

    @Test
    void createSupplierBindsCamelCasePayloadAndReturnsChineseMessage() throws Exception {
        FinanceVendorDetailVO detail = new FinanceVendorDetailVO();
        detail.setCVenCode("VEN001");
        detail.setCVenName("\u6838\u5fc3\u4f9b\u5e94\u5546");
        detail.setReceiptAccountName("\u6838\u5fc3\u4f9b\u5e94\u5546");
        detail.setCompanyId("COMPANY_A");

        doNothing().when(accessControlService)
                .requireAnyPermission(1L, "finance:archives:suppliers:create", "finance:archives:suppliers:edit");
        when(financeVendorService.createVendor(eq("COMPANY_A"), any(), eq("tester"), eq(false))).thenReturn(detail);

        mockMvc.perform(post("/auth/finance/archives/suppliers")
                        .param("companyId", "COMPANY_A")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .contentType("application/json")
                        .content("""
                                {
                                  "cVenName": "\u6838\u5fc3\u4f9b\u5e94\u5546",
                                  "receiptAccountName": "\u6838\u5fc3\u6536\u6b3e\u540d"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(SUPPLIER_CREATED))
                .andExpect(jsonPath("$.data.cVenCode").value("VEN001"))
                .andExpect(jsonPath("$.data.cVenName").value("\u6838\u5fc3\u4f9b\u5e94\u5546"));

        ArgumentCaptor<FinanceVendorSaveDTO> captor = ArgumentCaptor.forClass(FinanceVendorSaveDTO.class);
        verify(financeVendorService).createVendor(eq("COMPANY_A"), captor.capture(), eq("tester"), eq(false));
        assertEquals("\u6838\u5fc3\u4f9b\u5e94\u5546", captor.getValue().getCVenName());
        assertEquals("\u6838\u5fc3\u6536\u6b3e\u540d", captor.getValue().getReceiptAccountName());
    }

    @Test
    void updateSupplierBindsCamelCasePayloadAndReturnsChineseMessage() throws Exception {
        FinanceVendorDetailVO detail = new FinanceVendorDetailVO();
        detail.setCVenCode("VEN001");
        detail.setCVenName("\u66f4\u65b0\u540e\u4f9b\u5e94\u5546");
        detail.setCVenAbbName("\u66f4\u65b0\u540e\u7b80\u79f0");
        detail.setReceiptAccountName("\u66f4\u65b0\u540e\u5f00\u6237\u540d");
        detail.setCompanyId("COMPANY_A");

        doNothing().when(accessControlService).requirePermission(1L, "finance:archives:suppliers:edit");
        when(financeVendorService.updateVendor(eq("COMPANY_A"), eq("VEN001"), any(), eq("tester"), eq(false))).thenReturn(detail);

        mockMvc.perform(put("/auth/finance/archives/suppliers/VEN001")
                        .param("companyId", "COMPANY_A")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .contentType("application/json")
                        .content("""
                                {
                                  "cVenName": "\u66f4\u65b0\u540e\u4f9b\u5e94\u5546",
                                  "receiptAccountName": "\u66f4\u65b0\u540e\u5f00\u6237\u540d"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(SUPPLIER_UPDATED))
                .andExpect(jsonPath("$.data.cVenCode").value("VEN001"))
                .andExpect(jsonPath("$.data.cVenName").value("\u66f4\u65b0\u540e\u4f9b\u5e94\u5546"));

        ArgumentCaptor<FinanceVendorSaveDTO> captor = ArgumentCaptor.forClass(FinanceVendorSaveDTO.class);
        verify(financeVendorService).updateVendor(eq("COMPANY_A"), eq("VEN001"), captor.capture(), eq("tester"), eq(false));
        assertEquals("\u66f4\u65b0\u540e\u4f9b\u5e94\u5546", captor.getValue().getCVenName());
        assertEquals("\u66f4\u65b0\u540e\u5f00\u6237\u540d", captor.getValue().getReceiptAccountName());
    }

    @Test
    void createSupplierRejectsOverlongTightenedField() throws Exception {
        FinanceVendorSaveDTO dto = new FinanceVendorSaveDTO();
        dto.setCVenName("\u6838\u5fc3\u4f9b\u5e94\u5546");
        dto.setCVenCode("V".repeat(65));

        mockMvc.perform(post("/auth/finance/archives/suppliers")
                        .param("companyId", "COMPANY_A")
                        .requestAttr("currentUserId", 1L)
                        .contentType("application/json")
                        .content(OBJECT_MAPPER.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(SUPPLIER_CODE_TOO_LONG));

        verifyNoInteractions(financeVendorService, accessControlService);
    }
}
