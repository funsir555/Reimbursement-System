package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.ExpenseExportSubmitDTO;
import com.finex.auth.dto.InvoiceTaskSubmitDTO;
import com.finex.auth.dto.NotificationItemVO;
import com.finex.auth.dto.NotificationSummaryVO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.DownloadRecord;
import com.finex.auth.entity.NotificationRecord;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.NotificationRecordMapper;
import com.finex.auth.service.AsyncTaskService;
import com.finex.auth.support.AsyncTaskSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AsyncTaskServiceImpl implements AsyncTaskService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final DownloadRecordMapper downloadRecordMapper;
    private final NotificationRecordMapper notificationRecordMapper;
    private final AsyncTaskWorker asyncTaskWorker;
    private final ObjectMapper objectMapper;

    @Override
    public AsyncTaskSubmitResultVO submitInvoiceExport(Long userId) {
        DownloadRecord downloadRecord = new DownloadRecord();
        downloadRecord.setUserId(userId);
        downloadRecord.setFileName("发票列表导出-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx");
        downloadRecord.setBusinessType("发票导出");
        downloadRecord.setStatus(AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING);
        downloadRecord.setProgress(0);
        downloadRecord.setFileSize("生成中");
        downloadRecordMapper.insert(downloadRecord);

        AsyncTaskRecord task = createTask(
                userId,
                AsyncTaskSupport.TASK_TYPE_EXPORT,
                AsyncTaskSupport.BUSINESS_TYPE_INVOICE_EXPORT,
                "invoice-list",
                "发票列表导出",
                null,
                downloadRecord.getId()
        );
        asyncTaskWorker.runExportTask(task.getId());
        return toSubmitResult(task, "导出任务已提交，请到下载中心查看进度");
    }

    @Override
    public AsyncTaskSubmitResultVO submitExpenseExport(Long userId, ExpenseExportSubmitDTO dto) {
        ExpenseExportSubmitDTO payload = normalizeExpenseExportPayload(dto);
        DownloadRecord downloadRecord = new DownloadRecord();
        downloadRecord.setUserId(userId);
        downloadRecord.setFileName(resolveExpenseExportFileName(payload));
        downloadRecord.setBusinessType(resolveExpenseExportBusinessType(payload));
        downloadRecord.setStatus(AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING);
        downloadRecord.setProgress(0);
        downloadRecord.setFileSize("生成中");
        downloadRecordMapper.insert(downloadRecord);

        AsyncTaskRecord task = createTask(
                userId,
                AsyncTaskSupport.TASK_TYPE_EXPORT,
                AsyncTaskSupport.BUSINESS_TYPE_EXPENSE_EXPORT,
                resolveExpenseExportBusinessKey(payload),
                resolveExpenseExportDisplayName(payload),
                writeResultPayload(payload),
                downloadRecord.getId()
        );
        asyncTaskWorker.runExpenseExportTask(task.getId());
        return toSubmitResult(task, "导出任务已提交，请到下载中心查看进度");
    }

    @Override
    public AsyncTaskSubmitResultVO submitInvoiceVerify(Long userId, InvoiceTaskSubmitDTO dto) {
        String businessKey = AsyncTaskSupport.buildInvoiceBusinessKey(dto.getCode(), dto.getNumber());
        AsyncTaskRecord existing = findActiveTask(userId, AsyncTaskSupport.TASK_TYPE_INVOICE_VERIFY, businessKey);
        if (existing != null) {
            return toSubmitResult(existing, "当前已有相同发票验真任务正在执行，请稍后查看结果");
        }

        AsyncTaskRecord task = createTask(
                userId,
                AsyncTaskSupport.TASK_TYPE_INVOICE_VERIFY,
                AsyncTaskSupport.BUSINESS_TYPE_INVOICE,
                businessKey,
                "发票验真",
                null,
                null
        );
        asyncTaskWorker.runInvoiceVerifyTask(task.getId());
        return toSubmitResult(task, "发票验真任务已提交");
    }

    @Override
    public AsyncTaskSubmitResultVO submitInvoiceOcr(Long userId, InvoiceTaskSubmitDTO dto) {
        String businessKey = AsyncTaskSupport.buildInvoiceBusinessKey(dto.getCode(), dto.getNumber());
        AsyncTaskRecord existing = findActiveTask(userId, AsyncTaskSupport.TASK_TYPE_INVOICE_OCR, businessKey);
        if (existing != null) {
            return toSubmitResult(existing, "当前已有相同发票 OCR 任务正在执行，请稍后查看结果");
        }

        AsyncTaskRecord task = createTask(
                userId,
                AsyncTaskSupport.TASK_TYPE_INVOICE_OCR,
                AsyncTaskSupport.BUSINESS_TYPE_INVOICE,
                businessKey,
                "发票 OCR 识别",
                null,
                null
        );
        asyncTaskWorker.runInvoiceOcrTask(task.getId());
        return toSubmitResult(task, "发票 OCR 任务已提交");
    }

    @Override
    public NotificationSummaryVO getNotificationSummary(Long userId) {
        Long unreadCount = notificationRecordMapper.selectCount(
                Wrappers.<NotificationRecord>lambdaQuery()
                        .eq(NotificationRecord::getUserId, userId)
                        .eq(NotificationRecord::getStatus, AsyncTaskSupport.NOTIFICATION_STATUS_UNREAD)
        );

        List<NotificationRecord> latestRecords = notificationRecordMapper.selectList(
                Wrappers.<NotificationRecord>lambdaQuery()
                        .eq(NotificationRecord::getUserId, userId)
                        .orderByDesc(NotificationRecord::getCreatedAt, NotificationRecord::getId)
                        .last("limit 1")
        );

        NotificationSummaryVO summary = new NotificationSummaryVO();
        summary.setUnreadCount(unreadCount == null ? 0L : unreadCount);
        if (!latestRecords.isEmpty()) {
            NotificationRecord latest = latestRecords.get(0);
            summary.setLatestTitle(latest.getTitle());
            summary.setLatestContent(latest.getContent());
            summary.setLatestCreatedAt(latest.getCreatedAt() == null ? "" : latest.getCreatedAt().format(DATE_TIME_FORMATTER));
        }
        return summary;
    }

    @Override
    public List<NotificationItemVO> listNotifications(Long userId) {
        return notificationRecordMapper.selectList(
                        Wrappers.<NotificationRecord>lambdaQuery()
                                .eq(NotificationRecord::getUserId, userId)
                                .orderByDesc(NotificationRecord::getCreatedAt, NotificationRecord::getId)
                                .last("limit 50")
                ).stream()
                .map(this::toNotificationItem)
                .toList();
    }

    @Override
    public boolean markNotificationRead(Long userId, Long notificationId) {
        NotificationRecord record = notificationRecordMapper.selectOne(
                Wrappers.<NotificationRecord>lambdaQuery()
                        .eq(NotificationRecord::getId, notificationId)
                        .eq(NotificationRecord::getUserId, userId)
                        .last("limit 1")
        );
        if (record == null) {
            throw new IllegalArgumentException("通知不存在");
        }
        if (AsyncTaskSupport.NOTIFICATION_STATUS_UNREAD.equalsIgnoreCase(record.getStatus())) {
            NotificationRecord update = new NotificationRecord();
            update.setId(record.getId());
            update.setStatus(AsyncTaskSupport.NOTIFICATION_STATUS_READ);
            update.setReadAt(LocalDateTime.now());
            notificationRecordMapper.updateById(update);
        }
        return true;
    }

    @Override
    public boolean markAllNotificationsRead(Long userId) {
        NotificationRecord update = new NotificationRecord();
        update.setStatus(AsyncTaskSupport.NOTIFICATION_STATUS_READ);
        update.setReadAt(LocalDateTime.now());
        notificationRecordMapper.update(
                update,
                Wrappers.<NotificationRecord>lambdaUpdate()
                        .eq(NotificationRecord::getUserId, userId)
                        .eq(NotificationRecord::getStatus, AsyncTaskSupport.NOTIFICATION_STATUS_UNREAD)
        );
        return true;
    }

    private ExpenseExportSubmitDTO normalizeExpenseExportPayload(ExpenseExportSubmitDTO dto) {
        if (dto == null || dto.getScene() == null || dto.getScene().isBlank()) {
            throw new IllegalArgumentException("导出场景不能为空");
        }
        ExpenseExportSubmitDTO payload = new ExpenseExportSubmitDTO();
        payload.setScene(dto.getScene().trim().toUpperCase(Locale.ROOT));
        payload.setDocumentCodes(normalizeDocumentCodes(dto.getDocumentCodes()));
        payload.setTaskIds(normalizeTaskIds(dto.getTaskIds()));
        payload.setKind(dto.getKind() == null ? null : dto.getKind().trim().toUpperCase(Locale.ROOT));
        switch (payload.getScene()) {
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_MY_EXPENSES,
                 AsyncTaskSupport.EXPENSE_EXPORT_SCENE_DOCUMENT_QUERY -> {
                if (payload.getDocumentCodes().isEmpty()) {
                    throw new IllegalArgumentException("当前没有可导出的单据");
                }
            }
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_PENDING_APPROVAL -> {
                if (payload.getTaskIds().isEmpty()) {
                    throw new IllegalArgumentException("当前没有可导出的审批任务");
                }
            }
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_OUTSTANDING -> {
                if (payload.getDocumentCodes().isEmpty()) {
                    throw new IllegalArgumentException("当前没有可导出的待处理单据");
                }
                if (!"LOAN".equals(payload.getKind()) && !"PREPAY_REPORT".equals(payload.getKind())) {
                    throw new IllegalArgumentException("待处理导出类型不合法");
                }
            }
            default -> throw new IllegalArgumentException("不支持的导出场景");
        }
        return payload;
    }

    private List<String> normalizeDocumentCodes(List<String> documentCodes) {
        if (documentCodes == null || documentCodes.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String documentCode : documentCodes) {
            if (documentCode != null && !documentCode.isBlank()) {
                normalized.add(documentCode.trim());
            }
        }
        return List.copyOf(normalized);
    }

    private List<Long> normalizeTaskIds(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return List.of();
        }
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long taskId : taskIds) {
            if (taskId != null) {
                normalized.add(taskId);
            }
        }
        return List.copyOf(normalized);
    }

    private String resolveExpenseExportFileName(ExpenseExportSubmitDTO payload) {
        return switch (payload.getScene()) {
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_MY_EXPENSES -> "我的报销-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx";
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_PENDING_APPROVAL -> "待我审批-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx";
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_DOCUMENT_QUERY -> "单据查询-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx";
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_OUTSTANDING -> (
                    "LOAN".equals(payload.getKind()) ? "待还款单据-" : "待核销单据-"
            ) + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx";
            default -> "报销导出-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx";
        };
    }

    private String resolveExpenseExportBusinessType(ExpenseExportSubmitDTO payload) {
        return switch (payload.getScene()) {
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_MY_EXPENSES -> "我的报销导出";
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_PENDING_APPROVAL -> "待我审批导出";
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_DOCUMENT_QUERY -> "单据查询导出";
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_OUTSTANDING -> "LOAN".equals(payload.getKind()) ? "待还款导出" : "待核销导出";
            default -> "报销导出";
        };
    }

    private String resolveExpenseExportDisplayName(ExpenseExportSubmitDTO payload) {
        return resolveExpenseExportBusinessType(payload);
    }

    private String resolveExpenseExportBusinessKey(ExpenseExportSubmitDTO payload) {
        return payload.getScene() + "#" + LocalDateTime.now().format(FILE_TIME_FORMATTER);
    }

    private String writeResultPayload(ExpenseExportSubmitDTO payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("导出任务参数序列化失败", ex);
        }
    }

    private AsyncTaskRecord createTask(
            Long userId,
            String taskType,
            String businessType,
            String businessKey,
            String displayName,
            String resultPayload,
            Long downloadRecordId
    ) {
        AsyncTaskRecord task = new AsyncTaskRecord();
        task.setTaskNo(AsyncTaskSupport.buildTaskNo(taskType));
        task.setUserId(userId);
        task.setTaskType(taskType);
        task.setBusinessType(businessType);
        task.setBusinessKey(businessKey);
        task.setDisplayName(displayName);
        task.setStatus(AsyncTaskSupport.TASK_STATUS_PENDING);
        task.setProgress(0);
        task.setResultMessage("任务已提交");
        task.setResultPayload(resultPayload);
        task.setDownloadRecordId(downloadRecordId);
        asyncTaskRecordMapper.insert(task);
        return task;
    }

    private AsyncTaskRecord findActiveTask(Long userId, String taskType, String businessKey) {
        List<AsyncTaskRecord> tasks = asyncTaskRecordMapper.selectList(
                Wrappers.<AsyncTaskRecord>lambdaQuery()
                        .eq(AsyncTaskRecord::getUserId, userId)
                        .eq(AsyncTaskRecord::getTaskType, taskType)
                        .eq(AsyncTaskRecord::getBusinessKey, businessKey)
                        .in(AsyncTaskRecord::getStatus, AsyncTaskSupport.TASK_STATUS_PENDING, AsyncTaskSupport.TASK_STATUS_RUNNING)
                        .orderByDesc(AsyncTaskRecord::getCreatedAt, AsyncTaskRecord::getId)
                        .last("limit 1")
        );
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    private AsyncTaskSubmitResultVO toSubmitResult(AsyncTaskRecord task, String message) {
        AsyncTaskSubmitResultVO result = new AsyncTaskSubmitResultVO();
        result.setTaskNo(task.getTaskNo());
        result.setTaskType(task.getTaskType());
        result.setBusinessType(task.getBusinessType());
        result.setStatus(task.getStatus());
        result.setMessage(message);
        result.setDownloadRecordId(task.getDownloadRecordId());
        return result;
    }

    private NotificationItemVO toNotificationItem(NotificationRecord record) {
        NotificationItemVO item = new NotificationItemVO();
        item.setId(record.getId());
        item.setTitle(record.getTitle());
        item.setContent(record.getContent());
        item.setType(record.getType());
        item.setStatus(record.getStatus());
        item.setRelatedTaskNo(record.getRelatedTaskNo());
        item.setCreatedAt(record.getCreatedAt() == null ? "" : record.getCreatedAt().format(DATE_TIME_FORMATTER));
        item.setReadAt(record.getReadAt() == null ? "" : record.getReadAt().format(DATE_TIME_FORMATTER));
        return item;
    }
}
