package com.finex.auth.service.impl;

import com.finex.auth.mapper.SystemPermissionMapper;
import com.finex.auth.mapper.SystemRoleMapper;
import com.finex.auth.mapper.SystemRolePermissionMapper;
import com.finex.auth.mapper.SystemUserRoleMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.impl.auth.AuthAuthorizationDomainSupport;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AccessControlServiceImpl implements AccessControlService {

    private final AuthAuthorizationDomainSupport authAuthorizationDomainSupport;

    public AccessControlServiceImpl(
            UserMapper userMapper,
            SystemUserRoleMapper systemUserRoleMapper,
            SystemRoleMapper systemRoleMapper,
            SystemRolePermissionMapper systemRolePermissionMapper,
            SystemPermissionMapper systemPermissionMapper
    ) {
        this.authAuthorizationDomainSupport = new AuthAuthorizationDomainSupport(
                userMapper,
                systemUserRoleMapper,
                systemRoleMapper,
                systemRolePermissionMapper,
                systemPermissionMapper
        );
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        return authAuthorizationDomainSupport.getRoleCodes(userId);
    }

    @Override
    public List<String> getPermissionCodes(Long userId) {
        return authAuthorizationDomainSupport.getPermissionCodes(userId);
    }

    @Override
    public void requirePermission(Long userId, String permissionCode) {
        if (getPermissionCodes(userId).contains(permissionCode)) {
            return;
        }
        throw new SecurityException("No permission for current action");
    }

    @Override
    public void requireAnyPermission(Long userId, String... permissionCodes) {
        List<String> ownedCodes = getPermissionCodes(userId);
        boolean matched = Arrays.stream(permissionCodes).anyMatch(ownedCodes::contains);
        if (!matched) {
            throw new SecurityException("No permission for current module");
        }
    }
}
