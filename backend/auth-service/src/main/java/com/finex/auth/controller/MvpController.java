package com.finex.auth.controller;

import com.finex.auth.dto.DashboardVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.InvoiceSummaryVO;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.MvpDataService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/me")
    public Result<UserProfileVO> me(HttpServletRequest request) {
        return Result.success(mvpDataService.getCurrentUser(getCurrentUserId(request)));
    }

    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), DASHBOARD_VIEW);
        return Result.success(mvpDataService.getDashboard(getCurrentUserId(request)));
    }

    @GetMapping("/expenses")
    public Result<List<ExpenseSummaryVO>> expenses(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), EXPENSE_LIST_VIEW);
        return Result.success(expenseDocumentService.listExpenseSummaries(getCurrentUserId(request)));
    }

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
