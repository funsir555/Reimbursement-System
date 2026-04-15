// 业务域：系统设置
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 组织、角色、公司信息和基础设置页面，下游会继续协调 公司、组织、角色、同步任务和系统参数。
// 风险提醒：改坏后最容易影响 权限体系、组织架构和历史单据可用性。

package com.finex.auth.service.impl.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.DepartmentSaveDTO;
import com.finex.auth.dto.DepartmentTreeNodeVO;
import com.finex.auth.dto.EmployeeQueryDTO;
import com.finex.auth.dto.EmployeeSaveDTO;
import com.finex.auth.dto.EmployeeVO;
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
 * SettingsOrganizationDomainSupport：领域规则支撑类。
 * 承接 系统设置组织的核心业务规则。
 * 改这里时，要特别关注 权限体系、组织架构和历史单据可用性是否会被一起带坏。
 */
public final class SettingsOrganizationDomainSupport extends AbstractSystemSettingsDomainSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public SettingsOrganizationDomainSupport(
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
     * 查询Departments列表。
     */
    public List<DepartmentTreeNodeVO> listDepartments() { return super.listDepartments(); }
    /**
     * 创建Department。
     */
    public DepartmentTreeNodeVO createDepartment(DepartmentSaveDTO dto) { return super.createDepartment(dto); }
    /**
     * 更新Department。
     */
    public DepartmentTreeNodeVO updateDepartment(Long id, DepartmentSaveDTO dto) { return super.updateDepartment(id, dto); }
    /**
     * 删除Department。
     */
    public Boolean deleteDepartment(Long id) { return super.deleteDepartment(id); }
    /**
     * 查询Employees列表。
     */
    public List<EmployeeVO> listEmployees(EmployeeQueryDTO query) { return super.listEmployees(query); }
    /**
     * 创建Employee。
     */
    public EmployeeVO createEmployee(EmployeeSaveDTO dto) { return super.createEmployee(dto); }
    /**
     * 更新Employee。
     */
    public EmployeeVO updateEmployee(Long id, EmployeeSaveDTO dto) { return super.updateEmployee(id, dto); }
    /**
     * 删除Employee。
     */
    public Boolean deleteEmployee(Long id) { return super.deleteEmployee(id); }
}
