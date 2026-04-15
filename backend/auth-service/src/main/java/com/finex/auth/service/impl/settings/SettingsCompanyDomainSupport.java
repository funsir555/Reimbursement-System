// 业务域：系统设置
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 组织、角色、公司信息和基础设置页面，下游会继续协调 公司、组织、角色、同步任务和系统参数。
// 风险提醒：改坏后最容易影响 权限体系、组织架构和历史单据可用性。

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

/**
 * SettingsCompanyDomainSupport：领域规则支撑类。
 * 承接 系统设置公司的核心业务规则。
 * 改这里时，要特别关注 权限体系、组织架构和历史单据可用性是否会被一起带坏。
 */
public final class SettingsCompanyDomainSupport extends AbstractSystemSettingsDomainSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
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

    /**
     * 查询Companies列表。
     */
    public List<CompanyVO> listCompanies() { return super.listCompanies(); }
    /**
     * 创建公司。
     */
    public CompanyVO createCompany(CompanySaveDTO dto) { return super.createCompany(dto); }
    /**
     * 更新公司。
     */
    public CompanyVO updateCompany(String companyId, CompanySaveDTO dto) { return super.updateCompany(companyId, dto); }
    /**
     * 删除公司。
     */
    public Boolean deleteCompany(String companyId) { return super.deleteCompany(companyId); }
    /**
     * 查询公司银行账户列表。
     */
    public List<CompanyBankAccountVO> listCompanyBankAccounts() { return super.listCompanyBankAccounts(); }
    /**
     * 创建公司银行账户。
     */
    public CompanyBankAccountVO createCompanyBankAccount(CompanyBankAccountSaveDTO dto) { return super.createCompanyBankAccount(dto); }
    /**
     * 更新公司银行账户。
     */
    public CompanyBankAccountVO updateCompanyBankAccount(Long id, CompanyBankAccountSaveDTO dto) { return super.updateCompanyBankAccount(id, dto); }
    /**
     * 删除公司银行账户。
     */
    public Boolean deleteCompanyBankAccount(Long id) { return super.deleteCompanyBankAccount(id); }
}
