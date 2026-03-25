package com.finex.auth.controller;

import com.finex.auth.dto.DashboardVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.InvoiceSummaryVO;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.service.MvpDataService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MVP 动态数据控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class MvpController {

    private final MvpDataService mvpDataService;

    @GetMapping("/me")
    public Result<UserProfileVO> me(HttpServletRequest request) {
        return Result.success(mvpDataService.getCurrentUser(getCurrentUserId(request)));
    }

    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard(HttpServletRequest request) {
        return Result.success(mvpDataService.getDashboard(getCurrentUserId(request)));
    }

    @GetMapping("/expenses")
    public Result<List<ExpenseSummaryVO>> expenses(HttpServletRequest request) {
        return Result.success(mvpDataService.listExpenses(getCurrentUserId(request)));
    }

    @GetMapping("/invoices")
    public Result<List<InvoiceSummaryVO>> invoices(HttpServletRequest request) {
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
