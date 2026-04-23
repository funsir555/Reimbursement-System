// 涓氬姟鍩燂細鎶ラ攢鍗曞綍鍏ャ€佹祦杞笌鏌ヨ
// 鏂囦欢瑙掕壊锛氶鍩熻鍒欐敮鎾戠被
// 涓婁笅娓稿叧绯伙細涓婃父閫氬父鏉ヨ嚜 鎶ラ攢鍗曢〉闈€佸鎵归〉闈€佷粯娆鹃〉闈㈠搴旂殑 Controller锛屼笅娓镐細缁х画鍗忚皟 鎶ラ攢鍗曘€佹祦绋嬭妭鐐广€侀檮浠躲€佷粯娆句笌鏍搁攢绛夋暟鎹€?
// 椋庨櫓鎻愰啋锛氭敼鍧忓悗鏈€瀹规槗褰卞搷 鍗曟嵁鐘舵€併€佸鎵归摼銆侀噾棰濈粨鏋滃拰閲嶅鎻愪氦銆?

package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseDocumentCommentDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentNavigationVO;
import com.finex.auth.dto.ExpenseDocumentReminderDTO;
import com.finex.auth.dto.ExpenseManualApproverSelectionDTO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentRelation;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.ProcessDocumentWriteOff;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentRelationMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.mapper.ProcessDocumentWriteOffMapper;
import com.finex.auth.mapper.PmBankPaymentRecordMapper;
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
 * ExpenseQueryDomainSupport锛氶鍩熻鍒欐敮鎾戠被銆?
 * 鎵挎帴 鎶ラ攢鍗曠殑鏍稿績涓氬姟瑙勫垯銆?
 * 鏀硅繖閲屾椂锛岃鐗瑰埆鍏虫敞 鍗曟嵁鐘舵€併€佸鎵归摼銆侀噾棰濈粨鏋滃拰閲嶅鎻愪氦鏄惁浼氳涓€璧峰甫鍧忋€?
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
    private static final String LOG_SUBMIT = "SUBMIT";
    private static final String LOG_RESUBMIT = "RESUBMIT";
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
    private final ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;
    private final ProcessDocumentTaskMapper processDocumentTaskMapper;
    private final ProcessDocumentActionLogMapper processDocumentActionLogMapper;
    private final ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    private final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    private final ProcessDocumentRelationMapper processDocumentRelationMapper;
    private final ProcessDocumentWriteOffMapper processDocumentWriteOffMapper;
    private final PmBankPaymentRecordMapper pmBankPaymentRecordMapper;
    private final NotificationService notificationService;

    /**
     * 鏌ヨ鎶ラ攢鍗昐ummaries鍒楄〃銆?
     */
    public List<ExpenseSummaryVO> listExpenseSummaries(Long userId) {
        List<ProcessDocumentInstance> instances = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getSubmitterUserId, userId)
                        .orderByDesc(ProcessDocumentInstance::getCreatedAt, ProcessDocumentInstance::getId)
        );
        return sortSummaries(expenseSummaryAssembler.toExpenseSummaries(instances));
    }

    /**
     * 鏌ヨ鏌ヨ鍗曟嵁Summaries鍒楄〃銆?
     */
    public List<ExpenseSummaryVO> listQueryDocumentSummaries(Long userId) {
        List<ProcessDocumentInstance> instances = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .ne(ProcessDocumentInstance::getStatus, DOCUMENT_STATUS_DRAFT)
                        .orderByDesc(ProcessDocumentInstance::getCreatedAt, ProcessDocumentInstance::getId)
        );
        return sortSummaries(expenseSummaryAssembler.toExpenseSummaries(instances));
    }

    /**
     * 鏌ヨOutstanding鍗曟嵁鍒楄〃銆?
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
     * 鑾峰彇鍗曟嵁鏄庣粏銆?
     */
    public ExpenseDocumentDetailVO getDocumentDetail(Long userId, String documentCode, boolean allowCrossView) {
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(documentCode);
        expenseDocumentReadSupport.assertCanViewDocument(instance, userId, allowCrossView);
        return expenseDocumentReadSupport.buildDocumentDetail(instance);
    }

    /**
     * 鑾峰彇鎶ラ攢鍗曟槑缁嗐€?
     */
    public ExpenseDetailInstanceDetailVO getExpenseDetail(Long userId, String documentCode, String detailNo, boolean allowCrossView) {
        return expenseDocumentReadSupport.getExpenseDetail(userId, documentCode, detailNo, allowCrossView);
    }

    /**
     * 澶勭悊鎶ラ攢鍗曚腑鐨勮繖涓€姝ャ€?
     */
    public ExpenseDocumentDetailVO recallDocument(Long userId, String username, String documentCode) {
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(documentCode);
        expenseDocumentReadSupport.requireSubmitter(instance, userId);
        String status = trimToNull(instance.getStatus());
        if (!Objects.equals(status, DOCUMENT_STATUS_PENDING) && !Objects.equals(status, DOCUMENT_STATUS_EXCEPTION)) {
            throw new IllegalStateException("鐟滅増鎸告晶鐘诲础閺囩喎绁﹀☉鎾崇У閺侇噣骞愭担绋垮皢闁?");
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
     * 澶勭悊鎶ラ攢鍗曚腑鐨勮繖涓€姝ャ€?
     */
    public ExpenseDocumentDetailVO commentOnDocument(Long userId, String username, String documentCode, ExpenseDocumentCommentDTO dto, boolean allowCrossView) {
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(documentCode);
        expenseDocumentReadSupport.assertCanViewDocument(instance, userId, allowCrossView);
        if (!isFlowRelatedUser(instance, userId)) {
            throw new IllegalStateException("闁告瑯浜濆﹢浣该规担琛℃煠闁烩晝顭堥崣褎绂嶉崫鍕濞寸姰鍎撮惁搴ｆ媼閸濆嫮绉奸柛鎾崇Т瀹曠喖骞?");
        }
        String comment = trimToNull(dto == null ? null : dto.getComment());
        List<String> attachmentFileNames = normalizeStringList(dto == null ? Collections.emptyList() : dto.getAttachmentFileNames());
        if (comment == null && attachmentFileNames.isEmpty()) {
            throw new IllegalArgumentException("閻犲洤瀚鎴﹀礃閸涱収鍟囧☉鎾崇Х閸忔ɑ绋夐搹鍏夋晞");
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
     * 澶勭悊鎶ラ攢鍗曚腑鐨勮繖涓€姝ャ€?
     */
    public ExpenseDocumentDetailVO remindDocument(Long userId, String username, String documentCode, ExpenseDocumentReminderDTO dto) {
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(documentCode);
        expenseDocumentReadSupport.requireSubmitter(instance, userId);
        if (!Objects.equals(trimToNull(instance.getStatus()), DOCUMENT_STATUS_PENDING)) {
            throw new IllegalStateException("\u53ea\u6709\u5ba1\u6279\u4e2d\u7684\u5355\u636e\u624d\u53ef\u4ee5\u50ac\u529e");
        }
        List<ProcessDocumentTask> currentTasks = loadPendingTasks(instance.getDocumentCode());
        if (currentTasks.isEmpty()) {
            throw new IllegalStateException("\u5f53\u524d\u6ca1\u6709\u53ef\u50ac\u529e\u7684\u5ba1\u6279\u4eba");
        }
        ensureReminderThrottle(instance.getDocumentCode(), userId);
        String remark = trimToNull(dto == null ? null : dto.getRemark());
        List<ProcessDocumentTask> distinctTasks = currentTasks.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(ProcessDocumentTask::getAssigneeUserId, item -> item, (left, right) -> left, LinkedHashMap::new),
                        item -> new ArrayList<>(item.values())
                ));
        for (ProcessDocumentTask task : distinctTasks) {
            String title = "\u50ac\u529e\u63d0\u9192";
            String content = "\u5355\u636e " + instance.getDocumentCode() + " \u6b63\u5728\u7b49\u5f85\u4f60\u5904\u7406\u3002";
            if (remark != null) {
                content = content + " \u50ac\u529e\u5907\u6ce8\uff1a" + remark;
            }
            notificationService.sendAsyncNotification(task.getAssigneeUserId(), "EXPENSE_REMINDER", title, content, instance.getDocumentCode());
        }
        expenseDocumentActionLogSupport.appendLog(instance.getDocumentCode(), instance.getCurrentNodeKey(), instance.getCurrentNodeName(), LOG_REMIND, userId, defaultUsername(username), remark, Map.of(
                "recipientUserIds", distinctTasks.stream().map(ProcessDocumentTask::getAssigneeUserId).toList(),
                "recipientNames", distinctTasks.stream().map(ProcessDocumentTask::getAssigneeName).toList()
        ));
        return expenseDocumentReadSupport.buildDocumentDetail(expenseDocumentReadSupport.requireDocument(instance.getDocumentCode()));
    }

    public ExpenseDocumentDetailVO submitManualApproverSelection(Long userId, String username, String documentCode, ExpenseManualApproverSelectionDTO dto) {
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(documentCode);
        expenseDocumentReadSupport.requireSubmitter(instance, userId);
        expenseWorkflowRuntimeSupport.submitManualApproverSelection(
                instance,
                userId,
                username,
                dto == null ? null : dto.getNodeKey(),
                dto == null ? Collections.emptyList() : dto.getUserIds()
        );
        return expenseDocumentReadSupport.buildDocumentDetail(
                expenseDocumentReadSupport.requireDocument(instance.getDocumentCode())
        );
    }

    /**
     * 鑾峰彇鍗曟嵁Navigation銆?
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
     * 鑾峰彇鍗曟嵁Edit涓婁笅鏂囥€?
     */
    public ExpenseDocumentEditContextVO getDocumentEditContext(Long userId, String documentCode) {
        return expenseDocumentTemplateSupport.getDocumentEditContext(userId, documentCode);
    }

    public boolean deleteDraftDocument(Long userId, String documentCode) {
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(documentCode);
        expenseDocumentReadSupport.requireSubmitter(instance, userId);
        if (!Objects.equals(trimToNull(instance.getStatus()), DOCUMENT_STATUS_DRAFT)) {
            throw new IllegalStateException("\u4ec5\u8349\u7a3f\u72b6\u6001\u5355\u636e\u5141\u8bb8\u5220\u9664");
        }
        if (hasFormalProcessHistory(instance.getDocumentCode())) {
            throw new IllegalStateException("\u5df2\u63d0\u4ea4\u8fc7\u7684\u5355\u636e\u53ec\u56de\u540e\u4e0d\u5141\u8bb8\u5220\u9664");
        }
        String normalizedDocumentCode = instance.getDocumentCode();
        pmBankPaymentRecordMapper.delete(
                Wrappers.<com.finex.auth.entity.PmBankPaymentRecord>lambdaQuery()
                        .eq(com.finex.auth.entity.PmBankPaymentRecord::getDocumentCode, normalizedDocumentCode)
        );
        processDocumentWriteOffMapper.delete(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .and(wrapper -> wrapper
                                .eq(ProcessDocumentWriteOff::getSourceDocumentCode, normalizedDocumentCode)
                                .or()
                                .eq(ProcessDocumentWriteOff::getTargetDocumentCode, normalizedDocumentCode))
        );
        processDocumentRelationMapper.delete(
                Wrappers.<ProcessDocumentRelation>lambdaQuery()
                        .and(wrapper -> wrapper
                                .eq(ProcessDocumentRelation::getSourceDocumentCode, normalizedDocumentCode)
                                .or()
                                .eq(ProcessDocumentRelation::getTargetDocumentCode, normalizedDocumentCode))
        );
        processDocumentTaskMapper.delete(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, normalizedDocumentCode)
        );
        processDocumentActionLogMapper.delete(
                Wrappers.<ProcessDocumentActionLog>lambdaQuery()
                        .eq(ProcessDocumentActionLog::getDocumentCode, normalizedDocumentCode)
        );
        processDocumentExpenseDetailMapper.delete(
                Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                        .eq(ProcessDocumentExpenseDetail::getDocumentCode, normalizedDocumentCode)
        );
        processDocumentInstanceMapper.deleteById(instance.getId());
        return true;
    }

    private boolean hasFormalProcessHistory(String documentCode) {
        Long lifecycleLogCount = processDocumentActionLogMapper.selectCount(
                Wrappers.<ProcessDocumentActionLog>lambdaQuery()
                        .eq(ProcessDocumentActionLog::getDocumentCode, documentCode)
                        .in(ProcessDocumentActionLog::getActionType, List.of(LOG_SUBMIT, LOG_RESUBMIT, LOG_RECALL))
        );
        return lifecycleLogCount != null && lifecycleLogCount > 0;
    }

    private List<ExpenseSummaryVO> sortSummaries(List<ExpenseSummaryVO> summaries) {
        return summaries.stream()
                .sorted((left, right) -> {
                    String leftSubmittedAt = trimToNull(left == null ? null : left.getSubmittedAt());
                    String rightSubmittedAt = trimToNull(right == null ? null : right.getSubmittedAt());
                    if (Objects.equals(leftSubmittedAt, rightSubmittedAt)) {
                        return 0;
                    }
                    if (leftSubmittedAt == null) {
                        return 1;
                    }
                    if (rightSubmittedAt == null) {
                        return -1;
                    }
                    return rightSubmittedAt.compareTo(leftSubmittedAt);
                })
                .toList();
    }

    /**
     * 鍒ゆ柇娴佺▼Related鐢ㄦ埛鏄惁鎴愮珛銆?
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
            throw new IllegalStateException("10\u5206\u949f\u5185\u53ea\u80fd\u50ac\u529e\u4e00\u6b21\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5");
        }
    }

    /**
     * 鍔犺浇Pending浠诲姟銆?
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
     * 鍔犺浇寮€绔嬩换鍔°€?
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
     * 鍔犺浇Navigation鍗曟嵁缂栫爜銆?
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

