package com.finex.auth.service.impl;

import com.finex.auth.entity.SystemPermission;
import com.finex.auth.entity.SystemRolePermission;
import com.finex.auth.entity.SystemUserRole;
import com.finex.auth.mapper.SystemPermissionMapper;
import com.finex.auth.mapper.SystemRoleMapper;
import com.finex.auth.mapper.SystemRolePermissionMapper;
import com.finex.auth.mapper.SystemUserRoleMapper;
import com.finex.auth.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceImplTest {

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

    @InjectMocks
    private AccessControlServiceImpl accessControlService;

    @Test
    void requirePermissionAllowsOwnedPermission() {
        mockPermissions(1L, "profile:view", "profile:downloads:view");

        assertDoesNotThrow(() -> accessControlService.requirePermission(1L, "profile:view"));
    }

    @Test
    void requirePermissionRejectsMissingPermission() {
        mockPermissions(1L, "profile:view");

        assertThrows(SecurityException.class, () -> accessControlService.requirePermission(1L, "settings:organization:view"));
    }

    @Test
    void requireAnyPermissionAllowsWhenAnyCodeMatches() {
        mockPermissions(1L, "archives:invoices:view", "profile:view");

        assertDoesNotThrow(() -> accessControlService.requireAnyPermission(
                1L,
                "archives:invoices:export",
                "archives:invoices:view"
        ));
    }

    @Test
    void requireAnyPermissionRejectsWhenNoCodesMatch() {
        mockPermissions(1L, "profile:view");

        assertThrows(SecurityException.class, () -> accessControlService.requireAnyPermission(
                1L,
                "finance:general_ledger:new_voucher:view",
                "settings:organization:view"
        ));
    }

    private void mockPermissions(Long userId, String... permissionCodes) {
        SystemUserRole relation = new SystemUserRole();
        relation.setUserId(userId);
        relation.setRoleId(10L);

        when(systemUserRoleMapper.selectList(any())).thenReturn(List.of(relation));

        List<SystemRolePermission> rolePermissions = new ArrayList<>();
        List<SystemPermission> permissions = new ArrayList<>();
        for (int index = 0; index < permissionCodes.length; index++) {
            SystemRolePermission rolePermission = new SystemRolePermission();
            rolePermission.setRoleId(10L);
            rolePermission.setPermissionId(100L + index);
            rolePermissions.add(rolePermission);

            SystemPermission permission = new SystemPermission();
            permission.setId(100L + index);
            permission.setPermissionCode(permissionCodes[index]);
            permission.setStatus(1);
            permission.setSortOrder(index + 1);
            permissions.add(permission);
        }

        when(systemRolePermissionMapper.selectList(any())).thenReturn(rolePermissions);
        when(systemPermissionMapper.selectList(any())).thenReturn(permissions);
    }
}
