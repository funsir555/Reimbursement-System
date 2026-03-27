package com.finex.auth.controller;

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

    private final SystemSettingsService systemSettingsService;
    private final AccessControlService accessControlService;

    @GetMapping("/bootstrap")
    public Result<SystemSettingsBootstrapVO> bootstrap(HttpServletRequest request) {
        accessControlService.requireAnyPermission(
                getCurrentUserId(request),
                SETTINGS_MENU, ORG_VIEW, EMP_VIEW, ROLE_VIEW, COMPANY_VIEW
        );
        return Result.success(systemSettingsService.getBootstrap(getCurrentUserId(request)));
    }

    @GetMapping("/departments")
    public Result<List<DepartmentTreeNodeVO>> departments(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SETTINGS_MENU, ORG_VIEW);
        return Result.success(systemSettingsService.listDepartments());
    }

    @PostMapping("/departments")
    public Result<DepartmentTreeNodeVO> createDepartment(
            @Valid @RequestBody DepartmentSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), ORG_CREATE);
        return Result.success("部门创建成功", systemSettingsService.createDepartment(dto));
    }

    @PutMapping("/departments/{id}")
    public Result<DepartmentTreeNodeVO> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), ORG_EDIT);
        return Result.success("部门更新成功", systemSettingsService.updateDepartment(id, dto));
    }

    @DeleteMapping("/departments/{id}")
    public Result<Boolean> deleteDepartment(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), ORG_DELETE);
        return Result.success("部门删除成功", systemSettingsService.deleteDepartment(id));
    }

    @PostMapping("/employees/query")
    public Result<List<EmployeeVO>> employees(@RequestBody(required = false) EmployeeQueryDTO query, HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SETTINGS_MENU, EMP_VIEW);
        return Result.success(systemSettingsService.listEmployees(query == null ? new EmployeeQueryDTO() : query));
    }

    @PostMapping("/employees")
    public Result<EmployeeVO> createEmployee(@Valid @RequestBody EmployeeSaveDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), EMP_CREATE);
        return Result.success("员工创建成功", systemSettingsService.createEmployee(dto));
    }

    @PutMapping("/employees/{id}")
    public Result<EmployeeVO> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), EMP_EDIT);
        return Result.success("员工更新成功", systemSettingsService.updateEmployee(id, dto));
    }

    @DeleteMapping("/employees/{id}")
    public Result<Boolean> deleteEmployee(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), EMP_DELETE);
        return Result.success("员工删除成功", systemSettingsService.deleteEmployee(id));
    }

    @GetMapping("/roles")
    public Result<List<RoleVO>> roles(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SETTINGS_MENU, ROLE_VIEW);
        return Result.success(systemSettingsService.listRoles());
    }

    @PostMapping("/roles")
    public Result<RoleVO> createRole(@Valid @RequestBody RoleSaveDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), ROLE_CREATE);
        return Result.success("角色创建成功", systemSettingsService.createRole(dto));
    }

    @PutMapping("/roles/{id}")
    public Result<RoleVO> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), ROLE_EDIT);
        return Result.success("角色更新成功", systemSettingsService.updateRole(id, dto));
    }

    @DeleteMapping("/roles/{id}")
    public Result<Boolean> deleteRole(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), ROLE_DELETE);
        return Result.success("角色删除成功", systemSettingsService.deleteRole(id));
    }

    @PostMapping("/roles/{id}/permissions")
    public Result<Boolean> assignRolePermissions(
            @PathVariable Long id,
            @RequestBody RolePermissionAssignDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), ROLE_ASSIGN_PERMISSION);
        return Result.success("角色权限分配成功", systemSettingsService.assignRolePermissions(id, dto));
    }

    @PostMapping("/users/{id}/roles")
    public Result<Boolean> assignUserRoles(
            @PathVariable Long id,
            @RequestBody UserRoleAssignDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), ROLE_ASSIGN_USER);
        return Result.success("用户角色分配成功", systemSettingsService.assignUserRoles(id, dto));
    }

    @GetMapping("/permissions/tree")
    public Result<List<PermissionTreeNodeVO>> permissionTree(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SETTINGS_MENU, ROLE_VIEW);
        return Result.success(systemSettingsService.getPermissionTree());
    }

    @GetMapping("/companies")
    public Result<List<CompanyVO>> companies(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SETTINGS_MENU, COMPANY_VIEW);
        return Result.success(systemSettingsService.listCompanies());
    }

    @PostMapping("/companies")
    public Result<CompanyVO> createCompany(@Valid @RequestBody CompanySaveDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), COMPANY_CREATE);
        return Result.success("公司创建成功", systemSettingsService.createCompany(dto));
    }

    @PutMapping("/companies/{companyId}")
    public Result<CompanyVO> updateCompany(
            @PathVariable String companyId,
            @Valid @RequestBody CompanySaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), COMPANY_EDIT);
        return Result.success("公司更新成功", systemSettingsService.updateCompany(companyId, dto));
    }

    @DeleteMapping("/companies/{companyId}")
    public Result<Boolean> deleteCompany(@PathVariable String companyId, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), COMPANY_DELETE);
        return Result.success("公司删除成功", systemSettingsService.deleteCompany(companyId));
    }

    @GetMapping("/sync/connectors")
    public Result<List<SyncConnectorVO>> syncConnectors(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SETTINGS_MENU, ORG_VIEW, ORG_SYNC_CONFIG);
        return Result.success(systemSettingsService.listSyncConnectors());
    }

    @PutMapping("/sync/connectors")
    public Result<SyncConnectorVO> updateSyncConnector(
            @Valid @RequestBody SyncConnectorSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), ORG_SYNC_CONFIG);
        return Result.success("同步连接配置已保存", systemSettingsService.updateSyncConnector(dto));
    }

    @PostMapping("/sync/run")
    public Result<SyncJobVO> runSync(@RequestBody(required = false) SyncRunDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), ORG_SYNC_RUN);
        SyncRunDTO payload = dto == null ? new SyncRunDTO() : dto;
        return Result.success(
                "组织同步任务已执行",
                systemSettingsService.runSync(payload, getCurrentUsername(request))
        );
    }

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
