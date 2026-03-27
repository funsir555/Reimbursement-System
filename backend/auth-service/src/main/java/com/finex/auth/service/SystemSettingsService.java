package com.finex.auth.service;

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

import java.util.List;

public interface SystemSettingsService {

    SystemSettingsBootstrapVO getBootstrap(Long currentUserId);

    List<DepartmentTreeNodeVO> listDepartments();

    DepartmentTreeNodeVO createDepartment(DepartmentSaveDTO dto);

    DepartmentTreeNodeVO updateDepartment(Long id, DepartmentSaveDTO dto);

    Boolean deleteDepartment(Long id);

    List<EmployeeVO> listEmployees(EmployeeQueryDTO query);

    EmployeeVO createEmployee(EmployeeSaveDTO dto);

    EmployeeVO updateEmployee(Long id, EmployeeSaveDTO dto);

    Boolean deleteEmployee(Long id);

    List<RoleVO> listRoles();

    RoleVO createRole(RoleSaveDTO dto);

    RoleVO updateRole(Long id, RoleSaveDTO dto);

    Boolean deleteRole(Long id);

    Boolean assignRolePermissions(Long roleId, RolePermissionAssignDTO dto);

    Boolean assignUserRoles(Long userId, UserRoleAssignDTO dto);

    List<PermissionTreeNodeVO> getPermissionTree();

    List<CompanyVO> listCompanies();

    CompanyVO createCompany(CompanySaveDTO dto);

    CompanyVO updateCompany(String companyId, CompanySaveDTO dto);

    Boolean deleteCompany(String companyId);

    List<SyncConnectorVO> listSyncConnectors();

    SyncConnectorVO updateSyncConnector(SyncConnectorSaveDTO dto);

    List<SyncJobVO> listSyncJobs();

    SyncJobVO runSync(SyncRunDTO dto, String operator);

    void runDueSyncJobs();
}
