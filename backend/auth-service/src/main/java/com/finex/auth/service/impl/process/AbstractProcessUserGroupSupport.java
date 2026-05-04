package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowConditionDTO;
import com.finex.auth.dto.ProcessFlowConditionFieldVO;
import com.finex.auth.dto.ProcessFlowConditionGroupDTO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.dto.ProcessUserGroupDetailVO;
import com.finex.auth.dto.ProcessUserGroupMetaVO;
import com.finex.auth.dto.ProcessUserGroupSaveDTO;
import com.finex.auth.dto.ProcessUserGroupTreeVO;
import com.finex.auth.entity.ProcessUserGroup;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.mapper.CodeSequenceMapper;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
import com.finex.auth.mapper.ProcessCustomArchiveRuleMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessTemplateCategoryMapper;
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
import com.finex.auth.mapper.ProcessUserGroupMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.ProcessExpenseDetailDesignService;
import com.finex.auth.service.ProcessFlowDesignService;
import com.finex.auth.service.ProcessFormDesignService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

abstract class AbstractProcessUserGroupSupport extends AbstractProcessManagementSupport {

    private final ProcessUserGroupMapper processUserGroupMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final ProcessUserGroupResolverSupport resolverSupport;

    protected AbstractProcessUserGroupSupport(
            ProcessTemplateCategoryMapper categoryMapper,
            ProcessDocumentTemplateMapper templateMapper,
            CodeSequenceMapper codeSequenceMapper,
            ProcessTemplateScopeMapper scopeMapper,
            ProcessCustomArchiveDesignMapper customArchiveDesignMapper,
            ProcessCustomArchiveItemMapper customArchiveItemMapper,
            ProcessCustomArchiveRuleMapper customArchiveRuleMapper,
            ProcessExpenseTypeMapper processExpenseTypeMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper,
            ProcessFormDesignService processFormDesignService,
            ProcessExpenseDetailDesignService processExpenseDetailDesignService,
            ProcessFlowDesignService processFlowDesignService,
            ObjectMapper objectMapper,
            ProcessUserGroupMapper processUserGroupMapper,
            SystemCompanyMapper systemCompanyMapper,
            ProcessUserGroupResolverSupport resolverSupport
    ) {
        super(
                categoryMapper,
                templateMapper,
                codeSequenceMapper,
                scopeMapper,
                customArchiveDesignMapper,
                customArchiveItemMapper,
                customArchiveRuleMapper,
                processExpenseTypeMapper,
                systemDepartmentMapper,
                userMapper,
                processFormDesignService,
                processExpenseDetailDesignService,
                processFlowDesignService,
                objectMapper
        );
        this.processUserGroupMapper = processUserGroupMapper;
        this.systemCompanyMapper = systemCompanyMapper;
        this.resolverSupport = resolverSupport;
    }

    protected List<ProcessUserGroup> loadAllUserGroups() {
        return processUserGroupMapper.selectList(
                Wrappers.<ProcessUserGroup>lambdaQuery()
                        .orderByAsc(ProcessUserGroup::getGroupCode, ProcessUserGroup::getId)
        );
    }

    protected ProcessUserGroup requireUserGroup(Long id) {
        ProcessUserGroup group = processUserGroupMapper.selectById(id);
        if (group == null) {
            throw new IllegalStateException("\u7528\u6237\u7ec4\u4e0d\u5b58\u5728");
        }
        return group;
    }

    protected List<ProcessUserGroupTreeVO> buildTree(List<ProcessUserGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ProcessUserGroupTreeVO> nodeMap = new LinkedHashMap<>();
        List<ProcessUserGroupTreeVO> roots = new ArrayList<>();
        for (ProcessUserGroup group : groups) {
            nodeMap.put(group.getId(), toTreeNode(group));
        }
        for (ProcessUserGroup group : groups) {
            ProcessUserGroupTreeVO node = nodeMap.get(group.getId());
            if (group.getParentId() == null || !nodeMap.containsKey(group.getParentId())) {
                roots.add(node);
                continue;
            }
            nodeMap.get(group.getParentId()).getChildren().add(node);
        }
        return roots;
    }

    protected ProcessUserGroupDetailVO buildDetail(ProcessUserGroup group) {
        ProcessUserGroupDetailVO detail = new ProcessUserGroupDetailVO();
        detail.setId(group.getId());
        detail.setParentId(group.getParentId());
        detail.setGroupCode(group.getGroupCode());
        detail.setGroupName(group.getGroupName());
        detail.setCodeLevel(group.getCodeLevel());
        detail.setMemberUserIds(deserializeStringList(group.getMemberUserIdsJson()));
        detail.setScopeConditionGroups(deserializeScopeConditionGroups(group.getScopeConditionGroupsJson()));
        return detail;
    }

    protected ProcessUserGroupMetaVO buildMeta() {
        ProcessUserGroupMetaVO meta = new ProcessUserGroupMetaVO();
        meta.setScopeConditionFields(ProcessUserGroupScopeSupport.buildConditionFields());
        meta.setScopeOperatorOptions(ProcessUserGroupScopeSupport.buildOperatorOptions());
        meta.setCompanyOptions(loadCompanyOptions());
        meta.setDepartmentOptions(loadDepartmentOptions());
        meta.setUserOptions(loadUserOptions());
        return meta;
    }

    protected void validateUserGroupSave(ProcessUserGroupSaveDTO dto, ProcessUserGroup existing) {
        validatePmNameLength(dto.getGroupName(), "\u7528\u6237\u7ec4\u540d\u79f0");
        ProcessUserGroup parent = resolveTargetParent(dto, existing);
        int targetLevel = resolveTargetLevel(parent, existing);
        if (targetLevel < 1 || targetLevel > 3) {
            throw new IllegalStateException("\u7528\u6237\u7ec4\u6700\u591a\u53ea\u652f\u6301 3 \u7ea7");
        }

        List<String> memberUserIds = normalizeIdList(dto.getMemberUserIds());
        List<ProcessFlowConditionGroupDTO> scopeConditionGroups = normalizeScopeConditionGroups(dto.getScopeConditionGroups());

        if (targetLevel < 3) {
            if (!memberUserIds.isEmpty()) {
                throw new IllegalStateException("1 \u7ea7\u548c 2 \u7ea7\u7528\u6237\u7ec4\u4e0d\u5141\u8bb8\u914d\u7f6e\u6210\u5458");
            }
            if (!scopeConditionGroups.isEmpty()) {
                throw new IllegalStateException("1 \u7ea7\u548c 2 \u7ea7\u7528\u6237\u7ec4\u4e0d\u5141\u8bb8\u914d\u7f6e\u7ba1\u7406\u8303\u56f4");
            }
        }

        validateSelectableIds(memberUserIds, loadValidUserIdSet(), "\u7528\u6237\u7ec4\u6210\u5458");
        validateScopeConditionGroups(scopeConditionGroups);
    }

    protected void applyUserGroup(ProcessUserGroup entity, ProcessUserGroupSaveDTO dto, boolean creating) {
        ProcessUserGroup parent = resolveTargetParent(dto, creating ? null : entity);
        int targetLevel = resolveTargetLevel(parent, creating ? null : entity);
        entity.setParentId(parent == null ? null : parent.getId());
        if (creating) {
            String groupCode = buildNextGroupCode(parent);
            entity.setGroupCode(groupCode);
            entity.setCodeLevel(targetLevel);
            entity.setCodePrefix(groupCode.substring(0, Math.min(4, groupCode.length())));
        }
        entity.setGroupName(trimToEmpty(dto.getGroupName()));
        entity.setMemberUserIdsJson(targetLevel == 3 ? serializeStringList(dto.getMemberUserIds()) : serializeStringList(Collections.emptyList()));
        entity.setScopeConditionGroupsJson(targetLevel == 3 ? serializeScopeConditionGroups(dto.getScopeConditionGroups()) : serializeScopeConditionGroups(Collections.emptyList()));
    }

    protected boolean hasChildren(Long id) {
        Long count = processUserGroupMapper.selectCount(
                Wrappers.<ProcessUserGroup>lambdaQuery()
                        .eq(ProcessUserGroup::getParentId, id)
        );
        return count != null && count > 0;
    }

    protected boolean isReferencedByFlow(Long id) {
        return resolverSupport.isReferencedByFlow(id);
    }

    protected List<ProcessFormOptionVO> listSecondLevelGroupOptions() {
        return resolverSupport.listSecondLevelGroupOptions();
    }

    protected ProcessUserGroupResolverSupport getResolverSupport() {
        return resolverSupport;
    }

    protected ProcessUserGroupMapper getProcessUserGroupMapper() {
        return processUserGroupMapper;
    }

    private ProcessUserGroupTreeVO toTreeNode(ProcessUserGroup group) {
        ProcessUserGroupTreeVO node = new ProcessUserGroupTreeVO();
        node.setId(group.getId());
        node.setParentId(group.getParentId());
        node.setGroupCode(group.getGroupCode());
        node.setGroupName(group.getGroupName());
        node.setCodeLevel(group.getCodeLevel());
        return node;
    }

    private List<ProcessFormOptionVO> loadCompanyOptions() {
        return systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .eq(SystemCompany::getStatus, 1)
                        .orderByAsc(SystemCompany::getCompanyCode, SystemCompany::getCompanyId)
        ).stream().map(company -> {
            ProcessFormOptionVO option = new ProcessFormOptionVO();
            option.setValue(company.getCompanyId());
            option.setLabel(trimToNull(company.getCompanyName()) != null ? company.getCompanyName() : company.getCompanyCode());
            return option;
        }).toList();
    }

    private ProcessUserGroup resolveTargetParent(ProcessUserGroupSaveDTO dto, ProcessUserGroup existing) {
        if (existing != null) {
            Long currentParentId = existing.getParentId();
            Long requestedParentId = dto.getParentId();
            if (!Objects.equals(currentParentId, requestedParentId)) {
                throw new IllegalStateException("\u7528\u6237\u7ec4\u4e0d\u652f\u6301\u8c03\u6574\u7236\u5b50\u5c42\u7ea7");
            }
            return currentParentId == null ? null : requireUserGroup(currentParentId);
        }
        return dto.getParentId() == null ? null : requireUserGroup(dto.getParentId());
    }

    private int resolveTargetLevel(ProcessUserGroup parent, ProcessUserGroup existing) {
        if (existing != null) {
            return existing.getCodeLevel() == null ? 1 : existing.getCodeLevel();
        }
        return parent == null ? 1 : (parent.getCodeLevel() == null ? 1 : parent.getCodeLevel() + 1);
    }

    private String buildNextGroupCode(ProcessUserGroup parent) {
        int targetLevel = parent == null ? 1 : parent.getCodeLevel() + 1;
        if (targetLevel > 3) {
            throw new IllegalStateException("3 \u7ea7\u7528\u6237\u7ec4\u4e0d\u80fd\u518d\u65b0\u589e\u4e0b\u7ea7");
        }
        String prefix = parent == null ? "" : trimToEmpty(parent.getGroupCode());
        List<ProcessUserGroup> siblings = processUserGroupMapper.selectList(
                parent == null
                        ? Wrappers.<ProcessUserGroup>lambdaQuery()
                        .isNull(ProcessUserGroup::getParentId)
                        .eq(ProcessUserGroup::getCodeLevel, 1)
                        .orderByDesc(ProcessUserGroup::getGroupCode, ProcessUserGroup::getId)
                        : Wrappers.<ProcessUserGroup>lambdaQuery()
                        .eq(ProcessUserGroup::getParentId, parent.getId())
                        .eq(ProcessUserGroup::getCodeLevel, targetLevel)
                        .orderByDesc(ProcessUserGroup::getGroupCode, ProcessUserGroup::getId)
        );
        int nextSequence = siblings.stream()
                .map(ProcessUserGroup::getGroupCode)
                .map(code -> parseTailSequence(code, prefix, targetLevel == 1 ? 4 : 2))
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        String segment = String.format(targetLevel == 1 ? "%04d" : "%02d", nextSequence);
        return prefix + segment;
    }

    private Integer parseTailSequence(String groupCode, String prefix, int length) {
        String normalizedCode = trimToNull(groupCode);
        if (normalizedCode == null || normalizedCode.length() < prefix.length() + length || !normalizedCode.startsWith(prefix)) {
            return null;
        }
        try {
            return Integer.parseInt(normalizedCode.substring(normalizedCode.length() - length));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<ProcessFlowConditionGroupDTO> normalizeScopeConditionGroups(List<ProcessFlowConditionGroupDTO> groups) {
        if (groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProcessFlowConditionGroupDTO> result = new ArrayList<>();
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            ProcessFlowConditionGroupDTO sourceGroup = groups.get(groupIndex);
            ProcessFlowConditionGroupDTO group = new ProcessFlowConditionGroupDTO();
            group.setGroupNo(groupIndex + 1);
            List<ProcessFlowConditionDTO> conditions = new ArrayList<>();
            List<ProcessFlowConditionDTO> sourceConditions = sourceGroup == null || sourceGroup.getConditions() == null
                    ? Collections.emptyList()
                    : sourceGroup.getConditions();
            for (ProcessFlowConditionDTO sourceCondition : sourceConditions) {
                if (sourceCondition == null || trimToNull(sourceCondition.getFieldKey()) == null) {
                    continue;
                }
                ProcessFlowConditionDTO condition = new ProcessFlowConditionDTO();
                condition.setFieldKey(trimToEmpty(sourceCondition.getFieldKey()));
                condition.setOperator(trimToEmpty(sourceCondition.getOperator()));
                condition.setCompareValue(sourceCondition.getCompareValue());
                conditions.add(condition);
            }
            group.setConditions(conditions);
            result.add(group);
        }
        return result;
    }

    private void validateScopeConditionGroups(List<ProcessFlowConditionGroupDTO> groups) {
        Set<String> validFieldKeys = ProcessUserGroupScopeSupport.supportedFieldKeys();
        Map<String, List<String>> operatorMap = ProcessUserGroupScopeSupport.fieldOperatorMap();
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            ProcessFlowConditionGroupDTO group = groups.get(groupIndex);
            if (group.getConditions() == null) {
                continue;
            }
            for (int conditionIndex = 0; conditionIndex < group.getConditions().size(); conditionIndex++) {
                ProcessFlowConditionDTO condition = group.getConditions().get(conditionIndex);
                String fieldKey = trimToNull(condition.getFieldKey());
                if (fieldKey == null || !validFieldKeys.contains(fieldKey)) {
                    throw new IllegalStateException("\u7b2c " + (groupIndex + 1) + " \u7ec4\u7b2c " + (conditionIndex + 1) + " \u6761\u8303\u56f4\u6761\u4ef6\u5b57\u6bb5\u5df2\u5931\u6548");
                }
                String operator = trimToNull(condition.getOperator());
                if (operator == null || !operatorMap.getOrDefault(fieldKey, Collections.emptyList()).contains(operator)) {
                    throw new IllegalStateException("\u7b2c " + (groupIndex + 1) + " \u7ec4\u7b2c " + (conditionIndex + 1) + " \u6761\u8303\u56f4\u6761\u4ef6\u64cd\u4f5c\u7b26\u4e0d\u5408\u6cd5");
                }
            }
        }
    }

    private String serializeScopeConditionGroups(List<ProcessFlowConditionGroupDTO> groups) {
        List<ProcessFlowConditionGroupDTO> normalized = normalizeScopeConditionGroups(groups);
        try {
            return getObjectMapper().writeValueAsString(normalized);
        } catch (Exception ex) {
            throw new IllegalStateException("\u5e8f\u5217\u5316\u7528\u6237\u7ec4\u7ba1\u7406\u8303\u56f4\u5931\u8d25", ex);
        }
    }

    private List<ProcessFlowConditionGroupDTO> deserializeScopeConditionGroups(String json) {
        if (trimToNull(json) == null) {
            return Collections.emptyList();
        }
        try {
            List<ProcessFlowConditionGroupDTO> groups = getObjectMapper().readValue(
                    json,
                    new TypeReference<List<ProcessFlowConditionGroupDTO>>() {}
            );
            return groups == null ? Collections.emptyList() : groups;
        } catch (Exception ex) {
            throw new IllegalStateException("\u53cd\u5e8f\u5217\u5316\u7528\u6237\u7ec4\u7ba1\u7406\u8303\u56f4\u5931\u8d25", ex);
        }
    }
}
