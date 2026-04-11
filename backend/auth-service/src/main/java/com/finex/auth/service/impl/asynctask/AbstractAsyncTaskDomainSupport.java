package com.finex.auth.service.impl.asynctask;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.NotificationRecordMapper;
import com.finex.auth.service.impl.AsyncTaskWorker;
import com.finex.auth.support.AsyncTaskSupport;

import java.time.format.DateTimeFormatter;
import java.util.List;

public abstract class AbstractAsyncTaskDomainSupport {

    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    protected static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final DownloadRecordMapper downloadRecordMapper;
    private final NotificationRecordMapper notificationRecordMapper;
    private final AsyncTaskWorker asyncTaskWorker;
    private final ObjectMapper objectMapper;

    protected AbstractAsyncTaskDomainSupport(
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            DownloadRecordMapper downloadRecordMapper,
            NotificationRecordMapper notificationRecordMapper,
            AsyncTaskWorker asyncTaskWorker,
            ObjectMapper objectMapper
    ) {
        this.asyncTaskRecordMapper = asyncTaskRecordMapper;
        this.downloadRecordMapper = downloadRecordMapper;
        this.notificationRecordMapper = notificationRecordMapper;
        this.asyncTaskWorker = asyncTaskWorker;
        this.objectMapper = objectMapper;
    }

    protected AsyncTaskRecordMapper asyncTaskRecordMapper() {
        return asyncTaskRecordMapper;
    }

    protected DownloadRecordMapper downloadRecordMapper() {
        return downloadRecordMapper;
    }

    protected NotificationRecordMapper notificationRecordMapper() {
        return notificationRecordMapper;
    }

    protected AsyncTaskWorker asyncTaskWorker() {
        return asyncTaskWorker;
    }

    protected ObjectMapper objectMapper() {
        return objectMapper;
    }

    protected AsyncTaskRecord createTask(
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

    protected AsyncTaskRecord findActiveTask(Long userId, String taskType, String businessKey) {
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

    protected AsyncTaskSubmitResultVO toSubmitResult(AsyncTaskRecord task, String message) {
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
