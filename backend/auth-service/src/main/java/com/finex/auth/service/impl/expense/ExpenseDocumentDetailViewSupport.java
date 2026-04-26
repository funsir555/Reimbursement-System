package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseApprovalLogVO;
import com.finex.auth.dto.ExpenseApprovalNodeStatusVO;
import com.finex.auth.dto.ExpenseApprovalTaskVO;
import com.finex.auth.dto.ExpenseApprovalTimelineItemVO;
import com.finex.auth.dto.ExpenseDetailInstanceSummaryVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.entity.PmBankPaymentRecord;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
class ExpenseDocumentDetailViewSupport {

    private static final String APPROVER_TYPE_MANUAL_SELECT = "MANUAL_SELECT";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AbstractExpenseDocumentSupport support;
    private final ExpenseDocumentActionLogSupport expenseDocumentActionLogSupport;
    private final ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;
    private final ExpenseApprovalProjectionSupport expenseApprovalProjectionSupport;
    private final ExpenseReadonlyPayeeAccountSnapshotEnhancer readonlyPayeeAccountSnapshotEnhancer;

    ExpenseDocumentDetailVO buildDocumentDetail(ProcessDocumentInstance instance) {
        long totalStartedAt = System.nanoTime();
        String documentCode = instance.getDocumentCode();
        String templateType = support.trimToNull(instance.getTemplateType());
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        detail.setDocumentCode(instance.getDocumentCode());
        detail.setDocumentTitle(instance.getDocumentTitle());
        detail.setDocumentReason(instance.getDocumentReason());
        detail.setStatus(instance.getStatus());
        detail.setStatusLabel(support.resolveStatusLabel(instance.getStatus()));
        detail.setTotalAmount(defaultDecimal(instance.getTotalAmount()));
        detail.setSubmitterUserId(instance.getSubmitterUserId());
        detail.setSubmitterName(instance.getSubmitterName());
        detail.setTemplateName(instance.getTemplateName());
        detail.setTemplateType(instance.getTemplateType());
        detail.setCurrentNodeKey(instance.getCurrentNodeKey());
        detail.setCurrentNodeName(instance.getCurrentNodeName());
        detail.setCurrentTaskType(instance.getCurrentTaskType());
        detail.setSubmittedAt(formatTime(support.resolveDisplaySubmittedAt(instance)));
        detail.setFinishedAt(formatTime(instance.getFinishedAt()));

        long snapshotStartedAt = System.nanoTime();
        Map<String, Object> templateSnapshot = support.readMap(instance.getTemplateSnapshotJson());
        Map<String, Object> formSchemaSnapshot = support.readMap(instance.getFormSchemaSnapshotJson());
        Map<String, Object> formData = support.readFormData(instance.getFormDataJson());
        readonlyPayeeAccountSnapshotEnhancer.enhanceFormData(formSchemaSnapshot, formData, null);
        Map<String, Object> flowSnapshot = support.readMap(instance.getFlowSnapshotJson());
        long snapshotElapsedAt = elapsedMillis(snapshotStartedAt);
        detail.setTemplateSnapshot(templateSnapshot);
        detail.setFormSchemaSnapshot(formSchemaSnapshot);
        detail.setFormData(formData);
        detail.setFlowSnapshot(flowSnapshot);

        long companyOptionsStartedAt = System.nanoTime();
        List<ProcessFormOptionVO> companyOptions = support.loadCompanyOptionsForDetail(formSchemaSnapshot, formData);
        long companyOptionsElapsedAt = elapsedMillis(companyOptionsStartedAt);
        detail.setCompanyOptions(companyOptions);

        long departmentOptionsStartedAt = System.nanoTime();
        List<ProcessFormOptionVO> departmentOptions = support.loadDepartmentOptionsForDetail(formSchemaSnapshot, formData);
        long departmentOptionsElapsedAt = elapsedMillis(departmentOptionsStartedAt);
        detail.setDepartmentOptions(departmentOptions);

        long expenseDetailsStartedAt = System.nanoTime();
        List<ExpenseDetailInstanceSummaryVO> expenseDetails = support.safeLoadExpenseDetailSummaries(documentCode);
        long expenseDetailsElapsedAt = elapsedMillis(expenseDetailsStartedAt);
        detail.setExpenseDetails(expenseDetails);

        long currentTasksStartedAt = System.nanoTime();
        List<ExpenseApprovalTaskVO> currentTasks = support.loadPendingTasks(documentCode).stream()
                .map(support::toTaskVO)
                .toList();
        long currentTasksElapsedAt = elapsedMillis(currentTasksStartedAt);
        detail.setCurrentTasks(currentTasks);

        long actionLogsStartedAt = System.nanoTime();
        List<ProcessDocumentActionLog> actionLogEntities = expenseDocumentActionLogSupport.loadActionLogs(documentCode);
        List<ExpenseApprovalLogVO> actionLogs = actionLogEntities.stream()
                .map(support::toLogVO)
                .toList();
        long actionLogsElapsedAt = elapsedMillis(actionLogsStartedAt);
        detail.setActionLogs(actionLogs);

        long approvalProjectionStartedAt = System.nanoTime();
        List<ProcessDocumentTask> allTasks = support.loadAllTasks(documentCode);
        FlowRuntimeSnapshot runtimeSnapshot = support.readFlowRuntimeSnapshot(instance.getFlowSnapshotJson());
        Map<String, Object> runtimeContext = expenseWorkflowRuntimeSupport.buildRuntimeContextForInstance(instance);
        ExpenseApprovalProjectionSupport.ApprovalProjectionResult approvalProjection = expenseApprovalProjectionSupport.build(
                instance,
                runtimeSnapshot,
                runtimeContext,
                allTasks,
                actionLogEntities
        );
        List<ExpenseApprovalNodeStatusVO> approvalNodeStatuses = approvalProjection.approvalNodeStatuses();
        List<ExpenseApprovalTimelineItemVO> approvalTimeline = approvalProjection.approvalTimeline();
        long approvalProjectionElapsedAt = elapsedMillis(approvalProjectionStartedAt);
        detail.setApprovalNodeStatuses(approvalNodeStatuses);
        detail.setApprovalTimeline(approvalTimeline);

        boolean manualApproverSelectionPending = Objects.equals(
                support.trimToNull(instance.getCurrentTaskType()),
                APPROVER_TYPE_MANUAL_SELECT
        ) && support.trimToNull(instance.getCurrentNodeKey()) != null;
        detail.setManualApproverSelectionPending(manualApproverSelectionPending);
        if (manualApproverSelectionPending) {
            detail.setManualApproverSelectionNodeKey(instance.getCurrentNodeKey());
            detail.setManualApproverSelectionNodeName(instance.getCurrentNodeName());
            detail.setManualApproverOptions(support.loadUserOptions(flowSnapshot));
        }

        PmBankPaymentRecord bankPaymentRecord = support.findLatestBankPaymentRecord(documentCode);
        if (bankPaymentRecord != null) {
            Map<Long, String> companyBankAccountNameMap = support.loadCompanyBankAccountNameMap(
                    bankPaymentRecord.getCompanyBankAccountId() == null
                            ? Collections.emptySet()
                            : Set.of(bankPaymentRecord.getCompanyBankAccountId())
            );
            detail.setBankPayment(support.toDetailBankPayment(
                    bankPaymentRecord,
                    companyBankAccountNameMap.get(bankPaymentRecord.getCompanyBankAccountId()),
                    instance.getStatus()
            ));
            detail.setBankReceipts(support.toDetailBankReceipts(bankPaymentRecord));
        }

        log.info(
                "Expense detail built documentCode={} templateType={} totalMs={} snapshotMs={} companyOptionsMs={} departmentOptionsMs={} expenseDetailsMs={} pendingTasksMs={} actionLogsMs={} approvalProjectionMs={} expenseDetailCount={} pendingTaskCount={} actionLogCount={} approvalNodeStatusCount={} approvalTimelineCount={}",
                documentCode,
                defaultText(templateType, "-"),
                elapsedMillis(totalStartedAt),
                snapshotElapsedAt,
                companyOptionsElapsedAt,
                departmentOptionsElapsedAt,
                expenseDetailsElapsedAt,
                currentTasksElapsedAt,
                actionLogsElapsedAt,
                approvalProjectionElapsedAt,
                expenseDetails.size(),
                currentTasks.size(),
                actionLogs.size(),
                approvalNodeStatuses.size(),
                approvalTimeline.size()
        );
        return detail;
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
