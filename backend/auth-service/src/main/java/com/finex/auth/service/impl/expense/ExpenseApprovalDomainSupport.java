// 业务域：报销单录入、流转与查询
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自报销单页面、审批页面、付款页面对应的控制器，下游会继续协调报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseActionUserOptionVO;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.dto.ExpenseTaskAddSignDTO;
import com.finex.auth.dto.ExpenseTaskTransferDTO;
import com.finex.auth.dto.EmployeeDepartmentRefVO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.support.EmployeeDirectorySupport;
import com.finex.auth.support.UserDepartmentSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

/**
 * ExpenseApprovalDomainSupport：领域规则支撑类。
 * 承接报销单审批的核心业务规则。
 * 改这里时，要特别关注单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
@RequiredArgsConstructor
public class ExpenseApprovalDomainSupport {

    private static final String NODE_TYPE_APPROVAL = "APPROVAL";
    private static final String TASK_STATUS_PENDING = "PENDING";
    private static final String TASK_KIND_NORMAL = "NORMAL";
    private static final String TASK_KIND_ADD_SIGN = "ADD_SIGN";
    private static final String LOG_MODIFY = "MODIFY";
    private static final String LOG_TRANSFER = "TRANSFER";
    private static final String PAYEE_ACCOUNT_COMPONENT_CODE = "payee-account";
    private static final String APPROVAL_EDIT_STAGE = "IN_APPROVAL";
    private static final String APPROVAL_EDITABLE = "EDITABLE";

    private static final String MESSAGE_TASK_MODIFY_NOT_ALLOWED =
            "\u5f53\u524d\u5ba1\u6279\u8282\u70b9\u672a\u5f00\u542f\u5355\u636e\u4fee\u6539\u6743\u9650";
    private static final String MESSAGE_TASK_MODIFY_PAY_ACCOUNT_ONLY =
            "\u5f53\u524d\u5ba1\u6279\u8282\u70b9\u4ec5\u5141\u8bb8\u4fee\u6539\u6536\u6b3e\u8d26\u6237";
    private static final String MESSAGE_TASK_MODIFY_FIELDS_NOT_ALLOWED =
            "\u5f53\u524d\u5ba1\u6279\u8282\u70b9\u4e0d\u5141\u8bb8\u4fee\u6539\u6240\u9009\u5b57\u6bb5\uff0c\u8bf7\u8c03\u6574\u540e\u91cd\u8bd5";
    private static final String MESSAGE_TASK_MODIFY_IMPACTS_WORKFLOW =
            "\u5f53\u524d\u5b57\u6bb5\u4f1a\u5f71\u54cd\u5ba1\u6279\u6d41\uff0c\u8bf7\u9a73\u56de\u540e\u4fee\u6539\u6216\u8054\u7cfb\u6d41\u7a0b\u7ba1\u7406\u5458";
    private static final Set<String> FLOW_RUNTIME_KEYS = Set.of(
            "amount",
            "expenseTypeCode",
            "documentType",
            "undertakeDeptIds"
    );

    private final ExpenseDocumentReadSupport expenseDocumentReadSupport;
    private final ExpenseDocumentActionLogSupport expenseDocumentActionLogSupport;
    private final ExpenseDocumentMutationDomainSupport expenseDocumentMutationDomainSupport;
    private final ExpenseDocumentTemplateSupport expenseDocumentTemplateSupport;
    private final ExpenseSummaryAssembler expenseSummaryAssembler;
    private final ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;
    private final ExpenseRelationWriteOffService expenseRelationWriteOffService;
    private final ProcessDocumentTaskMapper processDocumentTaskMapper;
    private final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    private final UserMapper userMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final ObjectMapper objectMapper;

    /**
     * 查询待处理审批列表。
     */
    public List<ExpenseApprovalPendingItemVO> listPendingApprovals(Long userId) {
        List<ProcessDocumentTask> tasks = processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getAssigneeUserId, userId)
                        .eq(ProcessDocumentTask::getNodeType, NODE_TYPE_APPROVAL)
                        .eq(ProcessDocumentTask::getStatus, TASK_STATUS_PENDING)
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
        if (tasks.isEmpty()) {
            return List.of();
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
        return expenseSummaryAssembler.toPendingItems(tasks, instanceMap);
    }

            /**
     * 审批通过任务。
     */
public ExpenseDocumentDetailVO approveTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        ProcessDocumentTask task = requirePendingTask(taskId, userId);
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
        String comment = dto == null ? null : dto.getComment();
        if (Objects.equals(trimToNull(task.getTaskKind()), TASK_KIND_ADD_SIGN)) {
            expenseWorkflowRuntimeSupport.approveAddSignTask(instance, task, userId, username, comment);
            return expenseDocumentReadSupport.buildDocumentDetail(
                    expenseDocumentReadSupport.requireDocument(instance.getDocumentCode())
            );
        }
        expenseWorkflowRuntimeSupport.approvePendingTask(instance, task, userId, username, comment);
        ProcessDocumentInstance latest = expenseDocumentReadSupport.requireDocument(instance.getDocumentCode());
        if (isEffectiveApprovedStatus(latest.getStatus())) {
            expenseRelationWriteOffService.finalizeEffectiveWriteOffs(instance.getDocumentCode());
            latest = expenseDocumentReadSupport.requireDocument(instance.getDocumentCode());
        }
        return expenseDocumentReadSupport.buildDocumentDetail(latest);
    }

    public ExpenseDocumentDetailVO rejectTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        ProcessDocumentTask task = requirePendingTask(taskId, userId);
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
        expenseWorkflowRuntimeSupport.rejectPendingTask(
                instance,
                task,
                userId,
                username,
                dto == null ? null : dto.getComment(),
                dto == null ? null : dto.getTargetNodeKey()
        );
        expenseRelationWriteOffService.voidPendingWriteOffs(instance.getDocumentCode());
        return expenseDocumentReadSupport.buildDocumentDetail(
                expenseDocumentReadSupport.requireDocument(instance.getDocumentCode())
        );
    }

            /**
     * 获取任务修改上下文。
     */
public ExpenseDocumentEditContextVO getTaskModifyContext(Long userId, Long taskId) {
        ProcessDocumentTask task = requirePendingTask(taskId, userId);
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
        ExpenseDocumentEditContextVO context = expenseDocumentTemplateSupport.buildEditContext(userId, instance, task.getId(), "MODIFY");
        ensureTaskModifyAllowed(context);
        return context;
    }

            /**
     * 处理审批中的单据修改。
     */
public ExpenseDocumentDetailVO modifyTaskDocument(Long userId, String username, Long taskId, ExpenseDocumentUpdateDTO dto) {
        ProcessDocumentTask task = requirePendingTask(taskId, userId);
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
        ExpenseDocumentEditContextVO context = expenseDocumentTemplateSupport.buildEditContext(userId, instance, task.getId(), "MODIFY");
        ensureTaskModifyAllowed(context);
        AbstractExpenseDocumentSupport.DocumentMutationContext mutation = expenseDocumentMutationDomainSupport.buildMutationContext(instance, dto, false);
        validateTaskModifyMutation(instance, context, mutation);
        expenseDocumentMutationDomainSupport.applyDocumentMutation(instance, mutation, false);
        expenseRelationWriteOffService.syncDocumentBusinessRelations(
                instance.getDocumentCode(),
                mutation.formDesign(),
                mutation.formData()
        );
        expenseDocumentActionLogSupport.appendLog(
                instance.getDocumentCode(),
                task.getNodeKey(),
                task.getNodeName(),
                LOG_MODIFY,
                userId,
                defaultUsername(username),
                null,
                Map.of(
                        "taskId", task.getId(),
                        "taskKind", defaultText(task.getTaskKind(), TASK_KIND_NORMAL)
                )
        );
        return expenseDocumentReadSupport.buildDocumentDetail(
                expenseDocumentReadSupport.requireDocument(instance.getDocumentCode())
        );
    }

            /**
     * 处理审批中的转办。
     */
public ExpenseDocumentDetailVO transferTask(Long userId, String username, Long taskId, ExpenseTaskTransferDTO dto) {
        ProcessDocumentTask task = requirePendingTask(taskId, userId);
        User targetUser = requireActiveUser(dto == null ? null : dto.getTargetUserId());
        if (Objects.equals(targetUser.getId(), userId)) {
            throw new IllegalArgumentException("\u8f6c\u529e\u76ee\u6807\u7528\u6237\u4e0d\u80fd\u662f\u5f53\u524d\u5904\u7406\u4eba");
        }
        String remark = trimToNull(dto == null ? null : dto.getRemark());
        task.setAssigneeUserId(targetUser.getId());
        task.setAssigneeName(normalizeUserName(targetUser));
        processDocumentTaskMapper.updateById(task);
        expenseDocumentActionLogSupport.appendLog(
                task.getDocumentCode(),
                task.getNodeKey(),
                task.getNodeName(),
                LOG_TRANSFER,
                userId,
                defaultUsername(username),
                remark,
                Map.of(
                        "taskId", task.getId(),
                        "targetUserId", targetUser.getId(),
                        "targetUserName", normalizeUserName(targetUser)
                )
        );
        return expenseDocumentReadSupport.buildDocumentDetail(
                expenseDocumentReadSupport.requireDocument(task.getDocumentCode())
        );
    }

    public ExpenseDocumentDetailVO addSignTask(Long userId, String username, Long taskId, ExpenseTaskAddSignDTO dto) {
        ProcessDocumentTask task = requirePendingTask(taskId, userId);
        User targetUser = requireActiveUser(dto == null ? null : dto.getTargetUserId());
        if (Objects.equals(targetUser.getId(), userId)) {
            throw new IllegalArgumentException("\u52a0\u7b7e\u76ee\u6807\u7528\u6237\u4e0d\u80fd\u662f\u5f53\u524d\u5904\u7406\u4eba");
        }
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
        String remark = trimToNull(dto == null ? null : dto.getRemark());
        expenseWorkflowRuntimeSupport.createAddSignTask(instance, task, targetUser, userId, username, remark);
        return expenseDocumentReadSupport.buildDocumentDetail(
                expenseDocumentReadSupport.requireDocument(task.getDocumentCode())
        );
    }

    public List<ExpenseActionUserOptionVO> searchActionUsers(Long userId, String keyword) {
        String normalizedKeyword = trimToNull(keyword);
        Map<Long, String> departmentNameMap = systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getStatus, 1)
        ).stream().collect(Collectors.toMap(
                SystemDepartment::getId,
                SystemDepartment::getDeptName,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        List<User> users = userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getName, User::getId)
        );
        Map<Long, List<com.finex.auth.dto.EmployeeDepartmentRefVO>> departmentsByUserId =
                UserDepartmentSupport.loadDepartmentRefsByUserId(
                        userMapper,
                        systemDepartmentMapper,
                        users.stream().map(User::getId).toList()
                );
        return users.stream()
                .filter(item -> matchesKeyword(normalizedKeyword, item.getName(), item.getUsername(), item.getPhone(), item.getEmail()))
                .map(item -> {
                    List<EmployeeDepartmentRefVO> departments = new ArrayList<>(
                            departmentsByUserId.getOrDefault(item.getId(), Collections.emptyList())
                    );
                    ExpenseActionUserOptionVO option = new ExpenseActionUserOptionVO();
                    option.setUserId(item.getId());
                    option.setName(EmployeeDirectorySupport.buildEmployeeDirectoryOption(item, departments, departmentNameMap).getName());
                    option.setUsername(item.getUsername());
                    option.setPhone(item.getPhone());
                    option.setEmail(item.getEmail());
                    option.setStatus(item.getStatus());
                    option.setDeptId(item.getDeptId() != null ? item.getDeptId() : UserDepartmentSupport.resolvePrimaryDepartmentId(departments));
                    option.setDepartments(departments);
                    String deptName = UserDepartmentSupport.joinDepartmentNames(departments);
                    option.setDeptName(
                            trimToNull(deptName) == null && item.getDeptId() != null
                                    ? departmentNameMap.get(item.getDeptId())
                                    : deptName
                    );
                    return option;
                })
                .toList();
    }

    private ProcessDocumentTask requirePendingTask(Long taskId, Long userId) {
        ProcessDocumentTask task = processDocumentTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalStateException("Approval task not found");
        }
        if (!Objects.equals(task.getAssigneeUserId(), userId)) {
            throw new IllegalStateException("Current user cannot handle this task");
        }
        if (!NODE_TYPE_APPROVAL.equals(trimToNull(task.getNodeType()))) {
            throw new IllegalStateException("Current task is not an approval task");
        }
        if (!TASK_STATUS_PENDING.equals(task.getStatus())) {
            throw new IllegalStateException("Task has already been handled");
        }
        return task;
    }

    private User requireActiveUser(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getStatus(), 1)) {
            throw new IllegalStateException("\u76ee\u6807\u5904\u7406\u4eba\u4e0d\u5b58\u5728\u6216\u5df2\u505c\u7528");
        }
        return user;
    }

            /**
     * 判断是否属于已生效的审批通过状态。
     */
private boolean isEffectiveApprovedStatus(String status) {
        String normalized = trimToNull(status);
        return Objects.equals(normalized, "APPROVED")
                || Objects.equals(normalized, "COMPLETED")
                || Objects.equals(normalized, "PENDING_PAYMENT")
                || Objects.equals(normalized, "PAYMENT_COMPLETED")
                || Objects.equals(normalized, "PAYMENT_FINISHED");
    }

    private void ensureTaskModifyAllowed(ExpenseDocumentEditContextVO context) {
        boolean allowEditFormModule = Boolean.TRUE.equals(context.getAllowEditFormModule());
        boolean allowEditPayAccount = Boolean.TRUE.equals(context.getAllowEditPayAccount());
        if (!allowEditFormModule && !allowEditPayAccount) {
            throw new IllegalStateException(MESSAGE_TASK_MODIFY_NOT_ALLOWED);
        }
    }

    private void validateTaskModifyMutation(
            ProcessDocumentInstance instance,
            ExpenseDocumentEditContextVO context,
            AbstractExpenseDocumentSupport.DocumentMutationContext mutation
    ) {
        Set<String> changedFormFieldKeys = collectChangedFormFieldKeys(context.getFormData(), mutation.formData());
        boolean expenseDetailsChanged = hasExpenseDetailChanges(context.getExpenseDetails(), mutation.expenseDetails());

        boolean allowEditFormModule = Boolean.TRUE.equals(context.getAllowEditFormModule());
        boolean allowEditPayAccount = Boolean.TRUE.equals(context.getAllowEditPayAccount());

        Set<String> editableFormFieldKeys = allowEditFormModule
                ? collectApprovalEditableFieldKeys(context.getSchema(), false)
                : Collections.emptySet();
        Set<String> payeeAccountFieldKeys = allowEditPayAccount
                ? collectApprovalEditableFieldKeys(context.getSchema(), true)
                : Collections.emptySet();

        Set<String> allowedFieldKeys = new LinkedHashSet<>(editableFormFieldKeys);
        allowedFieldKeys.addAll(payeeAccountFieldKeys);

        if (expenseDetailsChanged) {
            throw new IllegalStateException(allowEditFormModule
                    ? MESSAGE_TASK_MODIFY_IMPACTS_WORKFLOW
                    : MESSAGE_TASK_MODIFY_PAY_ACCOUNT_ONLY);
        }

        Set<String> unauthorizedFieldKeys = new LinkedHashSet<>(changedFormFieldKeys);
        unauthorizedFieldKeys.removeAll(allowedFieldKeys);
        if (!unauthorizedFieldKeys.isEmpty()) {
            throw new IllegalStateException(allowEditFormModule
                    ? MESSAGE_TASK_MODIFY_FIELDS_NOT_ALLOWED
                    : MESSAGE_TASK_MODIFY_PAY_ACCOUNT_ONLY);
        }

        if (!changedFormFieldKeys.isEmpty() && isWorkflowDrivingChange(instance, mutation)) {
            throw new IllegalStateException(MESSAGE_TASK_MODIFY_IMPACTS_WORKFLOW);
        }
    }

    private Set<String> collectChangedFormFieldKeys(Map<String, Object> currentFormData, Map<String, Object> nextFormData) {
        Map<String, Object> current = currentFormData == null ? Collections.emptyMap() : currentFormData;
        Map<String, Object> next = nextFormData == null ? Collections.emptyMap() : nextFormData;
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.addAll(current.keySet());
        keys.addAll(next.keySet());
        LinkedHashSet<String> changed = new LinkedHashSet<>();
        for (String key : keys) {
            if (!valuesEqual(current.get(key), next.get(key))) {
                changed.add(key);
            }
        }
        return changed;
    }

    private boolean hasExpenseDetailChanges(List<ExpenseDetailInstanceDTO> currentDetails, List<ExpenseDetailInstanceDTO> nextDetails) {
        Object current = normalizeComparableValue(normalizeExpenseDetailsForComparison(currentDetails));
        Object next = normalizeComparableValue(normalizeExpenseDetailsForComparison(nextDetails));
        return !Objects.equals(current, next);
    }

    private List<Map<String, Object>> normalizeExpenseDetailsForComparison(List<ExpenseDetailInstanceDTO> details) {
        if (details == null || details.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (ExpenseDetailInstanceDTO detail : details) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("detailNo", trimToNull(detail == null ? null : detail.getDetailNo()));
            item.put("detailDesignCode", trimToNull(detail == null ? null : detail.getDetailDesignCode()));
            item.put("detailType", trimToNull(detail == null ? null : detail.getDetailType()));
            item.put("enterpriseMode", trimToNull(detail == null ? null : detail.getEnterpriseMode()));
            item.put("expenseTypeCode", trimToNull(detail == null ? null : detail.getExpenseTypeCode()));
            item.put("businessSceneMode", trimToNull(detail == null ? null : detail.getBusinessSceneMode()));
            item.put("detailTitle", trimToNull(detail == null ? null : detail.getDetailTitle()));
            item.put("sortOrder", detail == null ? null : detail.getSortOrder());
            item.put("formData", detail == null ? Collections.emptyMap() : detail.getFormData());
            normalized.add(item);
        }
        return normalized;
    }

    private Set<String> collectApprovalEditableFieldKeys(Map<String, Object> schema, boolean payeeAccountOnly) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Object rawBlock : toObjectList(schema == null ? null : schema.get("blocks"))) {
            Map<String, Object> block = toObjectMap(rawBlock);
            String fieldKey = trimToNull(asText(block.get("fieldKey")));
            if (fieldKey == null) {
                continue;
            }
            boolean payeeAccountField = isPayeeAccountBlock(block);
            if (payeeAccountOnly) {
                if (payeeAccountField) {
                    result.add(fieldKey);
                }
                continue;
            }
            if (payeeAccountField) {
                continue;
            }
            if (Objects.equals(resolveBlockFixedStagePermission(block, APPROVAL_EDIT_STAGE), APPROVAL_EDITABLE)) {
                result.add(fieldKey);
            }
        }
        return result;
    }

    private boolean isWorkflowDrivingChange(
            ProcessDocumentInstance instance,
            AbstractExpenseDocumentSupport.DocumentMutationContext mutation
    ) {
        Map<String, Object> currentFlowSnapshot = readFlowSnapshot(instance.getFlowSnapshotJson());
        Set<String> conditionFieldKeys = collectFlowConditionFieldKeys(currentFlowSnapshot);

        Map<String, Object> currentRuntimeContext = expenseWorkflowRuntimeSupport.buildRuntimeContextForInstance(instance);
        User submitter = instance.getSubmitterUserId() == null ? null : userMapper.selectById(instance.getSubmitterUserId());
        Map<String, Object> nextRuntimeContext = expenseWorkflowRuntimeSupport.buildRuntimeFlowContext(
                submitter,
                mutation.template(),
                mutation.formDesign(),
                mutation.formData(),
                mutation.expenseDetailDesign(),
                mutation.expenseDetails()
        );

        for (String key : conditionFieldKeys) {
            if (!valuesEqual(currentRuntimeContext.get(key), nextRuntimeContext.get(key))) {
                return true;
            }
        }
        for (String key : FLOW_RUNTIME_KEYS) {
            if (!valuesEqual(currentRuntimeContext.get(key), nextRuntimeContext.get(key))) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> readFlowSnapshot(String flowSnapshotJson) {
        if (trimToNull(flowSnapshotJson) == null) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(flowSnapshotJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse current flow snapshot", ex);
        }
    }

    private Set<String> collectFlowConditionFieldKeys(Map<String, Object> flowSnapshot) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Object rawRoute : toObjectList(flowSnapshot == null ? null : flowSnapshot.get("routes"))) {
            Map<String, Object> route = toObjectMap(rawRoute);
            for (Object rawGroup : toObjectList(route.get("conditionGroups"))) {
                Map<String, Object> group = toObjectMap(rawGroup);
                for (Object rawCondition : toObjectList(group.get("conditions"))) {
                    String fieldKey = trimToNull(asText(toObjectMap(rawCondition).get("fieldKey")));
                    if (fieldKey != null) {
                        result.add(fieldKey);
                    }
                }
            }
        }
        return result;
    }

    private String resolveBlockFixedStagePermission(Map<String, Object> block, String stageKey) {
        Map<String, Object> permission = toObjectMap(block.get("permission"));
        Map<String, Object> fixedStages = toObjectMap(permission.get("fixedStages"));
        return trimToNull(asText(fixedStages.get(stageKey)));
    }

    private boolean isPayeeAccountBlock(Map<String, Object> block) {
        return Objects.equals(trimToNull(asText(block.get("kind"))), "BUSINESS_COMPONENT")
                && Objects.equals(trimToNull(asText(toObjectMap(block.get("props")).get("componentCode"))), PAYEE_ACCOUNT_COMPONENT_CODE);
    }

    private boolean valuesEqual(Object left, Object right) {
        return Objects.equals(normalizeComparableValue(left), normalizeComparableValue(right));
    }

    private Object normalizeComparableValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return normalizeNumber(number);
        }
        if (value instanceof CharSequence sequence) {
            return trimToNull(sequence.toString());
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalizedMap = toObjectMap(map);
            String lookupIdentifier = resolveLookupIdentifier(normalizedMap);
            if (lookupIdentifier != null) {
                return lookupIdentifier;
            }
            LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
            normalizedMap.forEach((key, item) -> {
                Object comparable = normalizeComparableValue(item);
                if (comparable != null) {
                    normalized.put(key, comparable);
                }
            });
            return normalized.isEmpty() ? null : normalized;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> normalized = new ArrayList<>();
            for (Object item : collection) {
                Object comparable = normalizeComparableValue(item);
                if (comparable != null) {
                    normalized.add(comparable);
                }
            }
            return normalized.isEmpty() ? null : normalized;
        }
        return trimToNull(String.valueOf(value));
    }

    private Object normalizeNumber(Number number) {
        try {
            return new BigDecimal(String.valueOf(number)).stripTrailingZeros();
        } catch (Exception ex) {
            return number.doubleValue();
        }
    }

    private String resolveLookupIdentifier(Map<String, Object> value) {
        return firstNonBlank(
                asText(value.get("value")),
                asText(value.get("code")),
                asText(value.get("id")),
                asText(value.get("documentCode")),
                asText(value.get("attachmentId"))
        );
    }

    private List<Object> toObjectList(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(collection);
    }

    private Map<String, Object> toObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return new LinkedHashMap<>();
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private boolean matchesKeyword(String keyword, String... candidates) {
        if (keyword == null) {
            return true;
        }
        for (String candidate : candidates) {
            String normalizedCandidate = trimToNull(candidate);
            if (normalizedCandidate != null && normalizedCandidate.toLowerCase().contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String normalizeUserName(User user) {
        String name = trimToNull(user.getName());
        if (name != null) {
            return name;
        }
        String username = trimToNull(user.getUsername());
        return username == null ? "\u672a\u547d\u540d\u7528\u6237" : username;
    }

    private String defaultText(String value, String fallback) {
        return trimToNull(value) == null ? fallback : value.trim();
    }

    private String defaultUsername(String username) {
        String normalized = trimToNull(username);
        return normalized == null ? "\u5f53\u524d\u7528\u6237" : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
