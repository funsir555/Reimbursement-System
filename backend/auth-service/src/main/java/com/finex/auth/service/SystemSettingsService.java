// 业务域：系统设置
// 文件角色：service 接口
// 上下游关系：上游通常来自 组织、角色、公司信息和基础设置页面，下游会继续协调 公司、组织、角色、同步任务和系统参数。
// 风险提醒：改坏后最容易影响 权限体系、组织架构和历史单据可用性。

package com.finex.auth.service;

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

import java.util.List;

/**
 * SystemSettingsService：service 接口。
 * 定义系统系统设置这块对外提供的业务入口能力。
 * 改这里时，要特别关注 权限体系、组织架构和历史单据可用性是否会被一起带坏。
 */
public interface SystemSettingsService {

    /**
     * 获取初始化。
     */
    SystemSettingsBootstrapVO getBootstrap(Long currentUserId);

    /**
     * 查询Departments列表。
     */
    List<DepartmentTreeNodeVO> listDepartments();

    /**
     * 创建Department。
     */
    DepartmentTreeNodeVO createDepartment(DepartmentSaveDTO dto);

    /**
     * 更新Department。
     */
    DepartmentTreeNodeVO updateDepartment(Long id, DepartmentSaveDTO dto);

    /**
     * 删除Department。
     */
    Boolean deleteDepartment(Long id);

    /**
     * 查询Employees列表。
     */
    List<EmployeeVO> listEmployees(EmployeeQueryDTO query);

    /**
     * 创建Employee。
     */
    EmployeeVO createEmployee(EmployeeSaveDTO dto);

    /**
     * 更新Employee。
     */
    EmployeeVO updateEmployee(Long id, EmployeeSaveDTO dto);

    /**
     * 删除Employee。
     */
    Boolean deleteEmployee(Long id);

    /**
     * 查询角色列表。
     */
    List<RoleVO> listRoles();

    /**
     * 创建角色。
     */
    RoleVO createRole(RoleSaveDTO dto);

    /**
     * 更新角色。
     */
    RoleVO updateRole(Long id, RoleSaveDTO dto);

    /**
     * 删除角色。
     */
    Boolean deleteRole(Long id);

    Boolean assignRolePermissions(Long roleId, RolePermissionAssignDTO dto, Long currentUserId);

    Boolean assignUserRoles(Long userId, UserRoleAssignDTO dto);

    /**
     * 获取权限Tree。
     */
    List<PermissionTreeNodeVO> getPermissionTree();

    /**
     * 查询Companies列表。
     */
    List<CompanyVO> listCompanies();

    /**
     * 创建公司。
     */
    CompanyVO createCompany(CompanySaveDTO dto);

    /**
     * 更新公司。
     */
    CompanyVO updateCompany(String companyId, CompanySaveDTO dto);

    /**
     * 删除公司。
     */
    Boolean deleteCompany(String companyId);

    /**
     * 查询公司银行账户列表。
     */
    List<CompanyBankAccountVO> listCompanyBankAccounts();

    /**
     * 创建公司银行账户。
     */
    CompanyBankAccountVO createCompanyBankAccount(CompanyBankAccountSaveDTO dto);

    /**
     * 更新公司银行账户。
     */
    CompanyBankAccountVO updateCompanyBankAccount(Long id, CompanyBankAccountSaveDTO dto);

    /**
     * 删除公司银行账户。
     */
    Boolean deleteCompanyBankAccount(Long id);

    /**
     * 查询同步Connectors列表。
     */
    List<SyncConnectorVO> listSyncConnectors();

    /**
     * 更新同步Connector。
     */
    SyncConnectorVO updateSyncConnector(SyncConnectorSaveDTO dto);

    /**
     * 查询同步Jobs列表。
     */
    List<SyncJobVO> listSyncJobs();

    /**
     * 执行同步。
     */
    SyncJobVO runSync(SyncRunDTO dto, String operator);

    /**
     * 执行Due同步Jobs。
     */
    void runDueSyncJobs();
}
