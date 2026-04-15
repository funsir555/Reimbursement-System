// 业务域：登录认证与权限
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 AuthController、用户相关 service 入口，下游会继续协调 用户、角色、权限与 JWT 等基础能力。
// 风险提醒：改坏后最容易影响 登录成功率、权限判断和当前用户识别。

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

/**
 * AccessControlServiceImpl：service 入口实现。
 * 接住上层请求，并把 访问控制相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 登录成功率、权限判断和当前用户识别是否会被一起带坏。
 */
@Service
public class AccessControlServiceImpl implements AccessControlService {

    private final AuthAuthorizationDomainSupport authAuthorizationDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
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

    /**
     * 获取角色编码。
     */
    @Override
    public List<String> getRoleCodes(Long userId) {
        return authAuthorizationDomainSupport.getRoleCodes(userId);
    }

    /**
     * 获取权限编码。
     */
    @Override
    public List<String> getPermissionCodes(Long userId) {
        return authAuthorizationDomainSupport.getPermissionCodes(userId);
    }

    /**
     * 处理访问控制中的这一步。
     */
    @Override
    public void requirePermission(Long userId, String permissionCode) {
        if (getPermissionCodes(userId).contains(permissionCode)) {
            return;
        }
        throw new SecurityException("No permission for current action");
    }

    /**
     * 处理访问控制中的这一步。
     */
    @Override
    public void requireAnyPermission(Long userId, String... permissionCodes) {
        List<String> ownedCodes = getPermissionCodes(userId);
        boolean matched = Arrays.stream(permissionCodes).anyMatch(ownedCodes::contains);
        if (!matched) {
            throw new SecurityException("No permission for current module");
        }
    }
}
