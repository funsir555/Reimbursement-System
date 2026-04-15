// 业务域：系统设置
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 组织、角色、公司信息和基础设置页面，下游会继续协调 公司、组织、角色、同步任务和系统参数。
// 风险提醒：改坏后最容易影响 权限体系、组织架构和历史单据可用性。

package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.finex.auth.mapper.SystemCompanyBankAccountMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.SystemPermissionMapper;
import com.finex.auth.mapper.SystemRoleMapper;
import com.finex.auth.mapper.SystemRolePermissionMapper;
import com.finex.auth.mapper.SystemSyncConnectorMapper;
import com.finex.auth.mapper.SystemSyncJobDetailMapper;
import com.finex.auth.mapper.SystemSyncJobMapper;
import com.finex.auth.mapper.SystemUserRoleMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.SystemSettingsService;
import com.finex.auth.service.UserService;
import com.finex.auth.service.impl.settings.SettingsBootstrapSupport;
import com.finex.auth.service.impl.settings.SettingsCompanyDomainSupport;
import com.finex.auth.service.impl.settings.SettingsOrganizationDomainSupport;
import com.finex.auth.service.impl.settings.SettingsRoleDomainSupport;
import com.finex.auth.service.impl.settings.SettingsSyncDomainSupport;
import com.finex.auth.support.systemsettings.OrganizationSyncAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SystemSettingsServiceImpl：service 入口实现。
 * 接住上层请求，并把 系统系统设置相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 权限体系、组织架构和历史单据可用性是否会被一起带坏。
 */
@Service
public class SystemSettingsServiceImpl implements SystemSettingsService {

    private final SettingsBootstrapSupport settingsBootstrapSupport;
    private final SettingsOrganizationDomainSupport settingsOrganizationDomainSupport;
    private final SettingsRoleDomainSupport settingsRoleDomainSupport;
    private final SettingsCompanyDomainSupport settingsCompanyDomainSupport;
    private final SettingsSyncDomainSupport settingsSyncDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public SystemSettingsServiceImpl(
            UserService userService,
            AccessControlService accessControlService,
            UserMapper userMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            SystemCompanyBankAccountMapper systemCompanyBankAccountMapper,
            SystemCompanyMapper systemCompanyMapper,
            SystemRoleMapper systemRoleMapper,
            SystemPermissionMapper systemPermissionMapper,
            SystemRolePermissionMapper systemRolePermissionMapper,
            SystemUserRoleMapper systemUserRoleMapper,
            SystemSyncConnectorMapper systemSyncConnectorMapper,
            SystemSyncJobMapper systemSyncJobMapper,
            SystemSyncJobDetailMapper systemSyncJobDetailMapper,
            ObjectMapper objectMapper,
            List<OrganizationSyncAdapter> syncAdapters
    ) {
        this.settingsBootstrapSupport = new SettingsBootstrapSupport(
                userService,
                accessControlService,
                userMapper,
                systemDepartmentMapper,
                systemCompanyBankAccountMapper,
                systemCompanyMapper,
                systemRoleMapper,
                systemPermissionMapper,
                systemRolePermissionMapper,
                systemUserRoleMapper,
                systemSyncConnectorMapper,
                systemSyncJobMapper,
                systemSyncJobDetailMapper,
                objectMapper,
                syncAdapters
        );
        this.settingsOrganizationDomainSupport = new SettingsOrganizationDomainSupport(
                userService,
                accessControlService,
                userMapper,
                systemDepartmentMapper,
                systemCompanyBankAccountMapper,
                systemCompanyMapper,
                systemRoleMapper,
                systemPermissionMapper,
                systemRolePermissionMapper,
                systemUserRoleMapper,
                systemSyncConnectorMapper,
                systemSyncJobMapper,
                systemSyncJobDetailMapper,
                objectMapper,
                syncAdapters
        );
        this.settingsRoleDomainSupport = new SettingsRoleDomainSupport(
                userService,
                accessControlService,
                userMapper,
                systemDepartmentMapper,
                systemCompanyBankAccountMapper,
                systemCompanyMapper,
                systemRoleMapper,
                systemPermissionMapper,
                systemRolePermissionMapper,
                systemUserRoleMapper,
                systemSyncConnectorMapper,
                systemSyncJobMapper,
                systemSyncJobDetailMapper,
                objectMapper,
                syncAdapters
        );
        this.settingsCompanyDomainSupport = new SettingsCompanyDomainSupport(
                userService,
                accessControlService,
                userMapper,
                systemDepartmentMapper,
                systemCompanyBankAccountMapper,
                systemCompanyMapper,
                systemRoleMapper,
                systemPermissionMapper,
                systemRolePermissionMapper,
                systemUserRoleMapper,
                systemSyncConnectorMapper,
                systemSyncJobMapper,
                systemSyncJobDetailMapper,
                objectMapper,
                syncAdapters
        );
        this.settingsSyncDomainSupport = new SettingsSyncDomainSupport(
                userService,
                accessControlService,
                userMapper,
                systemDepartmentMapper,
                systemCompanyBankAccountMapper,
                systemCompanyMapper,
                systemRoleMapper,
                systemPermissionMapper,
                systemRolePermissionMapper,
                systemUserRoleMapper,
                systemSyncConnectorMapper,
                systemSyncJobMapper,
                systemSyncJobDetailMapper,
                objectMapper,
                syncAdapters
        );
    }

    /**
     * 获取初始化。
     */
    @Override
    public SystemSettingsBootstrapVO getBootstrap(Long currentUserId) {
        return settingsBootstrapSupport.getBootstrap(currentUserId);
    }

    /**
     * 查询Departments列表。
     */
    @Override
    public List<DepartmentTreeNodeVO> listDepartments() {
        return settingsOrganizationDomainSupport.listDepartments();
    }

    /**
     * 创建Department。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentTreeNodeVO createDepartment(DepartmentSaveDTO dto) {
        return settingsOrganizationDomainSupport.createDepartment(dto);
    }

    /**
     * 更新Department。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentTreeNodeVO updateDepartment(Long id, DepartmentSaveDTO dto) {
        return settingsOrganizationDomainSupport.updateDepartment(id, dto);
    }

    /**
     * 删除Department。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDepartment(Long id) {
        return settingsOrganizationDomainSupport.deleteDepartment(id);
    }

    /**
     * 查询Employees列表。
     */
    @Override
    public List<EmployeeVO> listEmployees(EmployeeQueryDTO query) {
        return settingsOrganizationDomainSupport.listEmployees(query);
    }

    /**
     * 创建Employee。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeVO createEmployee(EmployeeSaveDTO dto) {
        return settingsOrganizationDomainSupport.createEmployee(dto);
    }

    /**
     * 更新Employee。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeVO updateEmployee(Long id, EmployeeSaveDTO dto) {
        return settingsOrganizationDomainSupport.updateEmployee(id, dto);
    }

    /**
     * 删除Employee。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteEmployee(Long id) {
        return settingsOrganizationDomainSupport.deleteEmployee(id);
    }

    /**
     * 查询角色列表。
     */
    @Override
    public List<RoleVO> listRoles() {
        return settingsRoleDomainSupport.listRoles();
    }

    /**
     * 创建角色。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO createRole(RoleSaveDTO dto) {
        return settingsRoleDomainSupport.createRole(dto);
    }

    /**
     * 更新角色。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO updateRole(Long id, RoleSaveDTO dto) {
        return settingsRoleDomainSupport.updateRole(id, dto);
    }

    /**
     * 删除角色。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteRole(Long id) {
        return settingsRoleDomainSupport.deleteRole(id);
    }

    /**
     * 处理系统系统设置中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignRolePermissions(Long roleId, RolePermissionAssignDTO dto, Long currentUserId) {
        return settingsRoleDomainSupport.assignRolePermissions(roleId, dto, currentUserId);
    }

    /**
     * 处理系统系统设置中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignUserRoles(Long userId, UserRoleAssignDTO dto) {
        return settingsRoleDomainSupport.assignUserRoles(userId, dto);
    }

    /**
     * 获取权限Tree。
     */
    @Override
    public List<PermissionTreeNodeVO> getPermissionTree() {
        return settingsRoleDomainSupport.getPermissionTree();
    }

    /**
     * 查询Companies列表。
     */
    @Override
    public List<CompanyVO> listCompanies() {
        return settingsCompanyDomainSupport.listCompanies();
    }

    /**
     * 创建公司。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyVO createCompany(CompanySaveDTO dto) {
        return settingsCompanyDomainSupport.createCompany(dto);
    }

    /**
     * 更新公司。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyVO updateCompany(String companyId, CompanySaveDTO dto) {
        return settingsCompanyDomainSupport.updateCompany(companyId, dto);
    }

    /**
     * 删除公司。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCompany(String companyId) {
        return settingsCompanyDomainSupport.deleteCompany(companyId);
    }

    /**
     * 查询公司银行账户列表。
     */
    @Override
    public List<CompanyBankAccountVO> listCompanyBankAccounts() {
        return settingsCompanyDomainSupport.listCompanyBankAccounts();
    }

    /**
     * 创建公司银行账户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyBankAccountVO createCompanyBankAccount(CompanyBankAccountSaveDTO dto) {
        return settingsCompanyDomainSupport.createCompanyBankAccount(dto);
    }

    /**
     * 更新公司银行账户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyBankAccountVO updateCompanyBankAccount(Long id, CompanyBankAccountSaveDTO dto) {
        return settingsCompanyDomainSupport.updateCompanyBankAccount(id, dto);
    }

    /**
     * 删除公司银行账户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCompanyBankAccount(Long id) {
        return settingsCompanyDomainSupport.deleteCompanyBankAccount(id);
    }

    /**
     * 查询同步Connectors列表。
     */
    @Override
    public List<SyncConnectorVO> listSyncConnectors() {
        return settingsSyncDomainSupport.listSyncConnectors();
    }

    /**
     * 更新同步Connector。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SyncConnectorVO updateSyncConnector(SyncConnectorSaveDTO dto) {
        return settingsSyncDomainSupport.updateSyncConnector(dto);
    }

    /**
     * 查询同步Jobs列表。
     */
    @Override
    public List<SyncJobVO> listSyncJobs() {
        return settingsSyncDomainSupport.listSyncJobs();
    }

    /**
     * 执行同步。
     */
    @Override
    public SyncJobVO runSync(SyncRunDTO dto, String operator) {
        return settingsSyncDomainSupport.runSync(dto, operator);
    }

    /**
     * 执行Due同步Jobs。
     */
    @Override
    public void runDueSyncJobs() {
        settingsSyncDomainSupport.runDueSyncJobs();
    }
}
