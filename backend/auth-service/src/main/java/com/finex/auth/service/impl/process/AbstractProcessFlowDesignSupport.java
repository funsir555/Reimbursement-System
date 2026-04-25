package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowConditionFieldVO;
import com.finex.auth.dto.ProcessFlowConditionGroupDTO;
import com.finex.auth.dto.ProcessFlowConfigOptionVO;
import com.finex.auth.dto.ProcessFlowSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.entity.ProcessFlow;
import com.finex.auth.entity.ProcessFlowScene;
import com.finex.auth.entity.ProcessFlowVersion;
import com.finex.auth.entity.SystemCompany;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

abstract class AbstractProcessFlowDesignSupport {

    protected static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    protected static final int PM_NAME_MAX_LENGTH = 64;
    protected static final int PM_FIELD_KEY_MAX_LENGTH = 64;

    protected static final String FLOW_STATUS_DRAFT = "DRAFT";
    protected static final String FLOW_STATUS_ENABLED = "ENABLED";
    protected static final String FLOW_STATUS_DISABLED = "DISABLED";

    protected static final String VERSION_STATUS_DRAFT = "DRAFT";
    protected static final String VERSION_STATUS_PUBLISHED = "PUBLISHED";
    protected static final String VERSION_STATUS_HISTORY = "HISTORY";

    protected static final String NODE_TYPE_APPROVAL = "APPROVAL";
    protected static final String NODE_TYPE_CC = "CC";
    protected static final String NODE_TYPE_PAYMENT = "PAYMENT";
    protected static final String NODE_TYPE_BRANCH = "BRANCH";

    protected static final String APPROVER_TYPE_MANAGER = "MANAGER";
    protected static final String APPROVER_TYPE_DESIGNATED_MEMBER = "DESIGNATED_MEMBER";
    protected static final String APPROVER_TYPE_MANUAL_SELECT = "MANUAL_SELECT";

    protected static final String MANAGER_RULE_MODE_FORM_DEPT_MANAGER = "FORM_DEPT_MANAGER";
    protected static final String DEPT_SOURCE_UNDERTAKE = "UNDERTAKE_DEPT";
    protected static final String DEPT_SOURCE_SUBMITTER = "SUBMITTER_DEPT";
    protected static final String MISSING_HANDLER_AUTO_SKIP = "AUTO_SKIP";
    protected static final String MISSING_HANDLER_EXCEPTION = "EXCEPTION";
    protected static final String MISSING_HANDLER_AUTO_TRANSFER = "AUTO_TRANSFER";
    protected static final String MISSING_HANDLER_BLOCK_SUBMIT = "BLOCK_SUBMIT";
    protected static final String MISSING_HANDLER_MANUAL_SELECT_ON_SUBMIT = "MANUAL_SELECT_ON_SUBMIT";

    protected static final String APPROVAL_MODE_OR_SIGN = "OR_SIGN";
    protected static final String APPROVAL_MODE_AND_SIGN = "AND_SIGN";
    protected static final String MANUAL_SCOPE_ALL_ACTIVE_USERS = "ALL_ACTIVE_USERS";

    protected static final List<String> DEFAULT_OPINIONS = List.of("通过", "拒绝", "加签", "转交");
    protected static final Set<String> APPROVER_TYPES = Set.of(
            APPROVER_TYPE_MANAGER,
            APPROVER_TYPE_DESIGNATED_MEMBER,
            APPROVER_TYPE_MANUAL_SELECT
    );
    protected static final Set<String> DEPT_SOURCES = Set.of(DEPT_SOURCE_UNDERTAKE, DEPT_SOURCE_SUBMITTER);
    protected static final Set<String> APPROVAL_MODES = Set.of(APPROVAL_MODE_OR_SIGN, APPROVAL_MODE_AND_SIGN);
    protected static final Set<String> FLOW_STATUSES = Set.of(FLOW_STATUS_DRAFT, FLOW_STATUS_ENABLED, FLOW_STATUS_DISABLED);
    protected static final Set<String> MISSING_HANDLERS = Set.of(
            MISSING_HANDLER_AUTO_SKIP,
            MISSING_HANDLER_EXCEPTION,
            MISSING_HANDLER_AUTO_TRANSFER,
            MISSING_HANDLER_BLOCK_SUBMIT,
            MISSING_HANDLER_MANUAL_SELECT_ON_SUBMIT
    );

    protected final ProcessFlowMapper processFlowMapper;
    protected final ProcessFlowVersionMapper processFlowVersionMapper;
    protected final ProcessFlowNodeMapper processFlowNodeMapper;
    protected final ProcessFlowRouteMapper processFlowRouteMapper;
    protected final ProcessFlowSceneMapper processFlowSceneMapper;
    protected final SystemCompanyMapper systemCompanyMapper;
    protected final SystemDepartmentMapper systemDepartmentMapper;
    protected final UserMapper userMapper;
    protected final ProcessExpenseTypeMapper processExpenseTypeMapper;
    protected final ProcessCustomArchiveDesignMapper processCustomArchiveDesignMapper;
    protected final ProcessDocumentTemplateMapper processDocumentTemplateMapper;
    protected final ObjectMapper objectMapper;

    protected AbstractProcessFlowDesignSupport(
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
            ObjectMapper objectMapper
    ) {
        this.processFlowMapper = processFlowMapper;
        this.processFlowVersionMapper = processFlowVersionMapper;
        this.processFlowNodeMapper = processFlowNodeMapper;
        this.processFlowRouteMapper = processFlowRouteMapper;
        this.processFlowSceneMapper = processFlowSceneMapper;
        this.systemCompanyMapper = systemCompanyMapper;
        this.systemDepartmentMapper = systemDepartmentMapper;
        this.userMapper = userMapper;
        this.processExpenseTypeMapper = processExpenseTypeMapper;
        this.processCustomArchiveDesignMapper = processCustomArchiveDesignMapper;
        this.processDocumentTemplateMapper = processDocumentTemplateMapper;
        this.objectMapper = objectMapper;
    }

    protected Map<Long, ProcessFlowVersion> loadVersionMap(Collection<Long> versionIds) {
        if (versionIds == null || versionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return processFlowVersionMapper.selectBatchIds(versionIds).stream()
                .collect(Collectors.toMap(
                        ProcessFlowVersion::getId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    protected ProcessFlowVersion currentDraftVersion(ProcessFlow flow) {
        if (flow.getCurrentDraftVersionId() == null) {
            return null;
        }
        ProcessFlowVersion version = processFlowVersionMapper.selectById(flow.getCurrentDraftVersionId());
        return version != null && VERSION_STATUS_DRAFT.equals(version.getVersionStatus()) ? version : null;
    }

    protected ProcessFlowVersion currentPublishedVersion(ProcessFlow flow) {
        if (flow.getCurrentPublishedVersionId() != null) {
            ProcessFlowVersion version = processFlowVersionMapper.selectById(flow.getCurrentPublishedVersionId());
            if (version != null) {
                return version;
            }
        }
        if (flow.getCurrentDraftVersionId() != null) {
            ProcessFlowVersion fallback = processFlowVersionMapper.selectById(flow.getCurrentDraftVersionId());
            if (fallback != null && VERSION_STATUS_PUBLISHED.equals(fallback.getVersionStatus())) {
                return fallback;
            }
        }
        return null;
    }

    protected ProcessFlowVersion resolveEditableVersion(ProcessFlow flow) {
        ProcessFlowVersion draft = currentDraftVersion(flow);
        return draft != null ? draft : currentPublishedVersion(flow);
    }

    protected ProcessFlow requireFlow(Long id) {
        ProcessFlow flow = processFlowMapper.selectById(id);
        if (flow == null) {
            throw new IllegalStateException("未找到对应流程");
        }
        return flow;
    }

    protected int nextVersionNo(Long flowId) {
        List<ProcessFlowVersion> versions = processFlowVersionMapper.selectList(
                Wrappers.<ProcessFlowVersion>lambdaQuery()
                        .eq(ProcessFlowVersion::getFlowId, flowId)
                        .orderByDesc(ProcessFlowVersion::getVersionNo)
                        .last("limit 1")
        );
        return versions.isEmpty() ? 1 : versions.get(0).getVersionNo() + 1;
    }

    protected String buildFlowCode() {
        String prefix = "PF" + LocalDate.now().format(CODE_DATE_FORMATTER);
        Long count = processFlowMapper.selectCount(
                Wrappers.<ProcessFlow>lambdaQuery().likeRight(ProcessFlow::getFlowCode, prefix)
        );
        return prefix + String.format("%04d", (count == null ? 0L : count) + 1);
    }

    protected String buildSceneCode() {
        String prefix = "PS" + LocalDate.now().format(CODE_DATE_FORMATTER);
        Long count = processFlowSceneMapper.selectCount(
                Wrappers.<ProcessFlowScene>lambdaQuery().likeRight(ProcessFlowScene::getSceneCode, prefix)
        );
        return prefix + String.format("%04d", (count == null ? 0L : count) + 1);
    }

    protected List<ProcessFlowSceneVO> loadSceneOptions() {
        return processFlowSceneMapper.selectList(
                Wrappers.<ProcessFlowScene>lambdaQuery()
                        .eq(ProcessFlowScene::getStatus, 1)
                        .orderByAsc(ProcessFlowScene::getId)
        ).stream().map(this::toSceneVO).toList();
    }

    protected ProcessFlowSceneVO toSceneVO(ProcessFlowScene scene) {
        ProcessFlowSceneVO item = new ProcessFlowSceneVO();
        item.setId(scene.getId());
        item.setSceneCode(scene.getSceneCode());
        item.setSceneName(scene.getSceneName());
        item.setSceneDescription(scene.getSceneDescription());
        item.setStatus(scene.getStatus());
        return item;
    }

    protected List<ProcessFormOptionVO> loadCompanyOptions() {
        return systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .eq(SystemCompany::getStatus, 1)
                        .orderByAsc(SystemCompany::getCompanyCode, SystemCompany::getCompanyId)
        ).stream().map(item -> option(
                trimToNull(item.getCompanyName()) != null ? item.getCompanyName() : asText(item.getCompanyCode(), item.getCompanyId()),
                item.getCompanyId()
        )).toList();
    }

    protected List<ProcessFormOptionVO> buildLevelOptions(String pattern) {
        List<ProcessFormOptionVO> result = new ArrayList<>();
        for (int level = 1; level <= 10; level++) {
            result.add(option(String.format(pattern, level), level));
        }
        return result;
    }

    protected List<ProcessFlowConditionFieldVO> buildConditionFields() {
        return List.of(
                conditionField("submitterDeptId", "提单人部门", "department", List.of("EQ", "NE", "IN", "NOT_IN")),
                conditionField("submitterUserId", "提单人", "user", List.of("EQ", "NE", "IN", "NOT_IN")),
                conditionField("expenseTypeCode", "费用类型", "expenseType", List.of("EQ", "NE", "IN", "NOT_IN")),
                conditionField("documentType", "单据类型", "text", List.of("EQ", "NE", "IN", "NOT_IN")),
                conditionField("amount", "金额区间", "number", List.of("EQ", "NE", "GT", "GE", "LT", "LE", "BETWEEN")),
                conditionField("tagArchiveCode", "标签档案", "archive", List.of("EQ", "NE", "IN", "NOT_IN")),
                conditionField("installmentArchiveCode", "分期付款档案", "archive", List.of("EQ", "NE", "IN", "NOT_IN"))
        );
    }

    protected ProcessFlowConditionFieldVO conditionField(String key, String label, String valueType, List<String> operators) {
        ProcessFlowConditionFieldVO item = new ProcessFlowConditionFieldVO();
        item.setKey(key);
        item.setLabel(label);
        item.setValueType(valueType);
        item.setOperatorKeys(new ArrayList<>(operators));
        return item;
    }

    protected List<ProcessFormOptionVO> loadDepartmentOptions() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .eq(SystemDepartment::getStatus, 1)
                        .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId)
        ).stream().map(item -> option(item.getDeptName(), item.getId())).toList();
    }

    protected List<ProcessFormOptionVO> loadUserOptions() {
        return userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getId)
        ).stream().map(item -> option(normalizeUserName(item), item.getId())).toList();
    }

    protected List<ProcessFormOptionVO> loadExpenseTypeOptions() {
        return processExpenseTypeMapper.selectList(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getStatus, 1)
                        .orderByAsc(ProcessExpenseType::getExpenseCode, ProcessExpenseType::getId)
        ).stream().map(item -> option(item.getExpenseName(), item.getExpenseCode())).toList();
    }

    protected List<ProcessFormOptionVO> loadArchiveOptions() {
        return processCustomArchiveDesignMapper.selectList(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .eq(ProcessCustomArchiveDesign::getStatus, 1)
                        .orderByDesc(ProcessCustomArchiveDesign::getCreatedAt, ProcessCustomArchiveDesign::getId)
        ).stream().map(item -> option(item.getArchiveName(), item.getArchiveCode())).toList();
    }

    protected Map<Long, SystemDepartment> loadAllDepartmentMap() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getStatus, 1)
        ).stream().collect(Collectors.toMap(
                SystemDepartment::getId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    protected Map<Long, String> loadDeptNameMap(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return systemDepartmentMapper.selectBatchIds(deptIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        SystemDepartment::getId,
                        SystemDepartment::getDeptName,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    protected List<User> loadActiveUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(item.getStatus(), 1))
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    protected User loadActiveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            List<User> candidates = loadActiveUsers(List.of(userId));
            return candidates.isEmpty() ? null : candidates.get(0);
        }
        return user != null && Objects.equals(user.getStatus(), 1) ? user : null;
    }

    protected void validateActiveUsers(List<Long> userIds) {
        int validUserCount = loadActiveUsers(userIds).stream()
                .map(User::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .size();
        if (validUserCount != new LinkedHashSet<>(userIds).size()) {
            throw new IllegalStateException("审批节点中存在无效的系统成员");
        }
    }

    protected String writeSnapshot(ProcessFlowSaveDTO dto) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("flowName", dto.getFlowName());
        snapshot.put("flowDescription", dto.getFlowDescription());
        snapshot.put("nodes", dto.getNodes() == null ? Collections.emptyList() : dto.getNodes());
        snapshot.put("routes", dto.getRoutes() == null ? Collections.emptyList() : dto.getRoutes());
        return writeValue(snapshot);
    }

    protected String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("流程配置序列化失败", exception);
        }
    }

    protected Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("流程配置解析失败", exception);
        }
    }

    protected List<ProcessFlowConditionGroupDTO> readConditionGroups(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("流程分支条件解析失败", exception);
        }
    }

    protected String normalizeFlowStatus(String status) {
        String value = asText(status, FLOW_STATUS_DRAFT);
        if (!FLOW_STATUSES.contains(value)) {
            throw new IllegalStateException("流程状态不合法");
        }
        return value;
    }

    protected String normalizeMissingHandler(String value) {
        String normalized = asText(value, MISSING_HANDLER_AUTO_SKIP);
        if (MISSING_HANDLER_MANUAL_SELECT_ON_SUBMIT.equals(normalized)) {
            return MISSING_HANDLER_BLOCK_SUBMIT;
        }
        if (!MISSING_HANDLERS.contains(normalized)) {
            return MISSING_HANDLER_AUTO_SKIP;
        }
        return normalized;
    }

    protected String statusLabel(String status) {
        return switch (status) {
            case FLOW_STATUS_ENABLED -> "已发布";
            case FLOW_STATUS_DISABLED -> "已停用";
            default -> "草稿";
        };
    }

    protected String defaultNodeName(String nodeType, int index) {
        return switch (nodeType) {
            case NODE_TYPE_CC -> "抄送节点 " + index;
            case NODE_TYPE_PAYMENT -> "支付节点 " + index;
            case NODE_TYPE_BRANCH -> "流程分支 " + index;
            default -> "审批节点 " + index;
        };
    }

    protected String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }

    protected ProcessFormOptionVO option(String label, Object value) {
        ProcessFormOptionVO item = new ProcessFormOptionVO();
        item.setLabel(label);
        item.setValue(value == null ? null : String.valueOf(value));
        return item;
    }

    protected ProcessFlowConfigOptionVO configOption(String value, String label, String description) {
        ProcessFlowConfigOptionVO item = new ProcessFlowConfigOptionVO();
        item.setValue(value);
        item.setLabel(label);
        item.setDescription(description);
        return item;
    }

    protected void validatePmNameLength(String value, String label) {
        String normalized = trimToNull(value);
        if (normalized != null && normalized.length() > PM_NAME_MAX_LENGTH) {
            throw new IllegalStateException(label + "长度不能超过 64 个字符");
        }
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    protected String asText(Object value, Object defaultValue) {
        if (value == null) {
            return defaultValue == null ? null : String.valueOf(defaultValue);
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return defaultValue == null ? null : String.valueOf(defaultValue);
        }
        return text;
    }

    protected Integer asInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("数值格式不合法");
        }
    }

    protected Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("数值格式不合法");
        }
    }

    protected boolean asBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    protected int limitLevel(Integer value, String label) {
        int level = value == null ? 1 : value;
        if (level < 1 || level > 10) {
            throw new IllegalStateException(label + "只能选择 1-10");
        }
        return level;
    }

    protected List<String> toStringList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(item -> String.valueOf(item).trim())
                    .filter(item -> !item.isEmpty())
                    .toList();
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? new ArrayList<>() : List.of(text);
    }

    protected List<Long> toLongList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        List<Long> result = new ArrayList<>();
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                Long parsed = asLong(item);
                if (parsed != null) {
                    result.add(parsed);
                }
            }
            return result;
        }
        Long parsed = asLong(value);
        if (parsed != null) {
            result.add(parsed);
        }
        return result;
    }

    protected Map<String, Object> toObjectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return castMap(map);
        }
        return new LinkedHashMap<>();
    }

    protected Map<String, Object> castMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    protected String normalizeUserName(User user) {
        String name = trimToNull(user.getName());
        return name != null ? name : asText(user.getUsername(), "未命名用户");
    }
}
