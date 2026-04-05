package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.ProcessCenterOverviewVO;
import com.finex.auth.dto.ProcessFormDesignSummaryVO;
import com.finex.auth.dto.ProcessFlowDetailVO;
import com.finex.auth.dto.ProcessFlowSummaryVO;
import com.finex.auth.dto.ProcessTemplateCardVO;
import com.finex.auth.dto.ProcessTemplateCategoryVO;
import com.finex.auth.dto.ProcessTemplateSaveDTO;
import com.finex.auth.dto.ProcessTemplateSaveResultVO;
import com.finex.auth.dto.ProcessTemplateTypeVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ProcessManagementService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProcessManagementControllerTest {

    @Mock
    private ProcessManagementService processManagementService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProcessManagementController(processManagementService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void listFlowsReturnsDataAndChecksPermission() throws Exception {
        ProcessFlowSummaryVO flow = new ProcessFlowSummaryVO();
        flow.setId(10L);
        flow.setFlowCode("FLOW-010");
        flow.setFlowName("Flow Alpha");
        flow.setStatus("ENABLED");
        flow.setStatusLabel("Enabled");

        doNothing().when(accessControlService).requirePermission(1L, "expense:process_management:view");
        when(processManagementService.listFlows()).thenReturn(List.of(flow));

        mockMvc.perform(get("/auth/process-management/flows").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].flowCode").value("FLOW-010"))
                .andExpect(jsonPath("$.data[0].flowName").value("Flow Alpha"));

        verify(accessControlService).requirePermission(1L, "expense:process_management:view");
        verify(processManagementService).listFlows();
    }

    @Test
    void overviewReturnsBoundFlowAndFormFields() throws Exception {
        ProcessTemplateCardVO template = new ProcessTemplateCardVO();
        template.setId(12L);
        template.setTemplateCode("FX202604020001");
        template.setFlowCode("FLOW-001");
        template.setFlowName("Flow Alpha");
        template.setFormCode("FD-001");
        template.setFormName("Form Alpha");

        ProcessTemplateCategoryVO category = new ProcessTemplateCategoryVO();
        category.setCode("employee-expense");
        category.setTemplates(List.of(template));

        ProcessCenterOverviewVO overview = new ProcessCenterOverviewVO();
        overview.setCategories(List.of(category));

        doNothing().when(accessControlService).requirePermission(1L, "expense:process_management:view");
        when(processManagementService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/auth/process-management/overview").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.categories[0].templates[0].flowCode").value("FLOW-001"))
                .andExpect(jsonPath("$.data.categories[0].templates[0].formCode").value("FD-001"))
                .andExpect(jsonPath("$.data.categories[0].templates[0].formName").value("Form Alpha"));

        verify(accessControlService).requirePermission(1L, "expense:process_management:view");
        verify(processManagementService).getOverview();
    }

    @Test
    void templateTypesReturnsContractType() throws Exception {
        ProcessTemplateTypeVO type = new ProcessTemplateTypeVO();
        type.setCode("contract");
        type.setName("合同单");
        type.setSubtitle("合同管理");

        doNothing().when(accessControlService).requirePermission(1L, "expense:process_management:view");
        when(processManagementService.getTemplateTypes()).thenReturn(List.of(type));

        mockMvc.perform(get("/auth/process-management/template-types").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].code").value("contract"))
                .andExpect(jsonPath("$.data[0].name").value("合同单"));

        verify(accessControlService).requirePermission(1L, "expense:process_management:view");
        verify(processManagementService).getTemplateTypes();
    }

    @Test
    void createTemplateUsesFallbackUsernameWhenRequestAttributeMissing() throws Exception {
        ProcessTemplateSaveResultVO result = new ProcessTemplateSaveResultVO();
        result.setId(66L);
        result.setTemplateCode("TPL-066");
        result.setTemplateName("Template A");
        result.setStatus("DRAFT");

        doNothing().when(accessControlService).requirePermission(1L, "expense:process_management:create");
        when(processManagementService.saveTemplate(any(ProcessTemplateSaveDTO.class), eq("Operator"))).thenReturn(result);

        mockMvc.perform(post("/auth/process-management/templates")
                        .requestAttr("currentUserId", 1L)
                        .requestAttr("currentUsername", "Operator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateType": "report",
                                  "templateName": "Template A",
                                  "formDesign": "FD-001",
                                  "approvalFlow": "FLOW-001",
                                  "expenseDetailDesign": "DET-001",
                                  "expenseDetailModeDefault": "PREPAY_UNBILLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.templateCode").value("TPL-066"));

        verify(accessControlService).requirePermission(1L, "expense:process_management:create");
        ArgumentCaptor<ProcessTemplateSaveDTO> captor = ArgumentCaptor.forClass(ProcessTemplateSaveDTO.class);
        verify(processManagementService).saveTemplate(captor.capture(), eq("Operator"));
        assertEquals("report", captor.getValue().getTemplateType());
        assertEquals("FD-001", captor.getValue().getFormDesign());
        assertEquals("FLOW-001", captor.getValue().getApprovalFlow());
        assertEquals("DET-001", captor.getValue().getExpenseDetailDesign());
        assertEquals("PREPAY_UNBILLED", captor.getValue().getExpenseDetailModeDefault());
    }

    @Test
    void createFlowRejectsBlankName() throws Exception {
        mockMvc.perform(post("/auth/process-management/flows")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flowName": "",
                                  "nodes": [],
                                  "routes": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(processManagementService, accessControlService);
    }

    @Test
    void listFormDesignsReturnsDataAndChecksPermission() throws Exception {
        ProcessFormDesignSummaryVO formDesign = new ProcessFormDesignSummaryVO();
        formDesign.setId(8L);
        formDesign.setFormCode("FD202603280001");
        formDesign.setFormName("Form Alpha");
        formDesign.setTemplateType("report");

        doNothing().when(accessControlService).requirePermission(1L, "expense:process_management:view");
        when(processManagementService.listFormDesigns("report")).thenReturn(List.of(formDesign));

        mockMvc.perform(get("/auth/process-management/form-designs")
                        .requestAttr("currentUserId", 1L)
                        .param("templateType", "report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].formCode").value("FD202603280001"))
                .andExpect(jsonPath("$.data[0].formName").value("Form Alpha"));

        verify(accessControlService).requirePermission(1L, "expense:process_management:view");
        verify(processManagementService).listFormDesigns("report");
    }

    @Test
    void createFormDesignRejectsBlankName() throws Exception {
        mockMvc.perform(post("/auth/process-management/form-designs")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateType": "report",
                                  "formName": "",
                                  "schema": {
                                    "layoutMode": "TWO_COLUMN",
                                    "blocks": []
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(processManagementService, accessControlService);
    }

    @Test
    void publishFlowUsesAnyPermissionCheck() throws Exception {
        ProcessFlowDetailVO detail = new ProcessFlowDetailVO();
        detail.setId(10L);
        detail.setFlowCode("FLOW-010");
        detail.setFlowName("Flow Alpha");
        detail.setStatus("ENABLED");

        doNothing().when(accessControlService).requireAnyPermission(
                1L,
                "expense:process_management:create",
                "expense:process_management:edit",
                "expense:process_management:publish"
        );
        when(processManagementService.publishFlow(10L)).thenReturn(detail);

        mockMvc.perform(post("/auth/process-management/flows/10/publish").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.flowCode").value("FLOW-010"));

        verify(accessControlService).requireAnyPermission(
                1L,
                "expense:process_management:create",
                "expense:process_management:edit",
                "expense:process_management:publish"
        );
        verify(processManagementService).publishFlow(10L);
    }
}
