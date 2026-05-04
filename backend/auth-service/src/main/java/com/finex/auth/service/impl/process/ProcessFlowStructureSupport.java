package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowConditionDTO;
import com.finex.auth.dto.ProcessFlowConditionFieldVO;
import com.finex.auth.dto.ProcessFlowConditionGroupDTO;
import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;
import com.finex.auth.dto.ProcessFlowSaveDTO;
import com.finex.auth.entity.ProcessFlowNode;
import com.finex.auth.entity.ProcessFlowRoute;
import com.finex.auth.entity.ProcessFlowScene;
import com.finex.auth.entity.ProcessFlowVersion;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ProcessFlowStructureSupport extends AbstractProcessFlowDesignSupport {

    private final ProcessUserGroupResolverSupport userGroupResolverSupport;

    public ProcessFlowStructureSupport(
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
        this.userGroupResolverSupport = userGroupResolverSupport;
    }

    public void validateFlowSave(ProcessFlowSaveDTO dto) {
        String flowName = trimToNull(dto.getFlowName());
        if (flowName == null) {
            throw new IllegalStateException("流程名称不能为空");
        }
        validatePmNameLength(flowName, "流程名称");
        normalizeNodes(dto.getNodes());
        normalizeRoutes(dto.getRoutes());

        Set<String> validConditionFieldKeys = buildConditionFields().stream()
                .map(ProcessFlowConditionFieldVO::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        validConditionFieldKeys.addAll(loadSelectableSharedArchiveCodes());
        Set<Long> validSceneIds = processFlowSceneMapper.selectList(
                Wrappers.<ProcessFlowScene>lambdaQuery().eq(ProcessFlowScene::getStatus, 1)
        ).stream().map(ProcessFlowScene::getId).collect(Collectors.toCollection(LinkedHashSet::new));

        if (dto.getNodes() != null) {
            for (int index = 0; index < dto.getNodes().size(); index++) {
                ProcessFlowNodeDTO node = dto.getNodes().get(index);
                validatePmNameLength(node.getNodeName(), "第 " + (index + 1) + " 个节点名称");
                if (node.getSceneId() != null && !validSceneIds.contains(node.getSceneId())) {
                    throw new IllegalStateException("第 " + (index + 1) + " 个节点绑定的场景已失效，请重新选择");
                }
            }
        }

        if (dto.getRoutes() == null) {
            return;
        }
        for (int routeIndex = 0; routeIndex < dto.getRoutes().size(); routeIndex++) {
            ProcessFlowRouteDTO route = dto.getRoutes().get(routeIndex);
            validatePmNameLength(route.getRouteName(), "第 " + (routeIndex + 1) + " 条分支名称");
            if (route.getConditionGroups() == null) {
                continue;
            }
            for (int groupIndex = 0; groupIndex < route.getConditionGroups().size(); groupIndex++) {
                ProcessFlowConditionGroupDTO group = route.getConditionGroups().get(groupIndex);
                if (group.getConditions() == null) {
                    continue;
                }
                for (int conditionIndex = 0; conditionIndex < group.getConditions().size(); conditionIndex++) {
                    ProcessFlowConditionDTO condition = group.getConditions().get(conditionIndex);
                    String fieldKey = trimToNull(condition.getFieldKey());
                    if (fieldKey == null) {
                        continue;
                    }
                    if (fieldKey.length() > PM_FIELD_KEY_MAX_LENGTH) {
                        throw new IllegalStateException("第 " + (routeIndex + 1) + " 条分支第 " + (groupIndex + 1)
                                + " 组第 " + (conditionIndex + 1) + " 个条件字段标识长度不能超过 64 个字符");
                    }
                    if (!validConditionFieldKeys.contains(fieldKey)) {
                        throw new IllegalStateException("第 " + (routeIndex + 1) + " 条分支第 " + (groupIndex + 1)
                                + " 组第 " + (conditionIndex + 1) + " 个条件字段已失效，请重新选择");
                    }
                }
            }
        }
    }

    private Set<String> loadSelectableSharedArchiveCodes() {
        return processCustomArchiveDesignMapper.selectList(
                Wrappers.<com.finex.auth.entity.ProcessCustomArchiveDesign>lambdaQuery()
                        .eq(com.finex.auth.entity.ProcessCustomArchiveDesign::getStatus, 1)
                        .eq(com.finex.auth.entity.ProcessCustomArchiveDesign::getArchiveType, "SELECT")
        ).stream()
                .map(com.finex.auth.entity.ProcessCustomArchiveDesign::getArchiveCode)
                .map(this::trimToNull)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void normalizeNodes(List<ProcessFlowNodeDTO> nodes) {
        if (nodes == null) {
            return;
        }
        Set<String> nodeKeys = new LinkedHashSet<>();
        for (int index = 0; index < nodes.size(); index++) {
            ProcessFlowNodeDTO node = nodes.get(index);
            if (trimToNull(node.getNodeKey()) == null) {
                node.setNodeKey("node-" + (index + 1));
            }
            if (!nodeKeys.add(node.getNodeKey())) {
                throw new IllegalStateException("节点标识不能重复");
            }
            if (node.getDisplayOrder() == null) {
                node.setDisplayOrder(index + 1);
            }
            node.setParentNodeKey(trimToNull(node.getParentNodeKey()));
            node.setNodeType(asText(node.getNodeType(), NODE_TYPE_APPROVAL));
            if (trimToNull(node.getNodeName()) == null) {
                node.setNodeName(defaultNodeName(node.getNodeType(), index + 1));
            }
            validatePmNameLength(node.getNodeName(), "第 " + (index + 1) + " 个节点名称");
            node.setConfig(normalizeNodeConfig(node.getNodeType(), node.getConfig(), true));
        }
    }

    public void normalizeRoutes(List<ProcessFlowRouteDTO> routes) {
        if (routes == null) {
            return;
        }
        Set<String> routeKeys = new LinkedHashSet<>();
        for (int index = 0; index < routes.size(); index++) {
            ProcessFlowRouteDTO route = routes.get(index);
            if (trimToNull(route.getRouteKey()) == null) {
                route.setRouteKey("route-" + (index + 1));
            }
            if (!routeKeys.add(route.getRouteKey())) {
                throw new IllegalStateException("分支标识不能重复");
            }
            if (route.getPriority() == null) {
                route.setPriority(index + 1);
            }
            route.setDefaultRoute(Boolean.TRUE.equals(route.getDefaultRoute()));
            route.setAttachBelowNodes(Boolean.TRUE.equals(route.getAttachBelowNodes()));
            if (Boolean.TRUE.equals(route.getDefaultRoute())) {
                route.setConditionGroups(new ArrayList<>());
            }
            if (route.getConditionGroups() == null) {
                route.setConditionGroups(new ArrayList<>());
            }
            validatePmNameLength(route.getRouteName(), "第 " + (index + 1) + " 条分支名称");
        }

        Map<String, List<ProcessFlowRouteDTO>> routesBySourceNode = routes.stream()
                .collect(Collectors.groupingBy(
                        item -> asText(item.getSourceNodeKey(), "__ROOT__"),
                        LinkedHashMap::new,
                        Collectors.toCollection(ArrayList::new)
                ));
        for (List<ProcessFlowRouteDTO> branchRoutes : routesBySourceNode.values()) {
            long attachedCount = branchRoutes.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getAttachBelowNodes()))
                    .count();
            if (attachedCount > 1) {
                throw new IllegalStateException("同一分支块最多只能有 1 条通道开启附带下方节点");
            }
            long defaultRouteCount = branchRoutes.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getDefaultRoute()))
                    .count();
            if (defaultRouteCount > 1) {
                throw new IllegalStateException("同一分支块最多只能有 1 条 else 分支");
            }
            normalizeBranchRoutePriorities(branchRoutes);
        }
    }

    public void normalizeBranchRoutePriorities(List<ProcessFlowRouteDTO> branchRoutes) {
        if (branchRoutes == null || branchRoutes.isEmpty()) {
            return;
        }
        List<ProcessFlowRouteDTO> orderedRoutes = new ArrayList<>(branchRoutes);
        orderedRoutes.sort(
                Comparator.comparing((ProcessFlowRouteDTO item) -> !Boolean.TRUE.equals(item.getAttachBelowNodes()))
                        .thenComparing(item -> item.getPriority() == null ? Integer.MAX_VALUE : item.getPriority())
                        .thenComparing(item -> asText(item.getRouteKey(), ""))
        );
        for (int index = 0; index < orderedRoutes.size(); index++) {
            orderedRoutes.get(index).setPriority(index + 1);
        }
    }

    public Map<String, Object> normalizeNodeConfig(String nodeType, Map<String, Object> rawConfig, boolean strictValidation) {
        Map<String, Object> config = rawConfig == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rawConfig);
        if (NODE_TYPE_CC.equals(nodeType)) {
            return normalizeCcNodeConfig(config, strictValidation);
        }
        if (NODE_TYPE_PAYMENT.equals(nodeType)) {
            return normalizePaymentNodeConfig(config, strictValidation);
        }
        if (!NODE_TYPE_APPROVAL.equals(nodeType)) {
            return config;
        }

        String approverType = asText(config.get("approverType"), APPROVER_TYPE_MANAGER);
        if (!APPROVER_TYPES.contains(approverType)) {
            throw new IllegalStateException("审批人类型不合法");
        }
        config.put("approverType", approverType);

        String missingHandler = normalizeMissingHandler(asText(config.get("missingHandler"), MISSING_HANDLER_AUTO_SKIP));
        config.put("missingHandler", missingHandler);

        String approvalMode = asText(config.get("approvalMode"), APPROVAL_MODE_OR_SIGN);
        if (!APPROVAL_MODES.contains(approvalMode)) {
            throw new IllegalStateException("审批方式不合法");
        }
        Map<String, Object> managerConfig = normalizeManagerConfig(config.get("managerConfig"));
        Map<String, Object> designatedMemberConfig = normalizeDesignatedMemberConfig(config.get("designatedMemberConfig"));
        Map<String, Object> designatedUserGroupConfig = normalizeDesignatedUserGroupConfig(config.get("designatedUserGroupConfig"));
        Map<String, Object> manualSelectConfig = normalizeManualSelectConfig(config.get("manualSelectConfig"));
        if (shouldForceManagerAndSign(approverType, managerConfig)
                || APPROVER_TYPE_DESIGNATED_USER_GROUP.equals(approverType)) {
            approvalMode = APPROVAL_MODE_AND_SIGN;
        }
        config.put("approvalMode", approvalMode);
        config.put("opinionDefaults", defaultedOpinions(config.get("opinionDefaults")));
        config.put("specialSettings", toStringList(config.get("specialSettings")));
        config.put("managerConfig", managerConfig);
        config.put("designatedMemberConfig", designatedMemberConfig);
        config.put("designatedUserGroupConfig", designatedUserGroupConfig);
        config.put("manualSelectConfig", manualSelectConfig);

        if (strictValidation && APPROVER_TYPE_DESIGNATED_MEMBER.equals(approverType)) {
            List<Object> designatedMembers = normalizeDesignatedMemberEntries(designatedMemberConfig.get("userIds"));
            if (designatedMembers.isEmpty()) {
                throw new IllegalStateException("指定成员至少选择一名用户");
            }
            List<Long> userIds = extractDesignatedMemberUserIds(designatedMembers);
            validateActiveUsers(userIds);
        }
        if (strictValidation && APPROVER_TYPE_DESIGNATED_USER_GROUP.equals(approverType)) {
            Long groupId = asLong(designatedUserGroupConfig.get("groupId"));
            if (groupId == null) {
                throw new IllegalStateException("指定用户组至少选择一个 2 级用户组");
            }
            userGroupResolverSupport.requireSecondLevelGroup(groupId);
        }
        return config;
    }

    private Map<String, Object> normalizeCcNodeConfig(Map<String, Object> config, boolean strictValidation) {
        String approverType = normalizeApprovalStyleApproverType(config.get("approverType"), config.get("receiverType"));
        config.put("approverType", approverType);
        config.put("missingHandler", normalizeMissingHandler(asText(config.get("missingHandler"), MISSING_HANDLER_AUTO_SKIP)));
        config.put("timing", asText(config.get("timing"), "ON_ENTER"));
        config.put("receiverType", asText(config.get("receiverType"), APPROVER_TYPE_DESIGNATED_MEMBER));
        config.put("receiverUserIds", toLongList(config.get("receiverUserIds")));
        config.put("specialSettings", toStringList(config.get("specialSettings")));
        normalizeApprovalStyleConfigs(config, approverType, strictValidation);
        return config;
    }

    private Map<String, Object> normalizePaymentNodeConfig(Map<String, Object> config, boolean strictValidation) {
        String approverType = normalizeApprovalStyleApproverType(config.get("approverType"), config.get("executorType"));
        config.put("approverType", approverType);
        config.put("missingHandler", normalizeMissingHandler(asText(config.get("missingHandler"), MISSING_HANDLER_AUTO_SKIP)));
        String approvalMode = asText(config.get("approvalMode"), APPROVAL_MODE_OR_SIGN);
        if (!APPROVAL_MODES.contains(approvalMode)) {
            throw new IllegalStateException("审批方式不合法");
        }
        if (APPROVER_TYPE_DESIGNATED_USER_GROUP.equals(approverType)) {
            approvalMode = APPROVAL_MODE_AND_SIGN;
        }
        config.put("approvalMode", approvalMode);
        config.put("opinionDefaults", defaultedOpinions(config.get("opinionDefaults")));
        config.put("specialSettings", toStringList(config.get("specialSettings")));
        config.put("paymentAction", asText(config.get("paymentAction"), "GENERATE_PAYMENT"));
        config.put("executorType", asText(config.get("executorType"), APPROVER_TYPE_DESIGNATED_MEMBER));
        config.put("executorUserIds", toLongList(config.get("executorUserIds")));
        normalizeApprovalStyleConfigs(config, approverType, strictValidation);
        return config;
    }

    private void normalizeApprovalStyleConfigs(Map<String, Object> config, String approverType, boolean strictValidation) {
        Map<String, Object> designatedMemberConfig = normalizeDesignatedMemberConfig(config.get("designatedMemberConfig"));
        Map<String, Object> designatedUserGroupConfig = normalizeDesignatedUserGroupConfig(config.get("designatedUserGroupConfig"));
        Map<String, Object> manualSelectConfig = normalizeManualSelectConfig(config.get("manualSelectConfig"));
        config.put("designatedMemberConfig", designatedMemberConfig);
        config.put("designatedUserGroupConfig", designatedUserGroupConfig);
        config.put("manualSelectConfig", manualSelectConfig);

        if (!strictValidation || approverType == null) {
            return;
        }
        if (APPROVER_TYPE_DESIGNATED_MEMBER.equals(approverType)) {
            List<Object> designatedMembers = normalizeDesignatedMemberEntries(designatedMemberConfig.get("userIds"));
            if (designatedMembers.isEmpty()) {
                throw new IllegalStateException("指定成员至少选择一名用户");
            }
            List<Long> userIds = extractDesignatedMemberUserIds(designatedMembers);
            validateActiveUsers(userIds);
            return;
        }
        if (APPROVER_TYPE_DESIGNATED_USER_GROUP.equals(approverType)) {
            Long groupId = asLong(designatedUserGroupConfig.get("groupId"));
            if (groupId == null) {
                throw new IllegalStateException("指定用户组至少选择一个 2 级用户组");
            }
            userGroupResolverSupport.requireSecondLevelGroup(groupId);
        }
    }

    private String normalizeApprovalStyleApproverType(Object approverTypeSource, Object legacyTypeSource) {
        String approverType = trimToNull(asText(approverTypeSource, null));
        if (approverType != null && APPROVER_TYPES.contains(approverType) && !APPROVER_TYPE_MANAGER.equals(approverType)) {
            return approverType;
        }
        String legacyType = trimToNull(asText(legacyTypeSource, null));
        if (APPROVER_TYPE_DESIGNATED_MEMBER.equals(legacyType)) {
            return APPROVER_TYPE_DESIGNATED_MEMBER;
        }
        return null;
    }

    public ProcessFlowVersion createDraftVersion(Long flowId, int versionNo, ProcessFlowSaveDTO dto) {
        ProcessFlowVersion version = new ProcessFlowVersion();
        version.setFlowId(flowId);
        version.setVersionNo(versionNo);
        version.setVersionStatus(VERSION_STATUS_DRAFT);
        version.setSnapshotJson(writeSnapshot(dto));
        processFlowVersionMapper.insert(version);
        replaceVersionNodesAndRoutes(version.getId(), dto);
        return version;
    }

    public void replaceVersionNodesAndRoutes(Long versionId, ProcessFlowSaveDTO dto) {
        processFlowNodeMapper.delete(
                Wrappers.<ProcessFlowNode>lambdaQuery().eq(ProcessFlowNode::getVersionId, versionId)
        );
        processFlowRouteMapper.delete(
                Wrappers.<ProcessFlowRoute>lambdaQuery().eq(ProcessFlowRoute::getVersionId, versionId)
        );

        List<ProcessFlowNodeDTO> nodes = dto.getNodes() == null ? Collections.emptyList() : dto.getNodes();
        for (ProcessFlowNodeDTO item : nodes) {
            ProcessFlowNode node = new ProcessFlowNode();
            node.setVersionId(versionId);
            node.setNodeKey(item.getNodeKey());
            node.setNodeType(item.getNodeType());
            node.setNodeName(item.getNodeName());
            node.setSceneId(item.getSceneId());
            node.setParentNodeKey(trimToNull(item.getParentNodeKey()));
            node.setDisplayOrder(item.getDisplayOrder() == null ? 0 : item.getDisplayOrder());
            node.setConfigJson(writeValue(item.getConfig()));
            processFlowNodeMapper.insert(node);
        }

        List<ProcessFlowRouteDTO> routes = dto.getRoutes() == null ? Collections.emptyList() : dto.getRoutes();
        for (ProcessFlowRouteDTO item : routes) {
            ProcessFlowRoute route = new ProcessFlowRoute();
            route.setVersionId(versionId);
            route.setRouteKey(item.getRouteKey());
            route.setSourceNodeKey(trimToNull(item.getSourceNodeKey()));
            route.setTargetNodeKey(trimToNull(item.getTargetNodeKey()));
            route.setRouteName(asText(item.getRouteName(), "分支"));
            route.setPriority(item.getPriority() == null ? 1 : item.getPriority());
            route.setDefaultRoute(Boolean.TRUE.equals(item.getDefaultRoute()) ? 1 : 0);
            route.setAttachBelowNodes(Boolean.TRUE.equals(item.getAttachBelowNodes()) ? 1 : 0);
            route.setConditionJson(writeValue(item.getConditionGroups() == null ? Collections.emptyList() : item.getConditionGroups()));
            processFlowRouteMapper.insert(route);
        }
    }

    public Map<String, Object> normalizeManagerConfig(Object source) {
        Map<String, Object> raw = toObjectMap(source);
        Map<String, Object> config = new LinkedHashMap<>();

        boolean legacyFormDeptManagerEnabled = asBoolean(raw.get("formDeptManagerEnabled"), false);
        String ruleMode = asText(
                raw.get("ruleMode"),
                legacyFormDeptManagerEnabled ? MANAGER_RULE_MODE_FORM_DEPT_MANAGER : MANAGER_RULE_MODE_FORM_DEPT_MANAGER
        );
        config.put("ruleMode", ruleMode);

        String deptSource = asText(raw.get("deptSource"), DEPT_SOURCE_UNDERTAKE);
        if (!DEPT_SOURCES.contains(deptSource)) {
            throw new IllegalStateException("部门来源不合法");
        }
        config.put("deptSource", deptSource);
        config.put("managerLevel", limitLevel(asInteger(raw.get("managerLevel"), 1), "主管级次"));
        config.put("orgTreeLookupEnabled", asBoolean(raw.get("orgTreeLookupEnabled"), true));
        config.put("orgTreeLookupLevel", limitLevel(asInteger(raw.get("orgTreeLookupLevel"), 1), "向上查找级次"));
        return config;
    }

    public Map<String, Object> normalizeDesignatedMemberConfig(Object source) {
        Map<String, Object> raw = toObjectMap(source);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("userIds", normalizeDesignatedMemberEntries(raw.get("userIds")));
        return config;
    }

    public Map<String, Object> normalizeManualSelectConfig(Object source) {
        Map<String, Object> raw = toObjectMap(source);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("candidateScope", asText(raw.get("candidateScope"), MANUAL_SCOPE_ALL_ACTIVE_USERS));
        return config;
    }

    public Map<String, Object> normalizeDesignatedUserGroupConfig(Object source) {
        Map<String, Object> raw = toObjectMap(source);
        Map<String, Object> config = new LinkedHashMap<>();
        Long groupId = asLong(raw.get("groupId"));
        config.put("groupId", groupId);
        return config;
    }

    private List<String> defaultedOpinions(Object value) {
        List<String> opinions = toStringList(value);
        return opinions.isEmpty() ? new ArrayList<>(DEFAULT_OPINIONS) : opinions;
    }

    private boolean shouldForceManagerAndSign(String approverType, Map<String, Object> managerConfig) {
        if (!APPROVER_TYPE_MANAGER.equals(approverType)) {
            return false;
        }
        return limitLevel(asInteger(managerConfig.get("managerLevel"), 1), "主管级次") > 1;
    }
}
