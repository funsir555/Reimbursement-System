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

@Service
public class AsyncTaskServiceImpl implements AsyncTaskService {

    private final AsyncTaskSubmissionDomainSupport asyncTaskSubmissionDomainSupport;
    private final AsyncTaskNotificationDomainSupport asyncTaskNotificationDomainSupport;

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

    @Override
    public AsyncTaskSubmitResultVO submitInvoiceExport(Long userId) {
        return asyncTaskSubmissionDomainSupport.submitInvoiceExport(userId);
    }

    @Override
    public AsyncTaskSubmitResultVO submitExpenseExport(Long userId, ExpenseExportSubmitDTO dto) {
        return asyncTaskSubmissionDomainSupport.submitExpenseExport(userId, dto);
    }

    @Override
    public AsyncTaskSubmitResultVO submitInvoiceVerify(Long userId, InvoiceTaskSubmitDTO dto) {
        return asyncTaskSubmissionDomainSupport.submitInvoiceVerify(userId, dto);
    }

    @Override
    public AsyncTaskSubmitResultVO submitInvoiceOcr(Long userId, InvoiceTaskSubmitDTO dto) {
        return asyncTaskSubmissionDomainSupport.submitInvoiceOcr(userId, dto);
    }

    @Override
    public NotificationSummaryVO getNotificationSummary(Long userId) {
        return asyncTaskNotificationDomainSupport.getNotificationSummary(userId);
    }

    @Override
    public List<NotificationItemVO> listNotifications(Long userId) {
        return asyncTaskNotificationDomainSupport.listNotifications(userId);
    }

    @Override
    public boolean markNotificationRead(Long userId, Long notificationId) {
        return asyncTaskNotificationDomainSupport.markNotificationRead(userId, notificationId);
    }

    @Override
    public boolean markAllNotificationsRead(Long userId) {
        return asyncTaskNotificationDomainSupport.markAllNotificationsRead(userId);
    }
}
