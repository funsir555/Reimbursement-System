// 业务域：报销单录入、流转与查询
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ExpenseActionUserOptionVO;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.dto.ExpenseTaskAddSignDTO;
import com.finex.auth.dto.ExpenseTaskTransferDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ExpenseApprovalDomainSupport：领域规则支撑类。
 * 承接 报销单审批的核心业务规则。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
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

    /**
     * 查询Pending审批列表。
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

    /**
     * 审批驳回任务。
     */
    public ExpenseDocumentDetailVO rejectTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        ProcessDocumentTask task = requirePendingTask(taskId, userId);
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
        expenseWorkflowRuntimeSupport.rejectPendingTask(
                instance,
                task,
                userId,
                username,
                dto == null ? null : dto.getComment()
        );
        expenseRelationWriteOffService.voidPendingWriteOffs(instance.getDocumentCode());
        return expenseDocumentReadSupport.buildDocumentDetail(
                expenseDocumentReadSupport.requireDocument(instance.getDocumentCode())
        );
    }

    /**
     * 获取任务Modify上下文。
     */
    public ExpenseDocumentEditContextVO getTaskModifyContext(Long userId, Long taskId) {
        ProcessDocumentTask task = requirePendingTask(taskId, userId);
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
        return expenseDocumentTemplateSupport.buildEditContext(userId, instance, task.getId(), "MODIFY");
    }

    /**
     * 处理报销单审批中的这一步。
     */
    public ExpenseDocumentDetailVO modifyTaskDocument(Long userId, String username, Long taskId, ExpenseDocumentUpdateDTO dto) {
        ProcessDocumentTask task = requirePendingTask(taskId, userId);
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
        AbstractExpenseDocumentSupport.DocumentMutationContext mutation = expenseDocumentMutationDomainSupport.buildMutationContext(instance, dto, false);
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
     * 处理报销单审批中的这一步。
     */
    public ExpenseDocumentDetailVO transferTask(Long userId, String username, Long taskId, ExpenseTaskTransferDTO dto) {
        ProcessDocumentTask task = requirePendingTask(taskId, userId);
        User targetUser = requireActiveUser(dto == null ? null : dto.getTargetUserId());
        if (Objects.equals(targetUser.getId(), userId)) {
            throw new IllegalArgumentException("转办目标用户不能是当前处理人");
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

    /**
     * 处理报销单审批中的这一步。
     */
    public ExpenseDocumentDetailVO addSignTask(Long userId, String username, Long taskId, ExpenseTaskAddSignDTO dto) {
        ProcessDocumentTask task = requirePendingTask(taskId, userId);
        User targetUser = requireActiveUser(dto == null ? null : dto.getTargetUserId());
        if (Objects.equals(targetUser.getId(), userId)) {
            throw new IllegalArgumentException("加签目标用户不能是当前处理人");
        }
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
        String remark = trimToNull(dto == null ? null : dto.getRemark());
        expenseWorkflowRuntimeSupport.createAddSignTask(instance, task, targetUser, userId, username, remark);
        return expenseDocumentReadSupport.buildDocumentDetail(
                expenseDocumentReadSupport.requireDocument(task.getDocumentCode())
        );
    }

    /**
     * 查询Action用户。
     */
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
        return userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getName, User::getId)
        ).stream()
                .filter(item -> matchesKeyword(normalizedKeyword, item.getName(), item.getUsername(), item.getPhone(), item.getEmail()))
                .map(item -> {
                    ExpenseActionUserOptionVO option = new ExpenseActionUserOptionVO();
                    option.setUserId(item.getId());
                    option.setName(item.getName());
                    option.setUsername(item.getUsername());
                    option.setPhone(item.getPhone());
                    option.setDeptName(item.getDeptId() == null ? null : departmentNameMap.get(item.getDeptId()));
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
            throw new IllegalStateException("目标处理人不存在或已停用");
        }
        return user;
    }

    /**
     * 判断EffectiveApprovedStatus是否成立。
     */
    private boolean isEffectiveApprovedStatus(String status) {
        String normalized = trimToNull(status);
        return Objects.equals(normalized, "APPROVED")
                || Objects.equals(normalized, "COMPLETED")
                || Objects.equals(normalized, "PENDING_PAYMENT")
                || Objects.equals(normalized, "PAYMENT_COMPLETED")
                || Objects.equals(normalized, "PAYMENT_FINISHED");
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
        return username == null ? "未命名用户" : username;
    }

    private String defaultText(String value, String fallback) {
        return trimToNull(value) == null ? fallback : value.trim();
    }

    private String defaultUsername(String username) {
        String normalized = trimToNull(username);
        return normalized == null ? "当前用户" : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
