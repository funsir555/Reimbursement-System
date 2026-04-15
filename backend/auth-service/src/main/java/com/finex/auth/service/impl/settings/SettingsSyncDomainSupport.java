// 业务域：系统设置
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 组织、角色、公司信息和基础设置页面，下游会继续协调 公司、组织、角色、同步任务和系统参数。
// 风险提醒：改坏后最容易影响 权限体系、组织架构和历史单据可用性。

package com.finex.auth.service.impl.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.SyncConnectorSaveDTO;
import com.finex.auth.dto.SyncConnectorVO;
import com.finex.auth.dto.SyncJobVO;
import com.finex.auth.dto.SyncRunDTO;
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
 * SettingsSyncDomainSupport：领域规则支撑类。
 * 承接 系统设置同步的核心业务规则。
 * 改这里时，要特别关注 权限体系、组织架构和历史单据可用性是否会被一起带坏。
 */
public final class SettingsSyncDomainSupport extends AbstractSystemSettingsDomainSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public SettingsSyncDomainSupport(
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
     * 查询同步Connectors列表。
     */
    public List<SyncConnectorVO> listSyncConnectors() { return super.listSyncConnectors(); }
    /**
     * 更新同步Connector。
     */
    public SyncConnectorVO updateSyncConnector(SyncConnectorSaveDTO dto) { return super.updateSyncConnector(dto); }
    /**
     * 查询同步Jobs列表。
     */
    public List<SyncJobVO> listSyncJobs() { return super.listSyncJobs(); }
    /**
     * 执行同步。
     */
    public SyncJobVO runSync(SyncRunDTO dto, String operator) { return super.runSync(dto, operator); }
    /**
     * 执行Due同步Jobs。
     */
    public void runDueSyncJobs() { super.runDueSyncJobs(); }
}
