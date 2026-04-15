// 业务域：流程模板与流程配置
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 流程管理页面对应的 Controller，下游会继续协调 流程模板、报销类型、自定义档案和发布状态。
// 风险提醒：改坏后最容易影响 审批路由、模板发布和后续单据流转。

package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowConditionFieldVO;
import com.finex.auth.dto.ProcessFlowConditionDTO;
import com.finex.auth.dto.ProcessFlowConditionGroupDTO;
import com.finex.auth.dto.ProcessFlowConfigOptionVO;
import com.finex.auth.dto.ProcessFlowDetailVO;
import com.finex.auth.dto.ProcessFlowMetaVO;
import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowResolveApproversDTO;
import com.finex.auth.dto.ProcessFlowResolveApproversVO;
import com.finex.auth.dto.ProcessFlowResolvedUserVO;
import com.finex.auth.dto.ProcessFlowRouteDTO;
import com.finex.auth.dto.ProcessFlowSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneVO;
import com.finex.auth.dto.ProcessFlowSummaryVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.entity.ProcessFlow;
import com.finex.auth.entity.ProcessFlowNode;
import com.finex.auth.entity.ProcessFlowRoute;
import com.finex.auth.entity.ProcessFlowScene;
import com.finex.auth.entity.ProcessFlowVersion;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessFlowMapper;
import com.finex.auth.mapper.ProcessFlowNodeMapper;
import com.finex.auth.mapper.ProcessFlowRouteMapper;
import com.finex.auth.mapper.ProcessFlowSceneMapper;
import com.finex.auth.mapper.ProcessFlowVersionMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.ProcessFlowDesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * ProcessFlowDesignServiceImpl：service 入口实现。
 * 接住上层请求，并把 流程流程设计相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 审批路由、模板发布和后续单据流转是否会被一起带坏。
 */
@Service
@RequiredArgsConstructor
public class ProcessFlowDesignServiceImpl implements ProcessFlowDesignService {

    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int PM_NAME_MAX_LENGTH = 64;
    private static final int PM_FIELD_KEY_MAX_LENGTH = 64;

    private static final String FLOW_STATUS_DRAFT = "DRAFT";
    private static final String FLOW_STATUS_ENABLED = "ENABLED";
    private static final String FLOW_STATUS_DISABLED = "DISABLED";

    private static final String VERSION_STATUS_DRAFT = "DRAFT";
    private static final String VERSION_STATUS_PUBLISHED = "PUBLISHED";
    private static final String VERSION_STATUS_HISTORY = "HISTORY";

    private static final String NODE_TYPE_APPROVAL = "APPROVAL";
    private static final String NODE_TYPE_CC = "CC";
    private static final String NODE_TYPE_PAYMENT = "PAYMENT";
    private static final String NODE_TYPE_BRANCH = "BRANCH";

    private static final String APPROVER_TYPE_MANAGER = "MANAGER";
    private static final String APPROVER_TYPE_DESIGNATED_MEMBER = "DESIGNATED_MEMBER";
    private static final String APPROVER_TYPE_MANUAL_SELECT = "MANUAL_SELECT";

    private static final String MANAGER_RULE_MODE_FORM_DEPT_MANAGER = "FORM_DEPT_MANAGER";
    private static final String DEPT_SOURCE_UNDERTAKE = "UNDERTAKE_DEPT";
    private static final String DEPT_SOURCE_SUBMITTER = "SUBMITTER_DEPT";
    private static final String MISSING_HANDLER_AUTO_SKIP = "AUTO_SKIP";
    private static final String MISSING_HANDLER_EXCEPTION = "EXCEPTION";
    private static final String MISSING_HANDLER_AUTO_TRANSFER = "AUTO_TRANSFER";
    private static final String MISSING_HANDLER_BLOCK_SUBMIT = "BLOCK_SUBMIT";
    private static final String MISSING_HANDLER_MANUAL_SELECT_ON_SUBMIT = "MANUAL_SELECT_ON_SUBMIT";

    private static final String APPROVAL_MODE_OR_SIGN = "OR_SIGN";
    private static final String APPROVAL_MODE_AND_SIGN = "AND_SIGN";
    private static final String MANUAL_SCOPE_ALL_ACTIVE_USERS = "ALL_ACTIVE_USERS";

    private static final List<String> DEFAULT_OPINIONS = List.of("閫氳繃", "鎷掔粷", "鍔犵", "杞氦");
    private static final Set<String> APPROVER_TYPES = Set.of(
            APPROVER_TYPE_MANAGER,
            APPROVER_TYPE_DESIGNATED_MEMBER,
            APPROVER_TYPE_MANUAL_SELECT
    );
    private static final Set<String> DEPT_SOURCES = Set.of(DEPT_SOURCE_UNDERTAKE, DEPT_SOURCE_SUBMITTER);
    private static final Set<String> APPROVAL_MODES = Set.of(APPROVAL_MODE_OR_SIGN, APPROVAL_MODE_AND_SIGN);
    private static final Set<String> FLOW_STATUSES = Set.of(FLOW_STATUS_DRAFT, FLOW_STATUS_ENABLED, FLOW_STATUS_DISABLED);
    private static final Set<String> MISSING_HANDLERS = Set.of(
            MISSING_HANDLER_AUTO_SKIP,
            MISSING_HANDLER_EXCEPTION,
            MISSING_HANDLER_AUTO_TRANSFER,
            MISSING_HANDLER_BLOCK_SUBMIT,
            MISSING_HANDLER_MANUAL_SELECT_ON_SUBMIT
    );

    private final ProcessFlowMapper processFlowMapper;
    private final ProcessFlowVersionMapper processFlowVersionMapper;
    private final ProcessFlowNodeMapper processFlowNodeMapper;
    private final ProcessFlowRouteMapper processFlowRouteMapper;
    private final ProcessFlowSceneMapper processFlowSceneMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final UserMapper userMapper;
    private final ProcessExpenseTypeMapper processExpenseTypeMapper;
    private final ProcessCustomArchiveDesignMapper processCustomArchiveDesignMapper;
    private final ObjectMapper objectMapper;

    /**
     * 查询流程列表。
     */
    @Override
    public List<ProcessFlowSummaryVO> listFlows() {
        List<ProcessFlow> flows = processFlowMapper.selectList(
                Wrappers.<ProcessFlow>lambdaQuery()
                        .orderByDesc(ProcessFlow::getUpdatedAt, ProcessFlow::getId)
        );
        if (flows.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> versionIds = new LinkedHashSet<>();
        flows.forEach(item -> {
            if (item.getCurrentDraftVersionId() != null) {
                versionIds.add(item.getCurrentDraftVersionId());
            }
            if (item.getCurrentPublishedVersionId() != null) {
                versionIds.add(item.getCurrentPublishedVersionId());
            }
        });
        Map<Long, ProcessFlowVersion> versionMap = loadVersionMap(versionIds);

        return flows.stream().map(flow -> {
            ProcessFlowVersion currentVersion = versionMap.get(
                    flow.getCurrentDraftVersionId() != null
                            ? flow.getCurrentDraftVersionId()
                            : flow.getCurrentPublishedVersionId()
            );
            ProcessFlowSummaryVO item = new ProcessFlowSummaryVO();
            item.setId(flow.getId());
            item.setFlowCode(flow.getFlowCode());
            item.setFlowName(flow.getFlowName());
            item.setFlowDescription(flow.getFlowDescription());
            item.setStatus(flow.getStatus());
            item.setStatusLabel(statusLabel(flow.getStatus()));
            item.setCurrentVersionNo(currentVersion == null ? null : currentVersion.getVersionNo());
            item.setUpdatedAt(formatTime(flow.getUpdatedAt()));
            return item;
        }).toList();
    }

    /**
     * 获取流程元数据。
     */
    @Override
    public ProcessFlowMetaVO getFlowMeta() {
        ProcessFlowMetaVO meta = new ProcessFlowMetaVO();
        meta.setNodeTypeOptions(List.of(
                option("审批节点", NODE_TYPE_APPROVAL),
                option("抄送节点", NODE_TYPE_CC),
                option("支付节点", NODE_TYPE_PAYMENT),
                option("流程分支", NODE_TYPE_BRANCH)
        ));
        meta.setSceneOptions(loadSceneOptions());
        meta.setApprovalApproverTypeOptions(List.of(
                option("指定主管", APPROVER_TYPE_MANAGER),
                option("指定成员", APPROVER_TYPE_DESIGNATED_MEMBER),
                option("手动选择", APPROVER_TYPE_MANUAL_SELECT)
        ));
        meta.setApprovalManagerRuleModeOptions(List.of(
                option("根据表单上的部门查找指定主管", MANAGER_RULE_MODE_FORM_DEPT_MANAGER)
        ));
        meta.setApprovalManagerDeptSourceOptions(List.of(
                option("承担部门", DEPT_SOURCE_UNDERTAKE),
                option("提单人部门", DEPT_SOURCE_SUBMITTER)
        ));
        meta.setApprovalManagerLevelOptions(buildLevelOptions("第%s级主管"));
        meta.setApprovalManagerLookupLevelOptions(buildLevelOptions("第%s级"));
        meta.setApprovalManualCandidateScopeOptions(List.of(
                option("全部有效用户", MANUAL_SCOPE_ALL_ACTIVE_USERS)
        ));
        meta.setCcReceiverTypeOptions(List.of(
                option("指定成员", "DESIGNATED_MEMBER"),
                option("提单人", "SUBMITTER"),
                option("部门主管", "DEPT_MANAGER")
        ));
        meta.setPaymentExecutorTypeOptions(List.of(
                option("指定成员", "DESIGNATED_MEMBER"),
                option("财务角色", "FINANCE_ROLE"),
                option("提单人", "SUBMITTER")
        ));
        meta.setMissingHandlerOptions(List.of(
                option("自动跳过", MISSING_HANDLER_AUTO_SKIP),
                option("作为异常流程处理", MISSING_HANDLER_EXCEPTION),
                option("自动转交", MISSING_HANDLER_AUTO_TRANSFER),
                option("提单时找不到审批人不允许提交", MISSING_HANDLER_BLOCK_SUBMIT)
        ));
        meta.setApprovalModeOptions(List.of(
                option("或签", APPROVAL_MODE_OR_SIGN),
                option("会签", APPROVAL_MODE_AND_SIGN)
        ));
        meta.setDefaultApprovalOpinions(new ArrayList<>(DEFAULT_OPINIONS));
        meta.setApprovalSpecialOptions(List.of(
                configOption("AUTO_PASS_IF_APPOVER_IS_SUBMITTER", "审批人与提单人重复时自动通过", ""),
                configOption("AUTO_PASS_IF_APPROVED_BEFORE", "审批人已在前面节点审批过时自动通过", ""),
                configOption("DIRECT_REACH_AFTER_RESUBMIT", "驳回后再提交允许直达本节点", ""),
                configOption("REJECT_TO_ANY_NODE", "本节点可以驳回至任意节点", ""),
                configOption("DIRECT_REACH_AFTER_ANY_REJECT", "驳回至任意节点后再提交允许直达本节点", ""),
                configOption("ALLOW_EDIT_PAY_ACCOUNT", "可授权提单人修改收款账户", ""),
                configOption("ALLOW_EDIT_FORM_MODULE", "本节点允许修改开了修改权限的表单模块", "")
        ));
        meta.setCcTimingOptions(List.of(
                option("进入节点时", "ON_ENTER"),
                option("通过后", "ON_APPROVED")
        ));
        meta.setCcSpecialOptions(List.of(
                configOption("SEND_ONCE", "同一对象仅发送一次", ""),
                configOption("INCLUDE_SUBMITTER", "包含提单人", "")
        ));
        meta.setPaymentActionOptions(List.of(
                option("生成付款任务", "GENERATE_PAYMENT"),
                option("确认已付款", "CONFIRM_PAYMENT")
        ));
        meta.setPaymentSpecialOptions(List.of(
                configOption("ALLOW_RETRY", "支付失败允许重试", ""),
                configOption("REQUIRE_RESULT_FEEDBACK", "要求回写支付结果", "")
        ));
        meta.setBranchOperatorOptions(List.of(
                option("等于", "EQ"),
                option("不等于", "NE"),
                option("属于", "IN"),
                option("不属于", "NOT_IN"),
                option("大于", "GT"),
                option("大于等于", "GE"),
                option("小于", "LT"),
                option("小于等于", "LE"),
                option("介于", "BETWEEN"),
                option("包含", "CONTAINS")
        ));
        meta.setBranchConditionFields(buildConditionFields());
        meta.setDepartmentOptions(loadDepartmentOptions());
        meta.setUserOptions(loadUserOptions());
        meta.setExpenseTypeOptions(loadExpenseTypeOptions());
        meta.setArchiveOptions(loadArchiveOptions());
        return meta;
    }

    /**
     * 获取流程明细。
     */
    @Override
    public ProcessFlowDetailVO getFlowDetail(Long id) {
        return buildFlowDetail(requireFlow(id));
    }

    /**
     * 创建流程。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowDetailVO createFlow(ProcessFlowSaveDTO dto) {
        validateFlowSave(dto);

        ProcessFlow flow = new ProcessFlow();
        flow.setFlowCode(buildFlowCode());
        flow.setFlowName(dto.getFlowName().trim());
        flow.setFlowDescription(trimToNull(dto.getFlowDescription()));
        flow.setStatus(FLOW_STATUS_DRAFT);
        processFlowMapper.insert(flow);

        ProcessFlowVersion version = createDraftVersion(flow.getId(), 1, dto);
        flow.setCurrentDraftVersionId(version.getId());
        processFlowMapper.updateById(flow);
        return buildFlowDetail(requireFlow(flow.getId()));
    }

    /**
     * 更新流程。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowDetailVO updateFlow(Long id, ProcessFlowSaveDTO dto) {
        validateFlowSave(dto);

        ProcessFlow flow = requireFlow(id);
        flow.setFlowName(dto.getFlowName().trim());
        flow.setFlowDescription(trimToNull(dto.getFlowDescription()));

        ProcessFlowVersion draftVersion = currentDraftVersion(flow);
        if (draftVersion == null) {
            draftVersion = createDraftVersion(flow.getId(), nextVersionNo(flow.getId()), dto);
            flow.setCurrentDraftVersionId(draftVersion.getId());
        } else {
            draftVersion.setSnapshotJson(writeSnapshot(dto));
            processFlowVersionMapper.updateById(draftVersion);
            replaceVersionNodesAndRoutes(draftVersion.getId(), dto);
        }

        if (flow.getCurrentPublishedVersionId() == null) {
            flow.setStatus(FLOW_STATUS_DRAFT);
        }
        processFlowMapper.updateById(flow);
        return buildFlowDetail(requireFlow(id));
    }

    /**
     * 发布流程。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowDetailVO publishFlow(Long id) {
        ProcessFlow flow = requireFlow(id);
        ProcessFlowVersion draftVersion = currentDraftVersion(flow);
        if (draftVersion == null) {
            if (currentPublishedVersion(flow) == null) {
                throw new IllegalStateException("\u5f53\u524d\u6d41\u7a0b\u6ca1\u6709\u53ef\u53d1\u5e03\u7684\u8349\u7a3f\u7248\u672c");
            }
            flow.setStatus(FLOW_STATUS_ENABLED);
            processFlowMapper.updateById(flow);
            return buildFlowDetail(requireFlow(id));
        }

        ProcessFlowVersion publishedVersion = currentPublishedVersion(flow);
        if (publishedVersion != null) {
            publishedVersion.setVersionStatus(VERSION_STATUS_HISTORY);
            processFlowVersionMapper.updateById(publishedVersion);
        }

        draftVersion.setVersionStatus(VERSION_STATUS_PUBLISHED);
        draftVersion.setPublishedAt(LocalDateTime.now());
        processFlowVersionMapper.updateById(draftVersion);

        flow.setStatus(FLOW_STATUS_ENABLED);
        flow.setCurrentPublishedVersionId(draftVersion.getId());
        flow.setCurrentDraftVersionId(null);
        processFlowMapper.updateById(flow);
        return buildFlowDetail(requireFlow(id));
    }

    /**
     * 更新流程Status。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateFlowStatus(Long id, String status) {
        ProcessFlow flow = requireFlow(id);
        String targetStatus = normalizeFlowStatus(status);
        if (FLOW_STATUS_ENABLED.equals(targetStatus) && currentPublishedVersion(flow) == null) {
            throw new IllegalStateException("当前流程尚未发布，不能直接启用");
        }
        flow.setStatus(targetStatus);
        processFlowMapper.updateById(flow);
        return Boolean.TRUE;
    }

    /**
     * 创建流程Scene。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowSceneVO createFlowScene(ProcessFlowSceneSaveDTO dto) {
        String sceneName = trimToNull(dto.getSceneName());
        if (sceneName == null) {
            throw new IllegalStateException("\u573a\u666f\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        validatePmNameLength(sceneName, "\u573a\u666f\u540d\u79f0");

        ProcessFlowScene scene = new ProcessFlowScene();
        scene.setSceneCode(buildSceneCode());
        scene.setSceneName(sceneName);
        scene.setSceneDescription(trimToNull(dto.getSceneDescription()));
        scene.setStatus(dto.getStatus() == null ? 1 : (dto.getStatus() == 0 ? 0 : 1));
        processFlowSceneMapper.insert(scene);
        return toSceneVO(scene);
    }

    @Override
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

        Map<String, Object> config = normalizeNodeConfig(node.getNodeType(), readMap(node.getConfigJson()), false);
        String approverType = asText(config.get("approverType"), APPROVER_TYPE_MANAGER);
        String missingHandler = normalizeMissingHandler(asText(config.get("missingHandler"), MISSING_HANDLER_AUTO_SKIP));

        List<String> trace = new ArrayList<>();
        List<User> resolvedUsers;
        if (APPROVER_TYPE_DESIGNATED_MEMBER.equals(approverType)) {
            resolvedUsers = resolveDesignatedMembers(config, trace);
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

        Map<Long, String> deptNameMap = loadDeptNameMap(
                distinctUsers.stream().map(User::getDeptId).filter(Objects::nonNull).toList()
        );
        result.setResolutionType("RESOLVED");
        result.setApproverUserIds(distinctUsers.stream().map(User::getId).toList());
        result.setApproverUsers(distinctUsers.stream().map(user -> {
            ProcessFlowResolvedUserVO item = new ProcessFlowResolvedUserVO();
            item.setUserId(user.getId());
            item.setUserName(normalizeUserName(user));
            item.setDeptId(user.getDeptId());
            item.setDeptName(deptNameMap.get(user.getDeptId()));
            return item;
        }).toList());
        return result;
    }

    /**
     * 查询Published流程选项。
     */
    @Override
    public List<ProcessFormOptionVO> listPublishedFlowOptions() {
        return processFlowMapper.selectList(
                Wrappers.<ProcessFlow>lambdaQuery()
                        .eq(ProcessFlow::getStatus, FLOW_STATUS_ENABLED)
                        .isNotNull(ProcessFlow::getCurrentPublishedVersionId)
                        .orderByDesc(ProcessFlow::getUpdatedAt, ProcessFlow::getId)
        ).stream().map(item -> option(item.getFlowName(), item.getFlowCode())).toList();
    }

    /**
     * 处理流程流程设计中的这一步。
     */
    @Override
    public Map<String, String> publishedFlowLabelMap() {
        return processFlowMapper.selectList(
                Wrappers.<ProcessFlow>lambdaQuery()
                        .eq(ProcessFlow::getStatus, FLOW_STATUS_ENABLED)
                        .isNotNull(ProcessFlow::getCurrentPublishedVersionId)
        ).stream().collect(Collectors.toMap(
                ProcessFlow::getFlowCode,
                ProcessFlow::getFlowName,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    /**
     * 组装流程明细。
     */
    private ProcessFlowDetailVO buildFlowDetail(ProcessFlow flow) {
        ProcessFlowDetailVO detail = new ProcessFlowDetailVO();
        detail.setId(flow.getId());
        detail.setFlowCode(flow.getFlowCode());
        detail.setFlowName(flow.getFlowName());
        detail.setFlowDescription(flow.getFlowDescription());
        detail.setStatus(flow.getStatus());
        detail.setStatusLabel(statusLabel(flow.getStatus()));

        ProcessFlowVersion draftVersion = currentDraftVersion(flow);
        ProcessFlowVersion publishedVersion = currentPublishedVersion(flow);
        ProcessFlowVersion dataVersion = draftVersion != null ? draftVersion : publishedVersion;

        detail.setEditableVersionId(dataVersion == null ? null : dataVersion.getId());
        detail.setEditableVersionNo(dataVersion == null ? null : dataVersion.getVersionNo());
        detail.setPublishedVersionId(publishedVersion == null ? null : publishedVersion.getId());
        detail.setPublishedVersionNo(publishedVersion == null ? null : publishedVersion.getVersionNo());
        detail.setHasDraftVersion(draftVersion != null);
        detail.setNodes(dataVersion == null ? new ArrayList<>() : loadVersionNodes(dataVersion.getId()));
        detail.setRoutes(dataVersion == null ? new ArrayList<>() : loadVersionRoutes(dataVersion.getId()));
        return detail;
    }

    /**
     * 校验流程Save。
     */
    private void validateFlowSave(ProcessFlowSaveDTO dto) {
        String flowName = trimToNull(dto.getFlowName());
        if (flowName == null) {
            throw new IllegalStateException("\u6d41\u7a0b\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        validatePmNameLength(flowName, "\u6d41\u7a0b\u540d\u79f0");
        normalizeNodes(dto.getNodes());
        normalizeRoutes(dto.getRoutes());

        Set<String> validConditionFieldKeys = buildConditionFields().stream()
                .map(ProcessFlowConditionFieldVO::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> validSceneIds = processFlowSceneMapper.selectList(
                Wrappers.<ProcessFlowScene>lambdaQuery()
                        .eq(ProcessFlowScene::getStatus, 1)
        ).stream().map(ProcessFlowScene::getId).collect(Collectors.toCollection(LinkedHashSet::new));

        if (dto.getNodes() != null) {
            for (int index = 0; index < dto.getNodes().size(); index++) {
                ProcessFlowNodeDTO node = dto.getNodes().get(index);
                validatePmNameLength(node.getNodeName(), "\u7b2c " + (index + 1) + " \u4e2a\u8282\u70b9\u540d\u79f0");
                if (node.getSceneId() != null && !validSceneIds.contains(node.getSceneId())) {
                    throw new IllegalStateException("\u7b2c " + (index + 1) + " \u4e2a\u8282\u70b9\u7ed1\u5b9a\u7684\u573a\u666f\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9");
                }
            }
        }

        if (dto.getRoutes() != null) {
            for (int routeIndex = 0; routeIndex < dto.getRoutes().size(); routeIndex++) {
                ProcessFlowRouteDTO route = dto.getRoutes().get(routeIndex);
                validatePmNameLength(route.getRouteName(), "\u7b2c " + (routeIndex + 1) + " \u6761\u5206\u652f\u540d\u79f0");
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
                            throw new IllegalStateException("\u7b2c " + (routeIndex + 1) + " \u6761\u5206\u652f\u7b2c " + (groupIndex + 1) + " \u7ec4\u7b2c " + (conditionIndex + 1) + " \u4e2a\u6761\u4ef6\u5b57\u6bb5\u6807\u8bc6\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26");
                        }
                        if (!validConditionFieldKeys.contains(fieldKey)) {
                            throw new IllegalStateException("\u7b2c " + (routeIndex + 1) + " \u6761\u5206\u652f\u7b2c " + (groupIndex + 1) + " \u7ec4\u7b2c " + (conditionIndex + 1) + " \u4e2a\u6761\u4ef6\u5b57\u6bb5\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9");
                        }
                    }
                }
            }
        }
    }

    private void normalizeNodes(List<ProcessFlowNodeDTO> nodes) {
        if (nodes == null) {
            return;
        }

        Set<String> nodeKeys = new LinkedHashSet<>();
        for (int index = 0; index < nodes.size(); index++) {
            ProcessFlowNodeDTO node = nodes.get(index);
            if (node.getNodeKey() == null || node.getNodeKey().trim().isEmpty()) {
                node.setNodeKey("node-" + (index + 1));
            }
            if (!nodeKeys.add(node.getNodeKey())) {
                throw new IllegalStateException("\u8282\u70b9\u6807\u8bc6\u4e0d\u80fd\u91cd\u590d");
            }
            if (node.getDisplayOrder() == null) {
                node.setDisplayOrder(index + 1);
            }
            node.setParentNodeKey(trimToNull(node.getParentNodeKey()));
            node.setNodeType(asText(node.getNodeType(), NODE_TYPE_APPROVAL));
            if (node.getNodeName() == null || node.getNodeName().trim().isEmpty()) {
                node.setNodeName(defaultNodeName(node.getNodeType(), index + 1));
            }
            validatePmNameLength(node.getNodeName(), "\u7b2c " + (index + 1) + " \u4e2a\u8282\u70b9\u540d\u79f0");
            node.setConfig(normalizeNodeConfig(node.getNodeType(), node.getConfig(), true));
        }
    }

    private void normalizeRoutes(List<ProcessFlowRouteDTO> routes) {
        if (routes == null) {
            return;
        }

        Set<String> routeKeys = new LinkedHashSet<>();
        for (int index = 0; index < routes.size(); index++) {
            ProcessFlowRouteDTO route = routes.get(index);
            if (route.getRouteKey() == null || route.getRouteKey().trim().isEmpty()) {
                route.setRouteKey("route-" + (index + 1));
            }
            if (!routeKeys.add(route.getRouteKey())) {
                throw new IllegalStateException("\u5206\u652f\u6807\u8bc6\u4e0d\u80fd\u91cd\u590d");
            }
            if (route.getPriority() == null) {
                route.setPriority(index + 1);
            }
            if (route.getConditionGroups() == null) {
                route.setConditionGroups(new ArrayList<>());
            }
            validatePmNameLength(route.getRouteName(), "\u7b2c " + (index + 1) + " \u6761\u5206\u652f\u540d\u79f0");
        }
    }

    private void validatePmNameLength(String value, String label) {
        String normalized = trimToNull(value);
        if (normalized != null && normalized.length() > PM_NAME_MAX_LENGTH) {
            throw new IllegalStateException(label + "\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26");
        }
    }

    private Map<String, Object> normalizeNodeConfig(String nodeType, Map<String, Object> rawConfig, boolean strictValidation) {
        Map<String, Object> config = rawConfig == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rawConfig);
        if (!NODE_TYPE_APPROVAL.equals(nodeType)) {
            return config;
        }

        String approverType = asText(config.get("approverType"), APPROVER_TYPE_MANAGER);
        if (!APPROVER_TYPES.contains(approverType)) {
            throw new IllegalStateException("瀹℃壒浜虹被鍨嬩笉鍚堟硶");
        }
        config.put("approverType", approverType);

        String missingHandler = normalizeMissingHandler(asText(config.get("missingHandler"), MISSING_HANDLER_AUTO_SKIP));
        config.put("missingHandler", missingHandler);

        String approvalMode = asText(config.get("approvalMode"), APPROVAL_MODE_OR_SIGN);
        if (!APPROVAL_MODES.contains(approvalMode)) {
            throw new IllegalStateException("审批方式不合法");
        }
        config.put("approvalMode", approvalMode);

        List<String> opinions = toStringList(config.get("opinionDefaults"));
        config.put("opinionDefaults", opinions.isEmpty() ? new ArrayList<>(DEFAULT_OPINIONS) : opinions);
        config.put("specialSettings", toStringList(config.get("specialSettings")));

        Map<String, Object> managerConfig = normalizeManagerConfig(config.get("managerConfig"));
        Map<String, Object> designatedMemberConfig = normalizeDesignatedMemberConfig(config.get("designatedMemberConfig"));
        Map<String, Object> manualSelectConfig = normalizeManualSelectConfig(config.get("manualSelectConfig"));
        config.put("managerConfig", managerConfig);
        config.put("designatedMemberConfig", designatedMemberConfig);
        config.put("manualSelectConfig", manualSelectConfig);

        if (strictValidation && APPROVER_TYPE_DESIGNATED_MEMBER.equals(approverType)) {
            List<Long> userIds = toLongList(designatedMemberConfig.get("userIds"));
            if (userIds.isEmpty()) {
                throw new IllegalStateException("指定成员至少选择一名用户");
            }
            validateActiveUsers(userIds);
        }
        return config;
    }

    private Map<String, Object> normalizeManagerConfig(Object source) {
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
        config.put("managerLevel", limitLevel(asInteger(raw.get("managerLevel"), 1), "涓荤绾ф"));
        config.put("orgTreeLookupEnabled", asBoolean(raw.get("orgTreeLookupEnabled"), true));
        config.put("orgTreeLookupLevel", limitLevel(asInteger(raw.get("orgTreeLookupLevel"), 1), "鍚戜笂鏌ユ壘绾ф"));

        return config;
    }

    private Map<String, Object> normalizeDesignatedMemberConfig(Object source) {
        Map<String, Object> raw = toObjectMap(source);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("userIds", toLongList(raw.get("userIds")));
        return config;
    }

    private Map<String, Object> normalizeManualSelectConfig(Object source) {
        Map<String, Object> raw = toObjectMap(source);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("candidateScope", asText(raw.get("candidateScope"), MANUAL_SCOPE_ALL_ACTIVE_USERS));
        return config;
    }

    /**
     * 创建DraftVersion。
     */
    private ProcessFlowVersion createDraftVersion(Long flowId, int versionNo, ProcessFlowSaveDTO dto) {
        ProcessFlowVersion version = new ProcessFlowVersion();
        version.setFlowId(flowId);
        version.setVersionNo(versionNo);
        version.setVersionStatus(VERSION_STATUS_DRAFT);
        version.setSnapshotJson(writeSnapshot(dto));
        processFlowVersionMapper.insert(version);
        replaceVersionNodesAndRoutes(version.getId(), dto);
        return version;
    }

    private void replaceVersionNodesAndRoutes(Long versionId, ProcessFlowSaveDTO dto) {
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
            route.setRouteName(asText(item.getRouteName(), "鍒嗘敮"));
            route.setPriority(item.getPriority() == null ? 1 : item.getPriority());
            route.setDefaultRoute(Boolean.TRUE.equals(item.getDefaultRoute()) ? 1 : 0);
            route.setConditionJson(writeValue(item.getConditionGroups() == null ? Collections.emptyList() : item.getConditionGroups()));
            processFlowRouteMapper.insert(route);
        }
    }

    /**
     * 加载VersionNodes。
     */
    private List<ProcessFlowNodeDTO> loadVersionNodes(Long versionId) {
        return processFlowNodeMapper.selectList(
                Wrappers.<ProcessFlowNode>lambdaQuery()
                        .eq(ProcessFlowNode::getVersionId, versionId)
                        .orderByAsc(ProcessFlowNode::getDisplayOrder, ProcessFlowNode::getId)
        ).stream().map(node -> {
            ProcessFlowNodeDTO item = new ProcessFlowNodeDTO();
            item.setNodeKey(node.getNodeKey());
            item.setNodeType(node.getNodeType());
            item.setNodeName(node.getNodeName());
            item.setSceneId(node.getSceneId());
            item.setParentNodeKey(node.getParentNodeKey());
            item.setDisplayOrder(node.getDisplayOrder());
            item.setConfig(normalizeNodeConfig(node.getNodeType(), readMap(node.getConfigJson()), false));
            return item;
        }).toList();
    }

    /**
     * 加载VersionRoutes。
     */
    private List<ProcessFlowRouteDTO> loadVersionRoutes(Long versionId) {
        return processFlowRouteMapper.selectList(
                Wrappers.<ProcessFlowRoute>lambdaQuery()
                        .eq(ProcessFlowRoute::getVersionId, versionId)
                        .orderByAsc(ProcessFlowRoute::getPriority, ProcessFlowRoute::getId)
        ).stream().map(route -> {
            ProcessFlowRouteDTO item = new ProcessFlowRouteDTO();
            item.setRouteKey(route.getRouteKey());
            item.setSourceNodeKey(route.getSourceNodeKey());
            item.setTargetNodeKey(route.getTargetNodeKey());
            item.setRouteName(route.getRouteName());
            item.setPriority(route.getPriority());
            item.setDefaultRoute(route.getDefaultRoute() != null && route.getDefaultRoute() == 1);
            item.setConditionGroups(readConditionGroups(route.getConditionJson()));
            return item;
        }).toList();
    }

    /**
     * 解析ManagerMembers。
     */
    private List<User> resolveManagerMembers(Map<String, Object> config, Map<String, Object> context, List<String> trace) {
        Map<String, Object> managerConfig = normalizeManagerConfig(config.get("managerConfig"));
        String ruleMode = asText(managerConfig.get("ruleMode"), MANAGER_RULE_MODE_FORM_DEPT_MANAGER);
        String deptSource = asText(managerConfig.get("deptSource"), DEPT_SOURCE_UNDERTAKE);
        int managerLevel = limitLevel(asInteger(managerConfig.get("managerLevel"), 1), "涓荤绾ф");
        boolean orgTreeLookupEnabled = asBoolean(managerConfig.get("orgTreeLookupEnabled"), true);
        int lookupLevel = limitLevel(asInteger(managerConfig.get("orgTreeLookupLevel"), 1), "鍚戜笂鏌ユ壘绾ф");
        Map<Long, SystemDepartment> departmentMap = loadAllDepartmentMap();
        List<Long> startDeptIds = resolveStartDeptIds(deptSource, context);
        if (startDeptIds.isEmpty()) {
            trace.add("No start department available for manager resolution");
            return Collections.emptyList();
        }

        Long submitterUserId = asLong(context == null ? null : context.get("submitterUserId"));
        List<User> result = new ArrayList<>();
        for (Long deptId : startDeptIds) {
            SystemDepartment sourceDept = departmentMap.get(deptId);
            if (sourceDept == null) {
                trace.add("璧峰閮ㄩ棬涓嶅瓨鍦細" + deptId);
                continue;
            }

            SystemDepartment targetDept = sourceDept;
            if (MANAGER_RULE_MODE_FORM_DEPT_MANAGER.equals(ruleMode)) {
                targetDept = climbDepartment(sourceDept, departmentMap, Math.max(managerLevel - 1, 0));
                trace.add("Department " + deptId + " positioned to " + (targetDept == null ? "none" : targetDept.getDeptName()));
            }

            User approver = findLeaderForDepartment(
                    targetDept,
                    departmentMap,
                    submitterUserId,
                    orgTreeLookupEnabled,
                    lookupLevel,
                    trace
            );
            if (approver != null) {
                result.add(approver);
            }
        }
        return result;
    }

    /**
     * 查询上级ForDepartment。
     */
    private User findLeaderForDepartment(
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

    /**
     * 解析上级。
     */
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

    /**
     * 解析DesignatedMembers。
     */
    private List<User> resolveDesignatedMembers(Map<String, Object> config, List<String> trace) {
        List<Long> userIds = toLongList(toObjectMap(config.get("designatedMemberConfig")).get("userIds"));
        if (userIds.isEmpty()) {
            trace.add("No designated members configured");
            return Collections.emptyList();
        }
        trace.add("Designated members: " + userIds);
        return loadActiveUsers(userIds);
    }

    /**
     * 解析ManualMembers。
     */
    private List<User> resolveManualMembers(Map<String, Object> context, List<String> trace) {
        List<Long> userIds = toLongList(context == null ? null : context.get("manualSelectedUserIds"));
        if (userIds.isEmpty()) {
            trace.add("No manual approvers selected");
            return Collections.emptyList();
        }
        trace.add("Manual approvers: " + userIds);
        return loadActiveUsers(userIds);
    }

    /**
     * 解析StartDeptIds。
     */
    private List<Long> resolveStartDeptIds(String deptSource, Map<String, Object> context) {
        if (DEPT_SOURCE_SUBMITTER.equals(deptSource)) {
            Long submitterDeptId = asLong(context == null ? null : context.get("submitterDeptId"));
            return submitterDeptId == null ? Collections.emptyList() : List.of(submitterDeptId);
        }

        List<Long> undertakeDeptIds = toLongList(context == null ? null : context.get("undertakeDeptIds"));
        if (!undertakeDeptIds.isEmpty()) {
            return List.of(undertakeDeptIds.get(0));
        }

        Long submitterDeptId = asLong(context == null ? null : context.get("submitterDeptId"));
        return submitterDeptId == null ? Collections.emptyList() : List.of(submitterDeptId);
    }

    private SystemDepartment climbDepartment(SystemDepartment start, Map<Long, SystemDepartment> departmentMap, int steps) {
        SystemDepartment current = start;
        for (int index = 0; index < steps && current != null; index++) {
            current = current.getParentId() == null ? null : departmentMap.get(current.getParentId());
        }
        return current;
    }

    /**
     * 加载Version映射。
     */
    private Map<Long, ProcessFlowVersion> loadVersionMap(Collection<Long> versionIds) {
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

    private ProcessFlowVersion currentDraftVersion(ProcessFlow flow) {
        if (flow.getCurrentDraftVersionId() == null) {
            return null;
        }
        ProcessFlowVersion version = processFlowVersionMapper.selectById(flow.getCurrentDraftVersionId());
        return version != null && VERSION_STATUS_DRAFT.equals(version.getVersionStatus()) ? version : null;
    }

    private ProcessFlowVersion currentPublishedVersion(ProcessFlow flow) {
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

    /**
     * 解析EditableVersion。
     */
    private ProcessFlowVersion resolveEditableVersion(ProcessFlow flow) {
        ProcessFlowVersion draft = currentDraftVersion(flow);
        return draft != null ? draft : currentPublishedVersion(flow);
    }

    private ProcessFlow requireFlow(Long id) {
        ProcessFlow flow = processFlowMapper.selectById(id);
        if (flow == null) {
            throw new IllegalStateException("未找到对应流程");
        }
        return flow;
    }

    private int nextVersionNo(Long flowId) {
        List<ProcessFlowVersion> versions = processFlowVersionMapper.selectList(
                Wrappers.<ProcessFlowVersion>lambdaQuery()
                        .eq(ProcessFlowVersion::getFlowId, flowId)
                        .orderByDesc(ProcessFlowVersion::getVersionNo)
                        .last("limit 1")
        );
        return versions.isEmpty() ? 1 : versions.get(0).getVersionNo() + 1;
    }

    /**
     * 组装流程编码。
     */
    private String buildFlowCode() {
        String prefix = "PF" + LocalDate.now().format(CODE_DATE_FORMATTER);
        Long count = processFlowMapper.selectCount(
                Wrappers.<ProcessFlow>lambdaQuery().likeRight(ProcessFlow::getFlowCode, prefix)
        );
        return prefix + String.format("%04d", (count == null ? 0L : count) + 1);
    }

    /**
     * 组装Scene编码。
     */
    private String buildSceneCode() {
        String prefix = "PS" + LocalDate.now().format(CODE_DATE_FORMATTER);
        Long count = processFlowSceneMapper.selectCount(
                Wrappers.<ProcessFlowScene>lambdaQuery().likeRight(ProcessFlowScene::getSceneCode, prefix)
        );
        return prefix + String.format("%04d", (count == null ? 0L : count) + 1);
    }

    /**
     * 加载Scene选项。
     */
    private List<ProcessFlowSceneVO> loadSceneOptions() {
        return processFlowSceneMapper.selectList(
                Wrappers.<ProcessFlowScene>lambdaQuery()
                        .eq(ProcessFlowScene::getStatus, 1)
                        .orderByAsc(ProcessFlowScene::getId)
        ).stream().map(this::toSceneVO).toList();
    }

    private ProcessFlowSceneVO toSceneVO(ProcessFlowScene scene) {
        ProcessFlowSceneVO item = new ProcessFlowSceneVO();
        item.setId(scene.getId());
        item.setSceneCode(scene.getSceneCode());
        item.setSceneName(scene.getSceneName());
        item.setSceneDescription(scene.getSceneDescription());
        item.setStatus(scene.getStatus());
        return item;
    }

    /**
     * 组装Level选项。
     */
    private List<ProcessFormOptionVO> buildLevelOptions(String pattern) {
        List<ProcessFormOptionVO> result = new ArrayList<>();
        for (int level = 1; level <= 10; level++) {
            result.add(option(String.format(pattern, level), String.valueOf(level)));
        }
        return result;
    }

    /**
     * 组装Condition字段。
     */
    private List<ProcessFlowConditionFieldVO> buildConditionFields() {
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

    private ProcessFlowConditionFieldVO conditionField(String key, String label, String valueType, List<String> operators) {
        ProcessFlowConditionFieldVO item = new ProcessFlowConditionFieldVO();
        item.setKey(key);
        item.setLabel(label);
        item.setValueType(valueType);
        item.setOperatorKeys(new ArrayList<>(operators));
        return item;
    }

    /**
     * 加载Department选项。
     */
    private List<ProcessFormOptionVO> loadDepartmentOptions() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .eq(SystemDepartment::getStatus, 1)
                        .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId)
        ).stream().map(item -> option(item.getDeptName(), String.valueOf(item.getId()))).toList();
    }

    /**
     * 加载用户选项。
     */
    private List<ProcessFormOptionVO> loadUserOptions() {
        return userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getId)
        ).stream().map(item -> option(normalizeUserName(item), String.valueOf(item.getId()))).toList();
    }

    /**
     * 加载报销单类型选项。
     */
    private List<ProcessFormOptionVO> loadExpenseTypeOptions() {
        return processExpenseTypeMapper.selectList(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getStatus, 1)
                        .orderByAsc(ProcessExpenseType::getExpenseCode, ProcessExpenseType::getId)
        ).stream().map(item -> option(item.getExpenseName(), item.getExpenseCode())).toList();
    }

    /**
     * 加载档案选项。
     */
    private List<ProcessFormOptionVO> loadArchiveOptions() {
        return processCustomArchiveDesignMapper.selectList(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .eq(ProcessCustomArchiveDesign::getStatus, 1)
                        .orderByDesc(ProcessCustomArchiveDesign::getCreatedAt, ProcessCustomArchiveDesign::getId)
        ).stream().map(item -> option(item.getArchiveName(), item.getArchiveCode())).toList();
    }

    /**
     * 加载全部Department映射。
     */
    private Map<Long, SystemDepartment> loadAllDepartmentMap() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getStatus, 1)
        ).stream().collect(Collectors.toMap(
                SystemDepartment::getId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    /**
     * 加载DeptName映射。
     */
    private Map<Long, String> loadDeptNameMap(List<Long> deptIds) {
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

    /**
     * 加载Active用户。
     */
    private List<User> loadActiveUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(item.getStatus(), 1))
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    /**
     * 加载Active用户。
     */
    private User loadActiveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user != null && Objects.equals(user.getStatus(), 1) ? user : null;
    }

    /**
     * 校验Active用户。
     */
    private void validateActiveUsers(List<Long> userIds) {
        if (loadActiveUsers(userIds).stream().map(User::getId).collect(Collectors.toCollection(LinkedHashSet::new)).size()
                != new LinkedHashSet<>(userIds).size()) {
            throw new IllegalStateException("瀹℃壒鑺傜偣涓瓨鍦ㄦ棤鏁堢殑绯荤粺鎴愬憳");
        }
    }

    private String writeSnapshot(ProcessFlowSaveDTO dto) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("flowName", dto.getFlowName());
        snapshot.put("flowDescription", dto.getFlowDescription());
        snapshot.put("nodes", dto.getNodes() == null ? Collections.emptyList() : dto.getNodes());
        snapshot.put("routes", dto.getRoutes() == null ? Collections.emptyList() : dto.getRoutes());
        return writeValue(snapshot);
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("流程配置序列化失败", exception);
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("娴佺▼閰嶇疆瑙ｆ瀽澶辫触", exception);
        }
    }

    private List<ProcessFlowConditionGroupDTO> readConditionGroups(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("娴佺▼鍒嗘敮鏉′欢瑙ｆ瀽澶辫触", exception);
        }
    }

    private String normalizeFlowStatus(String status) {
        String value = asText(status, FLOW_STATUS_DRAFT);
        if (!FLOW_STATUSES.contains(value)) {
            throw new IllegalStateException("娴佺▼鐘舵€佷笉鍚堟硶");
        }
        return value;
    }

    private String normalizeMissingHandler(String value) {
        String normalized = asText(value, MISSING_HANDLER_AUTO_SKIP);
        if (MISSING_HANDLER_MANUAL_SELECT_ON_SUBMIT.equals(normalized)) {
            return MISSING_HANDLER_BLOCK_SUBMIT;
        }
        if (!MISSING_HANDLERS.contains(normalized)) {
            return MISSING_HANDLER_AUTO_SKIP;
        }
        return normalized;
    }

    private String statusLabel(String status) {
        return switch (status) {
            case FLOW_STATUS_ENABLED -> "已发布";
            case FLOW_STATUS_DISABLED -> "已停用";
            default -> "草稿";
        };
    }

    private String defaultNodeName(String nodeType, int index) {
        return switch (nodeType) {
            case NODE_TYPE_CC -> "\u6284\u9001\u8282\u70b9 " + index;
            case NODE_TYPE_PAYMENT -> "\u652f\u4ed8\u8282\u70b9 " + index;
            case NODE_TYPE_BRANCH -> "\u6d41\u7a0b\u5206\u652f " + index;
            default -> "\u5ba1\u6279\u8282\u70b9 " + index;
        };
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }

    private ProcessFormOptionVO option(String label, String value) {
        ProcessFormOptionVO item = new ProcessFormOptionVO();
        item.setLabel(label);
        item.setValue(value);
        return item;
    }

    private ProcessFlowConfigOptionVO configOption(String value, String label, String description) {
        ProcessFlowConfigOptionVO item = new ProcessFlowConfigOptionVO();
        item.setValue(value);
        item.setLabel(label);
        item.setDescription(description);
        return item;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String asText(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    private Integer asInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("鏁板€兼牸寮忎笉姝ｇ‘");
        }
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("鏁板€兼牸寮忎笉姝ｇ‘");
        }
    }

    private boolean asBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private int limitLevel(Integer value, String label) {
        int level = value == null ? 1 : value;
        if (level < 1 || level > 10) {
            throw new IllegalStateException(label + "鍙兘閫夋嫨 1-10");
        }
        return level;
    }

    private List<String> toStringList(Object value) {
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

    private List<Long> toLongList(Object value) {
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

    private Map<String, Object> toObjectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return castMap(map);
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> castMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private String normalizeUserName(User user) {
        String name = trimToNull(user.getName());
        return name != null ? name : asText(user.getUsername(), "未命名用户");
    }

    private record LeaderResolution(Long departmentId, Long userId) {
    }
}
