package com.finex.auth.service.impl;

import com.finex.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceImplTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AccessControlServiceImpl accessControlService;

    @Test
    void requirePermissionAllowsOwnedPermission() {
        when(userService.getPermissionCodes(1L)).thenReturn(List.of("profile:view", "profile:downloads:view"));

        assertDoesNotThrow(() -> accessControlService.requirePermission(1L, "profile:view"));
    }

    @Test
    void requirePermissionRejectsMissingPermission() {
        when(userService.getPermissionCodes(1L)).thenReturn(List.of("profile:view"));

        assertThrows(SecurityException.class, () -> accessControlService.requirePermission(1L, "settings:organization:view"));
    }

    @Test
    void requireAnyPermissionAllowsWhenAnyCodeMatches() {
        when(userService.getPermissionCodes(1L)).thenReturn(List.of("archives:invoices:view", "profile:view"));

        assertDoesNotThrow(() -> accessControlService.requireAnyPermission(
                1L,
                "archives:invoices:export",
                "archives:invoices:view"
        ));
    }

    @Test
    void requireAnyPermissionRejectsWhenNoCodesMatch() {
        when(userService.getPermissionCodes(1L)).thenReturn(List.of("profile:view"));

        assertThrows(SecurityException.class, () -> accessControlService.requireAnyPermission(
                1L,
                "finance:general_ledger:new_voucher:view",
                "settings:organization:view"
        ));
    }
}
