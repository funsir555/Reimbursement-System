package com.finex.auth.service;

import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.InvoiceTaskSubmitDTO;
import com.finex.auth.dto.NotificationSummaryVO;

public interface AsyncTaskService {

    AsyncTaskSubmitResultVO submitInvoiceExport(Long userId);

    AsyncTaskSubmitResultVO submitInvoiceVerify(Long userId, InvoiceTaskSubmitDTO dto);

    AsyncTaskSubmitResultVO submitInvoiceOcr(Long userId, InvoiceTaskSubmitDTO dto);

    NotificationSummaryVO getNotificationSummary(Long userId);
}
