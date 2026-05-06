package com.finex.auth.support;

import cn.hutool.core.util.StrUtil;
import com.finex.auth.dto.EmployeeDepartmentRefVO;
import com.finex.auth.dto.EmployeeDirectoryOptionVO;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EmployeeDirectorySupport {

    private EmployeeDirectorySupport() {
    }

    public static List<EmployeeDirectoryOptionVO> buildEmployeeDirectory(
            Collection<User> users,
            UserMapper userMapper,
            SystemDepartmentMapper systemDepartmentMapper
    ) {
        if (users == null || users.isEmpty() || userMapper == null || systemDepartmentMapper == null) {
            return Collections.emptyList();
        }
        List<User> normalizedUsers = users.stream()
                .filter(Objects::nonNull)
                .toList();
        if (normalizedUsers.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = normalizedUsers.stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, List<EmployeeDepartmentRefVO>> departmentsByUserId = UserDepartmentSupport.loadDepartmentRefsByUserId(
                userMapper,
                systemDepartmentMapper,
                userIds
        );
        List<Long> primaryDepartmentIds = normalizedUsers.stream()
                .map(User::getDeptId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> primaryDepartmentNameMap = UserDepartmentSupport.loadDepartmentNameMap(
                systemDepartmentMapper,
                primaryDepartmentIds
        );

        List<EmployeeDirectoryOptionVO> result = new ArrayList<>();
        for (User user : normalizedUsers) {
            EmployeeDirectoryOptionVO option = new EmployeeDirectoryOptionVO();
            option.setUserId(user.getId());
            option.setName(resolveDisplayName(user));
            option.setUsername(trimToNull(user.getUsername()));
            option.setPhone(trimToNull(user.getPhone()));
            option.setEmail(trimToNull(user.getEmail()));
            option.setStatus(user.getStatus());

            List<EmployeeDepartmentRefVO> departments = new ArrayList<>(
                    departmentsByUserId.getOrDefault(user.getId(), Collections.emptyList())
            );
            if (departments.isEmpty() && user.getDeptId() != null) {
                EmployeeDepartmentRefVO fallbackDepartment = new EmployeeDepartmentRefVO();
                fallbackDepartment.setDeptId(user.getDeptId());
                fallbackDepartment.setDeptName(StrUtil.blankToDefault(primaryDepartmentNameMap.get(user.getDeptId()), ""));
                departments.add(fallbackDepartment);
            }
            option.setDepartments(departments);

            Long primaryDeptId = user.getDeptId() != null
                    ? user.getDeptId()
                    : UserDepartmentSupport.resolvePrimaryDepartmentId(departments);
            option.setDeptId(primaryDeptId);
            option.setDeptName(resolvePrimaryDepartmentName(primaryDeptId, departments, primaryDepartmentNameMap));
            result.add(option);
        }
        return result;
    }

    public static EmployeeDirectoryOptionVO buildEmployeeDirectoryOption(
            User user,
            List<EmployeeDepartmentRefVO> departments,
            Map<Long, String> primaryDepartmentNameMap
    ) {
        if (user == null) {
            return null;
        }
        EmployeeDirectoryOptionVO option = new EmployeeDirectoryOptionVO();
        option.setUserId(user.getId());
        option.setName(resolveDisplayName(user));
        option.setUsername(trimToNull(user.getUsername()));
        option.setPhone(trimToNull(user.getPhone()));
        option.setEmail(trimToNull(user.getEmail()));
        option.setStatus(user.getStatus());
        List<EmployeeDepartmentRefVO> normalizedDepartments = departments == null
                ? new ArrayList<>()
                : new ArrayList<>(departments);
        option.setDepartments(normalizedDepartments);
        Long primaryDeptId = user.getDeptId() != null
                ? user.getDeptId()
                : UserDepartmentSupport.resolvePrimaryDepartmentId(normalizedDepartments);
        option.setDeptId(primaryDeptId);
        option.setDeptName(resolvePrimaryDepartmentName(primaryDeptId, normalizedDepartments, primaryDepartmentNameMap));
        return option;
    }

    private static String resolvePrimaryDepartmentName(
            Long primaryDeptId,
            List<EmployeeDepartmentRefVO> departments,
            Map<Long, String> primaryDepartmentNameMap
    ) {
        if (primaryDeptId == null) {
            return "";
        }
        if (departments != null) {
            for (EmployeeDepartmentRefVO department : departments) {
                if (department != null && Objects.equals(department.getDeptId(), primaryDeptId)) {
                    return StrUtil.blankToDefault(trimToNull(department.getDeptName()), "");
                }
            }
        }
        return StrUtil.blankToDefault(primaryDepartmentNameMap.get(primaryDeptId), "");
    }

    private static String resolveDisplayName(User user) {
        String name = trimToNull(user == null ? null : user.getName());
        if (name != null) {
            return name;
        }
        String username = trimToNull(user == null ? null : user.getUsername());
        return username == null ? "未命名用户" : username;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
