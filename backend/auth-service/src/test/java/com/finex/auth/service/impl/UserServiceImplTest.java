package com.finex.auth.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.finex.auth.dto.LoginDTO;
import com.finex.auth.dto.LoginVO;
import com.finex.auth.entity.SystemPermission;
import com.finex.auth.entity.SystemRole;
import com.finex.auth.entity.SystemRolePermission;
import com.finex.auth.entity.SystemUserRole;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

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

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userMapper,
                systemUserRoleMapper,
                systemRoleMapper,
                systemRolePermissionMapper,
                systemPermissionMapper
        );
    }

    @Test
    void loginDelegatesThroughFacade() {
        User user = new User();
        user.setId(9L);
        user.setUsername("alice");
        user.setName("Alice");
        user.setPassword(DigestUtil.md5Hex("secret"));
        user.setStatus(1);

        SystemUserRole userRole = new SystemUserRole();
        userRole.setUserId(9L);
        userRole.setRoleId(11L);

        SystemRole role = new SystemRole();
        role.setId(11L);
        role.setRoleCode("EMPLOYEE");
        role.setStatus(1);

        SystemRolePermission rolePermission = new SystemRolePermission();
        rolePermission.setRoleId(11L);
        rolePermission.setPermissionId(21L);

        SystemPermission permission = new SystemPermission();
        permission.setId(21L);
        permission.setPermissionCode("dashboard:view");
        permission.setStatus(1);
        permission.setSortOrder(1);

        when(userMapper.selectOne(any())).thenReturn(user);
        when(systemUserRoleMapper.selectList(any())).thenReturn(List.of(userRole));
        when(systemRoleMapper.selectList(any())).thenReturn(List.of(role));
        when(systemRolePermissionMapper.selectList(any())).thenReturn(List.of(rolePermission));
        when(systemPermissionMapper.selectList(any())).thenReturn(List.of(permission));

        LoginDTO dto = new LoginDTO();
        dto.setUsername("alice");
        dto.setPassword("secret");

        LoginVO result = userService.login(dto);

        assertEquals(9L, result.getUserId());
        assertEquals(List.of("EMPLOYEE"), result.getRoles());
        assertEquals(List.of("dashboard:view"), result.getPermissionCodes());
        assertNotNull(result.getToken());
    }

    @Test
    void getPermissionCodesDelegatesThroughFacade() {
        SystemUserRole userRole = new SystemUserRole();
        userRole.setUserId(9L);
        userRole.setRoleId(11L);

        SystemRolePermission firstRelation = new SystemRolePermission();
        firstRelation.setRoleId(11L);
        firstRelation.setPermissionId(21L);
        SystemRolePermission secondRelation = new SystemRolePermission();
        secondRelation.setRoleId(11L);
        secondRelation.setPermissionId(22L);

        SystemPermission firstPermission = new SystemPermission();
        firstPermission.setId(21L);
        firstPermission.setPermissionCode("dashboard:view");
        firstPermission.setStatus(1);
        firstPermission.setSortOrder(1);
        SystemPermission secondPermission = new SystemPermission();
        secondPermission.setId(22L);
        secondPermission.setPermissionCode("expense:list:view");
        secondPermission.setStatus(1);
        secondPermission.setSortOrder(2);

        when(systemUserRoleMapper.selectList(any())).thenReturn(List.of(userRole));
        when(systemRolePermissionMapper.selectList(any())).thenReturn(List.of(firstRelation, secondRelation));
        when(systemPermissionMapper.selectList(any())).thenReturn(List.of(firstPermission, secondPermission));

        assertEquals(List.of("dashboard:view", "expense:list:view"), userService.getPermissionCodes(9L));
    }
}
