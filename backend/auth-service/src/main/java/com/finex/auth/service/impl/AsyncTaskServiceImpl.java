// 业务域：异步任务
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 异步提交、查询和通知相关接口，下游会继续协调 任务记录、状态更新和通知消息。
// 风险提醒：改坏后最容易影响 任务重复提交、异步状态不准确和结果回传。

package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.ExpenseExportSubmitDTO;
import com.finex.auth.dto.InvoiceTaskSubmitDTO;
import com.finex.auth.dto.NotificationItemVO;
import com.finex.auth.dto.NotificationSummaryVO;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.NotificationRecordMapper;
import com.finex.auth.service.AsyncTaskService;
import com.finex.auth.service.impl.asynctask.AsyncTaskNotificationDomainSupport;
import com.finex.auth.service.impl.asynctask.AsyncTaskSubmissionDomainSupport;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AsyncTaskServiceImpl：service 入口实现。
 * 接住上层请求，并把 异步任务相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 任务重复提交、异步状态不准确和结果回传是否会被一起带坏。
 */
@Service
public class AsyncTaskServiceImpl implements AsyncTaskService {

    private final AsyncTaskSubmissionDomainSupport asyncTaskSubmissionDomainSupport;
    private final AsyncTaskNotificationDomainSupport asyncTaskNotificationDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public AsyncTaskServiceImpl(
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            DownloadRecordMapper downloadRecordMapper,
            NotificationRecordMapper notificationRecordMapper,
            AsyncTaskWorker asyncTaskWorker,
            ObjectMapper objectMapper
    ) {
        this.asyncTaskSubmissionDomainSupport = new AsyncTaskSubmissionDomainSupport(
                asyncTaskRecordMapper,
                downloadRecordMapper,
                notificationRecordMapper,
                asyncTaskWorker,
                objectMapper
        );
        this.asyncTaskNotificationDomainSupport = new AsyncTaskNotificationDomainSupport(
                asyncTaskRecordMapper,
                downloadRecordMapper,
                notificationRecordMapper,
                asyncTaskWorker,
                objectMapper
        );
    }

    /**
     * 提交发票Export。
     */
    @Override
    public AsyncTaskSubmitResultVO submitInvoiceExport(Long userId) {
        return asyncTaskSubmissionDomainSupport.submitInvoiceExport(userId);
    }

    /**
     * 提交报销单Export。
     */
    @Override
    public AsyncTaskSubmitResultVO submitExpenseExport(Long userId, ExpenseExportSubmitDTO dto) {
        return asyncTaskSubmissionDomainSupport.submitExpenseExport(userId, dto);
    }

    /**
     * 提交发票Verify。
     */
    @Override
    public AsyncTaskSubmitResultVO submitInvoiceVerify(Long userId, InvoiceTaskSubmitDTO dto) {
        return asyncTaskSubmissionDomainSupport.submitInvoiceVerify(userId, dto);
    }

    /**
     * 提交发票Ocr。
     */
    @Override
    public AsyncTaskSubmitResultVO submitInvoiceOcr(Long userId, InvoiceTaskSubmitDTO dto) {
        return asyncTaskSubmissionDomainSupport.submitInvoiceOcr(userId, dto);
    }

    /**
     * 获取通知汇总。
     */
    @Override
    public NotificationSummaryVO getNotificationSummary(Long userId) {
        return asyncTaskNotificationDomainSupport.getNotificationSummary(userId);
    }

    /**
     * 查询通知列表。
     */
    @Override
    public List<NotificationItemVO> listNotifications(Long userId) {
        return asyncTaskNotificationDomainSupport.listNotifications(userId);
    }

    /**
     * 处理异步任务中的这一步。
     */
    @Override
    public boolean markNotificationRead(Long userId, Long notificationId) {
        return asyncTaskNotificationDomainSupport.markNotificationRead(userId, notificationId);
    }

    /**
     * 处理异步任务中的这一步。
     */
    @Override
    public boolean markAllNotificationsRead(Long userId) {
        return asyncTaskNotificationDomainSupport.markAllNotificationsRead(userId);
    }
}
