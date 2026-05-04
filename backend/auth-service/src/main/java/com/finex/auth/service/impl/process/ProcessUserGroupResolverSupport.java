package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowConditionGroupDTO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.entity.ProcessFlowNode;
import com.finex.auth.entity.ProcessUserGroup;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ProcessFlowNodeMapper;
import com.finex.auth.mapper.ProcessUserGroupMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.support.ProcessConditionMatchSupport;

import java.math.BigDecimal;
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

public class ProcessUserGroupResolverSupport {

    private final ProcessUserGroupMapper processUserGroupMapper;
    private final ProcessFlowNodeMapper processFlowNodeMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final ProcessConditionMatchSupport conditionMatchSupport = new ProcessConditionMatchSupport();

    public ProcessUserGroupResolverSupport(
            ProcessUserGroupMapper processUserGroupMapper,
            ProcessFlowNodeMapper processFlowNodeMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper
    ) {
        this.processUserGroupMapper = processUserGroupMapper;
        this.processFlowNodeMapper = processFlowNodeMapper;
        this.systemDepartmentMapper = systemDepartmentMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    public List<ProcessFormOptionVO> listSecondLevelGroupOptions() {
        List<ProcessUserGroup> groups = processUserGroupMapper.selectList(
                Wrappers.<ProcessUserGroup>lambdaQuery()
                        .eq(ProcessUserGroup::getCodeLevel, 2)
                        .orderByAsc(ProcessUserGroup::getGroupCode, ProcessUserGroup::getId)
        );
        if (groups.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ProcessUserGroup> parentMap = loadGroupMap(groups.stream()
                .map(ProcessUserGroup::getParentId)
                .filter(Objects::nonNull)
                .toList());
        return groups.stream().map(group -> {
            ProcessFormOptionVO option = new ProcessFormOptionVO();
            option.setValue(String.valueOf(group.getId()));
            ProcessUserGroup parent = group.getParentId() == null ? null : parentMap.get(group.getParentId());
            option.setLabel(parent == null ? group.getGroupName() : parent.getGroupName() + " / " + group.getGroupName());
            return option;
        }).toList();
    }

    public ProcessUserGroup requireSecondLevelGroup(Long groupId) {
        ProcessUserGroup group = processUserGroupMapper.selectById(groupId);
        if (group == null) {
            throw new IllegalStateException("\u6307\u5b9a\u7528\u6237\u7ec4\u4e0d\u5b58\u5728");
        }
        if (!Objects.equals(group.getCodeLevel(), 2)) {
            throw new IllegalStateException("\u5ba1\u6279\u6d41\u53ea\u80fd\u9009\u62e9 2 \u7ea7\u7528\u6237\u7ec4");
        }
        return group;
    }

    public boolean isReferencedByFlow(Long groupId) {
        if (groupId == null) {
            return false;
        }
        if (processFlowNodeMapper == null) {
            return false;
        }
        List<ProcessFlowNode> nodes = processFlowNodeMapper.selectList(
                Wrappers.<ProcessFlowNode>lambdaQuery()
                        .eq(ProcessFlowNode::getNodeType, "APPROVAL")
        );
        for (ProcessFlowNode node : nodes) {
            Map<String, Object> config = readMap(node.getConfigJson());
            if (!Objects.equals("DESIGNATED_USER_GROUP", trimToNull(asText(config.get("approverType"))))) {
                continue;
            }
            Long configuredGroupId = asLong(toObjectMap(config.get("designatedUserGroupConfig")).get("groupId"));
            if (Objects.equals(configuredGroupId, groupId)) {
                return true;
            }
        }
        return false;
    }

    public List<User> resolveMatchedMembers(Long groupId, Map<String, Object> runtimeContext) {
        ProcessUserGroup secondLevelGroup = requireSecondLevelGroup(groupId);
        List<ProcessUserGroup> allGroups = loadAllGroups();
        Map<Long, List<ProcessUserGroup>> childrenByParent = allGroups.stream()
                .filter(group -> group.getParentId() != null)
                .collect(Collectors.groupingBy(ProcessUserGroup::getParentId, LinkedHashMap::new, Collectors.toList()));
        List<ProcessUserGroup> functionalGroups = new ArrayList<>();
        collectFunctionalGroups(secondLevelGroup.getId(), childrenByParent, functionalGroups);
        if (functionalGroups.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> evaluationContext = buildScopeEvaluationContext(runtimeContext);
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        for (ProcessUserGroup group : functionalGroups) {
            if (!matchesScope(group, evaluationContext)) {
                continue;
            }
            for (String userIdValue : deserializeStringList(group.getMemberUserIdsJson())) {
                Long userId = asLong(userIdValue);
                if (userId != null) {
                    userIds.add(userId);
                }
            }
        }
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .filter(user -> Objects.equals(user.getStatus(), 1))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(User::getId, user -> user, (left, right) -> left, LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())
                ));
    }

    private boolean matchesScope(ProcessUserGroup group, Map<String, Object> evaluationContext) {
        List<ProcessFlowConditionGroupDTO> groups = readConditionGroups(group.getScopeConditionGroupsJson());
        if (groups.isEmpty()) {
            return true;
        }
        return conditionMatchSupport.matches(groups, evaluationContext::get);
    }

    private Map<String, Object> buildScopeEvaluationContext(Map<String, Object> runtimeContext) {
        Map<String, Object> context = runtimeContext == null ? new LinkedHashMap<>() : new LinkedHashMap<>(runtimeContext);
        Map<Long, SystemDepartment> departmentMap = loadDepartmentMap();

        List<Long> undertakeDeptIds = toLongList(context.get("undertakeDeptIds"));
        if (!undertakeDeptIds.isEmpty()) {
            context.put(ProcessUserGroupScopeSupport.FIELD_UNDERTAKE_DEPT_EXACT, undertakeDeptIds.stream().map(String::valueOf).toList());
            context.put(ProcessUserGroupScopeSupport.FIELD_UNDERTAKE_DEPT_WITH_CHILDREN, expandDepartmentLineageIds(undertakeDeptIds, departmentMap));
        }

        List<Long> submitterDeptIds = toLongList(context.get("submitterDeptIds"));
        if (submitterDeptIds.isEmpty()) {
            submitterDeptIds = toLongList(context.get("submitterDeptId"));
        }
        if (!submitterDeptIds.isEmpty()) {
            context.put(ProcessUserGroupScopeSupport.FIELD_SUBMITTER_DEPT_EXACT, submitterDeptIds.stream().map(String::valueOf).toList());
            context.put(ProcessUserGroupScopeSupport.FIELD_SUBMITTER_DEPT_WITH_CHILDREN, expandDepartmentLineageIds(submitterDeptIds, departmentMap));
        }

        String paymentCompanyId = trimToNull(asText(context.get("paymentCompanyId")));
        if (paymentCompanyId != null) {
            context.put(ProcessUserGroupScopeSupport.FIELD_PAYMENT_COMPANY_ID, paymentCompanyId);
        }

        BigDecimal actualPaymentAmount = toBigDecimal(context.get("actualPaymentAmount"));
        if (actualPaymentAmount != null) {
            context.put(ProcessUserGroupScopeSupport.FIELD_ACTUAL_PAYMENT_AMOUNT, actualPaymentAmount);
        }
        return context;
    }

    private List<String> expandDepartmentLineageIds(List<Long> departmentIds, Map<Long, SystemDepartment> departmentMap) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Long departmentId : departmentIds) {
            SystemDepartment current = departmentMap.get(departmentId);
            while (current != null && current.getId() != null) {
                result.add(String.valueOf(current.getId()));
                current = current.getParentId() == null ? null : departmentMap.get(current.getParentId());
            }
        }
        return new ArrayList<>(result);
    }

    private void collectFunctionalGroups(
            Long parentId,
            Map<Long, List<ProcessUserGroup>> childrenByParent,
            List<ProcessUserGroup> collector
    ) {
        for (ProcessUserGroup child : childrenByParent.getOrDefault(parentId, Collections.emptyList())) {
            if (Objects.equals(child.getCodeLevel(), 3)) {
                collector.add(child);
                continue;
            }
            collectFunctionalGroups(child.getId(), childrenByParent, collector);
        }
    }

    private List<ProcessUserGroup> loadAllGroups() {
        return processUserGroupMapper.selectList(
                Wrappers.<ProcessUserGroup>lambdaQuery()
                        .orderByAsc(ProcessUserGroup::getGroupCode, ProcessUserGroup::getId)
        );
    }

    private Map<Long, ProcessUserGroup> loadGroupMap(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return processUserGroupMapper.selectBatchIds(ids).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        ProcessUserGroup::getId,
                        group -> group,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, SystemDepartment> loadDepartmentMap() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .eq(SystemDepartment::getStatus, 1)
        ).stream().collect(Collectors.toMap(
                SystemDepartment::getId,
                department -> department,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private List<ProcessFlowConditionGroupDTO> readConditionGroups(String json) {
        if (trimToNull(json) == null) {
            return Collections.emptyList();
        }
        try {
            List<ProcessFlowConditionGroupDTO> groups = objectMapper.readValue(json, new TypeReference<List<ProcessFlowConditionGroupDTO>>() {});
            return groups == null ? Collections.emptyList() : groups;
        } catch (Exception ex) {
            throw new IllegalStateException("\u7528\u6237\u7ec4\u7ba1\u7406\u8303\u56f4\u89e3\u6790\u5931\u8d25", ex);
        }
    }

    private List<String> deserializeStringList(String json) {
        if (trimToNull(json) == null) {
            return Collections.emptyList();
        }
        try {
            List<String> values = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return values == null ? Collections.emptyList() : values.stream()
                    .map(this::trimToNull)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (Exception ex) {
            throw new IllegalStateException("\u7528\u6237\u7ec4\u6210\u5458\u89e3\u6790\u5931\u8d25", ex);
        }
    }

    private Map<String, Object> readMap(String json) {
        if (trimToNull(json) == null) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("\u6d41\u7a0b\u914d\u7f6e\u89e3\u6790\u5931\u8d25", ex);
        }
    }

    private Map<String, Object> toObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private List<Long> toLongList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Collection<?> collection) {
            List<Long> result = new ArrayList<>();
            for (Object item : collection) {
                Long numeric = asLong(item);
                if (numeric != null) {
                    result.add(numeric);
                }
            }
            return result;
        }
        Long numeric = asLong(value);
        return numeric == null ? Collections.emptyList() : List.of(numeric);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(String.valueOf(number));
        }
        String normalized = trimToNull(String.valueOf(value));
        if (normalized == null) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String normalized = trimToNull(String.valueOf(value));
        if (normalized == null) {
            return null;
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
