package com.finex.auth.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class AsyncTaskSupport {

    public static final String TASK_TYPE_EXPORT = "EXPORT";
    public static final String TASK_TYPE_INVOICE_VERIFY = "INVOICE_VERIFY";
    public static final String TASK_TYPE_INVOICE_OCR = "INVOICE_OCR";

    public static final String TASK_STATUS_PENDING = "PENDING";
    public static final String TASK_STATUS_RUNNING = "RUNNING";
    public static final String TASK_STATUS_SUCCESS = "SUCCESS";
    public static final String TASK_STATUS_FAILED = "FAILED";

    public static final String BUSINESS_TYPE_INVOICE_EXPORT = "INVOICE_EXPORT";
    public static final String BUSINESS_TYPE_EXPENSE_EXPORT = "EXPENSE_EXPORT";
    public static final String BUSINESS_TYPE_INVOICE = "INVOICE";

    public static final String EXPENSE_EXPORT_SCENE_MY_EXPENSES = "MY_EXPENSES";
    public static final String EXPENSE_EXPORT_SCENE_PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String EXPENSE_EXPORT_SCENE_DOCUMENT_QUERY = "DOCUMENT_QUERY";
    public static final String EXPENSE_EXPORT_SCENE_OUTSTANDING = "OUTSTANDING";

    public static final String DOWNLOAD_STATUS_DOWNLOADING = "DOWNLOADING";
    public static final String DOWNLOAD_STATUS_COMPLETED = "COMPLETED";
    public static final String DOWNLOAD_STATUS_FAILED = "FAILED";

    public static final String NOTIFICATION_STATUS_UNREAD = "UNREAD";
    public static final String NOTIFICATION_STATUS_READ = "READ";
    public static final String NOTIFICATION_TYPE_TASK = "TASK";

    private static final DateTimeFormatter TASK_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private AsyncTaskSupport() {
    }

    public static String buildTaskNo(String taskType) {
        String prefix = switch (taskType) {
            case TASK_TYPE_INVOICE_VERIFY -> "VER";
            case TASK_TYPE_INVOICE_OCR -> "OCR";
            default -> "EXP";
        };
        return prefix
                + LocalDateTime.now().format(TASK_NO_TIME_FORMATTER)
                + ThreadLocalRandom.current().nextInt(100, 999);
    }

    public static String buildInvoiceBusinessKey(String code, String number) {
        return code + "#" + number;
    }

    public static boolean isActive(String status) {
        return TASK_STATUS_PENDING.equalsIgnoreCase(status) || TASK_STATUS_RUNNING.equalsIgnoreCase(status);
    }
}
