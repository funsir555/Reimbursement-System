package com.finex.auth.controller;

import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.InvoiceTaskSubmitDTO;
import com.finex.auth.dto.NotificationSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.AsyncTaskService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/async-tasks")
@RequiredArgsConstructor
public class AsyncTaskController {

    private static final String INVOICE_EXPORT = "archives:invoices:export";
    private static final String INVOICE_VERIFY = "archives:invoices:verify";
    private static final String INVOICE_OCR = "archives:invoices:ocr";

    private final AsyncTaskService asyncTaskService;
    private final AccessControlService accessControlService;

    @PostMapping("/exports/invoices")
    public Result<AsyncTaskSubmitResultVO> exportInvoices(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), INVOICE_EXPORT);
        return Result.success(
                "导出任务已提交，请稍后在下载中心查看",
                asyncTaskService.submitInvoiceExport(getCurrentUserId(request))
        );
    }

    @PostMapping("/invoices/verify")
    public Result<AsyncTaskSubmitResultVO> verifyInvoice(
            @Valid @RequestBody InvoiceTaskSubmitDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), INVOICE_VERIFY);
        return Result.success(
                "发票验真任务已提交",
                asyncTaskService.submitInvoiceVerify(getCurrentUserId(request), dto)
        );
    }

    @PostMapping("/invoices/ocr")
    public Result<AsyncTaskSubmitResultVO> ocrInvoice(
            @Valid @RequestBody InvoiceTaskSubmitDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), INVOICE_OCR);
        return Result.success(
                "发票 OCR 任务已提交",
                asyncTaskService.submitInvoiceOcr(getCurrentUserId(request), dto)
        );
    }

    @GetMapping("/notifications/summary")
    public Result<NotificationSummaryVO> notificationSummary(HttpServletRequest request) {
        return Result.success(asyncTaskService.getNotificationSummary(getCurrentUserId(request)));
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
