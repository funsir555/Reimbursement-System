package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.FinanceVendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        summary.setTemplateName("差旅报销");

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
        detail.setTemplateName("差旅报销");
        ProcessFormOptionVO companyOption = new ProcessFormOptionVO();
        companyOption.setValue("COMPANY-001");
        companyOption.setLabel("\u4e0a\u6d77\u5206\u516c\u53f8");
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
    void listVendorOptionsAllowsAnyExpenseCreateEntryPermission() throws Exception {
        ExpenseCreateVendorOptionVO option = new ExpenseCreateVendorOptionVO();
        option.setValue("VENDOR-001");
        option.setLabel("上海测试供应商");

        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "expense:create:view",
                "expense:create:create",
                "expense:create:submit"
        );
        when(expenseDocumentService.listVendorOptions("上")).thenReturn(List.of(option));

        mockMvc.perform(get("/auth/expenses/create/vendors/options")
                        .requestAttr("currentUserId", 1L)
                        .param("keyword", "上"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].value").value("VENDOR-001"));

        verify(accessControlService).requireAnyPermission(
                1L,
                "expense:create:view",
                "expense:create:create",
                "expense:create:submit"
        );
        verify(expenseDocumentService).listVendorOptions("上");
    }
}
