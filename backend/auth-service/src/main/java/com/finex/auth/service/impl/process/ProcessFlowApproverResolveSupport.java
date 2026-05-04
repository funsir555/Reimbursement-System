package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowResolveApproversDTO;
import com.finex.auth.dto.ProcessFlowResolveApproversVO;
import com.finex.auth.dto.ProcessFlowResolvedUserVO;
import com.finex.auth.entity.ProcessFlow;
import com.finex.auth.entity.ProcessFlowNode;
import com.finex.auth.entity.ProcessFlowVersion;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessFlowMapper;
import com.finex.auth.mapper.ProcessFlowNodeMapper;
import com.finex.auth.mapper.ProcessFlowRouteMapper;
import com.finex.auth.mapper.ProcessFlowSceneMapper;
import com.finex.auth.mapper.ProcessFlowVersionMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.support.UserDepartmentSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProcessFlowApproverResolveSupport extends AbstractProcessFlowDesignSupport {

    private final ProcessFlowStructureSupport structureSupport;
    private final ProcessUserGroupResolverSupport userGroupResolverSupport;

    public ProcessFlowApproverResolveSupport(
            ProcessFlowMapper processFlowMapper,
            ProcessFlowVersionMapper processFlowVersionMapper,
            ProcessFlowNodeMapper processFlowNodeMapper,
            ProcessFlowRouteMapper processFlowRouteMapper,
            ProcessFlowSceneMapper processFlowSceneMapper,
            SystemCompanyMapper systemCompanyMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper,
            ProcessExpenseTypeMapper processExpenseTypeMapper,
            ProcessCustomArchiveDesignMapper processCustomArchiveDesignMapper,
            ProcessDocumentTemplateMapper processDocumentTemplateMapper,
            ObjectMapper objectMapper,
            ProcessFlowStructureSupport structureSupport,
            ProcessUserGroupResolverSupport userGroupResolverSupport
    ) {
        super(
                processFlowMapper,
                processFlowVersionMapper,
                processFlowNodeMapper,
                processFlowRouteMapper,
                processFlowSceneMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper,
                processExpenseTypeMapper,
                processCustomArchiveDesignMapper,
                processDocumentTemplateMapper,
                objectMapper
        );
        this.structureSupport = structureSupport;
        this.userGroupResolverSupport = userGroupResolverSupport;
    }

    public ProcessFlowResolveApproversVO resolveApprovers(ProcessFlowResolveApproversDTO dto) {
        ProcessFlow flow = requireFlow(dto.getFlowId());
        ProcessFlowVersion version = resolveEditableVersion(flow);
        if (version == null) {
            throw new IllegalStateException("当前流程没有可解析的版本");
        }

        ProcessFlowNode node = processFlowNodeMapper.selectOne(
                Wrappers.<ProcessFlowNode>lambdaQuery()
                        .eq(ProcessFlowNode::getVersionId, version.getId())
                        .eq(ProcessFlowNode::getNodeKey, dto.getNodeKey())
                        .last("limit 1")
        );
        if (node == null) {
            throw new IllegalStateException("未找到指定流程节点");
        }

        Map<String, Object> config = structureSupport.normalizeNodeConfig(node.getNodeType(), readMap(node.getConfigJson()), false);
        String approverType = asText(config.get("approverType"), APPROVER_TYPE_MANAGER);
        String missingHandler = normalizeMissingHandler(asText(config.get("missingHandler"), MISSING_HANDLER_AUTO_SKIP));

        List<String> trace = new ArrayList<>();
        List<User> resolvedUsers;
        if (APPROVER_TYPE_DESIGNATED_MEMBER.equals(approverType)) {
            resolvedUsers = resolveDesignatedMembers(config, trace);
        } else if (APPROVER_TYPE_DESIGNATED_USER_GROUP.equals(approverType)) {
            resolvedUsers = resolveDesignatedUserGroupMembers(config, dto.getContext(), trace);
        } else if (APPROVER_TYPE_MANUAL_SELECT.equals(approverType)) {
            resolvedUsers = resolveManualMembers(dto.getContext(), trace);
        } else {
            resolvedUsers = resolveManagerMembers(config, dto.getContext(), trace);
        }

        List<User> distinctUsers = resolvedUsers.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())
                ));

        ProcessFlowResolveApproversVO result = new ProcessFlowResolveApproversVO();
        result.setTrace(trace);
        if (distinctUsers.isEmpty()) {
            result.setResolutionType("EMPTY");
            result.setNextAction(missingHandler);
            return result;
        }

        Map<Long, List<com.finex.auth.dto.EmployeeDepartmentRefVO>> departmentsByUserId =
                UserDepartmentSupport.loadDepartmentRefsByUserId(
                        userMapper,
                        systemDepartmentMapper,
                        distinctUsers.stream().map(User::getId).toList()
                );
        result.setResolutionType("RESOLVED");
        result.setApproverUserIds(distinctUsers.stream().map(User::getId).toList());
        result.setApproverUsers(distinctUsers.stream().map(user -> {
            ProcessFlowResolvedUserVO item = new ProcessFlowResolvedUserVO();
            List<com.finex.auth.dto.EmployeeDepartmentRefVO> departments = departmentsByUserId.getOrDefault(user.getId(), List.of());
            if (departments.isEmpty() && user.getDeptId() != null) {
                com.finex.auth.dto.EmployeeDepartmentRefVO fallbackDepartment = new com.finex.auth.dto.EmployeeDepartmentRefVO();
                fallbackDepartment.setDeptId(user.getDeptId());
                SystemDepartment department = systemDepartmentMapper.selectById(user.getDeptId());
                fallbackDepartment.setDeptName(department == null ? null : department.getDeptName());
                departments = List.of(fallbackDepartment);
            }
            item.setUserId(user.getId());
            item.setUserName(normalizeUserName(user));
            item.setDepartments(new ArrayList<>(departments));
            item.setDeptId(UserDepartmentSupport.resolvePrimaryDepartmentId(departments));
            item.setDeptName(UserDepartmentSupport.joinDepartmentNames(departments));
            return item;
        }).toList());
        return result;
    }

    public List<User> resolveManagerMembers(Map<String, Object> config, Map<String, Object> context, List<String> trace) {
        Map<String, Object> managerConfig = structureSupport.normalizeManagerConfig(config.get("managerConfig"));
        String ruleMode = asText(managerConfig.get("ruleMode"), MANAGER_RULE_MODE_FORM_DEPT_MANAGER);
        String deptSource = asText(managerConfig.get("deptSource"), DEPT_SOURCE_UNDERTAKE);
        int managerLevel = limitLevel(asInteger(managerConfig.get("managerLevel"), 1), "主管级次");
        boolean orgTreeLookupEnabled = asBoolean(managerConfig.get("orgTreeLookupEnabled"), true);
        int lookupLevel = limitLevel(asInteger(managerConfig.get("orgTreeLookupLevel"), 1), "向上查找级次");
        Map<Long, SystemDepartment> departmentMap = loadAllDepartmentMap();
        List<Long> startDeptIds = resolveStartDeptIds(deptSource, context);
        if (startDeptIds.isEmpty()) {
            trace.add("未找到可用于解析主管的起始部门");
            return Collections.emptyList();
        }

        Long submitterUserId = asLong(context == null ? null : context.get("submitterUserId"));
        List<User> result = new ArrayList<>();
        for (Long deptId : startDeptIds) {
            SystemDepartment sourceDept = departmentMap.get(deptId);
            if (sourceDept == null) {
                trace.add("起始部门不存在：" + deptId);
                continue;
            }

            for (int levelIndex = 0; levelIndex < managerLevel; levelIndex++) {
                SystemDepartment targetDept = sourceDept;
                if (MANAGER_RULE_MODE_FORM_DEPT_MANAGER.equals(ruleMode)) {
                    targetDept = climbDepartment(sourceDept, departmentMap, levelIndex);
                    trace.add("部门 " + deptId + " 解析第 " + (levelIndex + 1) + " 级主管，命中部门："
                            + (targetDept == null ? "无" : targetDept.getDeptName()));
                }
                if (targetDept == null) {
                    trace.add("第 " + (levelIndex + 1) + " 级主管对应部门不存在");
                    return Collections.emptyList();
                }

                User approver = findLeaderForDepartment(
                        targetDept,
                        departmentMap,
                        submitterUserId,
                        orgTreeLookupEnabled,
                        lookupLevel,
                        trace
                );
                if (approver == null) {
                    trace.add("第 " + (levelIndex + 1) + " 级主管未命中审批人");
                    return Collections.emptyList();
                }
                result.add(approver);
            }
        }
        return result;
    }

    public User findLeaderForDepartment(
            SystemDepartment targetDept,
            Map<Long, SystemDepartment> departmentMap,
            Long submitterUserId,
            boolean orgTreeLookupEnabled,
            int lookupLevel,
            List<String> trace
    ) {
        if (targetDept == null) {
            return null;
        }
        LeaderResolution leaderResolution = resolveLeader(
                targetDept,
                departmentMap,
                orgTreeLookupEnabled,
                lookupLevel,
                trace
        );
        if (leaderResolution == null) {
            trace.add("No leader found for department: " + targetDept.getDeptName());
            return null;
        }

        User user = loadActiveUser(leaderResolution.userId());
        if (user == null) {
            trace.add("Leader user is inactive: " + leaderResolution.userId());
            return null;
        }
        if (submitterUserId != null && Objects.equals(leaderResolution.userId(), submitterUserId)) {
            trace.add("Resolved approver is also submitter: " + submitterUserId);
        }
        return user;
    }

    public List<User> resolveDesignatedMembers(Map<String, Object> config, List<String> trace) {
        List<Long> userIds = toLongList(toObjectMap(config.get("designatedMemberConfig")).get("userIds"));
        if (userIds.isEmpty()) {
            trace.add("未配置指定成员");
            return Collections.emptyList();
        }
        trace.add("指定成员：" + userIds);
        return loadActiveUsers(userIds);
    }

    public List<User> resolveDesignatedUserGroupMembers(
            Map<String, Object> config,
            Map<String, Object> context,
            List<String> trace
    ) {
        Long groupId = asLong(toObjectMap(config.get("designatedUserGroupConfig")).get("groupId"));
        if (groupId == null) {
            trace.add("未配置指定用户组");
            return Collections.emptyList();
        }
        trace.add("指定用户组：" + groupId);
        return userGroupResolverSupport.resolveMatchedMembers(groupId, context);
    }

    public List<User> resolveManualMembers(Map<String, Object> context, List<String> trace) {
        List<Long> userIds = toLongList(context == null ? null : context.get("manualSelectedUserIds"));
        if (userIds.isEmpty()) {
            trace.add("未选择手动审批人");
            return Collections.emptyList();
        }
        trace.add("手动选择审批人：" + userIds);
        return loadActiveUsers(userIds);
    }

    public List<Long> resolveStartDeptIds(String deptSource, Map<String, Object> context) {
        if (DEPT_SOURCE_SUBMITTER.equals(deptSource)) {
            List<Long> submitterDeptIds = toLongList(context == null ? null : context.get("submitterDeptIds"));
            if (!submitterDeptIds.isEmpty()) {
                return submitterDeptIds;
            }
            return toLongList(context == null ? null : context.get("submitterDeptId"));
        }

        List<Long> undertakeDeptIds = toLongList(context == null ? null : context.get("undertakeDeptIds"));
        if (!undertakeDeptIds.isEmpty()) {
            return undertakeDeptIds;
        }

        List<Long> submitterDeptIds = toLongList(context == null ? null : context.get("submitterDeptIds"));
        if (!submitterDeptIds.isEmpty()) {
            return submitterDeptIds;
        }
        return toLongList(context == null ? null : context.get("submitterDeptId"));
    }

    public SystemDepartment climbDepartment(SystemDepartment start, Map<Long, SystemDepartment> departmentMap, int steps) {
        SystemDepartment current = start;
        for (int index = 0; index < steps && current != null; index++) {
            current = current.getParentId() == null ? null : departmentMap.get(current.getParentId());
        }
        return current;
    }

    private LeaderResolution resolveLeader(
            SystemDepartment startDept,
            Map<Long, SystemDepartment> departmentMap,
            boolean allowLookup,
            int lookupLevel,
            List<String> trace
    ) {
        SystemDepartment current = startDept;
        int remaining = lookupLevel;
        while (current != null) {
            if (current.getLeaderUserId() != null && current.getLeaderUserId() > 0) {
                trace.add("Leader department hit: " + current.getDeptName());
                return new LeaderResolution(current.getId(), current.getLeaderUserId());
            }
            if (!allowLookup || remaining <= 0 || current.getParentId() == null) {
                break;
            }
            current = departmentMap.get(current.getParentId());
            remaining--;
        }
        return null;
    }

    private record LeaderResolution(Long departmentId, Long userId) {
    }
}
