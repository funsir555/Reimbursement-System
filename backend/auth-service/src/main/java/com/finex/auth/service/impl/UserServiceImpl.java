// 业务域：登录认证与权限
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 AuthController、用户相关 service 入口，下游会继续协调 用户、角色、权限与 JWT 等基础能力。
// 风险提醒：改坏后最容易影响 登录成功率、权限判断和当前用户识别。

package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.finex.auth.dto.LoginDTO;
import com.finex.auth.dto.LoginVO;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.SystemPermissionMapper;
import com.finex.auth.mapper.SystemRoleMapper;
import com.finex.auth.mapper.SystemRolePermissionMapper;
import com.finex.auth.mapper.SystemUserRoleMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.UserService;
import com.finex.auth.service.impl.auth.AuthAuthorizationDomainSupport;
import com.finex.auth.service.impl.auth.AuthLoginDomainSupport;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UserServiceImpl：service 入口实现。
 * 接住上层请求，并把 用户相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 登录成功率、权限判断和当前用户识别是否会被一起带坏。
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final AuthLoginDomainSupport authLoginDomainSupport;
    private final AuthAuthorizationDomainSupport authAuthorizationDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public UserServiceImpl(
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
        this.authLoginDomainSupport = new AuthLoginDomainSupport(
                userMapper,
                systemUserRoleMapper,
                systemRoleMapper,
                systemRolePermissionMapper,
                systemPermissionMapper,
                authAuthorizationDomainSupport
        );
    }

    /**
     * 执行登录校验并返回登录结果。
     */
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        return authLoginDomainSupport.login(loginDTO);
    }

    /**
     * 按用户名获取用户。
     */
    @Override
    public User getByUsername(String username) {
        return authLoginDomainSupport.getByUsername(username);
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
}
