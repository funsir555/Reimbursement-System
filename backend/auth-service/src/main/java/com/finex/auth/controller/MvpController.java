// 这里是 MvpController 的后端接口入口。
// 它主要负责接收请求、校验权限并调用下游 Service。
// 如果改错，最容易影响这一组接口的查询、保存或状态流转。

package com.finex.auth.controller;

import com.finex.auth.dto.DashboardVO;
import com.finex.auth.dto.DashboardWriteOffBindingDTO;
import com.finex.auth.dto.ExpenseDocumentPickerVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.InvoiceSummaryVO;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.MvpDataService;
import com.finex.common.Result;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 这是 MvpController 控制器。
 * 它主要负责接收请求、校验权限并调用下游 Service。
 * 具体业务规则以 Service 层为准。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class MvpController {

    private static final String DASHBOARD_VIEW = "dashboard:view";
    private static final String EXPENSE_LIST_VIEW = "expense:list:view";
    private static final String INVOICE_VIEW = "archives:invoices:view";

    private final MvpDataService mvpDataService;
    private final ExpenseDocumentService expenseDocumentService;
    private final AccessControlService accessControlService;

    // 处理 me 请求。
    @GetMapping("/me")
    public Result<UserProfileVO> me(HttpServletRequest request) {
        return Result.success(mvpDataService.getCurrentUser(getCurrentUserId(request)));
    }

    // 处理 dashboard 请求。
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), DASHBOARD_VIEW);
        return Result.success(mvpDataService.getDashboard(getCurrentUserId(request)));
    }

    // 处理 outstandingDocuments 请求。
    @GetMapping("/dashboard/outstanding-documents")
    public Result<List<ExpenseSummaryVO>> outstandingDocuments(
            HttpServletRequest request,
            @RequestParam String kind
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), DASHBOARD_VIEW);
        return Result.success(expenseDocumentService.listOutstandingDocuments(getCurrentUserId(request), kind));
    }

    // 处理 writeoffReportPicker 请求。
    @GetMapping("/dashboard/writeoff-report-picker")
    public Result<ExpenseDocumentPickerVO> writeoffReportPicker(
            HttpServletRequest request,
            @RequestParam String targetDocumentCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), DASHBOARD_VIEW);
        return Result.success(expenseDocumentService.getDashboardWriteOffSourceReportPicker(
                getCurrentUserId(request),
                targetDocumentCode,
                keyword,
                page,
                pageSize
        ));
    }

    // 处理 bindWriteoff 请求。
    @PostMapping("/dashboard/writeoff-bindings")
    public Result<Boolean> bindWriteoff(
            HttpServletRequest request,
            @Valid @RequestBody DashboardWriteOffBindingDTO dto
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), DASHBOARD_VIEW);
        return Result.success(expenseDocumentService.bindDashboardWriteOff(
                getCurrentUserId(request),
                dto.getTargetDocumentCode(),
                dto.getSourceReportDocumentCode()
        ));
    }

    // 处理 expenses 请求。
    @GetMapping("/expenses")
    public Result<List<ExpenseSummaryVO>> expenses(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), EXPENSE_LIST_VIEW);
        return Result.success(expenseDocumentService.listExpenseSummaries(getCurrentUserId(request)));
    }

    // 处理 invoices 请求。
    @GetMapping("/invoices")
    public Result<List<InvoiceSummaryVO>> invoices(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), INVOICE_VIEW);
        return Result.success(mvpDataService.listInvoices(getCurrentUserId(request)));
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
