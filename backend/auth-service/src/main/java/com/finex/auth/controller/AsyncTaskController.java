// 这里是 AsyncTaskController 的后端接口入口。
// 它主要负责接收请求、校验权限并调用下游 Service。
// 如果改错，最容易影响这一组接口的查询、保存或状态流转。

package com.finex.auth.controller;

import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.ExpenseExportSubmitDTO;
import com.finex.auth.dto.InvoiceTaskSubmitDTO;
import com.finex.auth.dto.NotificationItemVO;
import com.finex.auth.dto.NotificationSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.AsyncTaskService;
import com.finex.auth.support.AsyncTaskSupport;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/**
 * 这是 AsyncTaskController 控制器。
 * 它主要负责接收请求、校验权限并调用下游 Service。
 * 具体业务规则以 Service 层为准。
 */
@RestController
@RequestMapping("/auth/async-tasks")
@RequiredArgsConstructor
public class AsyncTaskController {

    private static final String INVOICE_EXPORT = "archives:invoices:export";
    private static final String EXPENSE_LIST_VIEW = "expense:list:view";
    private static final String EXPENSE_APPROVAL_VIEW = "expense:approval:view";
    private static final String EXPENSE_DOCUMENTS_VIEW = "expense:documents:view";
    private static final String DASHBOARD_VIEW = "dashboard:view";
    private static final String INVOICE_VERIFY = "archives:invoices:verify";
    private static final String INVOICE_OCR = "archives:invoices:ocr";

    private final AsyncTaskService asyncTaskService;
    private final AccessControlService accessControlService;

    // 处理 exportInvoices 请求。
    @PostMapping("/exports/invoices")
    public Result<AsyncTaskSubmitResultVO> exportInvoices(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, INVOICE_EXPORT);
        return Result.success(
                "导出任务已提交，请到下载中心查看进度",
                asyncTaskService.submitInvoiceExport(userId)
        );
    }

    // 处理 exportExpenses 请求。
    @PostMapping("/exports/expenses")
    public Result<AsyncTaskSubmitResultVO> exportExpenses(
            @Valid @RequestBody ExpenseExportSubmitDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        String scene = dto.getScene() == null ? "" : dto.getScene().trim().toUpperCase(Locale.ROOT);
        switch (scene) {
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_MY_EXPENSES ->
                    accessControlService.requirePermission(userId, EXPENSE_LIST_VIEW);
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_PENDING_APPROVAL ->
                    accessControlService.requirePermission(userId, EXPENSE_APPROVAL_VIEW);
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_DOCUMENT_QUERY ->
                    accessControlService.requirePermission(userId, EXPENSE_DOCUMENTS_VIEW);
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_OUTSTANDING ->
                    accessControlService.requirePermission(userId, DASHBOARD_VIEW);
            default -> throw new IllegalArgumentException("不支持的导出场景");
        }
        return Result.success(
                "导出任务已提交，请到下载中心查看进度",
                asyncTaskService.submitExpenseExport(userId, dto)
        );
    }

    // 处理 verifyInvoice 请求。
    @PostMapping("/invoices/verify")
    public Result<AsyncTaskSubmitResultVO> verifyInvoice(
            @Valid @RequestBody InvoiceTaskSubmitDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, INVOICE_VERIFY);
        return Result.success("发票验真任务已提交", asyncTaskService.submitInvoiceVerify(userId, dto));
    }

    // 处理 ocrInvoice 请求。
    @PostMapping("/invoices/ocr")
    public Result<AsyncTaskSubmitResultVO> ocrInvoice(
            @Valid @RequestBody InvoiceTaskSubmitDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, INVOICE_OCR);
        return Result.success("发票 OCR 任务已提交", asyncTaskService.submitInvoiceOcr(userId, dto));
    }

    // 处理 notificationSummary 请求。
    @GetMapping("/notifications/summary")
    public Result<NotificationSummaryVO> notificationSummary(HttpServletRequest request) {
        return Result.success(asyncTaskService.getNotificationSummary(getCurrentUserId(request)));
    }

    // 处理 notifications 请求。
    @GetMapping("/notifications")
    public Result<List<NotificationItemVO>> notifications(HttpServletRequest request) {
        return Result.success(asyncTaskService.listNotifications(getCurrentUserId(request)));
    }

    // 处理 markNotificationRead 请求。
    @PostMapping("/notifications/{notificationId}/read")
    public Result<Boolean> markNotificationRead(
            @PathVariable Long notificationId,
            HttpServletRequest request
    ) {
        return Result.success(asyncTaskService.markNotificationRead(getCurrentUserId(request), notificationId));
    }

    // 处理 markAllNotificationsRead 请求。
    @PostMapping("/notifications/read-all")
    public Result<Boolean> markAllNotificationsRead(HttpServletRequest request) {
        return Result.success(asyncTaskService.markAllNotificationsRead(getCurrentUserId(request)));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId instanceof Long value) {
            return value;
        }
        if (userId instanceof Integer value) {
            return value.longValue();
        }
        throw new IllegalStateException("无法获取当前登录用户");
    }
}
