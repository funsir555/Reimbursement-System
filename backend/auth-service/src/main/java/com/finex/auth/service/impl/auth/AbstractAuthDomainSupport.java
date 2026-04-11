package com.finex.auth.service.impl.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.SystemRolePermission;
import com.finex.auth.entity.SystemUserRole;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.SystemPermissionMapper;
import com.finex.auth.mapper.SystemRoleMapper;
import com.finex.auth.mapper.SystemRolePermissionMapper;
import com.finex.auth.mapper.SystemUserRoleMapper;
import com.finex.auth.mapper.UserMapper;

import java.util.List;

public abstract class AbstractAuthDomainSupport {

    protected static final long TOKEN_EXPIRE_SECONDS = 7L * 24L * 60L * 60L;

    private final UserMapper userMapper;
    private final SystemUserRoleMapper systemUserRoleMapper;
    private final SystemRoleMapper systemRoleMapper;
    private final SystemRolePermissionMapper systemRolePermissionMapper;
    private final SystemPermissionMapper systemPermissionMapper;

    protected AbstractAuthDomainSupport(
            UserMapper userMapper,
            SystemUserRoleMapper systemUserRoleMapper,
            SystemRoleMapper systemRoleMapper,
            SystemRolePermissionMapper systemRolePermissionMapper,
            SystemPermissionMapper systemPermissionMapper
    ) {
        this.userMapper = userMapper;
        this.systemUserRoleMapper = systemUserRoleMapper;
        this.systemRoleMapper = systemRoleMapper;
        this.systemRolePermissionMapper = systemRolePermissionMapper;
        this.systemPermissionMapper = systemPermissionMapper;
    }

    protected UserMapper userMapper() {
        return userMapper;
    }

    protected SystemUserRoleMapper systemUserRoleMapper() {
        return systemUserRoleMapper;
    }

    protected SystemRoleMapper systemRoleMapper() {
        return systemRoleMapper;
    }

    protected SystemRolePermissionMapper systemRolePermissionMapper() {
        return systemRolePermissionMapper;
    }

    protected SystemPermissionMapper systemPermissionMapper() {
        return systemPermissionMapper;
    }

    protected User loadByUsername(String username) {
        return userMapper.selectOne(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, username)
        );
    }

    protected List<Long> findRoleIds(Long userId) {
        return systemUserRoleMapper.selectList(
                Wrappers.<SystemUserRole>lambdaQuery()
                        .eq(SystemUserRole::getUserId, userId)
        ).stream().map(SystemUserRole::getRoleId).distinct().toList();
    }

    protected List<Long> findPermissionIds(List<Long> roleIds) {
        return systemRolePermissionMapper.selectList(
                Wrappers.<SystemRolePermission>lambdaQuery()
                        .in(SystemRolePermission::getRoleId, roleIds)
        ).stream().map(SystemRolePermission::getPermissionId).distinct().toList();
    }
}
