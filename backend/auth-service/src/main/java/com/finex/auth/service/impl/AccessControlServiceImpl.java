package com.finex.auth.service.impl;

import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccessControlServiceImpl implements AccessControlService {

    private final UserService userService;

    @Override
    public List<String> getRoleCodes(Long userId) {
        return userService.getRoleCodes(userId);
    }

    @Override
    public List<String> getPermissionCodes(Long userId) {
        return userService.getPermissionCodes(userId);
    }

    @Override
    public void requirePermission(Long userId, String permissionCode) {
        if (getPermissionCodes(userId).contains(permissionCode)) {
            return;
        }
        throw new SecurityException("无权执行当前操作");
    }

    @Override
    public void requireAnyPermission(Long userId, String... permissionCodes) {
        List<String> ownedCodes = getPermissionCodes(userId);
        boolean matched = Arrays.stream(permissionCodes).anyMatch(ownedCodes::contains);
        if (!matched) {
            throw new SecurityException("无权访问当前模块");
        }
    }
}
