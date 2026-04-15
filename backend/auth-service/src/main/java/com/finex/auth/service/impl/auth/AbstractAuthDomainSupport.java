// 业务域：登录认证与权限
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 AuthController、用户相关 service 入口，下游会继续协调 用户、角色、权限与 JWT 等基础能力。
// 风险提醒：改坏后最容易影响 登录成功率、权限判断和当前用户识别。

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

/**
 * AbstractAuthDomainSupport：领域规则支撑类。
 * 承接 认证的核心业务规则。
 * 改这里时，要特别关注 登录成功率、权限判断和当前用户识别是否会被一起带坏。
 */
public abstract class AbstractAuthDomainSupport {

    protected static final long TOKEN_EXPIRE_SECONDS = 7L * 24L * 60L * 60L;

    private final UserMapper userMapper;
    private final SystemUserRoleMapper systemUserRoleMapper;
    private final SystemRoleMapper systemRoleMapper;
    private final SystemRolePermissionMapper systemRolePermissionMapper;
    private final SystemPermissionMapper systemPermissionMapper;

    /**
     * 初始化这个类所需的依赖组件。
     */
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

    /**
     * 处理认证中的这一步。
     */
    protected UserMapper userMapper() {
        return userMapper;
    }

    /**
     * 处理认证中的这一步。
     */
    protected SystemUserRoleMapper systemUserRoleMapper() {
        return systemUserRoleMapper;
    }

    /**
     * 处理认证中的这一步。
     */
    protected SystemRoleMapper systemRoleMapper() {
        return systemRoleMapper;
    }

    /**
     * 处理认证中的这一步。
     */
    protected SystemRolePermissionMapper systemRolePermissionMapper() {
        return systemRolePermissionMapper;
    }

    /**
     * 处理认证中的这一步。
     */
    protected SystemPermissionMapper systemPermissionMapper() {
        return systemPermissionMapper;
    }

    /**
     * 按用户名加载用户。
     */
    protected User loadByUsername(String username) {
        return userMapper.selectOne(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, username)
        );
    }

    /**
     * 查询角色Ids。
     */
    protected List<Long> findRoleIds(Long userId) {
        return systemUserRoleMapper.selectList(
                Wrappers.<SystemUserRole>lambdaQuery()
                        .eq(SystemUserRole::getUserId, userId)
        ).stream().map(SystemUserRole::getRoleId).distinct().toList();
    }

    /**
     * 查询权限Ids。
     */
    protected List<Long> findPermissionIds(List<Long> roleIds) {
        return systemRolePermissionMapper.selectList(
                Wrappers.<SystemRolePermission>lambdaQuery()
                        .in(SystemRolePermission::getRoleId, roleIds)
        ).stream().map(SystemRolePermission::getPermissionId).distinct().toList();
    }
}
