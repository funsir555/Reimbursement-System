package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.FinanceVendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

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
class ExpenseDocumentControllerTest {

    @Mock
    private ExpenseDocumentService expenseDocumentService;

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
                .standaloneSetup(new ExpenseDocumentController(expenseDocumentService, financeVendorService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void listTemplatesAllowsAnyExpenseCreateEntryPermission() throws Exception {
        ExpenseCreateTemplateSummaryVO summary = new ExpenseCreateTemplateSummaryVO();
        summary.setTemplateCode("TPL-001");
        summary.setTemplateName("Travel Reimbursement");

        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "expense:create:view",
                "expense:create:create",
                "expense:create:submit"
        );
        when(expenseDocumentService.listAvailableTemplates()).thenReturn(List.of(summary));

        mockMvc.perform(get("/auth/expenses/create/templates").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].templateCode").value("TPL-001"));

        verify(accessControlService).requireAnyPermission(
                1L,
                "expense:create:view",
                "expense:create:create",
                "expense:create:submit"
        );
        verify(expenseDocumentService).listAvailableTemplates();
    }

    @Test
    void getTemplateDetailAllowsAnyExpenseCreateEntryPermission() throws Exception {
        ExpenseCreateTemplateDetailVO detail = new ExpenseCreateTemplateDetailVO();
        detail.setTemplateCode("TPL-001");
        detail.setTemplateName("Travel Reimbursement");
        detail.setCurrentUserCompanyId("COMPANY-001");
        detail.setCurrentUserCompanyName("Shanghai Branch");
        ProcessFormOptionVO companyOption = new ProcessFormOptionVO();
        companyOption.setValue("COMPANY-001");
        companyOption.setLabel("Shanghai Branch");
        detail.setCompanyOptions(List.of(companyOption));

        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "expense:create:view",
                "expense:create:create",
                "expense:create:submit"
        );
        when(expenseDocumentService.getTemplateDetail(1L, "TPL-001")).thenReturn(detail);

        mockMvc.perform(get("/auth/expenses/create/templates/TPL-001").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.templateCode").value("TPL-001"))
                .andExpect(jsonPath("$.data.currentUserCompanyId").value("COMPANY-001"))
                .andExpect(jsonPath("$.data.currentUserCompanyName").value("Shanghai Branch"))
                .andExpect(jsonPath("$.data.companyOptions[0].value").value("COMPANY-001"));

        verify(accessControlService).requireAnyPermission(
                1L,
                "expense:create:view",
                "expense:create:create",
                "expense:create:submit"
        );
        verify(expenseDocumentService).getTemplateDetail(1L, "TPL-001");
    }

    @Test
    void listVendorOptionsPassesPaymentCompanyId() throws Exception {
        ExpenseCreateVendorOptionVO option = new ExpenseCreateVendorOptionVO();
        option.setValue("VENDOR-001");
        option.setLabel("Shanghai Test Vendor");

        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "expense:create:view",
                "expense:create:create",
                "expense:create:submit"
        );
        when(expenseDocumentService.listVendorOptions(1L, "Shanghai", null, "COMPANY-A"))
                .thenReturn(List.of(option));

        mockMvc.perform(get("/auth/expenses/create/vendors/options")
                        .requestAttr("currentUserId", 1L)
                        .param("keyword", "Shanghai")
                        .param("paymentCompanyId", "COMPANY-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].value").value("VENDOR-001"));

        verify(expenseDocumentService).listVendorOptions(1L, "Shanghai", null, "COMPANY-A");
    }

    @Test
    void getVendorDetailUsesSelectedPaymentCompanyId() throws Exception {
        FinanceVendorDetailVO detail = new FinanceVendorDetailVO();
        detail.setCVenCode("VENDOR-001");
        detail.setCompanyId("COMPANY-A");
        detail.setCVenName("Selected Vendor");

        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "expense:create:view",
                "expense:create:create",
                "expense:create:submit"
        );
        when(financeVendorService.getVendorDetail("COMPANY-A", "VENDOR-001")).thenReturn(detail);

        mockMvc.perform(get("/auth/expenses/create/vendors/VENDOR-001")
                        .requestAttr("currentUserId", 1L)
                        .param("paymentCompanyId", "COMPANY-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.cVenCode").value("VENDOR-001"))
                .andExpect(jsonPath("$.data.companyId").value("COMPANY-A"));

        verify(financeVendorService).getVendorDetail("COMPANY-A", "VENDOR-001");
    }

    @Test
    void createVendorUsesSelectedPaymentCompanyId() throws Exception {
        FinanceVendorDetailVO detail = new FinanceVendorDetailVO();
        detail.setCVenCode("VEN202604050001");
        detail.setCompanyId("COMPANY_A");

        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "expense:create:create",
                "expense:create:submit"
        );
        when(financeVendorService.createVendor(eq("COMPANY_A"), any(FinanceVendorSaveDTO.class), eq("tester"), eq(true)))
                .thenReturn(detail);

        mockMvc.perform(post("/auth/expenses/create/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .param("paymentCompanyId", "COMPANY_A")
                        .content("""
                                {
                                  "cVenName": "Quick Vendor"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.companyId").value("COMPANY_A"));

        verify(financeVendorService).createVendor(eq("COMPANY_A"), any(FinanceVendorSaveDTO.class), eq("tester"), eq(true));
    }

    @Test
    void updateVendorUsesSelectedPaymentCompanyId() throws Exception {
        FinanceVendorDetailVO detail = new FinanceVendorDetailVO();
        detail.setCVenCode("VEN202604050001");
        detail.setCompanyId("COMPANY_A");
        detail.setCVenAccount("6222020000000001");

        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "expense:create:create",
                "expense:create:submit"
        );
        when(financeVendorService.updateVendor(eq("COMPANY_A"), eq("VEN202604050001"), any(FinanceVendorSaveDTO.class), eq("tester"), eq(true)))
                .thenReturn(detail);

        mockMvc.perform(put("/auth/expenses/create/vendors/VEN202604050001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "tester")
                        .param("paymentCompanyId", "COMPANY_A")
                        .content("""
                                {
                                  "cVenName": "Quick Vendor",
                                  "receiptAccountName": "Quick Vendor Account",
                                  "cVenBank": "ICBC",
                                  "cVenAccount": "6222020000000001",
                                  "receiptBankProvince": "上海市",
                                  "receiptBankCity": "上海市",
                                  "receiptBranchName": "上海分行"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.companyId").value("COMPANY_A"));

        verify(financeVendorService).updateVendor(
                eq("COMPANY_A"),
                eq("VEN202604050001"),
                any(FinanceVendorSaveDTO.class),
                eq("tester"),
                eq(true)
        );
    }

    @Test
    void listPayeeOptionsPassesPersonalOnlyFlag() throws Exception {
        ExpenseCreatePayeeOptionVO option = new ExpenseCreatePayeeOptionVO();
        option.setValue("PERSONAL_PAYEE:张三");
        option.setLabel("张三");
        option.setSourceType("PERSONAL_PRIVATE_PAYEE");
        option.setSourceCode("张三");

        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "expense:create:view",
                "expense:create:create",
                "expense:create:submit"
        );
        when(expenseDocumentService.listPayeeOptions(1L, "张", true)).thenReturn(List.of(option));

        mockMvc.perform(get("/auth/expenses/create/payees/options")
                        .requestAttr("currentUserId", 1L)
                        .param("keyword", "张")
                        .param("personalOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].value").value("PERSONAL_PAYEE:张三"));

        verify(expenseDocumentService).listPayeeOptions(1L, "张", true);
    }

    @Test
    void listPayeeAccountOptionsPassesLinkageContextAndPaymentCompanyId() throws Exception {
        ExpenseCreatePayeeAccountOptionVO option = new ExpenseCreatePayeeAccountOptionVO();
        option.setValue("VENDOR_ACCOUNT:8");
        option.setOwnerCode("VENDOR-001");
        option.setOwnerName("上海测试供应商");
        option.setAccountNoMasked("6222 **** 8888");

        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "expense:create:view",
                "expense:create:create",
                "expense:create:submit"
        );
        when(expenseDocumentService.listPayeeAccountOptions(
                1L,
                "6222",
                "ENTERPRISE",
                null,
                "VENDOR-001",
                "COMPANY-A"
        )).thenReturn(List.of(option));

        mockMvc.perform(get("/auth/expenses/create/payee-accounts/options")
                        .requestAttr("currentUserId", 1L)
                        .param("keyword", "6222")
                        .param("linkageMode", "ENTERPRISE")
                        .param("counterpartyCode", "VENDOR-001")
                        .param("paymentCompanyId", "COMPANY-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].value").value("VENDOR_ACCOUNT:8"));

        verify(expenseDocumentService).listPayeeAccountOptions(
                1L,
                "6222",
                "ENTERPRISE",
                null,
                "VENDOR-001",
                "COMPANY-A"
        );
    }
}
