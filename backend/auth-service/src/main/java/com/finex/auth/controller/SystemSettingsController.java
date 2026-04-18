// 这里是 SystemSettingsController 的后端接口入口。
// 它主要负责接收请求、校验权限并调用下游 Service。
// 如果改错，最容易影响这一组接口的查询、保存或状态流转。

package com.finex.auth.controller;

import com.finex.auth.dto.CompanyBankAccountSaveDTO;
import com.finex.auth.dto.CompanyBankAccountVO;
import com.finex.auth.dto.CompanySaveDTO;
import com.finex.auth.dto.CompanyVO;
import com.finex.auth.dto.DepartmentSaveDTO;
import com.finex.auth.dto.DepartmentTreeNodeVO;
import com.finex.auth.dto.EmployeeQueryDTO;
import com.finex.auth.dto.EmployeeSaveDTO;
import com.finex.auth.dto.EmployeeVO;
import com.finex.auth.dto.PermissionTreeNodeVO;
import com.finex.auth.dto.RolePermissionAssignDTO;
import com.finex.auth.dto.RoleSaveDTO;
import com.finex.auth.dto.RoleVO;
import com.finex.auth.dto.SyncConnectorSaveDTO;
import com.finex.auth.dto.SyncConnectorVO;
import com.finex.auth.dto.SyncJobVO;
import com.finex.auth.dto.SyncRunDTO;
import com.finex.auth.dto.SystemSettingsBootstrapVO;
import com.finex.auth.dto.UserRoleAssignDTO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.SystemSettingsService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 这是 SystemSettingsController 控制器。
 * 它主要负责接收请求、校验权限并调用下游 Service。
 * 具体业务规则以 Service 层为准。
 */
@RestController
@RequestMapping("/auth/system-settings")
@RequiredArgsConstructor
public class SystemSettingsController {

    private static final String SETTINGS_MENU = "settings:menu";
    private static final String ORG_VIEW = "settings:organization:view";
    private static final String ORG_CREATE = "settings:organization:create";
    private static final String ORG_EDIT = "settings:organization:edit";
    private static final String ORG_DELETE = "settings:organization:delete";
    private static final String ORG_SYNC_CONFIG = "settings:organization:sync_config";
    private static final String ORG_SYNC_RUN = "settings:organization:run_sync";
    private static final String EMP_VIEW = "settings:employees:view";
    private static final String EMP_CREATE = "settings:employees:create";
    private static final String EMP_EDIT = "settings:employees:edit";
    private static final String EMP_DELETE = "settings:employees:delete";
    private static final String ROLE_VIEW = "settings:roles:view";
    private static final String ROLE_CREATE = "settings:roles:create";
    private static final String ROLE_EDIT = "settings:roles:edit";
    private static final String ROLE_DELETE = "settings:roles:delete";
    private static final String ROLE_ASSIGN_PERMISSION = "settings:roles:assign_permissions";
    private static final String ROLE_ASSIGN_USER = "settings:roles:assign_users";
    private static final String COMPANY_VIEW = "settings:companies:view";
    private static final String COMPANY_CREATE = "settings:companies:create";
    private static final String COMPANY_EDIT = "settings:companies:edit";
    private static final String COMPANY_DELETE = "settings:companies:delete";
    private static final String COMPANY_ACCOUNT_VIEW = "settings:company_accounts:view";
    private static final String COMPANY_ACCOUNT_CREATE = "settings:company_accounts:create";
    private static final String COMPANY_ACCOUNT_EDIT = "settings:company_accounts:edit";
    private static final String COMPANY_ACCOUNT_DELETE = "settings:company_accounts:delete";

    private final SystemSettingsService systemSettingsService;
    private final AccessControlService accessControlService;

    // 处理 bootstrap 请求。
    @GetMapping("/bootstrap")
    public Result<SystemSettingsBootstrapVO> bootstrap(HttpServletRequest request) {
        accessControlService.requireAnyPermission(
                getCurrentUserId(request),
                SETTINGS_MENU, ORG_VIEW, EMP_VIEW, ROLE_VIEW, COMPANY_VIEW, COMPANY_ACCOUNT_VIEW
        );
        return Result.success(systemSettingsService.getBootstrap(getCurrentUserId(request)));
    }

    // 处理 departments 请求。
    @GetMapping("/departments")
    public Result<List<DepartmentTreeNodeVO>> departments(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SETTINGS_MENU, ORG_VIEW);
        return Result.success(systemSettingsService.listDepartments());
    }

    // 处理 createDepartment 请求。
    @PostMapping("/departments")
    public Result<DepartmentTreeNodeVO> createDepartment(
            @Valid @RequestBody DepartmentSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), ORG_CREATE);
        return Result.success("部门创建成功", systemSettingsService.createDepartment(dto));
    }

    // 处理 updateDepartment 请求。
    @PutMapping("/departments/{id}")
    public Result<DepartmentTreeNodeVO> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), ORG_EDIT);
        return Result.success("部门更新成功", systemSettingsService.updateDepartment(id, dto));
    }

    // 处理 deleteDepartment 请求。
    @DeleteMapping("/departments/{id}")
    public Result<Boolean> deleteDepartment(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), ORG_DELETE);
        return Result.success("部门删除成功", systemSettingsService.deleteDepartment(id));
    }

    // 处理 employees 请求。
    @PostMapping("/employees/query")
    public Result<List<EmployeeVO>> employees(@RequestBody(required = false) EmployeeQueryDTO query, HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SETTINGS_MENU, EMP_VIEW);
        return Result.success(systemSettingsService.listEmployees(query == null ? new EmployeeQueryDTO() : query));
    }

    // 处理 createEmployee 请求。
    @PostMapping("/employees")
    public Result<EmployeeVO> createEmployee(@Valid @RequestBody EmployeeSaveDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), EMP_CREATE);
        return Result.success("员工创建成功", systemSettingsService.createEmployee(dto));
    }

    // 处理 updateEmployee 请求。
    @PutMapping("/employees/{id}")
    public Result<EmployeeVO> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), EMP_EDIT);
        return Result.success("员工更新成功", systemSettingsService.updateEmployee(id, dto));
    }

    // 处理 deleteEmployee 请求。
    @DeleteMapping("/employees/{id}")
    public Result<Boolean> deleteEmployee(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), EMP_DELETE);
        return Result.success("员工删除成功", systemSettingsService.deleteEmployee(id));
    }

    // 处理 roles 请求。
    @GetMapping("/roles")
    public Result<List<RoleVO>> roles(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SETTINGS_MENU, ROLE_VIEW);
        return Result.success(systemSettingsService.listRoles());
    }

    // 处理 createRole 请求。
    @PostMapping("/roles")
    public Result<RoleVO> createRole(@Valid @RequestBody RoleSaveDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), ROLE_CREATE);
        return Result.success("角色创建成功", systemSettingsService.createRole(dto));
    }

    // 处理 updateRole 请求。
    @PutMapping("/roles/{id}")
    public Result<RoleVO> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), ROLE_EDIT);
        return Result.success("角色更新成功", systemSettingsService.updateRole(id, dto));
    }

    // 处理 deleteRole 请求。
    @DeleteMapping("/roles/{id}")
    public Result<Boolean> deleteRole(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), ROLE_DELETE);
        return Result.success("角色删除成功", systemSettingsService.deleteRole(id));
    }

    // 处理 assignRolePermissions 请求。
    @PostMapping("/roles/{id}/permissions")
    public Result<Boolean> assignRolePermissions(
            @PathVariable Long id,
            @RequestBody RolePermissionAssignDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, ROLE_ASSIGN_PERMISSION);
        return Result.success("角色权限分配成功", systemSettingsService.assignRolePermissions(id, dto, currentUserId));
    }

    // 处理 assignUserRoles 请求。
    @PostMapping("/users/{id}/roles")
    public Result<Boolean> assignUserRoles(
            @PathVariable Long id,
            @RequestBody UserRoleAssignDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), ROLE_ASSIGN_USER);
        return Result.success("用户角色分配成功", systemSettingsService.assignUserRoles(id, dto));
    }

    // 处理 permissionTree 请求。
    @GetMapping("/permissions/tree")
    public Result<List<PermissionTreeNodeVO>> permissionTree(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SETTINGS_MENU, ROLE_VIEW);
        return Result.success(systemSettingsService.getPermissionTree());
    }

    // 处理 companies 请求。
    @GetMapping("/companies")
    public Result<List<CompanyVO>> companies(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SETTINGS_MENU, COMPANY_VIEW);
        return Result.success(systemSettingsService.listCompanies());
    }

    // 处理 createCompany 请求。
    @PostMapping("/companies")
    public Result<CompanyVO> createCompany(@Valid @RequestBody CompanySaveDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), COMPANY_CREATE);
        return Result.success("公司创建成功", systemSettingsService.createCompany(dto));
    }

    // 处理 updateCompany 请求。
    @PutMapping("/companies/{companyId}")
    public Result<CompanyVO> updateCompany(
            @PathVariable String companyId,
            @Valid @RequestBody CompanySaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), COMPANY_EDIT);
        return Result.success("公司更新成功", systemSettingsService.updateCompany(companyId, dto));
    }

    // 处理 deleteCompany 请求。
    @DeleteMapping("/companies/{companyId}")
    public Result<Boolean> deleteCompany(@PathVariable String companyId, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), COMPANY_DELETE);
        return Result.success("公司删除成功", systemSettingsService.deleteCompany(companyId));
    }

    // 处理 companyBankAccounts 请求。
    @GetMapping("/company-bank-accounts")
    public Result<List<CompanyBankAccountVO>> companyBankAccounts(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SETTINGS_MENU, COMPANY_ACCOUNT_VIEW);
        return Result.success(systemSettingsService.listCompanyBankAccounts());
    }

    // 处理 createCompanyBankAccount 请求。
    @PostMapping("/company-bank-accounts")
    public Result<CompanyBankAccountVO> createCompanyBankAccount(
            @Valid @RequestBody CompanyBankAccountSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), COMPANY_ACCOUNT_CREATE);
        return Result.success("公司银行账户已新增", systemSettingsService.createCompanyBankAccount(dto));
    }

    // 处理 updateCompanyBankAccount 请求。
    @PutMapping("/company-bank-accounts/{id}")
    public Result<CompanyBankAccountVO> updateCompanyBankAccount(
            @PathVariable Long id,
            @Valid @RequestBody CompanyBankAccountSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), COMPANY_ACCOUNT_EDIT);
        return Result.success("公司银行账户已更新", systemSettingsService.updateCompanyBankAccount(id, dto));
    }

    // 处理 deleteCompanyBankAccount 请求。
    @DeleteMapping("/company-bank-accounts/{id}")
    public Result<Boolean> deleteCompanyBankAccount(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), COMPANY_ACCOUNT_DELETE);
        return Result.success("公司银行账户已删除", systemSettingsService.deleteCompanyBankAccount(id));
    }

    // 处理 syncConnectors 请求。
    @GetMapping("/sync/connectors")
    public Result<List<SyncConnectorVO>> syncConnectors(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SETTINGS_MENU, ORG_VIEW, ORG_SYNC_CONFIG);
        return Result.success(systemSettingsService.listSyncConnectors());
    }

    // 处理 updateSyncConnector 请求。
    @PutMapping("/sync/connectors")
    public Result<SyncConnectorVO> updateSyncConnector(
            @Valid @RequestBody SyncConnectorSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), ORG_SYNC_CONFIG);
        return Result.success("同步连接配置已保存", systemSettingsService.updateSyncConnector(dto));
    }

    // 处理 runSync 请求。
    @PostMapping("/sync/run")
    public Result<SyncJobVO> runSync(@RequestBody(required = false) SyncRunDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), ORG_SYNC_RUN);
        SyncRunDTO payload = dto == null ? new SyncRunDTO() : dto;
        return Result.success(
                "组织同步任务已执行",
                systemSettingsService.runSync(payload, getCurrentUsername(request))
        );
    }

    // 处理 syncJobs 请求。
    @GetMapping("/sync/jobs")
    public Result<List<SyncJobVO>> syncJobs(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SETTINGS_MENU, ORG_VIEW, ORG_SYNC_RUN);
        return Result.success(systemSettingsService.listSyncJobs());
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId instanceof Long value) {
            return value;
        }
        if (userId instanceof Integer value) {
            return value.longValue();
        }
        throw new SecurityException("无法识别当前用户");
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "系统管理员";
    }
}
