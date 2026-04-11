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

@Slf4j
public class AuthLoginDomainSupport extends AbstractAuthDomainSupport {

    private final AuthAuthorizationDomainSupport authAuthorizationDomainSupport;

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

    public User getByUsername(String username) {
        return loadByUsername(username);
    }
}
