package com.finex.auth.service;

import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.ExpenseExportSubmitDTO;
import com.finex.auth.dto.InvoiceTaskSubmitDTO;
import com.finex.auth.dto.NotificationItemVO;
import com.finex.auth.dto.NotificationSummaryVO;

import java.util.List;

public interface AsyncTaskService {

    AsyncTaskSubmitResultVO submitInvoiceExport(Long userId);

    AsyncTaskSubmitResultVO submitExpenseExport(Long userId, ExpenseExportSubmitDTO dto);

    AsyncTaskSubmitResultVO submitInvoiceVerify(Long userId, InvoiceTaskSubmitDTO dto);

    AsyncTaskSubmitResultVO submitInvoiceOcr(Long userId, InvoiceTaskSubmitDTO dto);

    NotificationSummaryVO getNotificationSummary(Long userId);

    List<NotificationItemVO> listNotifications(Long userId);

    boolean markNotificationRead(Long userId, Long notificationId);

    boolean markAllNotificationsRead(Long userId);
}
