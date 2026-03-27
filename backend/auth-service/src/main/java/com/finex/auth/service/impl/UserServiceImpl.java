package com.finex.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.finex.auth.service.UserService;
import com.finex.common.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final SystemUserRoleMapper systemUserRoleMapper;
    private final SystemRoleMapper systemRoleMapper;
    private final SystemRolePermissionMapper systemRolePermissionMapper;
    private final SystemPermissionMapper systemPermissionMapper;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 查询用户
        User user = getByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 校验密码（MD5加密）
        String encryptedPassword = DigestUtil.md5Hex(loginDTO.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new RuntimeException("用户已被禁用");
        }

        // 生成Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());

        // 组装响应
        LoginVO loginVO = new LoginVO();
        BeanUtil.copyProperties(user, loginVO);
        loginVO.setUserId(user.getId());
        loginVO.setToken(token);
        loginVO.setExpireIn(7 * 24 * 60 * 60L); // 7天
        loginVO.setRoles(getRoleCodes(user.getId()));
        loginVO.setPermissionCodes(getPermissionCodes(user.getId()));

        log.info("用户登录成功: {}", user.getUsername());
        return loginVO;
    }

    @Override
    public User getByUsername(String username) {
        return lambdaQuery()
                .eq(User::getUsername, username)
                .one();
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        List<Long> roleIds = systemUserRoleMapper.selectList(
                Wrappers.<SystemUserRole>lambdaQuery()
                        .eq(SystemUserRole::getUserId, userId)
        ).stream().map(SystemUserRole::getRoleId).distinct().toList();

        if (roleIds.isEmpty()) {
            return List.of();
        }

        return systemRoleMapper.selectList(
                Wrappers.<SystemRole>lambdaQuery()
                        .in(SystemRole::getId, roleIds)
                        .eq(SystemRole::getStatus, 1)
                        .orderByAsc(SystemRole::getId)
        ).stream()
                .map(SystemRole::getRoleCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
    }

    @Override
    public List<String> getPermissionCodes(Long userId) {
        List<Long> roleIds = systemUserRoleMapper.selectList(
                Wrappers.<SystemUserRole>lambdaQuery()
                        .eq(SystemUserRole::getUserId, userId)
        ).stream().map(SystemUserRole::getRoleId).distinct().toList();

        if (roleIds.isEmpty()) {
            return List.of();
        }

        List<Long> permissionIds = systemRolePermissionMapper.selectList(
                Wrappers.<SystemRolePermission>lambdaQuery()
                        .in(SystemRolePermission::getRoleId, roleIds)
        ).stream().map(SystemRolePermission::getPermissionId).distinct().toList();

        if (permissionIds.isEmpty()) {
            return List.of();
        }

        Set<String> permissionCodes = new LinkedHashSet<>(systemPermissionMapper.selectList(
                Wrappers.<SystemPermission>lambdaQuery()
                        .in(SystemPermission::getId, permissionIds)
                        .eq(SystemPermission::getStatus, 1)
                        .orderByAsc(SystemPermission::getSortOrder, SystemPermission::getId)
        ).stream()
                .map(SystemPermission::getPermissionCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toList()));
        return List.copyOf(permissionCodes);
    }
}
