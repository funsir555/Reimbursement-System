package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.FinanceAccountSetTaskStatusVO;
import com.finex.auth.dto.FinanceAccountSetMetaVO;
import com.finex.auth.dto.FinanceAccountSetSummaryVO;
import com.finex.auth.dto.FinanceModuleBackupRecordVO;
import com.finex.auth.dto.FinanceModuleEnableMetaVO;
import com.finex.auth.dto.FinanceModuleEnableSummaryVO;
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

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
    void listAccountSetsRequiresPermissionAndReturnsPayload() throws Exception {
        FinanceAccountSetSummaryVO summary = new FinanceAccountSetSummaryVO();
        summary.setCompanyId("COMPANY_A");
        summary.setCompanyCode("COMP202604050001");
        summary.setCompanyName("广州测试公司");
        summary.setStatus("ACTIVE");
        summary.setStatusLabel("已启用");

        doNothing().when(accessControlService).requirePermission(1L, "finance:system_management:view");
        when(financeSystemManagementService.listAccountSets()).thenReturn(List.of(summary));

        mockMvc.perform(get("/auth/finance/system-management/account-sets").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].companyCode").value("COMP202604050001"))
                .andExpect(jsonPath("$.data[0].companyName").value("广州测试公司"));

        verify(accessControlService).requirePermission(1L, "finance:system_management:view");
        verify(financeSystemManagementService).listAccountSets();
    }

    @Test
    void getModuleEnablesRequiresViewPermissionAndReturnsPayload() throws Exception {
        FinanceModuleEnableSummaryVO summary = new FinanceModuleEnableSummaryVO();
        summary.setCompanyId("COMPANY_A");
        summary.setModuleCode("GENERAL_LEDGER");
        summary.setModuleName("总账");
        summary.setEnabled(true);
        summary.setImplemented(true);
        summary.setToggleAllowed(true);
        summary.setDisableAllowed(false);
        summary.setBackupAllowed(true);
        summary.setBackupRecordAllowed(true);
        summary.setClearAllowed(false);
        summary.setClearBlockedMessage("总账不支持清除数据");

        FinanceModuleEnableMetaVO meta = new FinanceModuleEnableMetaVO();
        meta.setCompanyId("COMPANY_A");
        meta.setCompanyName("广州测试公司");
        meta.setModules(List.of(summary));

        doNothing().when(accessControlService).requirePermission(1L, "finance:system_management:view");
        when(financeSystemManagementService.getModuleEnableMeta("COMPANY_A")).thenReturn(meta);

        mockMvc.perform(get("/auth/finance/system-management/module-enables")
                        .param("companyId", "COMPANY_A")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.companyId").value("COMPANY_A"))
                .andExpect(jsonPath("$.data.modules[0].moduleCode").value("GENERAL_LEDGER"))
                .andExpect(jsonPath("$.data.modules[0].moduleName").value("总账"))
                .andExpect(jsonPath("$.data.modules[0].enabled").value(true))
                .andExpect(jsonPath("$.data.modules[0].backupAllowed").value(true))
                .andExpect(jsonPath("$.data.modules[0].clearAllowed").value(false));

        verify(accessControlService).requirePermission(1L, "finance:system_management:view");
        verify(financeSystemManagementService).getModuleEnableMeta("COMPANY_A");
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

    @Test
    void toggleModuleEnableRequiresEnablePermissionAndReturnsPayload() throws Exception {
        FinanceModuleEnableSummaryVO summary = new FinanceModuleEnableSummaryVO();
        summary.setCompanyId("COMPANY_A");
        summary.setModuleCode("FIXED_ASSETS");
        summary.setModuleName("固定资产");
        summary.setEnabled(true);
        summary.setImplemented(true);
        summary.setToggleAllowed(true);
        summary.setDisableAllowed(true);

        FinanceModuleEnableMetaVO meta = new FinanceModuleEnableMetaVO();
        meta.setCompanyId("COMPANY_A");
        meta.setCompanyName("广州测试公司");
        meta.setModules(List.of(summary));

        doNothing().when(accessControlService).requirePermission(1L, "finance:system_management:enable");
        when(financeSystemManagementService.toggleModuleEnable(org.mockito.ArgumentMatchers.any())).thenReturn(meta);

        mockMvc.perform(post("/auth/finance/system-management/module-enables/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr("currentUserId", 1L)
                        .content("""
                                {
                                  "companyId": "COMPANY_A",
                                  "moduleCode": "FIXED_ASSETS",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("模块启停状态已更新"))
                .andExpect(jsonPath("$.data.modules[0].moduleCode").value("FIXED_ASSETS"))
                .andExpect(jsonPath("$.data.modules[0].enabled").value(true));

        verify(accessControlService).requirePermission(1L, "finance:system_management:enable");
        verify(financeSystemManagementService).toggleModuleEnable(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void toggleModuleEnableRejectsBlankCompanyId() throws Exception {
        mockMvc.perform(post("/auth/finance/system-management/module-enables/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr("currentUserId", 1L)
                        .content("""
                                {
                                  "companyId": "  ",
                                  "moduleCode": "GENERAL_LEDGER",
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("公司主体不能为空"));
    }

    @Test
    void backupModuleDataRequiresEnablePermissionAndReturnsPayload() throws Exception {
        FinanceModuleBackupRecordVO record = new FinanceModuleBackupRecordVO();
        record.setId(1L);
        record.setCompanyId("COMPANY_A");
        record.setModuleCode("GENERAL_LEDGER");
        record.setModuleName("总账");
        record.setBackupFileName("COMPANY_A-GENERAL_LEDGER-20260518_120000.sql");
        record.setBackupFilePath("C:/backup/COMPANY_A-GENERAL_LEDGER-20260518_120000.sql");
        record.setBackupStatus("SUCCESS");
        record.setBackupUserName("张会计");

        doNothing().when(accessControlService).requirePermission(1L, "finance:system_management:enable");
        when(financeSystemManagementService.backupModuleData(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(record);

        mockMvc.perform(post("/auth/finance/system-management/module-enables/backup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr("currentUserId", 1L)
                        .content("""
                                {
                                  "companyId": "COMPANY_A",
                                  "moduleCode": "GENERAL_LEDGER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("模块数据备份完成"))
                .andExpect(jsonPath("$.data.moduleCode").value("GENERAL_LEDGER"))
                .andExpect(jsonPath("$.data.backupFileName").value("COMPANY_A-GENERAL_LEDGER-20260518_120000.sql"));

        verify(accessControlService).requirePermission(1L, "finance:system_management:enable");
        verify(financeSystemManagementService).backupModuleData(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listModuleBackupRecordsRequiresViewPermissionAndReturnsPayload() throws Exception {
        FinanceModuleBackupRecordVO record = new FinanceModuleBackupRecordVO();
        record.setId(1L);
        record.setCompanyId("COMPANY_A");
        record.setModuleCode("FIXED_ASSETS");
        record.setModuleName("固定资产");
        record.setBackupFileName("COMPANY_A-FIXED_ASSETS-20260518_120000.sql");
        record.setBackupFilePath("C:/backup/COMPANY_A-FIXED_ASSETS-20260518_120000.sql");
        record.setBackupStatus("SUCCESS");
        record.setBackupUserName("张会计");

        doNothing().when(accessControlService).requirePermission(1L, "finance:system_management:view");
        when(financeSystemManagementService.listModuleBackupRecords("COMPANY_A", "FIXED_ASSETS")).thenReturn(List.of(record));

        mockMvc.perform(get("/auth/finance/system-management/module-enables/backup-records")
                        .param("companyId", "COMPANY_A")
                        .param("moduleCode", "FIXED_ASSETS")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].moduleCode").value("FIXED_ASSETS"))
                .andExpect(jsonPath("$.data[0].backupUserName").value("张会计"));

        verify(accessControlService).requirePermission(1L, "finance:system_management:view");
        verify(financeSystemManagementService).listModuleBackupRecords("COMPANY_A", "FIXED_ASSETS");
    }

    @Test
    void clearModuleDataRequiresEnablePermissionAndReturnsPayload() throws Exception {
        FinanceModuleEnableSummaryVO summary = new FinanceModuleEnableSummaryVO();
        summary.setCompanyId("COMPANY_A");
        summary.setModuleCode("FIXED_ASSETS");
        summary.setModuleName("固定资产");
        summary.setEnabled(true);
        summary.setImplemented(true);
        summary.setToggleAllowed(true);
        summary.setDisableAllowed(true);
        summary.setBackupAllowed(true);
        summary.setBackupRecordAllowed(true);
        summary.setClearAllowed(true);

        FinanceModuleEnableMetaVO meta = new FinanceModuleEnableMetaVO();
        meta.setCompanyId("COMPANY_A");
        meta.setModules(List.of(summary));

        doNothing().when(accessControlService).requirePermission(1L, "finance:system_management:enable");
        when(financeSystemManagementService.clearModuleData(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(meta);

        mockMvc.perform(post("/auth/finance/system-management/module-enables/clear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr("currentUserId", 1L)
                        .content("""
                                {
                                  "companyId": "COMPANY_A",
                                  "moduleCode": "FIXED_ASSETS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("模块数据已清除"))
                .andExpect(jsonPath("$.data.modules[0].moduleCode").value("FIXED_ASSETS"));

        verify(accessControlService).requirePermission(1L, "finance:system_management:enable");
        verify(financeSystemManagementService).clearModuleData(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createAccountSetReturnsChineseBusinessMessageWhenTemplateDisabled() throws Exception {
        doNothing().when(accessControlService).requirePermission(1L, "finance:system_management:create");
        doThrow(new IllegalStateException("账套模板已停用"))
                .when(financeSystemManagementService)
                .submitCreateTask(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any());

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
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("账套模板已停用"));
    }
}
