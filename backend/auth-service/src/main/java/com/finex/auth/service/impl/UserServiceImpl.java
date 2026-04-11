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

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final AuthLoginDomainSupport authLoginDomainSupport;
    private final AuthAuthorizationDomainSupport authAuthorizationDomainSupport;

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

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        return authLoginDomainSupport.login(loginDTO);
    }

    @Override
    public User getByUsername(String username) {
        return authLoginDomainSupport.getByUsername(username);
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        return authAuthorizationDomainSupport.getRoleCodes(userId);
    }

    @Override
    public List<String> getPermissionCodes(Long userId) {
        return authAuthorizationDomainSupport.getPermissionCodes(userId);
    }
}
