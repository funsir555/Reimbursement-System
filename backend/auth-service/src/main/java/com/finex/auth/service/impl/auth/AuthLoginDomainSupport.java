// 业务域：登录认证与权限
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 AuthController、用户相关 service 入口，下游会继续协调 用户、角色、权限与 JWT 等基础能力。
// 风险提醒：改坏后最容易影响 登录成功率、权限判断和当前用户识别。

package com.finex.auth.service.impl.auth;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.finex.auth.dto.LoginDTO;
import com.finex.auth.dto.LoginVO;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.SystemPermissionMapper;
import com.finex.auth.mapper.SystemRoleMapper;
import com.finex.auth.mapper.SystemRolePermissionMapper;
import com.finex.auth.mapper.SystemUserRoleMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.common.JwtUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * AuthLoginDomainSupport：领域规则支撑类。
 * 承接 认证登录的核心业务规则。
 * 改这里时，要特别关注 登录成功率、权限判断和当前用户识别是否会被一起带坏。
 */
@Slf4j
public class AuthLoginDomainSupport extends AbstractAuthDomainSupport {

    private final AuthAuthorizationDomainSupport authAuthorizationDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public AuthLoginDomainSupport(
            UserMapper userMapper,
            SystemUserRoleMapper systemUserRoleMapper,
            SystemRoleMapper systemRoleMapper,
            SystemRolePermissionMapper systemRolePermissionMapper,
            SystemPermissionMapper systemPermissionMapper,
            AuthAuthorizationDomainSupport authAuthorizationDomainSupport
    ) {
        super(userMapper, systemUserRoleMapper, systemRoleMapper, systemRolePermissionMapper, systemPermissionMapper);
        this.authAuthorizationDomainSupport = authAuthorizationDomainSupport;
    }

    /**
     * 执行登录校验并返回登录结果。
     */
    public LoginVO login(LoginDTO loginDTO) {
        User user = getByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new RuntimeException("User does not exist");
        }

        String encryptedPassword = DigestUtil.md5Hex(loginDTO.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new RuntimeException("Password is incorrect");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("User is disabled");
        }

        String token = JwtUtil.generateToken(user.getId(), user.getUsername());

        LoginVO loginVO = new LoginVO();
        BeanUtil.copyProperties(user, loginVO);
        loginVO.setUserId(user.getId());
        loginVO.setToken(token);
        loginVO.setExpireIn(TOKEN_EXPIRE_SECONDS);
        loginVO.setRoles(authAuthorizationDomainSupport.getRoleCodes(user.getId()));
        loginVO.setPermissionCodes(authAuthorizationDomainSupport.getPermissionCodes(user.getId()));

        log.info("User login succeeded: {}", user.getUsername());
        return loginVO;
    }

    /**
     * 按用户名获取用户。
     */
    public User getByUsername(String username) {
        return loadByUsername(username);
    }
}
