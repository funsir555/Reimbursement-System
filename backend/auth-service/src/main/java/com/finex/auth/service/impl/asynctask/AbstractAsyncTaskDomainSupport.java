// 业务域：异步任务
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 异步提交、查询和通知相关接口，下游会继续协调 任务记录、状态更新和通知消息。
// 风险提醒：改坏后最容易影响 任务重复提交、异步状态不准确和结果回传。

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

/**
 * AbstractAsyncTaskDomainSupport：领域规则支撑类。
 * 承接 异步任务的核心业务规则。
 * 改这里时，要特别关注 任务重复提交、异步状态不准确和结果回传是否会被一起带坏。
 */
public abstract class AbstractAsyncTaskDomainSupport {

    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    protected static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final DownloadRecordMapper downloadRecordMapper;
    private final NotificationRecordMapper notificationRecordMapper;
    private final AsyncTaskWorker asyncTaskWorker;
    private final ObjectMapper objectMapper;

    /**
     * 初始化这个类所需的依赖组件。
     */
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

    /**
     * 处理异步任务中的这一步。
     */
    protected AsyncTaskRecordMapper asyncTaskRecordMapper() {
        return asyncTaskRecordMapper;
    }

    /**
     * 下载RecordMapper。
     */
    protected DownloadRecordMapper downloadRecordMapper() {
        return downloadRecordMapper;
    }

    /**
     * 处理异步任务中的这一步。
     */
    protected NotificationRecordMapper notificationRecordMapper() {
        return notificationRecordMapper;
    }

    /**
     * 处理异步任务中的这一步。
     */
    protected AsyncTaskWorker asyncTaskWorker() {
        return asyncTaskWorker;
    }

    /**
     * 处理异步任务中的这一步。
     */
    protected ObjectMapper objectMapper() {
        return objectMapper;
    }

    /**
     * 创建任务。
     */
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

    /**
     * 查询Active任务。
     */
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

    /**
     * 处理异步任务中的这一步。
     */
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
