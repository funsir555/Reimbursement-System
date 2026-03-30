package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseApprovalLogVO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseApprovalTaskVO;
import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.ProcessCustomArchiveDetailVO;
import com.finex.auth.dto.ProcessCustomArchiveItemDTO;
import com.finex.auth.dto.ProcessCustomArchiveRuleDTO;
import com.finex.auth.dto.ProcessFlowConditionDTO;
import com.finex.auth.dto.ProcessFlowConditionGroupDTO;
import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessCustomArchiveItem;
import com.finex.auth.entity.ProcessCustomArchiveRule;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessFlow;
import com.finex.auth.entity.ProcessFlowVersion;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.entity.UserBankAccount;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
import com.finex.auth.mapper.ProcessCustomArchiveRuleMapper;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessFlowMapper;
import com.finex.auth.mapper.ProcessFlowVersionMapper;
import com.finex.auth.mapper.ProcessFormDesignMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.FinanceVendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

@Service
@RequiredArgsConstructor
public class ExpenseDocumentServiceImpl implements ExpenseDocumentService {

    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String UNDERTAKE_DEPARTMENT_COMPONENT_CODE = "undertake-department";

    private static final String NODE_TYPE_APPROVAL = "APPROVAL";
    private static final String NODE_TYPE_CC = "CC";
    private static final String NODE_TYPE_PAYMENT = "PAYMENT";
    private static final String NODE_TYPE_BRANCH = "BRANCH";

    private static final String APPROVER_TYPE_MANAGER = "MANAGER";
    private static final String APPROVER_TYPE_DESIGNATED_MEMBER = "DESIGNATED_MEMBER";
    private static final String APPROVER_TYPE_MANUAL_SELECT = "MANUAL_SELECT";

    private static final String DEPT_SOURCE_UNDERTAKE = "UNDERTAKE_DEPT";
    private static final String DEPT_SOURCE_SUBMITTER = "SUBMITTER_DEPT";
    private static final String MISSING_HANDLER_AUTO_SKIP = "AUTO_SKIP";
    private static final String APPROVAL_MODE_OR_SIGN = "OR_SIGN";
    private static final String APPROVAL_MODE_AND_SIGN = "AND_SIGN";

    private static final String DOCUMENT_STATUS_PENDING = "PENDING_APPROVAL";
    private static final String DOCUMENT_STATUS_APPROVED = "APPROVED";
    private static final String DOCUMENT_STATUS_REJECTED = "REJECTED";
    private static final String DOCUMENT_STATUS_EXCEPTION = "EXCEPTION";

    private static final String TASK_STATUS_PENDING = "PENDING";
    private static final String TASK_STATUS_APPROVED = "APPROVED";
    private static final String TASK_STATUS_REJECTED = "REJECTED";
    private static final String TASK_STATUS_CANCELLED = "CANCELLED";

    private static final String LOG_SUBMIT = "SUBMIT";
    private static final String LOG_ROUTE_HIT = "ROUTE_HIT";
    private static final String LOG_APPROVAL_PENDING = "APPROVAL_PENDING";
    private static final String LOG_APPROVE = "APPROVE";
    private static final String LOG_REJECT = "REJECT";
    private static final String LOG_AUTO_SKIP = "AUTO_SKIP";
    private static final String LOG_CC_REACHED = "CC_REACHED";
    private static final String LOG_PAYMENT_REACHED = "PAYMENT_REACHED";
    private static final String LOG_FINISH = "FINISH";
    private static final String LOG_EXCEPTION = "EXCEPTION";

    private final ProcessDocumentTemplateMapper templateMapper;
    private final ProcessFormDesignMapper processFormDesignMapper;
    private final ProcessCustomArchiveDesignMapper customArchiveDesignMapper;
    private final ProcessCustomArchiveItemMapper customArchiveItemMapper;
    private final ProcessCustomArchiveRuleMapper customArchiveRuleMapper;
    private final ProcessFlowMapper processFlowMapper;
    private final ProcessFlowVersionMapper processFlowVersionMapper;
    private final FinanceVendorMapper financeVendorMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final UserMapper userMapper;
    private final UserBankAccountMapper userBankAccountMapper;
    private final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    private final ProcessDocumentTaskMapper processDocumentTaskMapper;
    private final ProcessDocumentActionLogMapper processDocumentActionLogMapper;
    private final FinanceVendorService financeVendorService;
    private final ObjectMapper objectMapper;

    @Override
    public List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates() {
        return templateMapper.selectList(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getEnabled, 1)
                        .orderByAsc(ProcessDocumentTemplate::getSortOrder, ProcessDocumentTemplate::getId)
        ).stream().map(this::toTemplateSummary).toList();
    }

    @Override
    public ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode) {
        ProcessDocumentTemplate template = requireTemplate(templateCode);
        ExpenseCreateTemplateDetailVO detail = new ExpenseCreateTemplateDetailVO();
        detail.setTemplateCode(template.getTemplateCode());
        detail.setTemplateName(template.getTemplateName());
        detail.setTemplateType(template.getTemplateType());
        detail.setTemplateTypeLabel(resolveTemplateTypeLabel(template.getTemplateType(), template.getTemplateTypeLabel()));
        detail.setCategoryCode(template.getCategoryCode());
        detail.setTemplateDescription(template.getTemplateDescription());
        detail.setFormDesignCode(template.getFormDesignCode());
        detail.setApprovalFlowCode(template.getApprovalFlow());
        detail.setFlowName(template.getFlowName());

        ProcessFormDesign formDesign = loadFormDesign(template.getFormDesignCode());
        if (formDesign != null) {
            detail.setFormName(formDesign.getFormName());
            detail.setSchema(readSchema(formDesign.getSchemaJson()));
            detail.setSharedArchives(loadSharedArchives(detail.getSchema()));
        } else {
            detail.setSchema(defaultSchema());
            detail.setSharedArchives(Collections.emptyList());
        }
        detail.setDepartmentOptions(loadDepartmentOptions());
        User currentUser = userId == null ? null : userMapper.selectById(userId);
        if (currentUser != null && currentUser.getDeptId() != null) {
            detail.setCurrentUserDeptId(String.valueOf(currentUser.getDeptId()));
            SystemDepartment department = systemDepartmentMapper.selectById(currentUser.getDeptId());
            if (department != null) {
                detail.setCurrentUserDeptName(department.getDeptName());
            }
        }
        return detail;
    }

    @Override
    public List<ExpenseCreateVendorOptionVO> listVendorOptions(String keyword) {
        return financeVendorService.listActiveVendorOptions(keyword);
    }

    @Override
    public List<ExpenseCreatePayeeOptionVO> listPayeeOptions(String keyword) {
        String normalizedKeyword = trimToNull(keyword);
        List<ExpenseCreatePayeeOptionVO> options = new ArrayList<>();
        financeVendorService.listActiveVendorOptions(normalizedKeyword).forEach(item -> {
            ExpenseCreatePayeeOptionVO option = new ExpenseCreatePayeeOptionVO();
            option.setValue("VENDOR:" + item.getCVenCode());
            option.setLabel(item.getCVenName());
            option.setSourceType("VENDOR");
            option.setSourceCode(item.getCVenCode());
            option.setSecondaryLabel(item.getSecondaryLabel());
            options.add(option);
        });

        List<User> users = userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getName, User::getId)
        );
        users.stream()
                .filter(user -> matchesKeyword(normalizedKeyword, user.getName(), user.getUsername(), user.getPhone(), user.getEmail()))
                .forEach(user -> {
                    ExpenseCreatePayeeOptionVO option = new ExpenseCreatePayeeOptionVO();
                    option.setValue("USER:" + user.getId());
                    option.setLabel(user.getName());
                    option.setSourceType("USER");
                    option.setSourceCode(String.valueOf(user.getId()));
                    option.setSecondaryLabel(trimToNull(user.getPhone()) != null ? user.getPhone() : user.getUsername());
                    options.add(option);
                });
        return options;
    }

    @Override
    public List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(String keyword) {
        String normalizedKeyword = trimToNull(keyword);
        List<ExpenseCreatePayeeAccountOptionVO> options = new ArrayList<>();

        QueryWrapper<FinanceVendor> vendorQuery = new QueryWrapper<>();
        vendorQuery.isNull("dEndDate")
                .isNotNull("cVenAccount")
                .orderByAsc("cVenName", "cVenCode");
        financeVendorMapper.selectList(vendorQuery).stream()
                .filter(item -> trimToNull(item.getCVenAccount()) != null)
                .filter(item -> matchesKeyword(
                        normalizedKeyword,
                        item.getCVenName(),
                        item.getCVenAbbName(),
                        item.getCVenBank(),
                        item.getCVenAccount(),
                        item.getCVenBankNub()
                ))
                .forEach(item -> {
                    ExpenseCreatePayeeAccountOptionVO option = new ExpenseCreatePayeeAccountOptionVO();
                    option.setValue("VENDOR:" + item.getCVenCode());
                    option.setLabel(buildAccountLabel(item.getCVenName(), item.getCVenBank()));
                    option.setSourceType("VENDOR");
                    option.setOwnerCode(item.getCVenCode());
                    option.setOwnerName(item.getCVenName());
                    option.setBankName(item.getCVenBank());
                    option.setAccountName(item.getCVenName());
                    option.setAccountNoMasked(maskAccountNo(item.getCVenAccount()));
                    option.setSecondaryLabel(buildVendorAccountSecondary(item));
                    options.add(option);
                });

        List<User> users = userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getName, User::getId)
        );
        if (!users.isEmpty()) {
            Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
            List<UserBankAccount> accounts = userBankAccountMapper.selectList(
                    Wrappers.<UserBankAccount>lambdaQuery()
                            .eq(UserBankAccount::getStatus, 1)
                            .in(UserBankAccount::getUserId, userMap.keySet())
                            .orderByDesc(UserBankAccount::getDefaultAccount)
                            .orderByAsc(UserBankAccount::getId)
            );
            accounts.stream()
                    .filter(account -> {
                        User user = userMap.get(account.getUserId());
                        return user != null && matchesKeyword(
                                normalizedKeyword,
                                user.getName(),
                                user.getUsername(),
                                account.getBankName(),
                                account.getAccountName(),
                                account.getAccountNo()
                        );
                    })
                    .forEach(account -> {
                        User user = userMap.get(account.getUserId());
                        ExpenseCreatePayeeAccountOptionVO option = new ExpenseCreatePayeeAccountOptionVO();
                        option.setValue("USER_ACCOUNT:" + account.getId());
                        option.setLabel(buildAccountLabel(account.getAccountName(), account.getBankName()));
                        option.setSourceType("USER");
                        option.setOwnerCode(String.valueOf(user.getId()));
                        option.setOwnerName(user.getName());
                        option.setBankName(account.getBankName());
                        option.setAccountName(account.getAccountName());
                        option.setAccountNoMasked(maskAccountNo(account.getAccountNo()));
                        option.setSecondaryLabel(trimToNull(account.getBranchName()) != null ? account.getBranchName() : user.getUsername());
                        options.add(option);
                    });
        }

        return options;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto) {
        ProcessDocumentTemplate template = requireTemplate(dto.getTemplateCode());
        ProcessFormDesign formDesign = loadFormDesign(template.getFormDesignCode());
        Map<String, Object> formData = dto.getFormData() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.getFormData());
        User currentUser = userId == null ? null : userMapper.selectById(userId);

        Map<String, Object> runtimeFlowContext = buildRuntimeFlowContext(currentUser, template, formDesign, formData);

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode(buildDocumentCode());
        instance.setTemplateCode(template.getTemplateCode());
        instance.setTemplateName(template.getTemplateName());
        instance.setTemplateType(template.getTemplateType());
        instance.setFormDesignCode(template.getFormDesignCode());
        instance.setApprovalFlowCode(template.getApprovalFlow());
        instance.setFlowName(template.getFlowName());
        instance.setSubmitterUserId(userId);
        instance.setSubmitterName(defaultUsername(username));
        instance.setDocumentTitle(resolveDocumentTitle(template, formData, username));
        instance.setDocumentReason(resolveDocumentReason(template, formData));
        instance.setTotalAmount(resolveTotalAmount(formData));
        instance.setStatus(DOCUMENT_STATUS_PENDING);
        instance.setFormDataJson(writeJson(formData));
        instance.setTemplateSnapshotJson(writeJson(toTemplateSnapshot(template)));
        instance.setFormSchemaSnapshotJson(formDesign == null ? writeJson(defaultSchema()) : formDesign.getSchemaJson());
        instance.setFlowSnapshotJson(resolveFlowSnapshotJson(template));
        instance.setCreatedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        processDocumentInstanceMapper.insert(instance);

        appendLog(instance.getDocumentCode(), null, null, LOG_SUBMIT, userId, defaultUsername(username), null, Map.of(
                "templateCode", template.getTemplateCode(),
                "templateName", template.getTemplateName()
        ));
        initializeRuntime(instance, runtimeFlowContext);

        ExpenseDocumentSubmitResultVO result = new ExpenseDocumentSubmitResultVO();
        result.setId(instance.getId());
        result.setDocumentCode(instance.getDocumentCode());
        result.setStatus(instance.getStatus());
        return result;
    }

    @Override
    public List<ExpenseSummaryVO> listExpenseSummaries(Long userId) {
        return processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getSubmitterUserId, userId)
                        .orderByDesc(ProcessDocumentInstance::getCreatedAt, ProcessDocumentInstance::getId)
        ).stream().map(this::toExpenseSummary).toList();
    }

    @Override
    public ExpenseDocumentDetailVO getDocumentDetail(Long userId, String documentCode, boolean allowCrossView) {
        ProcessDocumentInstance instance = requireDocument(documentCode);
        if (!allowCrossView && !Objects.equals(instance.getSubmitterUserId(), userId)) {
            throw new IllegalStateException("Current user cannot view this document");
        }
        return buildDocumentDetail(instance);
    }

    @Override
    public List<ExpenseApprovalPendingItemVO> listPendingApprovals(Long userId) {
        List<ProcessDocumentTask> tasks = processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getAssigneeUserId, userId)
                        .eq(ProcessDocumentTask::getStatus, TASK_STATUS_PENDING)
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, ProcessDocumentInstance> instanceMap = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .in(ProcessDocumentInstance::getDocumentCode, tasks.stream().map(ProcessDocumentTask::getDocumentCode).toList())
        ).stream().collect(Collectors.toMap(
                ProcessDocumentInstance::getDocumentCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        return tasks.stream().map(task -> toPendingItem(task, instanceMap.get(task.getDocumentCode()))).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO approveTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        ProcessDocumentTask task = requirePendingTask(taskId, userId);
        ProcessDocumentInstance instance = requireDocument(task.getDocumentCode());
        FlowRuntimeSnapshot snapshot = readFlowSnapshot(instance.getFlowSnapshotJson());
        ProcessFlowNodeDTO node = snapshot.node(task.getNodeKey());
        if (node == null) {
            throw new IllegalStateException("Flow node not found for current task");
        }

        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TASK_STATUS_APPROVED);
        task.setHandledAt(now);
        task.setActionComment(trimToNull(dto == null ? null : dto.getComment()));
        processDocumentTaskMapper.updateById(task);
        appendLog(instance.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_APPROVE, userId, defaultUsername(username), task.getActionComment(), Map.of(
                "taskId", task.getId(),
                "approvalMode", defaultText(task.getApprovalMode(), APPROVAL_MODE_OR_SIGN)
        ));

        List<ProcessDocumentTask> sameBatchTasks = loadNodeBatchTasks(task.getDocumentCode(), task.getNodeKey(), task.getTaskBatchNo());
        String approvalMode = defaultText(task.getApprovalMode(), APPROVAL_MODE_OR_SIGN);
        boolean nodeCompleted;
        if (APPROVAL_MODE_AND_SIGN.equals(approvalMode)) {
            nodeCompleted = sameBatchTasks.stream().noneMatch(item -> TASK_STATUS_PENDING.equals(item.getStatus()));
        } else {
            cancelOtherPendingTasks(sameBatchTasks, task.getId(), now);
            nodeCompleted = true;
        }

        if (nodeCompleted) {
            Map<String, Object> context = buildRuntimeContextForInstance(instance);
            clearCurrentNode(instance);
            advanceFromPosition(instance, snapshot, context, node.getParentNodeKey(), nextIndex(snapshot, node));
        } else {
            instance.setUpdatedAt(now);
            processDocumentInstanceMapper.updateById(instance);
        }
        return buildDocumentDetail(requireDocument(instance.getDocumentCode()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDocumentDetailVO rejectTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        ProcessDocumentTask task = requirePendingTask(taskId, userId);
        ProcessDocumentInstance instance = requireDocument(task.getDocumentCode());
        LocalDateTime now = LocalDateTime.now();

        task.setStatus(TASK_STATUS_REJECTED);
        task.setHandledAt(now);
        task.setActionComment(trimToNull(dto == null ? null : dto.getComment()));
        processDocumentTaskMapper.updateById(task);
        cancelOtherPendingTasks(loadNodeBatchTasks(task.getDocumentCode(), task.getNodeKey(), task.getTaskBatchNo()), task.getId(), now);

        instance.setStatus(DOCUMENT_STATUS_REJECTED);
        instance.setCurrentNodeKey(task.getNodeKey());
        instance.setCurrentNodeName(task.getNodeName());
        instance.setCurrentTaskType("REJECTED");
        instance.setFinishedAt(now);
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);

        appendLog(instance.getDocumentCode(), task.getNodeKey(), task.getNodeName(), LOG_REJECT, userId, defaultUsername(username), task.getActionComment(), Map.of(
                "taskId", task.getId()
        ));
        return buildDocumentDetail(instance);
    }

    private void initializeRuntime(ProcessDocumentInstance instance, Map<String, Object> context) {
        FlowRuntimeSnapshot snapshot = readFlowSnapshot(instance.getFlowSnapshotJson());
        if (snapshot.nodes().isEmpty()) {
            markDocumentApproved(instance);
            appendLog(instance.getDocumentCode(), null, null, LOG_FINISH, null, "SYSTEM", "No approval nodes configured", Collections.emptyMap());
            return;
        }
        advanceFromPosition(instance, snapshot, context, null, 0);
    }

    private FlowAdvanceState advanceFromPosition(
            ProcessDocumentInstance instance,
            FlowRuntimeSnapshot snapshot,
            Map<String, Object> context,
            String containerKey,
            int startIndex
    ) {
        FlowAdvanceState state = processContainer(instance, snapshot, context, containerKey, startIndex);
        if (state == FlowAdvanceState.PAUSED) {
            return state;
        }

        ProcessFlowRouteDTO route = snapshot.routeByKey(containerKey);
        if (route != null) {
            ProcessFlowNodeDTO branchNode = snapshot.node(route.getSourceNodeKey());
            if (branchNode != null) {
                return advanceFromPosition(instance, snapshot, context, branchNode.getParentNodeKey(), nextIndex(snapshot, branchNode));
            }
        }

        markDocumentApproved(instance);
        appendLog(instance.getDocumentCode(), null, null, LOG_FINISH, null, "SYSTEM", "Approval flow finished", Collections.emptyMap());
        return FlowAdvanceState.COMPLETED;
    }

    private FlowAdvanceState processContainer(
            ProcessDocumentInstance instance,
            FlowRuntimeSnapshot snapshot,
            Map<String, Object> context,
            String containerKey,
            int startIndex
    ) {
        List<ProcessFlowNodeDTO> nodes = snapshot.children(containerKey);
        for (int index = startIndex; index < nodes.size(); index++) {
            ProcessFlowNodeDTO node = nodes.get(index);
            switch (defaultText(asText(node.getNodeType()), "")) {
                case NODE_TYPE_BRANCH -> {
                    ProcessFlowRouteDTO matchedRoute = matchRoute(snapshot.routes(node.getNodeKey()), context);
                    if (matchedRoute == null) {
                        markDocumentException(instance, node, "No branch route matched");
                        return FlowAdvanceState.PAUSED;
                    }
                    appendLog(instance.getDocumentCode(), node.getNodeKey(), node.getNodeName(), LOG_ROUTE_HIT, null, "SYSTEM", null, Map.of(
                            "routeKey", matchedRoute.getRouteKey(),
                            "routeName", defaultText(matchedRoute.getRouteName(), matchedRoute.getRouteKey())
                    ));
                    return advanceFromPosition(instance, snapshot, context, matchedRoute.getRouteKey(), 0);
                }
                case NODE_TYPE_APPROVAL -> {
                    List<User> approvers = resolveApprovers(node, context);
                    if (approvers.isEmpty()) {
                        String missingHandler = resolveMissingHandler(node.getConfig());
                        if (MISSING_HANDLER_AUTO_SKIP.equals(missingHandler)) {
                            appendLog(instance.getDocumentCode(), node.getNodeKey(), node.getNodeName(), LOG_AUTO_SKIP, null, "SYSTEM", "No approver resolved, auto skipped", Collections.emptyMap());
                            continue;
                        }
                        markDocumentException(instance, node, "No approver resolved");
                        return FlowAdvanceState.PAUSED;
                    }
                    createApprovalTasks(instance, node, approvers);
                    return FlowAdvanceState.PAUSED;
                }
                case NODE_TYPE_CC -> appendLog(instance.getDocumentCode(), node.getNodeKey(), node.getNodeName(), LOG_CC_REACHED, null, "SYSTEM", "CC node reached", Collections.emptyMap());
                case NODE_TYPE_PAYMENT -> appendLog(instance.getDocumentCode(), node.getNodeKey(), node.getNodeName(), LOG_PAYMENT_REACHED, null, "SYSTEM", "Payment node reached", Collections.emptyMap());
                default -> {
                }
            }
        }
        return FlowAdvanceState.COMPLETED;
    }

    private void createApprovalTasks(ProcessDocumentInstance instance, ProcessFlowNodeDTO node, List<User> approvers) {
        LocalDateTime now = LocalDateTime.now();
        String approvalMode = defaultText(asText(node.getConfig().get("approvalMode")), APPROVAL_MODE_OR_SIGN);
        String batchNo = buildTaskBatchNo(instance.getDocumentCode(), node.getNodeKey());
        List<User> distinctApprovers = approvers.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new),
                        item -> new ArrayList<>(item.values())
                ));
        for (User approver : distinctApprovers) {
            ProcessDocumentTask task = new ProcessDocumentTask();
            task.setDocumentCode(instance.getDocumentCode());
            task.setNodeKey(node.getNodeKey());
            task.setNodeName(node.getNodeName());
            task.setNodeType(node.getNodeType());
            task.setAssigneeUserId(approver.getId());
            task.setAssigneeName(normalizeUserName(approver));
            task.setStatus(TASK_STATUS_PENDING);
            task.setTaskBatchNo(batchNo);
            task.setApprovalMode(approvalMode);
            task.setCreatedAt(now);
            processDocumentTaskMapper.insert(task);
        }
        instance.setStatus(DOCUMENT_STATUS_PENDING);
        instance.setCurrentNodeKey(node.getNodeKey());
        instance.setCurrentNodeName(node.getNodeName());
        instance.setCurrentTaskType(NODE_TYPE_APPROVAL);
        instance.setFinishedAt(null);
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);

        appendLog(instance.getDocumentCode(), node.getNodeKey(), node.getNodeName(), LOG_APPROVAL_PENDING, null, "SYSTEM", null, Map.of(
                "approvalMode", approvalMode,
                "approverUserIds", distinctApprovers.stream().map(User::getId).toList()
        ));
    }

    private ProcessFlowRouteDTO matchRoute(List<ProcessFlowRouteDTO> routes, Map<String, Object> context) {
        if (routes == null || routes.isEmpty()) {
            return null;
        }
        return routes.stream()
                .sorted(Comparator.comparing(item -> item.getPriority() == null ? Integer.MAX_VALUE : item.getPriority()))
                .filter(route -> routeMatches(route, context))
                .findFirst()
                .orElse(null);
    }

    private boolean routeMatches(ProcessFlowRouteDTO route, Map<String, Object> context) {
        if (route.getConditionGroups() == null || route.getConditionGroups().isEmpty()) {
            return true;
        }
        return route.getConditionGroups().stream().anyMatch(group -> groupMatches(group, context));
    }

    private boolean groupMatches(ProcessFlowConditionGroupDTO group, Map<String, Object> context) {
        if (group.getConditions() == null || group.getConditions().isEmpty()) {
            return true;
        }
        return group.getConditions().stream().allMatch(condition -> conditionMatches(condition, context));
    }

    private boolean conditionMatches(ProcessFlowConditionDTO condition, Map<String, Object> context) {
        Object actual = context.get(condition.getFieldKey());
        Object compare = condition.getCompareValue();
        String operator = defaultText(condition.getOperator(), "EQ");
        return switch (operator) {
            case "NE" -> !valuesEqual(actual, compare);
            case "IN" -> anyIn(actual, compare, false);
            case "NOT_IN" -> !anyIn(actual, compare, false);
            case "GT" -> compareNumbers(actual, compare) > 0;
            case "GE" -> compareNumbers(actual, compare) >= 0;
            case "LT" -> compareNumbers(actual, compare) < 0;
            case "LE" -> compareNumbers(actual, compare) <= 0;
            case "BETWEEN" -> between(actual, compare);
            case "CONTAINS" -> containsValue(actual, compare);
            default -> valuesEqual(actual, compare);
        };
    }

    private List<User> resolveApprovers(ProcessFlowNodeDTO node, Map<String, Object> context) {
        Map<String, Object> config = node.getConfig() == null ? new LinkedHashMap<>() : node.getConfig();
        String approverType = defaultText(asText(config.get("approverType")), APPROVER_TYPE_MANAGER);
        List<User> users;
        if (APPROVER_TYPE_DESIGNATED_MEMBER.equals(approverType)) {
            users = resolveDesignatedMembers(config);
        } else if (APPROVER_TYPE_MANUAL_SELECT.equals(approverType)) {
            users = resolveManualMembers(context);
        } else {
            users = resolveManagerMembers(config, context);
        }
        return users.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new),
                        item -> new ArrayList<>(item.values())
                ));
    }

    private List<User> resolveManagerMembers(Map<String, Object> config, Map<String, Object> context) {
        Map<String, Object> managerConfig = toObjectMap(config.get("managerConfig"));
        String deptSource = defaultText(asText(managerConfig.get("deptSource")), DEPT_SOURCE_UNDERTAKE);
        int managerLevel = clampLevel(asInteger(managerConfig.get("managerLevel"), 1));
        boolean orgTreeLookupEnabled = asBoolean(managerConfig.get("orgTreeLookupEnabled"), true);
        int lookupLevel = clampLevel(asInteger(managerConfig.get("orgTreeLookupLevel"), 1));
        Map<Long, SystemDepartment> departmentMap = loadAllDepartmentMap();
        List<Long> startDeptIds = resolveStartDeptIds(deptSource, context);
        if (startDeptIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> result = new ArrayList<>();
        for (Long deptId : startDeptIds) {
            SystemDepartment sourceDept = departmentMap.get(deptId);
            if (sourceDept == null) {
                continue;
            }
            SystemDepartment targetDept = climbDepartment(sourceDept, departmentMap, Math.max(managerLevel - 1, 0));
            LeaderResolution leader = resolveLeader(targetDept, departmentMap, orgTreeLookupEnabled, lookupLevel);
            if (leader == null) {
                continue;
            }
            User user = loadActiveUser(leader.userId());
            if (user != null) {
                result.add(user);
            }
        }
        return result;
    }

    private List<User> resolveDesignatedMembers(Map<String, Object> config) {
        return loadActiveUsers(toLongList(toObjectMap(config.get("designatedMemberConfig")).get("userIds")));
    }

    private List<User> resolveManualMembers(Map<String, Object> context) {
        return loadActiveUsers(toLongList(context.get("manualSelectedUserIds")));
    }

    private List<Long> resolveStartDeptIds(String deptSource, Map<String, Object> context) {
        if (DEPT_SOURCE_SUBMITTER.equals(deptSource)) {
            Long submitterDeptId = asLong(context.get("submitterDeptId"));
            return submitterDeptId == null ? Collections.emptyList() : List.of(submitterDeptId);
        }
        List<Long> undertakeDeptIds = toLongList(context.get("undertakeDeptIds"));
        if (!undertakeDeptIds.isEmpty()) {
            return List.of(undertakeDeptIds.get(0));
        }
        Long submitterDeptId = asLong(context.get("submitterDeptId"));
        return submitterDeptId == null ? Collections.emptyList() : List.of(submitterDeptId);
    }

    private ExpenseDocumentDetailVO buildDocumentDetail(ProcessDocumentInstance instance) {
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        detail.setDocumentCode(instance.getDocumentCode());
        detail.setDocumentTitle(instance.getDocumentTitle());
        detail.setDocumentReason(instance.getDocumentReason());
        detail.setStatus(instance.getStatus());
        detail.setStatusLabel(resolveStatusLabel(instance.getStatus()));
        detail.setTotalAmount(instance.getTotalAmount() == null ? 0D : instance.getTotalAmount().doubleValue());
        detail.setSubmitterUserId(instance.getSubmitterUserId());
        detail.setSubmitterName(instance.getSubmitterName());
        detail.setTemplateName(instance.getTemplateName());
        detail.setTemplateType(instance.getTemplateType());
        detail.setCurrentNodeKey(instance.getCurrentNodeKey());
        detail.setCurrentNodeName(instance.getCurrentNodeName());
        detail.setCurrentTaskType(instance.getCurrentTaskType());
        detail.setSubmittedAt(formatTime(instance.getCreatedAt()));
        detail.setFinishedAt(formatTime(instance.getFinishedAt()));
        detail.setTemplateSnapshot(readMap(instance.getTemplateSnapshotJson()));
        detail.setFormSchemaSnapshot(readMap(instance.getFormSchemaSnapshotJson()));
        detail.setFormData(readFormData(instance.getFormDataJson()));
        detail.setFlowSnapshot(readMap(instance.getFlowSnapshotJson()));
        detail.setDepartmentOptions(loadDepartmentOptions());
        detail.setCurrentTasks(loadPendingTasks(instance.getDocumentCode()).stream().map(this::toTaskVO).toList());
        detail.setActionLogs(loadActionLogs(instance.getDocumentCode()).stream().map(this::toLogVO).toList());
        return detail;
    }

    private ExpenseCreateTemplateSummaryVO toTemplateSummary(ProcessDocumentTemplate template) {
        ExpenseCreateTemplateSummaryVO summary = new ExpenseCreateTemplateSummaryVO();
        summary.setTemplateCode(template.getTemplateCode());
        summary.setTemplateName(template.getTemplateName());
        summary.setTemplateType(template.getTemplateType());
        summary.setTemplateTypeLabel(resolveTemplateTypeLabel(template.getTemplateType(), template.getTemplateTypeLabel()));
        summary.setCategoryCode(template.getCategoryCode());
        summary.setFormDesignCode(template.getFormDesignCode());
        return summary;
    }

    private ExpenseSummaryVO toExpenseSummary(ProcessDocumentInstance instance) {
        ExpenseSummaryVO summary = new ExpenseSummaryVO();
        summary.setDocumentCode(instance.getDocumentCode());
        summary.setNo(instance.getDocumentCode());
        summary.setType(trimToNull(instance.getTemplateName()) != null ? instance.getTemplateName() : resolveTemplateTypeLabel(instance.getTemplateType(), null));
        summary.setReason(trimToNull(instance.getDocumentReason()) != null ? instance.getDocumentReason() : defaultReason(instance.getDocumentTitle()));
        summary.setAmount(instance.getTotalAmount() == null ? 0D : instance.getTotalAmount().doubleValue());
        summary.setDate(instance.getCreatedAt() == null ? "" : instance.getCreatedAt().format(DATE_FORMATTER));
        summary.setStatus(resolveStatusLabel(instance.getStatus()));
        return summary;
    }

    private ExpenseApprovalPendingItemVO toPendingItem(ProcessDocumentTask task, ProcessDocumentInstance instance) {
        ExpenseApprovalPendingItemVO item = new ExpenseApprovalPendingItemVO();
        item.setTaskId(task.getId());
        item.setDocumentCode(task.getDocumentCode());
        item.setDocumentTitle(instance == null ? "" : instance.getDocumentTitle());
        item.setDocumentReason(instance == null ? "" : instance.getDocumentReason());
        item.setTemplateName(instance == null ? "" : instance.getTemplateName());
        item.setSubmitterName(instance == null ? "" : instance.getSubmitterName());
        item.setAmount(instance == null || instance.getTotalAmount() == null ? 0D : instance.getTotalAmount().doubleValue());
        item.setNodeKey(task.getNodeKey());
        item.setNodeName(task.getNodeName());
        item.setStatus(task.getStatus());
        item.setSubmittedAt(instance == null ? null : formatTime(instance.getCreatedAt()));
        item.setTaskCreatedAt(formatTime(task.getCreatedAt()));
        return item;
    }

    private ExpenseApprovalTaskVO toTaskVO(ProcessDocumentTask task) {
        ExpenseApprovalTaskVO vo = new ExpenseApprovalTaskVO();
        vo.setId(task.getId());
        vo.setDocumentCode(task.getDocumentCode());
        vo.setNodeKey(task.getNodeKey());
        vo.setNodeName(task.getNodeName());
        vo.setNodeType(task.getNodeType());
        vo.setAssigneeUserId(task.getAssigneeUserId());
        vo.setAssigneeName(task.getAssigneeName());
        vo.setStatus(task.getStatus());
        vo.setTaskBatchNo(task.getTaskBatchNo());
        vo.setApprovalMode(task.getApprovalMode());
        vo.setActionComment(task.getActionComment());
        vo.setCreatedAt(formatTime(task.getCreatedAt()));
        vo.setHandledAt(formatTime(task.getHandledAt()));
        return vo;
    }

    private ExpenseApprovalLogVO toLogVO(ProcessDocumentActionLog log) {
        ExpenseApprovalLogVO vo = new ExpenseApprovalLogVO();
        vo.setId(log.getId());
        vo.setDocumentCode(log.getDocumentCode());
        vo.setNodeKey(log.getNodeKey());
        vo.setNodeName(log.getNodeName());
        vo.setActionType(log.getActionType());
        vo.setActorUserId(log.getActorUserId());
        vo.setActorName(log.getActorName());
        vo.setActionComment(log.getActionComment());
        vo.setPayload(readMap(log.getPayloadJson()));
        vo.setCreatedAt(formatTime(log.getCreatedAt()));
        return vo;
    }

    private void markDocumentApproved(ProcessDocumentInstance instance) {
        LocalDateTime now = LocalDateTime.now();
        instance.setStatus(DOCUMENT_STATUS_APPROVED);
        instance.setCurrentNodeKey(null);
        instance.setCurrentNodeName(null);
        instance.setCurrentTaskType(null);
        instance.setFinishedAt(now);
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);
    }

    private void markDocumentException(ProcessDocumentInstance instance, ProcessFlowNodeDTO node, String reason) {
        LocalDateTime now = LocalDateTime.now();
        instance.setStatus(DOCUMENT_STATUS_EXCEPTION);
        instance.setCurrentNodeKey(node == null ? null : node.getNodeKey());
        instance.setCurrentNodeName(node == null ? null : node.getNodeName());
        instance.setCurrentTaskType("EXCEPTION");
        instance.setFinishedAt(now);
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);
        appendLog(instance.getDocumentCode(), node == null ? null : node.getNodeKey(), node == null ? null : node.getNodeName(), LOG_EXCEPTION, null, "SYSTEM", reason, Collections.emptyMap());
    }

    private void clearCurrentNode(ProcessDocumentInstance instance) {
        instance.setCurrentNodeKey(null);
        instance.setCurrentNodeName(null);
        instance.setCurrentTaskType(null);
        instance.setUpdatedAt(LocalDateTime.now());
        processDocumentInstanceMapper.updateById(instance);
    }

    private void appendLog(
            String documentCode,
            String nodeKey,
            String nodeName,
            String actionType,
            Long actorUserId,
            String actorName,
            String actionComment,
            Map<String, Object> payload
    ) {
        ProcessDocumentActionLog log = new ProcessDocumentActionLog();
        log.setDocumentCode(documentCode);
        log.setNodeKey(nodeKey);
        log.setNodeName(nodeName);
        log.setActionType(actionType);
        log.setActorUserId(actorUserId);
        log.setActorName(actorName);
        log.setActionComment(trimToNull(actionComment));
        log.setPayloadJson(payload == null || payload.isEmpty() ? null : writeJson(payload));
        log.setCreatedAt(LocalDateTime.now());
        processDocumentActionLogMapper.insert(log);
    }

    private ProcessDocumentTemplate requireTemplate(String templateCode) {
        String normalizedCode = trimToNull(templateCode);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("Template code is required");
        }
        ProcessDocumentTemplate template = templateMapper.selectOne(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getTemplateCode, normalizedCode)
                        .eq(ProcessDocumentTemplate::getEnabled, 1)
                        .last("limit 1")
        );
        if (template == null) {
            throw new IllegalStateException("Available template not found");
        }
        return template;
    }

    private ProcessDocumentInstance requireDocument(String documentCode) {
        String normalizedCode = trimToNull(documentCode);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("Document code is required");
        }
        ProcessDocumentInstance instance = processDocumentInstanceMapper.selectOne(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getDocumentCode, normalizedCode)
                        .last("limit 1")
        );
        if (instance == null) {
            throw new IllegalStateException("Document not found");
        }
        return instance;
    }

    private ProcessDocumentTask requirePendingTask(Long taskId, Long userId) {
        ProcessDocumentTask task = processDocumentTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalStateException("Approval task not found");
        }
        if (!Objects.equals(task.getAssigneeUserId(), userId)) {
            throw new IllegalStateException("Current user cannot handle this task");
        }
        if (!TASK_STATUS_PENDING.equals(task.getStatus())) {
            throw new IllegalStateException("Task has already been handled");
        }
        return task;
    }

    private ProcessFormDesign loadFormDesign(String formDesignCode) {
        String normalizedCode = trimToNull(formDesignCode);
        if (normalizedCode == null) {
            return null;
        }
        return processFormDesignMapper.selectOne(
                Wrappers.<ProcessFormDesign>lambdaQuery()
                        .eq(ProcessFormDesign::getFormCode, normalizedCode)
                        .last("limit 1")
        );
    }

    private Map<String, Object> buildRuntimeFlowContext(
            User currentUser,
            ProcessDocumentTemplate template,
            ProcessFormDesign formDesign,
            Map<String, Object> formData
    ) {
        Map<String, Object> context = formData == null ? new LinkedHashMap<>() : new LinkedHashMap<>(formData);
        if (currentUser != null && currentUser.getId() != null) {
            context.put("submitterUserId", currentUser.getId());
        }
        if (currentUser != null && currentUser.getDeptId() != null) {
            context.put("submitterDeptId", currentUser.getDeptId());
        }
        BigDecimal amount = resolveTotalAmount(formData);
        if (amount != null) {
            context.put("amount", amount);
        }
        String documentType = trimToNull(template.getTemplateType());
        if (documentType != null) {
            context.put("documentType", documentType);
        }
        String expenseTypeCode = firstNonBlank(stringValue(formData.get("expenseTypeCode")), trimToNull(template.getCategoryCode()));
        if (expenseTypeCode != null) {
            context.put("expenseTypeCode", expenseTypeCode);
        }
        List<String> undertakeDeptIds = resolveUndertakeDeptIds(formDesign, formData);
        if (!undertakeDeptIds.isEmpty()) {
            context.put("undertakeDeptIds", undertakeDeptIds);
        }
        return context;
    }

    private Map<String, Object> buildRuntimeContextForInstance(ProcessDocumentInstance instance) {
        Map<String, Object> formData = readMap(instance.getFormDataJson());
        Map<String, Object> context = new LinkedHashMap<>(formData);
        if (instance.getSubmitterUserId() != null) {
            context.put("submitterUserId", instance.getSubmitterUserId());
        }
        User submitter = instance.getSubmitterUserId() == null ? null : userMapper.selectById(instance.getSubmitterUserId());
        if (submitter != null && submitter.getDeptId() != null) {
            context.put("submitterDeptId", submitter.getDeptId());
        }
        if (instance.getTotalAmount() != null) {
            context.put("amount", instance.getTotalAmount());
        }
        if (trimToNull(instance.getTemplateType()) != null) {
            context.put("documentType", instance.getTemplateType());
        }
        String expenseTypeCode = firstNonBlank(stringValue(formData.get("expenseTypeCode")), readMap(instance.getTemplateSnapshotJson()).get("categoryCode") == null ? null : String.valueOf(readMap(instance.getTemplateSnapshotJson()).get("categoryCode")));
        if (expenseTypeCode != null) {
            context.put("expenseTypeCode", expenseTypeCode);
        }
        Object schemaJson = readMap(instance.getFormSchemaSnapshotJson());
        Object rawBlocks = schemaJson instanceof Map<?, ?> map ? map.get("blocks") : null;
        if (rawBlocks instanceof List<?> blocks) {
            Set<String> undertakeDeptIds = new LinkedHashSet<>();
            for (Object rawBlock : blocks) {
                if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                    continue;
                }
                if (!Objects.equals(String.valueOf(blockMap.get("kind")), "BUSINESS_COMPONENT")) {
                    continue;
                }
                Object rawProps = blockMap.get("props");
                if (!(rawProps instanceof Map<?, ?> props)) {
                    continue;
                }
                if (!Objects.equals(String.valueOf(props.get("componentCode")), UNDERTAKE_DEPARTMENT_COMPONENT_CODE)) {
                    continue;
                }
                String fieldKey = trimToNull(String.valueOf(blockMap.get("fieldKey")));
                if (fieldKey != null) {
                    collectDeptIds(undertakeDeptIds, formData.get(fieldKey));
                }
            }
            if (!undertakeDeptIds.isEmpty()) {
                context.put("undertakeDeptIds", new ArrayList<>(undertakeDeptIds));
            }
        }
        return context;
    }

    private List<String> resolveUndertakeDeptIds(ProcessFormDesign formDesign, Map<String, Object> formData) {
        if (formDesign == null || trimToNull(formDesign.getSchemaJson()) == null || formData == null || formData.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> deptIds = new LinkedHashSet<>();
        Map<String, Object> schema = readSchema(formDesign.getSchemaJson());
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks)) {
            return Collections.emptyList();
        }
        for (Object rawBlock : blocks) {
            if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(blockMap.get("kind")), "BUSINESS_COMPONENT")) {
                continue;
            }
            Object rawProps = blockMap.get("props");
            if (!(rawProps instanceof Map<?, ?> props)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(props.get("componentCode")), UNDERTAKE_DEPARTMENT_COMPONENT_CODE)) {
                continue;
            }
            String fieldKey = trimToNull(String.valueOf(blockMap.get("fieldKey")));
            if (fieldKey == null) {
                continue;
            }
            collectDeptIds(deptIds, formData.get(fieldKey));
        }
        return new ArrayList<>(deptIds);
    }

    private void collectDeptIds(Set<String> result, Object value) {
        if (value instanceof List<?> items) {
            for (Object item : items) {
                String normalized = trimToNull(String.valueOf(item));
                if (normalized != null) {
                    result.add(normalized);
                }
            }
            return;
        }
        String normalized = trimToNull(value == null ? null : String.valueOf(value));
        if (normalized != null) {
            result.add(normalized);
        }
    }

    private Map<String, Object> readSchema(String schemaJson) {
        if (trimToNull(schemaJson) == null) {
            return defaultSchema();
        }
        try {
            return objectMapper.readValue(schemaJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse form schema", ex);
        }
    }

    private Map<String, Object> defaultSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("layoutMode", "TWO_COLUMN");
        schema.put("blocks", Collections.emptyList());
        return schema;
    }

    private List<ProcessCustomArchiveDetailVO> loadSharedArchives(Map<String, Object> schema) {
        Set<String> archiveCodes = extractArchiveCodes(schema);
        if (archiveCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProcessCustomArchiveDesign> archives = customArchiveDesignMapper.selectList(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .in(ProcessCustomArchiveDesign::getArchiveCode, archiveCodes)
                        .orderByAsc(ProcessCustomArchiveDesign::getId)
        );
        if (archives.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<ProcessCustomArchiveItem>> itemMap = customArchiveItemMapper.selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .in(ProcessCustomArchiveItem::getArchiveId, archives.stream().map(ProcessCustomArchiveDesign::getId).toList())
                        .orderByAsc(ProcessCustomArchiveItem::getPriority, ProcessCustomArchiveItem::getId)
        ).stream().collect(Collectors.groupingBy(
                ProcessCustomArchiveItem::getArchiveId,
                LinkedHashMap::new,
                Collectors.toList()
        ));

        List<Long> itemIds = itemMap.values().stream().flatMap(List::stream).map(ProcessCustomArchiveItem::getId).toList();
        Map<Long, List<ProcessCustomArchiveRule>> ruleMap = itemIds.isEmpty()
                ? Collections.emptyMap()
                : customArchiveRuleMapper.selectList(
                Wrappers.<ProcessCustomArchiveRule>lambdaQuery()
                        .in(ProcessCustomArchiveRule::getArchiveItemId, itemIds)
                        .orderByAsc(ProcessCustomArchiveRule::getGroupNo, ProcessCustomArchiveRule::getId)
        ).stream().collect(Collectors.groupingBy(
                ProcessCustomArchiveRule::getArchiveItemId,
                LinkedHashMap::new,
                Collectors.toList()
        ));

        return archives.stream().map(archive -> {
            ProcessCustomArchiveDetailVO detail = new ProcessCustomArchiveDetailVO();
            detail.setId(archive.getId());
            detail.setArchiveCode(archive.getArchiveCode());
            detail.setArchiveName(archive.getArchiveName());
            detail.setArchiveType(archive.getArchiveType());
            detail.setArchiveTypeLabel("AUTO_RULE".equals(archive.getArchiveType()) ? "自动划分" : "提供选择");
            detail.setArchiveDescription(archive.getArchiveDescription());
            detail.setStatus(archive.getStatus());
            detail.setItems(itemMap.getOrDefault(archive.getId(), Collections.emptyList()).stream().map(item -> {
                ProcessCustomArchiveItemDTO dto = new ProcessCustomArchiveItemDTO();
                dto.setId(item.getId());
                dto.setItemCode(item.getItemCode());
                dto.setItemName(item.getItemName());
                dto.setPriority(item.getPriority());
                dto.setStatus(item.getStatus());
                dto.setRules(ruleMap.getOrDefault(item.getId(), Collections.emptyList()).stream().map(rule -> {
                    ProcessCustomArchiveRuleDTO ruleDto = new ProcessCustomArchiveRuleDTO();
                    ruleDto.setId(rule.getId());
                    ruleDto.setGroupNo(rule.getGroupNo());
                    ruleDto.setFieldKey(rule.getFieldKey());
                    ruleDto.setOperator(rule.getOperator());
                    ruleDto.setCompareValue(readJsonValue(rule.getCompareValue()));
                    return ruleDto;
                }).toList());
                return dto;
            }).toList());
            return detail;
        }).toList();
    }

    private Set<String> extractArchiveCodes(Map<String, Object> schema) {
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks)) {
            return Collections.emptySet();
        }
        Set<String> archiveCodes = new LinkedHashSet<>();
        for (Object rawBlock : blocks) {
            if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(blockMap.get("kind")), "SHARED_FIELD")) {
                continue;
            }
            Object rawProps = blockMap.get("props");
            if (!(rawProps instanceof Map<?, ?> props)) {
                continue;
            }
            String archiveCode = trimToNull(String.valueOf(props.get("archiveCode")));
            if (archiveCode != null) {
                archiveCodes.add(archiveCode);
            }
        }
        return archiveCodes;
    }

    private Map<String, Object> toTemplateSnapshot(ProcessDocumentTemplate template) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("templateCode", template.getTemplateCode());
        snapshot.put("templateName", template.getTemplateName());
        snapshot.put("templateType", template.getTemplateType());
        snapshot.put("templateTypeLabel", resolveTemplateTypeLabel(template.getTemplateType(), template.getTemplateTypeLabel()));
        snapshot.put("categoryCode", template.getCategoryCode());
        snapshot.put("templateDescription", template.getTemplateDescription());
        snapshot.put("formDesignCode", template.getFormDesignCode());
        snapshot.put("approvalFlowCode", template.getApprovalFlow());
        snapshot.put("flowName", template.getFlowName());
        return snapshot;
    }

    private String resolveFlowSnapshotJson(ProcessDocumentTemplate template) {
        String flowCode = trimToNull(template.getApprovalFlow());
        if (flowCode == null) {
            return null;
        }
        ProcessFlow flow = processFlowMapper.selectOne(
                Wrappers.<ProcessFlow>lambdaQuery()
                        .eq(ProcessFlow::getFlowCode, flowCode)
                        .last("limit 1")
        );
        if (flow == null) {
            return null;
        }
        Long versionId = flow.getCurrentPublishedVersionId() != null
                ? flow.getCurrentPublishedVersionId()
                : flow.getCurrentDraftVersionId();
        if (versionId == null) {
            return null;
        }
        ProcessFlowVersion version = processFlowVersionMapper.selectById(versionId);
        return version == null ? null : version.getSnapshotJson();
    }

    private FlowRuntimeSnapshot readFlowSnapshot(String snapshotJson) {
        if (trimToNull(snapshotJson) == null) {
            return new FlowRuntimeSnapshot(Collections.emptyList(), Collections.emptyList());
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(snapshotJson, new TypeReference<LinkedHashMap<String, Object>>() {});
            List<ProcessFlowNodeDTO> nodes = objectMapper.convertValue(raw.getOrDefault("nodes", Collections.emptyList()), new TypeReference<List<ProcessFlowNodeDTO>>() {});
            List<ProcessFlowRouteDTO> routes = objectMapper.convertValue(raw.getOrDefault("routes", Collections.emptyList()), new TypeReference<List<ProcessFlowRouteDTO>>() {});
            return new FlowRuntimeSnapshot(nodes, routes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse flow snapshot", ex);
        }
    }

    private List<ProcessFormOptionVO> loadDepartmentOptions() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .eq(SystemDepartment::getStatus, 1)
                        .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId)
        ).stream().map(item -> {
            ProcessFormOptionVO option = new ProcessFormOptionVO();
            option.setLabel(item.getDeptName());
            option.setValue(String.valueOf(item.getId()));
            return option;
        }).toList();
    }

    private List<ProcessDocumentTask> loadPendingTasks(String documentCode) {
        return processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                        .eq(ProcessDocumentTask::getStatus, TASK_STATUS_PENDING)
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
    }

    private List<ProcessDocumentTask> loadNodeBatchTasks(String documentCode, String nodeKey, String batchNo) {
        return processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                        .eq(ProcessDocumentTask::getNodeKey, nodeKey)
                        .eq(ProcessDocumentTask::getTaskBatchNo, batchNo)
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
    }

    private void cancelOtherPendingTasks(List<ProcessDocumentTask> tasks, Long keepTaskId, LocalDateTime handledAt) {
        for (ProcessDocumentTask task : tasks) {
            if (Objects.equals(task.getId(), keepTaskId) || !TASK_STATUS_PENDING.equals(task.getStatus())) {
                continue;
            }
            task.setStatus(TASK_STATUS_CANCELLED);
            task.setHandledAt(handledAt);
            processDocumentTaskMapper.updateById(task);
        }
    }

    private List<ProcessDocumentActionLog> loadActionLogs(String documentCode) {
        return processDocumentActionLogMapper.selectList(
                Wrappers.<ProcessDocumentActionLog>lambdaQuery()
                        .eq(ProcessDocumentActionLog::getDocumentCode, documentCode)
                        .orderByAsc(ProcessDocumentActionLog::getCreatedAt, ProcessDocumentActionLog::getId)
        );
    }

    private String resolveDocumentTitle(ProcessDocumentTemplate template, Map<String, Object> formData, String username) {
        String title = firstNonBlank(
                stringValue(formData.get("__documentTitle")),
                stringValue(formData.get("documentTitle")),
                stringValue(formData.get("title"))
        );
        if (title != null) {
            return title;
        }
        return template.getTemplateName() + "-" + defaultUsername(username) + "-" + LocalDate.now().format(DATE_FORMATTER);
    }

    private String resolveDocumentReason(ProcessDocumentTemplate template, Map<String, Object> formData) {
        String reason = firstNonBlank(
                stringValue(formData.get("__documentReason")),
                stringValue(formData.get("documentReason")),
                stringValue(formData.get("reason")),
                stringValue(formData.get("summary")),
                stringValue(formData.get("bankPushSummary"))
        );
        return reason == null ? defaultReason(template.getTemplateName()) : reason;
    }

    private BigDecimal resolveTotalAmount(Map<String, Object> formData) {
        BigDecimal directAmount = toBigDecimal(formData.get("__totalAmount"));
        if (directAmount != null) {
            return directAmount;
        }
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase();
            if (key.contains("amount") || key.contains("money") || key.contains("金额")) {
                BigDecimal amount = toBigDecimal(entry.getValue());
                if (amount != null) {
                    return amount;
                }
            }
        }
        return null;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize data", ex);
        }
    }

    private Object readJsonValue(String rawValue) {
        if (trimToNull(rawValue) == null) {
            return null;
        }
        try {
            return objectMapper.readValue(rawValue, Object.class);
        } catch (Exception ex) {
            return rawValue;
        }
    }

    private Map<String, Object> readMap(String json) {
        if (trimToNull(json) == null) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse json map", ex);
        }
    }

    private Map<String, Object> readFormData(String json) {
        return readMap(json);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            String normalized = trimToNull(String.valueOf(value));
            return normalized == null ? null : new BigDecimal(normalized);
        } catch (Exception ex) {
            return null;
        }
    }

    private int compareNumbers(Object actual, Object compare) {
        BigDecimal left = toBigDecimal(actual);
        BigDecimal right = toBigDecimal(compare);
        if (left == null || right == null) {
            return 0;
        }
        return left.compareTo(right);
    }

    private boolean between(Object actual, Object compare) {
        BigDecimal current = toBigDecimal(actual);
        if (current == null) {
            return false;
        }
        List<Object> range = toObjectList(compare);
        if (range.size() < 2) {
            return false;
        }
        BigDecimal start = toBigDecimal(range.get(0));
        BigDecimal end = toBigDecimal(range.get(1));
        if (start == null || end == null) {
            return false;
        }
        return current.compareTo(start) >= 0 && current.compareTo(end) <= 0;
    }

    private boolean containsValue(Object actual, Object compare) {
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> valuesEqual(item, compare));
        }
        String actualText = trimToNull(String.valueOf(actual));
        String compareText = trimToNull(String.valueOf(compare));
        return actualText != null && compareText != null && actualText.contains(compareText);
    }

    private boolean anyIn(Object actual, Object compare, boolean defaultResult) {
        List<Object> compareList = toObjectList(compare);
        if (compareList.isEmpty()) {
            return defaultResult;
        }
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> compareList.stream().anyMatch(candidate -> valuesEqual(item, candidate)));
        }
        return compareList.stream().anyMatch(candidate -> valuesEqual(actual, candidate));
    }

    private boolean valuesEqual(Object actual, Object compare) {
        BigDecimal leftNumber = toBigDecimal(actual);
        BigDecimal rightNumber = toBigDecimal(compare);
        if (leftNumber != null && rightNumber != null) {
            return leftNumber.compareTo(rightNumber) == 0;
        }
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> valuesEqual(item, compare));
        }
        return Objects.equals(defaultText(asText(actual), ""), defaultText(asText(compare), ""));
    }

    private List<Object> toObjectList(Object value) {
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (value == null) {
            return new ArrayList<>();
        }
        return List.of(value);
    }

    private String buildDocumentCode() {
        String prefix = "DOC" + LocalDate.now().format(CODE_DATE_FORMATTER);
        Long count = processDocumentInstanceMapper.selectCount(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .likeRight(ProcessDocumentInstance::getDocumentCode, prefix)
        );
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }

    private String buildTaskBatchNo(String documentCode, String nodeKey) {
        return documentCode + "-" + nodeKey + "-" + System.currentTimeMillis();
    }

    private String resolveTemplateTypeLabel(String templateType, String currentLabel) {
        if (trimToNull(currentLabel) != null) {
            return currentLabel;
        }
        return switch (trimToNull(templateType) == null ? "report" : templateType.trim()) {
            case "application" -> "申请单";
            case "loan" -> "借款单";
            default -> "报销单";
        };
    }

    private String resolveStatusLabel(String status) {
        return switch (trimToNull(status) == null ? "" : status.trim()) {
            case DOCUMENT_STATUS_APPROVED -> "已通过";
            case DOCUMENT_STATUS_REJECTED -> "已驳回";
            case "DRAFT" -> "草稿";
            case DOCUMENT_STATUS_EXCEPTION -> "流程异常";
            default -> "审批中";
        };
    }

    private String buildAccountLabel(String accountName, String bankName) {
        String left = firstNonBlank(accountName, bankName);
        String right = left != null && Objects.equals(left, trimToNull(bankName)) ? null : trimToNull(bankName);
        return right == null ? (left == null ? "未命名账户" : left) : left + " / " + right;
    }

    private String buildVendorAccountSecondary(FinanceVendor vendor) {
        List<String> parts = new ArrayList<>();
        if (trimToNull(vendor.getCVenBankNub()) != null) {
            parts.add(vendor.getCVenBankNub().trim());
        }
        if (trimToNull(vendor.getCVenAccount()) != null) {
            parts.add(maskAccountNo(vendor.getCVenAccount()));
        }
        return String.join(" / ", parts);
    }

    private String maskAccountNo(String accountNo) {
        String normalized = trimToNull(accountNo);
        if (normalized == null) {
            return "";
        }
        if (normalized.length() <= 8) {
            return normalized;
        }
        return normalized.substring(0, 4) + " **** " + normalized.substring(normalized.length() - 4);
    }

    private boolean matchesKeyword(String keyword, String... values) {
        if (keyword == null) {
            return true;
        }
        for (String value : values) {
            if (value != null && value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return firstNonBlank(
                    stringValue(map.get("value")),
                    stringValue(map.get("label")),
                    stringValue(map.get("text"))
            );
        }
        return trimToNull(String.valueOf(value));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String defaultReason(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "审批单提交" : normalized;
    }

    private String defaultUsername(String username) {
        String normalized = trimToNull(username);
        return normalized == null ? "当前用户" : normalized;
    }

    private String resolveMissingHandler(Map<String, Object> config) {
        return defaultText(asText(config == null ? null : config.get("missingHandler")), MISSING_HANDLER_AUTO_SKIP);
    }

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

    private User loadActiveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user != null && Objects.equals(user.getStatus(), 1) ? user : null;
    }

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

    private SystemDepartment climbDepartment(SystemDepartment start, Map<Long, SystemDepartment> departmentMap, int steps) {
        SystemDepartment current = start;
        for (int index = 0; index < steps && current != null; index++) {
            current = current.getParentId() == null ? null : departmentMap.get(current.getParentId());
        }
        return current;
    }

    private LeaderResolution resolveLeader(SystemDepartment startDept, Map<Long, SystemDepartment> departmentMap, boolean allowLookup, int lookupLevel) {
        SystemDepartment current = startDept;
        int remaining = lookupLevel;
        while (current != null) {
            if (current.getLeaderUserId() != null && current.getLeaderUserId() > 0) {
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

    private int nextIndex(FlowRuntimeSnapshot snapshot, ProcessFlowNodeDTO node) {
        return snapshot.indexInContainer(node.getParentNodeKey(), node.getNodeKey()) + 1;
    }

    private String normalizeUserName(User user) {
        String name = trimToNull(user.getName());
        return name != null ? name : defaultText(asText(user.getUsername()), "未命名用户");
    }

    private Map<String, Object> toObjectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return new LinkedHashMap<>();
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
            return defaultValue;
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
            return null;
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

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private int clampLevel(Integer value) {
        int level = value == null ? 1 : value;
        if (level < 1) {
            return 1;
        }
        return Math.min(level, 10);
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private enum FlowAdvanceState {
        PAUSED,
        COMPLETED
    }

    private record LeaderResolution(Long departmentId, Long userId) {
    }

    private static final class FlowRuntimeSnapshot {
        private final List<ProcessFlowNodeDTO> nodes;
        private final Map<String, ProcessFlowNodeDTO> nodeByKey;
        private final Map<String, List<ProcessFlowNodeDTO>> childrenByContainer;
        private final Map<String, List<ProcessFlowRouteDTO>> routesBySourceNode;
        private final Map<String, ProcessFlowRouteDTO> routeByKey;

        private FlowRuntimeSnapshot(List<ProcessFlowNodeDTO> rawNodes, List<ProcessFlowRouteDTO> rawRoutes) {
            this.nodes = rawNodes == null ? Collections.emptyList() : rawNodes.stream()
                    .sorted(Comparator.comparing(item -> item.getDisplayOrder() == null ? Integer.MAX_VALUE : item.getDisplayOrder()))
                    .toList();
            this.nodeByKey = this.nodes.stream().collect(Collectors.toMap(
                    ProcessFlowNodeDTO::getNodeKey,
                    item -> item,
                    (left, right) -> left,
                    LinkedHashMap::new
            ));
            this.childrenByContainer = this.nodes.stream().collect(Collectors.groupingBy(
                    item -> item.getParentNodeKey() == null ? "__ROOT__" : item.getParentNodeKey(),
                    LinkedHashMap::new,
                    Collectors.collectingAndThen(Collectors.toList(), items -> items.stream()
                            .sorted(Comparator.comparing(node -> node.getDisplayOrder() == null ? Integer.MAX_VALUE : node.getDisplayOrder()))
                            .toList())
            ));
            List<ProcessFlowRouteDTO> routes = rawRoutes == null ? Collections.emptyList() : rawRoutes;
            this.routesBySourceNode = routes.stream().collect(Collectors.groupingBy(
                    ProcessFlowRouteDTO::getSourceNodeKey,
                    LinkedHashMap::new,
                    Collectors.collectingAndThen(Collectors.toList(), items -> items.stream()
                            .sorted(Comparator.comparing(route -> route.getPriority() == null ? Integer.MAX_VALUE : route.getPriority()))
                            .toList())
            ));
            this.routeByKey = routes.stream().collect(Collectors.toMap(
                    ProcessFlowRouteDTO::getRouteKey,
                    item -> item,
                    (left, right) -> left,
                    LinkedHashMap::new
            ));
        }

        private List<ProcessFlowNodeDTO> nodes() {
            return nodes;
        }

        private ProcessFlowNodeDTO node(String nodeKey) {
            return nodeByKey.get(nodeKey);
        }

        private List<ProcessFlowNodeDTO> children(String containerKey) {
            return childrenByContainer.getOrDefault(containerKey == null ? "__ROOT__" : containerKey, Collections.emptyList());
        }

        private List<ProcessFlowRouteDTO> routes(String sourceNodeKey) {
            return routesBySourceNode.getOrDefault(sourceNodeKey, Collections.emptyList());
        }

        private ProcessFlowRouteDTO routeByKey(String routeKey) {
            if (routeKey == null) {
                return null;
            }
            return routeByKey.get(routeKey);
        }

        private int indexInContainer(String containerKey, String nodeKey) {
            List<ProcessFlowNodeDTO> children = children(containerKey);
            for (int index = 0; index < children.size(); index++) {
                if (Objects.equals(children.get(index).getNodeKey(), nodeKey)) {
                    return index;
                }
            }
            return children.size();
        }
    }
}
