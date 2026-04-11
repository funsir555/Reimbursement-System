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

@Service
public class SystemSettingsServiceImpl implements SystemSettingsService {

    private final SettingsBootstrapSupport settingsBootstrapSupport;
    private final SettingsOrganizationDomainSupport settingsOrganizationDomainSupport;
    private final SettingsRoleDomainSupport settingsRoleDomainSupport;
    private final SettingsCompanyDomainSupport settingsCompanyDomainSupport;
    private final SettingsSyncDomainSupport settingsSyncDomainSupport;

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

    @Override
    public SystemSettingsBootstrapVO getBootstrap(Long currentUserId) {
        return settingsBootstrapSupport.getBootstrap(currentUserId);
    }

    @Override
    public List<DepartmentTreeNodeVO> listDepartments() {
        return settingsOrganizationDomainSupport.listDepartments();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentTreeNodeVO createDepartment(DepartmentSaveDTO dto) {
        return settingsOrganizationDomainSupport.createDepartment(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentTreeNodeVO updateDepartment(Long id, DepartmentSaveDTO dto) {
        return settingsOrganizationDomainSupport.updateDepartment(id, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDepartment(Long id) {
        return settingsOrganizationDomainSupport.deleteDepartment(id);
    }

    @Override
    public List<EmployeeVO> listEmployees(EmployeeQueryDTO query) {
        return settingsOrganizationDomainSupport.listEmployees(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeVO createEmployee(EmployeeSaveDTO dto) {
        return settingsOrganizationDomainSupport.createEmployee(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeVO updateEmployee(Long id, EmployeeSaveDTO dto) {
        return settingsOrganizationDomainSupport.updateEmployee(id, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteEmployee(Long id) {
        return settingsOrganizationDomainSupport.deleteEmployee(id);
    }

    @Override
    public List<RoleVO> listRoles() {
        return settingsRoleDomainSupport.listRoles();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO createRole(RoleSaveDTO dto) {
        return settingsRoleDomainSupport.createRole(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO updateRole(Long id, RoleSaveDTO dto) {
        return settingsRoleDomainSupport.updateRole(id, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteRole(Long id) {
        return settingsRoleDomainSupport.deleteRole(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignRolePermissions(Long roleId, RolePermissionAssignDTO dto, Long currentUserId) {
        return settingsRoleDomainSupport.assignRolePermissions(roleId, dto, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignUserRoles(Long userId, UserRoleAssignDTO dto) {
        return settingsRoleDomainSupport.assignUserRoles(userId, dto);
    }

    @Override
    public List<PermissionTreeNodeVO> getPermissionTree() {
        return settingsRoleDomainSupport.getPermissionTree();
    }

    @Override
    public List<CompanyVO> listCompanies() {
        return settingsCompanyDomainSupport.listCompanies();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyVO createCompany(CompanySaveDTO dto) {
        return settingsCompanyDomainSupport.createCompany(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyVO updateCompany(String companyId, CompanySaveDTO dto) {
        return settingsCompanyDomainSupport.updateCompany(companyId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCompany(String companyId) {
        return settingsCompanyDomainSupport.deleteCompany(companyId);
    }

    @Override
    public List<CompanyBankAccountVO> listCompanyBankAccounts() {
        return settingsCompanyDomainSupport.listCompanyBankAccounts();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyBankAccountVO createCompanyBankAccount(CompanyBankAccountSaveDTO dto) {
        return settingsCompanyDomainSupport.createCompanyBankAccount(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyBankAccountVO updateCompanyBankAccount(Long id, CompanyBankAccountSaveDTO dto) {
        return settingsCompanyDomainSupport.updateCompanyBankAccount(id, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCompanyBankAccount(Long id) {
        return settingsCompanyDomainSupport.deleteCompanyBankAccount(id);
    }

    @Override
    public List<SyncConnectorVO> listSyncConnectors() {
        return settingsSyncDomainSupport.listSyncConnectors();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SyncConnectorVO updateSyncConnector(SyncConnectorSaveDTO dto) {
        return settingsSyncDomainSupport.updateSyncConnector(dto);
    }

    @Override
    public List<SyncJobVO> listSyncJobs() {
        return settingsSyncDomainSupport.listSyncJobs();
    }

    @Override
    public SyncJobVO runSync(SyncRunDTO dto, String operator) {
        return settingsSyncDomainSupport.runSync(dto, operator);
    }

    @Override
    public void runDueSyncJobs() {
        settingsSyncDomainSupport.runDueSyncJobs();
    }
}
