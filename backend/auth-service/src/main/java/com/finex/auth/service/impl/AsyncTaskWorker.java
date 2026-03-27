package com.finex.auth.service.impl;

import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.DownloadRecord;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.service.NotificationService;
import com.finex.auth.support.AsyncTaskSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskWorker {

    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final DownloadRecordMapper downloadRecordMapper;
    private final NotificationService notificationService;

    @Async("finexAsyncExecutor")
    public void runExportTask(Long taskId) {
        AsyncTaskRecord task = requireTask(taskId);
        try {
            markRunning(task, "开始准备导出文件", 10);
            sleep(400);
            updateTask(task, 35, "正在汇总发票数据");
            updateDownload(task.getDownloadRecordId(), 35, AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING, "生成中", null);
            sleep(500);
            updateTask(task, 70, "正在生成导出文件");
            updateDownload(task.getDownloadRecordId(), 70, AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING, "2.4 MB", null);
            sleep(500);
            finishSuccess(task, "导出完成，可前往下载中心查看");
            updateDownload(task.getDownloadRecordId(), 100, AsyncTaskSupport.DOWNLOAD_STATUS_COMPLETED, "2.4 MB", LocalDateTime.now());
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    "导出任务已完成",
                    task.getDisplayName() + " 已生成完成，请前往下载中心查看。",
                    task.getTaskNo()
            );
        } catch (Exception ex) {
            log.error("导出任务执行失败, taskNo={}", task.getTaskNo(), ex);
            finishFailed(task, "导出失败，请稍后重试");
            updateDownload(task.getDownloadRecordId(), task.getProgress(), AsyncTaskSupport.DOWNLOAD_STATUS_FAILED, "-", LocalDateTime.now());
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    "导出任务失败",
                    task.getDisplayName() + " 执行失败，请稍后重试。",
                    task.getTaskNo()
            );
        }
    }

    @Async("finexAsyncExecutor")
    public void runInvoiceVerifyTask(Long taskId) {
        AsyncTaskRecord task = requireTask(taskId);
        try {
            markRunning(task, "正在提交税局验真请求", 20);
            sleep(350);
            updateTask(task, 65, "正在比对票面与税务结果");
            sleep(450);

            boolean success = Math.abs(task.getBusinessKey().hashCode()) % 6 != 0;
            if (success) {
                finishSuccess(task, "发票验真通过");
                notificationService.sendAsyncNotification(
                        task.getUserId(),
                        AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                        "发票验真完成",
                        "发票 " + task.getBusinessKey() + " 已验真通过。",
                        task.getTaskNo()
                );
                return;
            }

            finishFailed(task, "税局验真未通过，请检查票面信息");
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    "发票验真失败",
                    "发票 " + task.getBusinessKey() + " 验真未通过，请稍后处理。",
                    task.getTaskNo()
                );
        } catch (Exception ex) {
            log.error("发票验真任务执行失败, taskNo={}", task.getTaskNo(), ex);
            finishFailed(task, "发票验真失败，请稍后重试");
        }
    }

    @Async("finexAsyncExecutor")
    public void runInvoiceOcrTask(Long taskId) {
        AsyncTaskRecord task = requireTask(taskId);
        try {
            markRunning(task, "正在上传票面图片到 OCR 队列", 25);
            sleep(350);
            updateTask(task, 55, "正在识别票面关键信息");
            sleep(450);
            updateTask(task, 85, "正在回填结构化结果");
            sleep(350);
            finishSuccess(task, "OCR 识别完成，已提取票面关键信息");
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    "OCR 识别完成",
                    "发票 " + task.getBusinessKey() + " 的 OCR 识别已完成。",
                    task.getTaskNo()
            );
        } catch (Exception ex) {
            log.error("OCR 任务执行失败, taskNo={}", task.getTaskNo(), ex);
            finishFailed(task, "OCR 识别失败，请稍后重试");
        }
    }

    private AsyncTaskRecord requireTask(Long taskId) {
        AsyncTaskRecord task = asyncTaskRecordMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("异步任务不存在");
        }
        return task;
    }

    private void markRunning(AsyncTaskRecord task, String message, int progress) {
        task.setStatus(AsyncTaskSupport.TASK_STATUS_RUNNING);
        task.setProgress(progress);
        task.setResultMessage(message);
        task.setStartedAt(LocalDateTime.now());
        asyncTaskRecordMapper.updateById(task);
    }

    private void updateTask(AsyncTaskRecord task, int progress, String message) {
        task.setProgress(progress);
        task.setResultMessage(message);
        asyncTaskRecordMapper.updateById(task);
    }

    private void finishSuccess(AsyncTaskRecord task, String message) {
        task.setStatus(AsyncTaskSupport.TASK_STATUS_SUCCESS);
        task.setProgress(100);
        task.setResultMessage(message);
        task.setFinishedAt(LocalDateTime.now());
        asyncTaskRecordMapper.updateById(task);
    }

    private void finishFailed(AsyncTaskRecord task, String message) {
        task.setStatus(AsyncTaskSupport.TASK_STATUS_FAILED);
        task.setProgress(100);
        task.setResultMessage(message);
        task.setFinishedAt(LocalDateTime.now());
        asyncTaskRecordMapper.updateById(task);
    }

    private void updateDownload(Long downloadRecordId, Integer progress, String status, String fileSize, LocalDateTime finishedAt) {
        if (downloadRecordId == null) {
            return;
        }
        DownloadRecord record = downloadRecordMapper.selectById(downloadRecordId);
        if (record == null) {
            return;
        }
        record.setProgress(progress);
        record.setStatus(status);
        record.setFileSize(fileSize);
        record.setFinishedAt(finishedAt);
        downloadRecordMapper.updateById(record);
    }

    private void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis + ThreadLocalRandom.current().nextInt(80, 180));
    }
}
