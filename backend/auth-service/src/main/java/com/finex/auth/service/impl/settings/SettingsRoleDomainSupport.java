// 业务域：系统设置
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 组织、角色、公司信息和基础设置页面，下游会继续协调 公司、组织、角色、同步任务和系统参数。
// 风险提醒：改坏后最容易影响 权限体系、组织架构和历史单据可用性。

package com.finex.auth.service.impl.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.PermissionTreeNodeVO;
import com.finex.auth.dto.RolePermissionAssignDTO;
import com.finex.auth.dto.RoleSaveDTO;
import com.finex.auth.dto.RoleVO;
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
import com.finex.auth.service.UserService;
import com.finex.auth.support.systemsettings.OrganizationSyncAdapter;

import java.util.List;

/**
 * SettingsRoleDomainSupport：领域规则支撑类。
 * 承接 系统设置角色的核心业务规则。
 * 改这里时，要特别关注 权限体系、组织架构和历史单据可用性是否会被一起带坏。
 */
public final class SettingsRoleDomainSupport extends AbstractSystemSettingsDomainSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public SettingsRoleDomainSupport(
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
     * 查询角色列表。
     */
    public List<RoleVO> listRoles() { return super.listRoles(); }
    /**
     * 创建角色。
     */
    public RoleVO createRole(RoleSaveDTO dto) { return super.createRole(dto); }
    /**
     * 更新角色。
     */
    public RoleVO updateRole(Long id, RoleSaveDTO dto) { return super.updateRole(id, dto); }
    /**
     * 删除角色。
     */
    public Boolean deleteRole(Long id) { return super.deleteRole(id); }
    /**
     * 处理系统设置角色中的这一步。
     */
    public Boolean assignRolePermissions(Long roleId, RolePermissionAssignDTO dto, Long currentUserId) { return super.assignRolePermissions(roleId, dto, currentUserId); }
    /**
     * 处理系统设置角色中的这一步。
     */
    public Boolean assignUserRoles(Long userId, UserRoleAssignDTO dto) { return super.assignUserRoles(userId, dto); }
    /**
     * 获取权限Tree。
     */
    public List<PermissionTreeNodeVO> getPermissionTree() { return super.getPermissionTree(); }
}
