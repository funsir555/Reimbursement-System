package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.entity.PmBankPaymentRecord;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.SystemCompanyBankAccount;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class ExpensePaymentExecutionSupport extends AbstractExpensePaymentSupport {

    private final ExpensePaymentRecordSupport recordSupport;

    ExpensePaymentExecutionSupport(
            ExpensePaymentSupportContext context,
            ExpensePaymentRecordSupport recordSupport
    ) {
        super(context);
        this.recordSupport = recordSupport;
    }

    ExpenseDocumentDetailVO startPaymentTask(Long userId, String username, Long taskId) {
        ProcessDocumentTask task = requireOpenPaymentTask(taskId, userId);
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
        String status = trimToNull(instance.getStatus());
        boolean retrying = DOCUMENT_STATUS_PAYMENT_EXCEPTION.equals(status)
                && expenseWorkflowRuntimeSupport.paymentTaskAllowsRetry(instance, task);
        if (DOCUMENT_STATUS_PENDING_PAYMENT.equals(status) || retrying) {
            return pushPaymentTaskToBank(userId, username, task, instance, retrying);
        }
        throw new IllegalStateException("当前付款任务无法发起支付");
    }

    ExpenseDocumentDetailVO completePaymentTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        ProcessDocumentTask task = requireOpenPaymentTask(taskId, userId);
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
        String status = trimToNull(instance.getStatus());
        if (!DOCUMENT_STATUS_PAYING.equals(status) && !DOCUMENT_STATUS_PENDING_PAYMENT.equals(status)) {
            throw new IllegalStateException("当前付款任务不在可完成状态");
        }
        SystemCompanyBankAccount account = recordSupport.findActiveBankAccountForDocument(instance, false);
        PmBankPaymentRecord record = recordSupport.findOrCreateBankPaymentRecord(
                task,
                instance,
                account
        );
        record.setManualPaid(1);
        applyManualPaymentChannel(record, account);
        record.setLastErrorMessage(null);
        if (trimToNull(record.getReceiptStatus()) == null) {
            record.setReceiptStatus(RECEIPT_STATUS_PENDING);
        }
        recordSupport.saveBankPaymentRecord(record);
        return completePaymentTaskInternal(
                userId,
                username,
                task,
                instance,
                trimToNull(dto == null ? null : dto.getComment()),
                true,
                LocalDateTime.now()
        );
    }

    ExpenseDocumentDetailVO markPaymentTaskException(
            Long userId,
            String username,
            Long taskId,
            ExpenseApprovalActionDTO dto
    ) {
        ProcessDocumentTask task = requireOpenPaymentTask(taskId, userId);
        ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
        String status = trimToNull(instance.getStatus());
        if (!DOCUMENT_STATUS_PENDING_PAYMENT.equals(status)
                && !DOCUMENT_STATUS_PAYING.equals(status)
                && !DOCUMENT_STATUS_PAYMENT_EXCEPTION.equals(status)) {
            throw new IllegalStateException("当前付款任务不在可标记异常状态");
        }

        String comment = trimToNull(dto == null ? null : dto.getComment());
        boolean allowRetry = expenseWorkflowRuntimeSupport.paymentTaskAllowsRetry(instance, task);
        expenseWorkflowRuntimeSupport.markPaymentException(
                instance,
                task,
                userId,
                username,
                comment,
                allowRetry
        );
        PmBankPaymentRecord record = recordSupport.findLatestBankPaymentRecord(instance.getDocumentCode());
        if (record != null) {
            record.setLastErrorMessage(firstNonBlank(comment, "付款异常"));
            record.setReceiptStatus(RECEIPT_STATUS_FAILED);
            pmBankPaymentRecordMapper.updateById(record);
        }
        return expenseDocumentReadSupport.buildDocumentDetail(
                expenseDocumentReadSupport.requireDocument(instance.getDocumentCode())
        );
    }

    boolean validatePaymentTasksExportable(Long userId, List<Long> taskIds) {
        validateExportablePaymentTasks(userId, taskIds);
        return true;
    }

    boolean markPaymentTasksAsPaying(Long userId, String username, List<Long> taskIds) {
        for (PaymentTaskContext paymentTask : validateExportablePaymentTasks(userId, taskIds)) {
            String status = trimToNull(paymentTask.instance().getStatus());
            if (DOCUMENT_STATUS_PENDING_PAYMENT.equals(status)) {
                expenseWorkflowRuntimeSupport.markPaymentStarted(
                        paymentTask.instance(),
                        paymentTask.task(),
                        userId,
                        username,
                        false,
                        null,
                        null,
                        null
                );
                continue;
            }
        }
        return true;
    }

    boolean voidPaymentTasks(Long userId, String username, List<Long> taskIds) {
        List<Long> normalizedTaskIds = normalizeTaskIds(taskIds);
        if (normalizedTaskIds.isEmpty()) {
            throw new IllegalArgumentException("请选择可作废的付款单");
        }
        for (Long taskId : normalizedTaskIds) {
            ProcessDocumentTask task = requireAccessiblePaymentTask(taskId, userId);
            ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
            String currentStatus = trimToNull(instance.getStatus());
            if (!isVoidableStatus(currentStatus)) {
                throw new IllegalStateException("当前付款任务不在可作废状态");
            }
            String targetStatus = expenseWorkflowRuntimeSupport.resolvePaymentVoidTargetStatus(instance);
            reopenPaymentTaskIfNeeded(task);
            expenseWorkflowRuntimeSupport.revertPaymentToStatus(
                    instance,
                    task,
                    userId,
                    username,
                    buildVoidComment(targetStatus),
                    currentStatus,
                    targetStatus
            );
            PmBankPaymentRecord record = recordSupport.findLatestBankPaymentRecord(instance.getDocumentCode());
            if (record != null) {
                record.setManualPaid(0);
                record.setPaidAt(null);
                record.setReceiptStatus(RECEIPT_STATUS_PENDING);
                record.setLastErrorMessage(null);
                pmBankPaymentRecordMapper.updateById(record);
            }
        }
        return true;
    }

    boolean rejectPaymentTasks(Long userId, String username, List<Long> taskIds, ExpenseApprovalActionDTO dto) {
        List<Long> normalizedTaskIds = normalizeTaskIds(taskIds);
        if (normalizedTaskIds.isEmpty()) {
            throw new IllegalArgumentException("请选择待处理付款单");
        }
        String comment = trimToNull(dto == null ? null : dto.getComment());
        for (Long taskId : normalizedTaskIds) {
            ProcessDocumentTask task = requireOpenPaymentTask(taskId, userId);
            ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
            if (!DOCUMENT_STATUS_PENDING_PAYMENT.equals(trimToNull(instance.getStatus()))) {
                throw new IllegalStateException("当前付款任务不在可驳回状态");
            }
            expenseWorkflowRuntimeSupport.rejectPendingTask(instance, task, userId, username, comment, null);
            expenseRelationWriteOffService.voidPendingWriteOffs(instance.getDocumentCode());
            PmBankPaymentRecord record = recordSupport.findLatestBankPaymentRecord(instance.getDocumentCode());
            if (record != null) {
                record.setLastErrorMessage(firstNonBlank(comment, "付款驳回"));
                record.setReceiptStatus(RECEIPT_STATUS_FAILED);
                pmBankPaymentRecordMapper.updateById(record);
            }
        }
        return true;
    }

    ExpenseDocumentDetailVO completePaymentTaskInternal(
            Long userId,
            String username,
            ProcessDocumentTask task,
            ProcessDocumentInstance instance,
            String comment,
            boolean manualPaid,
            LocalDateTime paidAt
    ) {
        PmBankPaymentRecord record = recordSupport.findLatestBankPaymentRecord(instance.getDocumentCode());
        if (record == null) {
            SystemCompanyBankAccount account = recordSupport.findActiveBankAccountForDocument(instance, false);
            record = recordSupport.findOrCreateBankPaymentRecord(task, instance, account);
            if (account != null) {
                record.setCompanyBankAccountId(account.getId());
                record.setBankProvider(BANK_PROVIDER_CMB);
                record.setBankChannel(BANK_CHANNEL_CMB_CLOUD);
            } else if (manualPaid) {
                applyManualPaymentChannel(record, null);
            }
        } else if (manualPaid) {
            applyManualPaymentChannel(record, null);
        }
        record.setManualPaid(manualPaid ? 1 : 0);
        record.setPaidAt(paidAt == null ? LocalDateTime.now() : paidAt);
        if (trimToNull(record.getReceiptStatus()) == null) {
            record.setReceiptStatus(RECEIPT_STATUS_PENDING);
        }
        record.setLastErrorMessage(null);
        recordSupport.saveBankPaymentRecord(record);

        expenseWorkflowRuntimeSupport.completePaymentRuntime(
                instance,
                task,
                userId,
                username,
                comment,
                manualPaid,
                record.getPaidAt()
        );

        String finalStatus = trimToNull(expenseDocumentReadSupport.requireDocument(instance.getDocumentCode()).getStatus());
        if (isEffectiveApprovedStatus(finalStatus)) {
            expenseRelationWriteOffService.finalizeEffectiveWriteOffs(instance.getDocumentCode());
        }
        return expenseDocumentReadSupport.buildDocumentDetail(
                expenseDocumentReadSupport.requireDocument(instance.getDocumentCode())
        );
    }

    private void applyManualPaymentChannel(PmBankPaymentRecord record, SystemCompanyBankAccount account) {
        if (record == null) {
            return;
        }
        if (account != null) {
            record.setCompanyBankAccountId(account.getId());
            record.setBankProvider(BANK_PROVIDER_CMB);
            record.setBankChannel(BANK_CHANNEL_CMB_CLOUD);
            return;
        }
        if (trimToNull(record.getBankProvider()) == null) {
            record.setBankProvider(BANK_PROVIDER_MANUAL);
        }
        if (trimToNull(record.getBankChannel()) == null) {
            record.setBankChannel(BANK_CHANNEL_MANUAL_CONFIRM);
        }
    }

    private List<Long> normalizeTaskIds(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return List.of();
        }
        return taskIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<PaymentTaskContext> validateExportablePaymentTasks(Long userId, List<Long> taskIds) {
        List<Long> normalizedTaskIds = normalizeTaskIds(taskIds);
        if (normalizedTaskIds.isEmpty()) {
            throw new IllegalArgumentException("请选择付款单");
        }
        return normalizedTaskIds.stream()
                .map(taskId -> {
                    ProcessDocumentTask task = requireAccessiblePaymentTask(taskId, userId);
                    ProcessDocumentInstance instance = expenseDocumentReadSupport.requireDocument(task.getDocumentCode());
                    String status = trimToNull(instance.getStatus());
                    if (!isExportableStatus(status)) {
                        throw new IllegalStateException("当前付款任务不在可导出状态");
                    }
                    return new PaymentTaskContext(task, instance);
                })
                .toList();
    }

    private boolean isExportableStatus(String status) {
        return DOCUMENT_STATUS_PENDING_PAYMENT.equals(status)
                || DOCUMENT_STATUS_PAYING.equals(status)
                || DOCUMENT_STATUS_PAYMENT_COMPLETED.equals(status)
                || DOCUMENT_STATUS_PAYMENT_FINISHED.equals(status)
                || DOCUMENT_STATUS_PAYMENT_EXCEPTION.equals(status);
    }

    private boolean isVoidableStatus(String status) {
        return DOCUMENT_STATUS_PAYING.equals(status)
                || DOCUMENT_STATUS_PAYMENT_COMPLETED.equals(status)
                || DOCUMENT_STATUS_PAYMENT_EXCEPTION.equals(status);
    }

    private void reopenPaymentTaskIfNeeded(ProcessDocumentTask task) {
        if (TASK_STATUS_PENDING.equals(task.getStatus()) || TASK_STATUS_PAUSED.equals(task.getStatus())) {
            return;
        }
        task.setStatus(TASK_STATUS_PENDING);
        task.setHandledAt(null);
        task.setActionComment(null);
        processDocumentTaskMapper.updateById(task);
    }

    private String buildVoidComment(String targetStatus) {
        return DOCUMENT_STATUS_PAYING.equals(targetStatus) ? "作废后返回支付中" : "作废后返回待支付";
    }

    private ProcessDocumentTask requireAccessiblePaymentTask(Long taskId, Long userId) {
        ProcessDocumentTask task = processDocumentTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalStateException("付款任务不存在");
        }
        if (!Objects.equals(task.getAssigneeUserId(), userId)) {
            throw new IllegalStateException("当前用户无法处理该付款任务");
        }
        if (!NODE_TYPE_PAYMENT.equals(trimToNull(task.getNodeType()))) {
            throw new IllegalStateException("当前任务不是付款任务");
        }
        return task;
    }

    private ProcessDocumentTask requireOpenPaymentTask(Long taskId, Long userId) {
        ProcessDocumentTask task = requireAccessiblePaymentTask(taskId, userId);
        if (!TASK_STATUS_PENDING.equals(task.getStatus()) && !TASK_STATUS_PAUSED.equals(task.getStatus())) {
            throw new IllegalStateException("付款任务已被处理");
        }
        return task;
    }

    private ExpenseDocumentDetailVO pushPaymentTaskToBank(
            Long userId,
            String username,
            ProcessDocumentTask task,
            ProcessDocumentInstance instance,
            boolean retrying
    ) {
        SystemCompanyBankAccount account = recordSupport.findActiveBankAccountForDocument(instance);
        LocalDateTime now = LocalDateTime.now();
        String pushRequestNo = buildBankPushRequestNo(instance.getDocumentCode());
        PmBankPaymentRecord record = recordSupport.findOrCreateBankPaymentRecord(task, instance, account);
        record.setPushRequestNo(pushRequestNo);
        record.setManualPaid(0);
        record.setReceiptStatus(RECEIPT_STATUS_PENDING);
        record.setPushResultJson(writeJson(Map.of(
                "accepted", true,
                "retry", retrying,
                "message", "已推送至银行直连通道"
        )));
        record.setLastErrorMessage(null);
        recordSupport.saveBankPaymentRecord(record);

        expenseWorkflowRuntimeSupport.markPaymentStarted(
                instance,
                task,
                userId,
                username,
                retrying,
                account.getId(),
                buildCompanyBankAccountName(account),
                pushRequestNo
        );

        account.setDirectConnectLastSyncAt(now);
        account.setDirectConnectLastSyncStatus("PUSHED");
        account.setDirectConnectLastErrorMsg(null);
        systemCompanyBankAccountMapper.updateById(account);
        return expenseDocumentReadSupport.buildDocumentDetail(
                expenseDocumentReadSupport.requireDocument(instance.getDocumentCode())
        );
    }

    private record PaymentTaskContext(ProcessDocumentTask task, ProcessDocumentInstance instance) {
    }
}
