package com.finex.auth.service.impl.auth;

import com.finex.auth.entity.SystemPermission;
import com.finex.auth.entity.SystemRole;
import com.finex.auth.entity.SystemRolePermission;
import com.finex.auth.entity.SystemUserRole;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthAuthorizationDomainSupportTest {

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

    private AuthAuthorizationDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new AuthAuthorizationDomainSupport(
                userMapper,
                systemUserRoleMapper,
                systemRoleMapper,
                systemRolePermissionMapper,
                systemPermissionMapper
        );
    }

    @Test
    void getRoleCodesFiltersInactiveAndBlankCodes() {
        SystemUserRole firstRelation = new SystemUserRole();
        firstRelation.setUserId(7L);
        firstRelation.setRoleId(10L);
        SystemUserRole secondRelation = new SystemUserRole();
        secondRelation.setUserId(7L);
        secondRelation.setRoleId(11L);
        SystemUserRole duplicatedRelation = new SystemUserRole();
        duplicatedRelation.setUserId(7L);
        duplicatedRelation.setRoleId(10L);

        SystemRole activeRole = new SystemRole();
        activeRole.setId(10L);
        activeRole.setRoleCode("FINANCE");
        activeRole.setStatus(1);
        SystemRole blankRole = new SystemRole();
        blankRole.setId(11L);
        blankRole.setRoleCode(" ");
        blankRole.setStatus(1);

        when(systemUserRoleMapper.selectList(any())).thenReturn(List.of(firstRelation, secondRelation, duplicatedRelation));
        when(systemRoleMapper.selectList(any())).thenReturn(List.of(activeRole, blankRole));

        assertEquals(List.of("FINANCE"), support.getRoleCodes(7L));
    }

    @Test
    void getPermissionCodesDeduplicatesAndSkipsBlankCodes() {
        SystemUserRole relation = new SystemUserRole();
        relation.setUserId(7L);
        relation.setRoleId(10L);

        SystemRolePermission firstPermissionRelation = new SystemRolePermission();
        firstPermissionRelation.setRoleId(10L);
        firstPermissionRelation.setPermissionId(100L);
        SystemRolePermission secondPermissionRelation = new SystemRolePermission();
        secondPermissionRelation.setRoleId(10L);
        secondPermissionRelation.setPermissionId(101L);
        SystemRolePermission duplicatedPermissionRelation = new SystemRolePermission();
        duplicatedPermissionRelation.setRoleId(10L);
        duplicatedPermissionRelation.setPermissionId(100L);

        SystemPermission firstPermission = new SystemPermission();
        firstPermission.setId(100L);
        firstPermission.setPermissionCode("dashboard:view");
        firstPermission.setStatus(1);
        firstPermission.setSortOrder(1);
        SystemPermission duplicatedCode = new SystemPermission();
        duplicatedCode.setId(101L);
        duplicatedCode.setPermissionCode("dashboard:view");
        duplicatedCode.setStatus(1);
        duplicatedCode.setSortOrder(2);
        SystemPermission blankPermission = new SystemPermission();
        blankPermission.setId(102L);
        blankPermission.setPermissionCode(" ");
        blankPermission.setStatus(1);
        blankPermission.setSortOrder(3);

        when(systemUserRoleMapper.selectList(any())).thenReturn(List.of(relation));
        when(systemRolePermissionMapper.selectList(any())).thenReturn(
                List.of(firstPermissionRelation, secondPermissionRelation, duplicatedPermissionRelation)
        );
        when(systemPermissionMapper.selectList(any())).thenReturn(List.of(firstPermission, duplicatedCode, blankPermission));

        assertEquals(List.of("dashboard:view"), support.getPermissionCodes(7L));
    }
}
