package com.finex.auth.service.impl.auth;

import cn.hutool.crypto.digest.DigestUtil;
import com.finex.auth.dto.LoginDTO;
import com.finex.auth.dto.LoginVO;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.SystemPermissionMapper;
import com.finex.auth.mapper.SystemRoleMapper;
import com.finex.auth.mapper.SystemRolePermissionMapper;
import com.finex.auth.mapper.SystemUserRoleMapper;
import com.finex.auth.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthLoginDomainSupportTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private SystemUserRoleMapper systemUserRoleMapper;
    @Mock
    private SystemRoleMapper systemRoleMapper;
    @Mock
    private SystemRolePermissionMapper systemRolePermissionMapper;
    @Mock
    private SystemPermissionMapper systemPermissionMapper;

    private AuthLoginDomainSupport support;

    @BeforeEach
    void setUp() {
        AuthAuthorizationDomainSupport authorizationSupport = new AuthAuthorizationDomainSupport(
                userMapper,
                systemUserRoleMapper,
                systemRoleMapper,
                systemRolePermissionMapper,
                systemPermissionMapper
        );
        support = new AuthLoginDomainSupport(
                userMapper,
                systemUserRoleMapper,
                systemRoleMapper,
                systemRolePermissionMapper,
                systemPermissionMapper,
                authorizationSupport
        );
    }

    @Test
    void loginBuildsTokenAndAuthorizationSnapshot() {
        User user = new User();
        user.setId(5L);
        user.setUsername("alice");
        user.setName("Alice");
        user.setPassword(DigestUtil.md5Hex("secret"));
        user.setStatus(1);

        when(userMapper.selectOne(any())).thenReturn(user);
        when(systemUserRoleMapper.selectList(any())).thenReturn(List.of());

        LoginDTO dto = new LoginDTO();
        dto.setUsername("alice");
        dto.setPassword("secret");

        LoginVO result = support.login(dto);

        assertEquals(5L, result.getUserId());
        assertEquals("alice", result.getUsername());
        assertEquals("Alice", result.getName());
        assertEquals(List.of(), result.getRoles());
        assertEquals(List.of(), result.getPermissionCodes());
        assertEquals(AbstractAuthDomainSupport.TOKEN_EXPIRE_SECONDS, result.getExpireIn());
        assertNotNull(result.getToken());
    }

    @Test
    void loginRejectsDisabledUser() {
        User user = new User();
        user.setId(5L);
        user.setUsername("alice");
        user.setPassword(DigestUtil.md5Hex("secret"));
        user.setStatus(0);

        when(userMapper.selectOne(any())).thenReturn(user);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("alice");
        dto.setPassword("secret");

        RuntimeException error = assertThrows(RuntimeException.class, () -> support.login(dto));

        assertEquals("User is disabled", error.getMessage());
    }
}
