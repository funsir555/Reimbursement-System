package com.finex.auth.service.impl.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.SystemPermission;
import com.finex.auth.entity.SystemRole;
import com.finex.auth.mapper.SystemPermissionMapper;
import com.finex.auth.mapper.SystemRoleMapper;
import com.finex.auth.mapper.SystemRolePermissionMapper;
import com.finex.auth.mapper.SystemUserRoleMapper;
import com.finex.auth.mapper.UserMapper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthAuthorizationDomainSupport extends AbstractAuthDomainSupport {

    public AuthAuthorizationDomainSupport(
            UserMapper userMapper,
            SystemUserRoleMapper systemUserRoleMapper,
            SystemRoleMapper systemRoleMapper,
            SystemRolePermissionMapper systemRolePermissionMapper,
            SystemPermissionMapper systemPermissionMapper
    ) {
        super(userMapper, systemUserRoleMapper, systemRoleMapper, systemRolePermissionMapper, systemPermissionMapper);
    }

    public List<String> getRoleCodes(Long userId) {
        List<Long> roleIds = findRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }

        return systemRoleMapper().selectList(
                Wrappers.<SystemRole>lambdaQuery()
                        .in(SystemRole::getId, roleIds)
                        .eq(SystemRole::getStatus, 1)
                        .orderByAsc(SystemRole::getId)
        ).stream()
                .map(SystemRole::getRoleCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
    }

    public List<String> getPermissionCodes(Long userId) {
        List<Long> roleIds = findRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }

        List<Long> permissionIds = findPermissionIds(roleIds);
        if (permissionIds.isEmpty()) {
            return List.of();
        }

        Set<String> permissionCodes = new LinkedHashSet<>(systemPermissionMapper().selectList(
                Wrappers.<SystemPermission>lambdaQuery()
                        .in(SystemPermission::getId, permissionIds)
                        .eq(SystemPermission::getStatus, 1)
                        .orderByAsc(SystemPermission::getSortOrder, SystemPermission::getId)
        ).stream()
                .map(SystemPermission::getPermissionCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toList()));
        return List.copyOf(permissionCodes);
    }
}
