package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentInstance;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 付款运行态的状态和动作编码，以及撤销/回退时的目标状态判定。
 * 不修改单据、任务或日志；历史日志仅在需要判定已完成/异常付款时由调用方提供。
 */
final class ExpensePaymentStatusPolicy {

    static final String DOCUMENT_STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    static final String DOCUMENT_STATUS_PAYING = "PAYING";
    static final String DOCUMENT_STATUS_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    static final String DOCUMENT_STATUS_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";

    static final String LOG_PAYMENT_PENDING = "PAYMENT_PENDING";
    static final String LOG_PAYMENT_START = "PAYMENT_START";
    static final String LOG_PAYMENT_COMPLETE = "PAYMENT_COMPLETE";
    static final String LOG_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";

    private ExpensePaymentStatusPolicy() {
    }

    static String normalizeRevertTargetStatus(String status) {
        String normalized = trimToNull(status);
        if (DOCUMENT_STATUS_PAYING.equals(normalized)) {
            return DOCUMENT_STATUS_PAYING;
        }
        return DOCUMENT_STATUS_PENDING_PAYMENT;
    }

    static String resolveVoidTargetStatus(
            ProcessDocumentInstance instance,
            Function<String, List<ProcessDocumentActionLog>> actionLogReader
    ) {
        String currentStatus = trimToNull(instance == null ? null : instance.getStatus());
        if (DOCUMENT_STATUS_PAYING.equals(currentStatus)) {
            return DOCUMENT_STATUS_PENDING_PAYMENT;
        }
        if (!DOCUMENT_STATUS_PAYMENT_COMPLETED.equals(currentStatus)
                && !DOCUMENT_STATUS_PAYMENT_EXCEPTION.equals(currentStatus)) {
            return DOCUMENT_STATUS_PENDING_PAYMENT;
        }
        String anchorAction = DOCUMENT_STATUS_PAYMENT_COMPLETED.equals(currentStatus)
                ? LOG_PAYMENT_COMPLETE : LOG_PAYMENT_EXCEPTION;
        List<ProcessDocumentActionLog> logs = actionLogReader.apply(instance.getDocumentCode());
        int anchorIndex = findLastPaymentActionIndex(logs, anchorAction);
        if (anchorIndex < 0) {
            return DOCUMENT_STATUS_PENDING_PAYMENT;
        }
        for (int index = anchorIndex - 1; index >= 0; index--) {
            String actionType = trimToNull(logs.get(index).getActionType());
            if (LOG_PAYMENT_START.equals(actionType)) {
                return DOCUMENT_STATUS_PAYING;
            }
            if (LOG_PAYMENT_PENDING.equals(actionType)
                    || LOG_PAYMENT_COMPLETE.equals(actionType)
                    || LOG_PAYMENT_EXCEPTION.equals(actionType)) {
                return DOCUMENT_STATUS_PENDING_PAYMENT;
            }
        }
        return DOCUMENT_STATUS_PENDING_PAYMENT;
    }

    private static int findLastPaymentActionIndex(List<ProcessDocumentActionLog> logs, String actionType) {
        if (logs == null || logs.isEmpty() || trimToNull(actionType) == null) {
            return -1;
        }
        for (int index = logs.size() - 1; index >= 0; index--) {
            if (Objects.equals(trimToNull(logs.get(index).getActionType()), actionType)) {
                return index;
            }
        }
        return -1;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
