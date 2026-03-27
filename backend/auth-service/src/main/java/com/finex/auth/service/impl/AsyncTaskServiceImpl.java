package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.InvoiceTaskSubmitDTO;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class AsyncTaskServiceImpl implements AsyncTaskService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final DownloadRecordMapper downloadRecordMapper;
    private final NotificationRecordMapper notificationRecordMapper;
    private final AsyncTaskWorker asyncTaskWorker;

    @Override
    public AsyncTaskSubmitResultVO submitInvoiceExport(Long userId) {
        DownloadRecord downloadRecord = new DownloadRecord();
        downloadRecord.setUserId(userId);
        downloadRecord.setFileName("发票列表导出-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx");
        downloadRecord.setBusinessType("发票列表导出");
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
                downloadRecord.getId()
        );
        asyncTaskWorker.runExportTask(task.getId());
        return toSubmitResult(task, "导出任务已提交");
    }

    @Override
    public AsyncTaskSubmitResultVO submitInvoiceVerify(Long userId, InvoiceTaskSubmitDTO dto) {
        String businessKey = AsyncTaskSupport.buildInvoiceBusinessKey(dto.getCode(), dto.getNumber());
        AsyncTaskRecord existing = findActiveTask(userId, AsyncTaskSupport.TASK_TYPE_INVOICE_VERIFY, businessKey);
        if (existing != null) {
            return toSubmitResult(existing, "该发票已有进行中的验真任务");
        }

        AsyncTaskRecord task = createTask(
                userId,
                AsyncTaskSupport.TASK_TYPE_INVOICE_VERIFY,
                AsyncTaskSupport.BUSINESS_TYPE_INVOICE,
                businessKey,
                "发票验真",
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
            return toSubmitResult(existing, "该发票已有进行中的 OCR 任务");
        }

        AsyncTaskRecord task = createTask(
                userId,
                AsyncTaskSupport.TASK_TYPE_INVOICE_OCR,
                AsyncTaskSupport.BUSINESS_TYPE_INVOICE,
                businessKey,
                "发票 OCR 识别",
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

    private AsyncTaskRecord createTask(
            Long userId,
            String taskType,
            String businessType,
            String businessKey,
            String displayName,
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
        task.setResultMessage("任务已入队");
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
}
