package com.finex.auth.service.impl.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.CompanyBankAccountSaveDTO;
import com.finex.auth.dto.CompanyBankAccountVO;
import com.finex.auth.dto.CompanySaveDTO;
import com.finex.auth.dto.CompanyVO;
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
import com.finex.auth.service.UserService;
import com.finex.auth.support.systemsettings.OrganizationSyncAdapter;

import java.util.List;

public final class SettingsCompanyDomainSupport extends AbstractSystemSettingsDomainSupport {

    public SettingsCompanyDomainSupport(
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
        super(userService, accessControlService, userMapper, systemDepartmentMapper, systemCompanyBankAccountMapper, systemCompanyMapper, systemRoleMapper, systemPermissionMapper, systemRolePermissionMapper, systemUserRoleMapper, systemSyncConnectorMapper, systemSyncJobMapper, systemSyncJobDetailMapper, objectMapper, syncAdapters);
    }

    public List<CompanyVO> listCompanies() { return super.listCompanies(); }
    public CompanyVO createCompany(CompanySaveDTO dto) { return super.createCompany(dto); }
    public CompanyVO updateCompany(String companyId, CompanySaveDTO dto) { return super.updateCompany(companyId, dto); }
    public Boolean deleteCompany(String companyId) { return super.deleteCompany(companyId); }
    public List<CompanyBankAccountVO> listCompanyBankAccounts() { return super.listCompanyBankAccounts(); }
    public CompanyBankAccountVO createCompanyBankAccount(CompanyBankAccountSaveDTO dto) { return super.createCompanyBankAccount(dto); }
    public CompanyBankAccountVO updateCompanyBankAccount(Long id, CompanyBankAccountSaveDTO dto) { return super.updateCompanyBankAccount(id, dto); }
    public Boolean deleteCompanyBankAccount(Long id) { return super.deleteCompanyBankAccount(id); }
}
