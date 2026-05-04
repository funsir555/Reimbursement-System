package com.finex.auth.support;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.EmployeeDepartmentRefVO;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class UserDepartmentSupport {

    private UserDepartmentSupport() {
    }

    public static List<Long> normalizeDepartmentIds(Collection<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return departmentIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public static Map<Long, List<Long>> loadDepartmentIdsByUserId(UserMapper userMapper, Collection<Long> userIds) {
        if (userMapper == null || userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserDepartmentRelationRecord> relations = userMapper.selectDepartmentRelationsByUserIds(userIds);
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        for (UserDepartmentRelationRecord relation : relations) {
            if (relation == null || relation.getUserId() == null || relation.getDeptId() == null) {
                continue;
            }
            result.computeIfAbsent(relation.getUserId(), key -> new ArrayList<>()).add(relation.getDeptId());
        }
        return result;
    }

    public static Map<Long, List<EmployeeDepartmentRefVO>> loadDepartmentRefsByUserId(
            UserMapper userMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            Collection<Long> userIds
    ) {
        Map<Long, List<Long>> departmentIdsByUserId = loadDepartmentIdsByUserId(userMapper, userIds);
        if (departmentIdsByUserId.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> departmentIds = departmentIdsByUserId.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> departmentNameMap = loadDepartmentNameMap(systemDepartmentMapper, departmentIds);
        Map<Long, List<EmployeeDepartmentRefVO>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Long>> entry : departmentIdsByUserId.entrySet()) {
            List<EmployeeDepartmentRefVO> refs = entry.getValue().stream()
                    .map(deptId -> buildDepartmentRef(deptId, departmentNameMap.get(deptId)))
                    .filter(Objects::nonNull)
                    .toList();
            result.put(entry.getKey(), refs);
        }
        return result;
    }

    public static Map<Long, String> loadDepartmentNameMap(
            SystemDepartmentMapper systemDepartmentMapper,
            Collection<Long> departmentIds
    ) {
        if (systemDepartmentMapper == null || departmentIds == null || departmentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery().in(SystemDepartment::getId, departmentIds)
        ).stream().collect(Collectors.toMap(
                SystemDepartment::getId,
                item -> StrUtil.blankToDefault(item.getDeptName(), ""),
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    public static String joinDepartmentNames(List<EmployeeDepartmentRefVO> departments) {
        if (departments == null || departments.isEmpty()) {
            return "";
        }
        return departments.stream()
                .map(EmployeeDepartmentRefVO::getDeptName)
                .map(UserDepartmentSupport::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("、"));
    }

    public static Long resolvePrimaryDepartmentId(List<EmployeeDepartmentRefVO> departments) {
        if (departments == null || departments.isEmpty()) {
            return null;
        }
        return departments.stream()
                .map(EmployeeDepartmentRefVO::getDeptId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public static List<Long> expandDepartmentLineageIds(
            Collection<Long> departmentIds,
            Map<Long, SystemDepartment> departmentMap
    ) {
        if (departmentIds == null || departmentIds.isEmpty() || departmentMap == null || departmentMap.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (Long departmentId : departmentIds) {
            SystemDepartment current = departmentId == null ? null : departmentMap.get(departmentId);
            while (current != null && current.getId() != null) {
                result.add(current.getId());
                current = current.getParentId() == null ? null : departmentMap.get(current.getParentId());
            }
        }
        return new ArrayList<>(result);
    }

    private static EmployeeDepartmentRefVO buildDepartmentRef(Long deptId, String deptName) {
        if (deptId == null) {
            return null;
        }
        EmployeeDepartmentRefVO ref = new EmployeeDepartmentRefVO();
        ref.setDeptId(deptId);
        ref.setDeptName(StrUtil.blankToDefault(deptName, ""));
        return ref;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
