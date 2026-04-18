package com.finex.auth.controller;

import com.finex.auth.config.GlobalExceptionHandler;
import com.finex.auth.dto.CompanyBankAccountVO;
import com.finex.auth.dto.CompanyVO;
import com.finex.auth.dto.DepartmentTreeNodeVO;
import com.finex.auth.dto.EmployeeVO;
import com.finex.auth.dto.RolePermissionAssignDTO;
import com.finex.auth.dto.SyncJobVO;
import com.finex.auth.dto.SystemSettingsBootstrapVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.SystemSettingsService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SystemSettingsControllerTest {

    @Mock
    private SystemSettingsService systemSettingsService;

    @Mock
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new SystemSettingsController(systemSettingsService, accessControlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void bootstrapReturnsDataAndChecksAnyPermission() throws Exception {
        SystemSettingsBootstrapVO bootstrap = new SystemSettingsBootstrapVO();
        DepartmentTreeNodeVO department = new DepartmentTreeNodeVO();
        department.setId(9L);
        department.setDeptName("Finance");
        bootstrap.setDepartments(List.of(department));

        doNothing().when(accessControlService).requireAnyPermission(1L,
                "settings:menu",
                "settings:organization:view",
                "settings:employees:view",
                "settings:roles:view",
                "settings:companies:view",
                "settings:company_accounts:view");
        when(systemSettingsService.getBootstrap(1L)).thenReturn(bootstrap);

        mockMvc.perform(get("/auth/system-settings/bootstrap").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.departments[0].deptName").value("Finance"));

        verify(systemSettingsService).getBootstrap(1L);
    }

    @Test
    void createDepartmentUsesExpectedPermissionAndPayload() throws Exception {
        DepartmentTreeNodeVO department = new DepartmentTreeNodeVO();
        department.setId(11L);
        department.setDeptName("Shared Service Center");

        doNothing().when(accessControlService).requirePermission(1L, "settings:organization:create");
        when(systemSettingsService.createDepartment(any())).thenReturn(department);

        mockMvc.perform(post("/auth/system-settings/departments")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deptName": "Shared Service Center",
                                  "companyId": "COMPANY001",
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(11));

        ArgumentCaptor<com.finex.auth.dto.DepartmentSaveDTO> captor = ArgumentCaptor.forClass(com.finex.auth.dto.DepartmentSaveDTO.class);
        verify(systemSettingsService).createDepartment(captor.capture());
        assertEquals("Shared Service Center", captor.getValue().getDeptName());
        assertEquals("COMPANY001", captor.getValue().getCompanyId());
    }

    @Test
    void employeesQueryReturnsDataAndChecksAnyPermission() throws Exception {
        EmployeeVO employee = new EmployeeVO();
        employee.setUserId(7L);
        employee.setUsername("zhangsan");
        employee.setName("张三");

        doNothing().when(accessControlService).requireAnyPermission(1L, "settings:menu", "settings:employees:view");
        when(systemSettingsService.listEmployees(any())).thenReturn(List.of(employee));

        mockMvc.perform(post("/auth/system-settings/employees/query")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "keyword": "张三"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].username").value("zhangsan"));
    }

    @Test
    void assignRolePermissionsUsesExpectedPermission() throws Exception {
        doNothing().when(accessControlService).requirePermission(1L, "settings:roles:assign_permissions");
        when(systemSettingsService.assignRolePermissions(eq(5L), any(RolePermissionAssignDTO.class), eq(1L))).thenReturn(Boolean.TRUE);

        mockMvc.perform(post("/auth/system-settings/roles/5/permissions")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permissionCodes": ["settings:roles:view", "settings:roles:edit"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        ArgumentCaptor<RolePermissionAssignDTO> captor = ArgumentCaptor.forClass(RolePermissionAssignDTO.class);
        verify(systemSettingsService).assignRolePermissions(eq(5L), captor.capture(), eq(1L));
        assertEquals(List.of("settings:roles:view", "settings:roles:edit"), captor.getValue().getPermissionCodes());
    }

    @Test
    void companiesReturnsDataAndChecksAnyPermission() throws Exception {
        CompanyVO company = new CompanyVO();
        company.setCompanyId("COMPANY001");
        company.setCompanyName("Finex Co");

        doNothing().when(accessControlService).requireAnyPermission(1L, "settings:menu", "settings:companies:view");
        when(systemSettingsService.listCompanies()).thenReturn(List.of(company));

        mockMvc.perform(get("/auth/system-settings/companies").requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].companyName").value("Finex Co"));
    }

    @Test
    void createCompanyBankAccountReturnsUnifiedMessage() throws Exception {
        CompanyBankAccountVO account = new CompanyBankAccountVO();
        account.setId(8L);
        account.setAccountName("测试公司账户");

        doNothing().when(accessControlService).requirePermission(1L, "settings:company_accounts:create");
        when(systemSettingsService.createCompanyBankAccount(any())).thenReturn(account);

        mockMvc.perform(post("/auth/system-settings/company-bank-accounts")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": "COMPANY001",
                                  "accountName": "测试公司账户",
                                  "accountNo": "6222020202020202",
                                  "bankCode": "ICBC",
                                  "bankName": "中国工商银行",
                                  "province": "上海市",
                                  "city": "上海市",
                                  "branchCode": "ICBC-SH-001",
                                  "branchName": "中国工商银行上海分行",
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("公司银行账户已新增"))
                .andExpect(jsonPath("$.data.id").value(8));
    }

    @Test
    void updateCompanyBankAccountReturnsUnifiedMessage() throws Exception {
        CompanyBankAccountVO account = new CompanyBankAccountVO();
        account.setId(8L);
        account.setAccountName("测试公司账户");

        doNothing().when(accessControlService).requirePermission(1L, "settings:company_accounts:edit");
        when(systemSettingsService.updateCompanyBankAccount(eq(8L), any())).thenReturn(account);

        mockMvc.perform(put("/auth/system-settings/company-bank-accounts/8")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": "COMPANY001",
                                  "accountName": "测试公司账户",
                                  "accountNo": "6222020202020202",
                                  "bankCode": "ICBC",
                                  "bankName": "中国工商银行",
                                  "province": "上海市",
                                  "city": "上海市",
                                  "branchCode": "ICBC-SH-001",
                                  "branchName": "中国工商银行上海分行",
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("公司银行账户已更新"))
                .andExpect(jsonPath("$.data.id").value(8));
    }

    @Test
    void deleteCompanyBankAccountReturnsUnifiedMessage() throws Exception {
        doNothing().when(accessControlService).requirePermission(1L, "settings:company_accounts:delete");
        when(systemSettingsService.deleteCompanyBankAccount(8L)).thenReturn(Boolean.TRUE);

        mockMvc.perform(delete("/auth/system-settings/company-bank-accounts/8")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("公司银行账户已删除"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void runSyncFallsBackToDefaultOperator() throws Exception {
        SyncJobVO job = new SyncJobVO();
        job.setJobNo("SYNC-001");
        job.setStatus("SUCCESS");

        doNothing().when(accessControlService).requirePermission(1L, "settings:organization:run_sync");
        when(systemSettingsService.runSync(any(), eq("系统管理员"))).thenReturn(job);

        mockMvc.perform(post("/auth/system-settings/sync/run")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformCodes": ["DINGTALK"],
                                  "triggerType": "MANUAL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jobNo").value("SYNC-001"));
    }

    @Test
    void createDepartmentRejectsBlankName() throws Exception {
        mockMvc.perform(post("/auth/system-settings/departments")
                        .requestAttr("currentUserId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deptName": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(systemSettingsService, accessControlService);
    }
}
