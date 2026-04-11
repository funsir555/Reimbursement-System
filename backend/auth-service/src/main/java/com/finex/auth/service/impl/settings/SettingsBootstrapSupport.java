package com.finex.auth.service.impl.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.SystemSettingsBootstrapVO;
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

public final class SettingsBootstrapSupport extends AbstractSystemSettingsDomainSupport {

    public SettingsBootstrapSupport(
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

    public SystemSettingsBootstrapVO getBootstrap(Long currentUserId) {
        return super.getBootstrap(currentUserId);
    }
}
