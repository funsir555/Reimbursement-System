package com.finex.auth.service;

import java.util.List;

public interface AccessControlService {

    List<String> getRoleCodes(Long userId);

    List<String> getPermissionCodes(Long userId);

    void requirePermission(Long userId, String permissionCode);

    void requireAnyPermission(Long userId, String... permissionCodes);
}
