// 业务域：异步任务
// 文件角色：service 接口
// 上下游关系：上游通常来自 异步提交、查询和通知相关接口，下游会继续协调 任务记录、状态更新和通知消息。
// 风险提醒：改坏后最容易影响 任务重复提交、异步状态不准确和结果回传。

package com.finex.auth.service;

import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.ExpenseExportSubmitDTO;
import com.finex.auth.dto.InvoiceTaskSubmitDTO;
import com.finex.auth.dto.NotificationItemVO;
import com.finex.auth.dto.NotificationSummaryVO;

import java.util.List;

/**
 * AsyncTaskService：service 接口。
 * 定义异步任务这块对外提供的业务入口能力。
 * 改这里时，要特别关注 任务重复提交、异步状态不准确和结果回传是否会被一起带坏。
 */
public interface AsyncTaskService {

    /**
     * 提交发票Export。
     */
    AsyncTaskSubmitResultVO submitInvoiceExport(Long userId);

    /**
     * 提交报销单Export。
     */
    AsyncTaskSubmitResultVO submitExpenseExport(Long userId, ExpenseExportSubmitDTO dto);

    /**
     * 提交发票Verify。
     */
    AsyncTaskSubmitResultVO submitInvoiceVerify(Long userId, InvoiceTaskSubmitDTO dto);

    /**
     * 提交发票Ocr。
     */
    AsyncTaskSubmitResultVO submitInvoiceOcr(Long userId, InvoiceTaskSubmitDTO dto);

    /**
     * 获取通知汇总。
     */
    NotificationSummaryVO getNotificationSummary(Long userId);

    /**
     * 查询通知列表。
     */
    List<NotificationItemVO> listNotifications(Long userId);

    boolean markNotificationRead(Long userId, Long notificationId);

    boolean markAllNotificationsRead(Long userId);
}
