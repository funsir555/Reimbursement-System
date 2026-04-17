// 这里是 FinanceContextController 的后端接口入口。
// 它主要负责接收请求、校验权限并调用下游 Service。
// 如果改错，最容易影响这一组接口的查询、保存或状态流转。

package com.finex.auth.controller;

import com.finex.auth.dto.FinanceContextMetaVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceContextService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 这是 FinanceContextController 控制器。
 * 它主要负责接收请求、校验权限并调用下游 Service。
 * 具体业务规则以 Service 层为准。
 */
@RestController
@RequestMapping("/auth/finance/context")
@RequiredArgsConstructor
public class FinanceContextController {

    private static final String[] FINANCE_VIEW_PERMISSIONS = {
            "finance:menu",
            "finance:general_ledger:new_voucher:view",
            "finance:general_ledger:query_voucher:view",
            "finance:general_ledger:review_voucher:view",
            "finance:general_ledger:post_voucher:view",
            "finance:general_ledger:close_ledger:view",
            "finance:general_ledger:balance_sheet:view",
            "finance:general_ledger:detail_ledger:view",
            "finance:general_ledger:general_ledger:view",
            "finance:general_ledger:project_detail_ledger:view",
            "finance:general_ledger:supplier_detail_ledger:view",
            "finance:general_ledger:customer_detail_ledger:view",
            "finance:general_ledger:personal_detail_ledger:view",
            "finance:general_ledger:quantity_amount_detail_ledger:view",
            "finance:fixed_assets:view",
            "finance:reports:balance_sheet:view",
            "finance:reports:income_statement:view",
            "finance:reports:cash_flow:view",
            "finance:archives:customers:view",
            "finance:archives:suppliers:view",
            "finance:archives:employees:view",
            "finance:archives:departments:view",
            "finance:archives:account_subjects:view",
            "finance:archives:projects:view",
            "finance:system_management:view"
    };

    private final FinanceContextService financeContextService;
    private final AccessControlService accessControlService;

    // 处理 meta 请求。
    @GetMapping("/meta")
    public Result<FinanceContextMetaVO> meta(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), FINANCE_VIEW_PERMISSIONS);
        return Result.success(financeContextService.getMeta(getCurrentUserId(request)));
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
