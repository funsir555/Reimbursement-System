package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceAccountSetTaskStatusVO;
import com.finex.auth.dto.FinanceAccountSetMetaVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceSystemManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceSystemManagementControllerTest {

    @Mock
    private FinanceSystemManagementService financeSystemManagementService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new FinanceSystemManagementController(financeSystemManagementService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void metaRequiresPermissionAndReturnsPayload() throws Exception {
        FinanceAccountSetMetaVO meta = new FinanceAccountSetMetaVO();
        meta.setDefaultSubjectCodeScheme("4-2-2-2");

        doNothing().when(accessControlService).requirePermission(1L, "finance:system_management:view");
        when(financeSystemManagementService.getMeta()).thenReturn(meta);

        mockMvc.perform(get("/auth/finance/system-management/meta").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.defaultSubjectCodeScheme").value("4-2-2-2"));

        verify(accessControlService).requirePermission(1L, "finance:system_management:view");
        verify(financeSystemManagementService).getMeta();
    }

    @Test
    void createAccountSetAcceptsValidYearMonth() throws Exception {
        FinanceAccountSetTaskStatusVO taskStatus = new FinanceAccountSetTaskStatusVO();
        taskStatus.setTaskNo("FAS202604080001");
        taskStatus.setStatus("PENDING");
        taskStatus.setProgress(0);
        taskStatus.setFinished(false);

        doNothing().when(accessControlService).requirePermission(1L, "finance:system_management:create");
        when(financeSystemManagementService.submitCreateTask(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(taskStatus);

        mockMvc.perform(post("/auth/finance/system-management/account-sets/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr("currentUserId", 1L)
                        .content("""
                                {
                                  "createMode": "BLANK",
                                  "targetCompanyId": "COMPANY_A",
                                  "enabledYearMonth": "2022-11",
                                  "templateCode": "AS_2007_ENTERPRISE",
                                  "supervisorUserId": 2,
                                  "subjectCodeScheme": "4-2-2-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskNo").value("FAS202604080001"));

        verify(accessControlService).requirePermission(1L, "finance:system_management:create");
        verify(financeSystemManagementService).submitCreateTask(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createAccountSetRejectsInvalidYearMonth() throws Exception {
        mockMvc.perform(post("/auth/finance/system-management/account-sets/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr("currentUserId", 1L)
                        .content("""
                                {
                                  "createMode": "BLANK",
                                  "targetCompanyId": "COMPANY_A",
                                  "enabledYearMonth": "2022/11",
                                  "templateCode": "AS_2007_ENTERPRISE",
                                  "supervisorUserId": 2,
                                  "subjectCodeScheme": "4-2-2-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("启用年月格式必须为 YYYY-MM"));
    }
}
