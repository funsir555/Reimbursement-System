// 业务域：登录认证与权限
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 AuthController、用户相关 service 入口，下游会继续协调 用户、角色、权限与 JWT 等基础能力。
// 风险提醒：改坏后最容易影响 登录成功率、权限判断和当前用户识别。

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

/**
 * AuthAuthorizationDomainSupport：领域规则支撑类。
 * 承接 认证授权的核心业务规则。
 * 改这里时，要特别关注 登录成功率、权限判断和当前用户识别是否会被一起带坏。
 */
public class AuthAuthorizationDomainSupport extends AbstractAuthDomainSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public AuthAuthorizationDomainSupport(
            UserMapper userMapper,
            SystemUserRoleMapper systemUserRoleMapper,
            SystemRoleMapper systemRoleMapper,
            SystemRolePermissionMapper systemRolePermissionMapper,
            SystemPermissionMapper systemPermissionMapper
    ) {
        super(userMapper, systemUserRoleMapper, systemRoleMapper, systemRolePermissionMapper, systemPermissionMapper);
    }

    /**
     * 获取角色编码。
     */
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

    /**
     * 获取权限编码。
     */
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
