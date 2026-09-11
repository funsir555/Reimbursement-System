package com.finex.auth.controller;

import com.finex.auth.dto.FinanceGeneralLedgerPeriodActionDTO;
import com.finex.auth.dto.FinanceGeneralLedgerPeriodActionResultVO;
import com.finex.auth.dto.FinanceGeneralLedgerPeriodStatusOverviewVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceGeneralLedgerPeriodStatusService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/finance/general-ledger/period-status")
@RequiredArgsConstructor
public class FinanceGeneralLedgerPeriodStatusController {

    private static final String POST_VIEW_PERMISSION = "finance:general_ledger:post_voucher:view";
    private static final String CLOSE_VIEW_PERMISSION = "finance:general_ledger:close_ledger:view";
    private static final String POST_RUN_PERMISSION = "finance:general_ledger:post_voucher:run";
    private static final String CLOSE_RUN_PERMISSION = "finance:general_ledger:close_ledger:close";

    private final FinanceGeneralLedgerPeriodStatusService financeGeneralLedgerPeriodStatusService;
    private final AccessControlService accessControlService;

    @GetMapping("/overview")
    public Result<FinanceGeneralLedgerPeriodStatusOverviewVO> overview(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) Integer iyear,
            @RequestParam Integer currentIyear,
            @RequestParam Integer currentIperiod,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(currentUserId, POST_VIEW_PERMISSION, CLOSE_VIEW_PERMISSION);
        return Result.success(financeGeneralLedgerPeriodStatusService.getOverview(
                currentUserId,
                companyId,
                iyear,
                currentIyear,
                currentIperiod
        ));
    }

    @PostMapping("/reopen")
    public Result<FinanceGeneralLedgerPeriodActionResultVO> reopen(
            @Valid @RequestBody FinanceGeneralLedgerPeriodActionDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(currentUserId, POST_RUN_PERMISSION, CLOSE_RUN_PERMISSION);
        return Result.success(
                "反结账成功",
                financeGeneralLedgerPeriodStatusService.reopenPeriod(currentUserId, getCurrentUsername(request), dto)
        );
    }

    @PostMapping("/unpost")
    public Result<FinanceGeneralLedgerPeriodActionResultVO> unpost(
            @Valid @RequestBody FinanceGeneralLedgerPeriodActionDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(currentUserId, POST_RUN_PERMISSION, CLOSE_RUN_PERMISSION);
        return Result.success(
                "反记账成功",
                financeGeneralLedgerPeriodStatusService.unpostPeriod(currentUserId, getCurrentUsername(request), dto)
        );
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

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "system";
    }
}
