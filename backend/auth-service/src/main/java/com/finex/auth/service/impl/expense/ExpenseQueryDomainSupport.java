// 业务域：报销单录入、流转与查询
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseDocumentCommentDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentNavigationVO;
import com.finex.auth.dto.ExpenseDocumentReminderDTO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ExpenseQueryDomainSupport：领域规则支撑类。
 * 承接 报销单的核心业务规则。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
@RequiredArgsConstructor
public class ExpenseQueryDomainSupport {

    private static final String DOCUMENT_STATUS_PENDING = "PENDING_APPROVAL";
    private static final String DOCUMENT_STATUS_EXCEPTION = "EXCEPTION";
    private static final String DOCUMENT_STATUS_APPROVED = "APPROVED";
    private static final String DOCUMENT_STATUS_COMPLETED = "COMPLETED";
    private static final String DOCUMENT_STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String DOCUMENT_STATUS_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    private static final String DOCUMENT_STATUS_PAYMENT_FINISHED = "PAYMENT_FINISHED";
    private static final String DOCUMENT_STATUS_DRAFT = "DRAFT";
    private static final String TASK_STATUS_PENDING = "PENDING";
    private static final String TASK_STATUS_PAUSED = "PAUSED";
    private static final String TASK_STATUS_CANCELLED = "CANCELLED";
    private static final String LOG_RECALL = "RECALL";
    private static final String LOG_COMMENT = "COMMENT";
    private static final String LOG_REMIND = "REMIND";
    private static final int NAVIGATION_HISTORY_LIMIT = 200;
    private static final String OUTSTANDING_KIND_LOAN = "LOAN";

    private final ExpenseDocumentReadSupport expenseDocumentReadSupport;
    private final ExpenseDocumentActionLogSupport expenseDocumentActionLogSupport;
    private final ExpenseDocumentTemplateSupport expenseDocumentTemplateSupport;
    private final ExpenseRelationWriteOffService expenseRelationWriteOffService;
    private final ExpenseSummaryAssembler expenseSummaryAssembler;
    private final ProcessDocumentTaskMapper processDocumentTaskMapper;
    private final ProcessDocumentActionLogMapper processDocumentActionLogMapper;
    private final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    private final NotificationService notificationService;

    /**
     * 查询报销单Summaries列表。
     */
    public List<ExpenseSummaryVO> listExpenseSummaries(Long userId) {
        List<ProcessDocumentInstance> instances = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getSubmitterUserId, userId)
                        .orderByDesc(ProcessDocumentInstance::getCreatedAt, ProcessDocumentInstance::getId)
        );
        return expenseSummaryAssembler.toExpenseSummaries(instances);
    }

    /**
     * 查询查询单据Summaries列表。
     */
    public List<ExpenseSummaryVO> listQueryDocumentSummaries(Long userId) {
        List<ProcessDocumentInstance> instances = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .ne(ProcessDocumentInstance::getStatus, DOCUMENT_STATUS_DRAFT)
                        .orderByDesc(ProcessDocumentInstance::getCreatedAt, ProcessDocumentInstance::getId)
        );
        return expenseSummaryAssembler.toExpenseSummaries(instances);
    }

    /**
     * 查询Outstanding单据列表。
     */
    public List<ExpenseSummaryVO> listOutstandingDocuments(Long userId, String kind) {
        String normalizedKind = normalizeOutstandingKind(kind);
        String templateType = Objects.equals(normalizedKind, OUTSTANDING_KIND_LOAN) ? "loan" : "report";
        List<ProcessDocumentInstance> instances = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getSubmitterUserId, userId)
                        .in(ProcessDocumentInstance::getStatus, List.of(
                                DOCUMENT_STATUS_APPROVED,
                                DOCUMENT_STATUS_COMPLETED,
                                DOCUMENT_STATUS_PENDING_PAYMENT,
                                DOCUMENT_STATUS_PAYMENT_COMPLETED,
                                DOCUMENT_STATUS_PAYMENT_FINISHED
                        ))
                        .eq(ProcessDocumentInstance::getTemplateType, templateType)
                        .orderByDesc(ProcessDocumentInstance::getFinishedAt, ProcessDocumentInstance::getUpdatedAt, ProcessDocumentInstance::getId)
        );
        if (instances.isEmpty()) {
            return List.of();
        }

        Map<String, java.math.BigDecimal> outstandingAmountMap = expenseRelationWriteOffService.buildOutstandingAmountMap(instances, normalizedKind);
        if (outstandingAmountMap.isEmpty()) {
            return List.of();
        }
        List<ProcessDocumentInstance> outstandingInstances = instances.stream()
                .filter(item -> outstandingAmountMap.containsKey(item.getDocumentCode()))
                .toList();
        return expenseSummaryAssembler.toExpenseSummaries(outstandingInstances).stream()
                .peek(item -> item.setOutstandingAmount(outstandingAmountMap.get(item.getDocumentCode())))
                .toList();
    }

    /**
     * 获取单据明细。
     */
    public ExpenseDocumentDetailVO getDocumentDetail(Long userId, String documentCode, boolean allowCrossView) {
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(documentCode);
        expenseDocumentReadSupport.assertCanViewDocument(instance, userId, allowCrossView);
        return expenseDocumentReadSupport.buildDocumentDetail(instance);
    }

    /**
     * 获取报销单明细。
     */
    public ExpenseDetailInstanceDetailVO getExpenseDetail(Long userId, String documentCode, String detailNo, boolean allowCrossView) {
        return expenseDocumentReadSupport.getExpenseDetail(userId, documentCode, detailNo, allowCrossView);
    }

    /**
     * 处理报销单中的这一步。
     */
    public ExpenseDocumentDetailVO recallDocument(Long userId, String username, String documentCode) {
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(documentCode);
        expenseDocumentReadSupport.requireSubmitter(instance, userId);
        String status = trimToNull(instance.getStatus());
        if (!Objects.equals(status, DOCUMENT_STATUS_PENDING) && !Objects.equals(status, DOCUMENT_STATUS_EXCEPTION)) {
            throw new IllegalStateException("瑜版挸澧犻崡鏇熷祦娑撳秵鏁幐浣稿将閸?");
        }
        LocalDateTime now = LocalDateTime.now();
        cancelOpenTasks(loadOpenTasks(instance.getDocumentCode()), now);
        instance.setStatus(DOCUMENT_STATUS_DRAFT);
        instance.setCurrentNodeKey(null);
        instance.setCurrentNodeName(null);
        instance.setCurrentTaskType(null);
        instance.setFinishedAt(null);
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);
        expenseDocumentActionLogSupport.appendLog(instance.getDocumentCode(), null, null, LOG_RECALL, userId, defaultUsername(username), null, Map.of(
                "fromStatus", defaultText(status, DOCUMENT_STATUS_PENDING)
        ));
        expenseRelationWriteOffService.voidPendingWriteOffs(instance.getDocumentCode());
        return expenseDocumentReadSupport.buildDocumentDetail(expenseDocumentReadSupport.requireDocument(instance.getDocumentCode()));
    }

    /**
     * 处理报销单中的这一步。
     */
    public ExpenseDocumentDetailVO commentOnDocument(Long userId, String username, String documentCode, ExpenseDocumentCommentDTO dto, boolean allowCrossView) {
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(documentCode);
        expenseDocumentReadSupport.assertCanViewDocument(instance, userId, allowCrossView);
        if (!isFlowRelatedUser(instance, userId)) {
            throw new IllegalStateException("閸欘亝婀佸ù浣衡柤閻╃鍙ф禍鍝勫讲娴犮儴鐦庣拋鍝勭秼閸撳秴宕熼幑?");
        }
        String comment = trimToNull(dto == null ? null : dto.getComment());
        List<String> attachmentFileNames = normalizeStringList(dto == null ? Collections.emptyList() : dto.getAttachmentFileNames());
        if (comment == null && attachmentFileNames.isEmpty()) {
            throw new IllegalArgumentException("鐠囧嫯顔戦崘鍛啇娑撳秷鍏樻稉铏光敄");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        if (comment != null) {
            payload.put("comment", comment);
        }
        if (!attachmentFileNames.isEmpty()) {
            payload.put("attachmentFileNames", attachmentFileNames);
        }
        expenseDocumentActionLogSupport.appendLog(
                instance.getDocumentCode(),
                instance.getCurrentNodeKey(),
                instance.getCurrentNodeName(),
                LOG_COMMENT,
                userId,
                defaultUsername(username),
                comment,
                payload
        );
        return expenseDocumentReadSupport.buildDocumentDetail(expenseDocumentReadSupport.requireDocument(instance.getDocumentCode()));
    }

    /**
     * 处理报销单中的这一步。
     */
    public ExpenseDocumentDetailVO remindDocument(Long userId, String username, String documentCode, ExpenseDocumentReminderDTO dto) {
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(documentCode);
        expenseDocumentReadSupport.requireSubmitter(instance, userId);
        if (!Objects.equals(trimToNull(instance.getStatus()), DOCUMENT_STATUS_PENDING)) {
            throw new IllegalStateException("閸欘亝婀佺€光剝澹掓稉顓犳畱閸楁洘宓侀幍宥呭讲娴犮儱鍋撻崝?");
        }
        List<ProcessDocumentTask> currentTasks = loadPendingTasks(instance.getDocumentCode());
        if (currentTasks.isEmpty()) {
            throw new IllegalStateException("瑜版挸澧犻崡鏇熷祦濞屸剝婀佸鍛吀閹甸€涙眽閿涘本娈忛弮鑸垫￥濞夋洖鍋撻崝?");
        }
        ensureReminderThrottle(instance.getDocumentCode(), userId);
        String remark = trimToNull(dto == null ? null : dto.getRemark());
        List<ProcessDocumentTask> distinctTasks = currentTasks.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(ProcessDocumentTask::getAssigneeUserId, item -> item, (left, right) -> left, LinkedHashMap::new),
                        item -> new ArrayList<>(item.values())
                ));
        for (ProcessDocumentTask task : distinctTasks) {
            String title = "鐎光剝澹掗崒顒€濮欓幓鎰板晪";
            String content = "閸楁洘宓?" + instance.getDocumentCode() + " 濮濓絽婀粵澶婄窡娴ｇ姷娈戞径鍕倞";
            if (remark != null) {
                content = content + "閿涘苯顦▔顭掔窗" + remark;
            }
            notificationService.sendAsyncNotification(task.getAssigneeUserId(), "EXPENSE_REMINDER", title, content, instance.getDocumentCode());
        }
        expenseDocumentActionLogSupport.appendLog(instance.getDocumentCode(), instance.getCurrentNodeKey(), instance.getCurrentNodeName(), LOG_REMIND, userId, defaultUsername(username), remark, Map.of(
                "recipientUserIds", distinctTasks.stream().map(ProcessDocumentTask::getAssigneeUserId).toList(),
                "recipientNames", distinctTasks.stream().map(ProcessDocumentTask::getAssigneeName).toList()
        ));
        return expenseDocumentReadSupport.buildDocumentDetail(expenseDocumentReadSupport.requireDocument(instance.getDocumentCode()));
    }

    /**
     * 获取单据Navigation。
     */
    public ExpenseDocumentNavigationVO getDocumentNavigation(Long userId, String documentCode, boolean approvalViewer) {
        ExpenseDocumentNavigationVO navigation = new ExpenseDocumentNavigationVO();
        if (!approvalViewer) {
            return navigation;
        }
        expenseDocumentReadSupport.requireDocument(documentCode);
        List<String> orderedCodes = loadNavigationDocumentCodes(userId, documentCode);
        int index = orderedCodes.indexOf(documentCode);
        if (index < 0) {
            return navigation;
        }
        if (index > 0) {
            navigation.setPrevDocumentCode(orderedCodes.get(index - 1));
        }
        if (index + 1 < orderedCodes.size()) {
            navigation.setNextDocumentCode(orderedCodes.get(index + 1));
        }
        return navigation;
    }

    /**
     * 获取单据Edit上下文。
     */
    public ExpenseDocumentEditContextVO getDocumentEditContext(Long userId, String documentCode) {
        return expenseDocumentTemplateSupport.getDocumentEditContext(userId, documentCode);
    }

    /**
     * 判断流程Related用户是否成立。
     */
    private boolean isFlowRelatedUser(ProcessDocumentInstance instance, Long userId) {
        if (Objects.equals(instance.getSubmitterUserId(), userId)) {
            return true;
        }
        Long taskCount = processDocumentTaskMapper.selectCount(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, instance.getDocumentCode())
                        .eq(ProcessDocumentTask::getAssigneeUserId, userId)
        );
        if (taskCount != null && taskCount > 0) {
            return true;
        }
        Long logCount = processDocumentActionLogMapper.selectCount(
                Wrappers.<ProcessDocumentActionLog>lambdaQuery()
                        .eq(ProcessDocumentActionLog::getDocumentCode, instance.getDocumentCode())
                        .eq(ProcessDocumentActionLog::getActorUserId, userId)
        );
        return logCount != null && logCount > 0;
    }

    private void ensureReminderThrottle(String documentCode, Long userId) {
        ProcessDocumentActionLog latestLog = processDocumentActionLogMapper.selectOne(
                Wrappers.<ProcessDocumentActionLog>lambdaQuery()
                        .eq(ProcessDocumentActionLog::getDocumentCode, documentCode)
                        .eq(ProcessDocumentActionLog::getActionType, LOG_REMIND)
                        .eq(ProcessDocumentActionLog::getActorUserId, userId)
                        .orderByDesc(ProcessDocumentActionLog::getCreatedAt, ProcessDocumentActionLog::getId)
                        .last("limit 1")
        );
        if (latestLog != null
                && latestLog.getCreatedAt() != null
                && latestLog.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(10))) {
            throw new IllegalStateException("閸氬奔绔撮崡鏇熷祦 10 閸掑棝鎸撻崘鍛涧閼宠棄鍋撻崝鐐扮濞?");
        }
    }

    /**
     * 加载Pending任务。
     */
    private List<ProcessDocumentTask> loadPendingTasks(String documentCode) {
        return processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                        .eq(ProcessDocumentTask::getStatus, TASK_STATUS_PENDING)
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
    }

    /**
     * 加载开立任务。
     */
    private List<ProcessDocumentTask> loadOpenTasks(String documentCode) {
        return processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                        .in(ProcessDocumentTask::getStatus, List.of(TASK_STATUS_PENDING, TASK_STATUS_PAUSED))
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
    }

    private void cancelOpenTasks(List<ProcessDocumentTask> tasks, LocalDateTime handledAt) {
        for (ProcessDocumentTask task : tasks) {
            if (!TASK_STATUS_PENDING.equals(task.getStatus()) && !TASK_STATUS_PAUSED.equals(task.getStatus())) {
                continue;
            }
            task.setStatus(TASK_STATUS_CANCELLED);
            task.setHandledAt(handledAt);
            processDocumentTaskMapper.updateById(task);
        }
    }

    private List<String> normalizeStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 加载Navigation单据编码。
     */
    private List<String> loadNavigationDocumentCodes(Long userId, String currentDocumentCode) {
        List<String> pendingCodes = processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getAssigneeUserId, userId)
                        .eq(ProcessDocumentTask::getStatus, TASK_STATUS_PENDING)
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        ).stream().map(ProcessDocumentTask::getDocumentCode).distinct().toList();
        if (pendingCodes.contains(currentDocumentCode)) {
            return pendingCodes;
        }

        LinkedHashSet<String> visibleDocumentCodes = new LinkedHashSet<>(pendingCodes);
        processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getAssigneeUserId, userId)
                        .orderByDesc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
                        .last("limit " + NAVIGATION_HISTORY_LIMIT)
        ).forEach(item -> visibleDocumentCodes.add(item.getDocumentCode()));
        processDocumentActionLogMapper.selectList(
                Wrappers.<ProcessDocumentActionLog>lambdaQuery()
                        .eq(ProcessDocumentActionLog::getActorUserId, userId)
                        .orderByDesc(ProcessDocumentActionLog::getCreatedAt, ProcessDocumentActionLog::getId)
                        .last("limit " + NAVIGATION_HISTORY_LIMIT)
        ).forEach(item -> visibleDocumentCodes.add(item.getDocumentCode()));
        String normalizedCurrentDocumentCode = trimToNull(currentDocumentCode);
        if (normalizedCurrentDocumentCode != null) {
            visibleDocumentCodes.add(normalizedCurrentDocumentCode);
        }

        if (visibleDocumentCodes.isEmpty()) {
            return Collections.emptyList();
        }

        return processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .in(ProcessDocumentInstance::getDocumentCode, visibleDocumentCodes)
                        .in(ProcessDocumentInstance::getStatus, List.of(
                                DOCUMENT_STATUS_PENDING,
                                DOCUMENT_STATUS_EXCEPTION,
                                DOCUMENT_STATUS_APPROVED,
                                DOCUMENT_STATUS_COMPLETED,
                                DOCUMENT_STATUS_PENDING_PAYMENT,
                                DOCUMENT_STATUS_PAYMENT_COMPLETED,
                                DOCUMENT_STATUS_PAYMENT_FINISHED
                        ))
                        .orderByDesc(ProcessDocumentInstance::getUpdatedAt, ProcessDocumentInstance::getId)
        ).stream().map(ProcessDocumentInstance::getDocumentCode).toList();
    }

    private String defaultUsername(String username) {
        String normalized = trimToNull(username);
        return normalized == null ? "SYSTEM" : normalized;
    }

    private String defaultText(String value, String fallback) {
        return trimToNull(value) == null ? fallback : value.trim();
    }

    private String normalizeOutstandingKind(String kind) {
        String normalized = trimToNull(kind);
        return Objects.equals(normalized, OUTSTANDING_KIND_LOAN) ? OUTSTANDING_KIND_LOAN : "PREPAY_REPORT";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

