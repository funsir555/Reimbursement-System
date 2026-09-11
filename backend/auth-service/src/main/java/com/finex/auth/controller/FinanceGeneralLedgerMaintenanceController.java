package com.finex.auth.controller;

import com.finex.auth.dto.FinanceGeneralLedgerRollbackPeriodDTO;
import com.finex.auth.dto.FinanceGeneralLedgerRollbackPeriodResultVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceGeneralLedgerMaintenanceService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/finance/system-maintenance/general-ledger")
@RequiredArgsConstructor
public class FinanceGeneralLedgerMaintenanceController {

    private static final String REPAIR_PERMISSION = "finance:general_ledger:repair:run";

    private final FinanceGeneralLedgerMaintenanceService financeGeneralLedgerMaintenanceService;
    private final AccessControlService accessControlService;

    @PostMapping("/rollback-period")
    public Result<FinanceGeneralLedgerRollbackPeriodResultVO> rollbackPeriod(
            @Valid @RequestBody FinanceGeneralLedgerRollbackPeriodDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, REPAIR_PERMISSION);
        return Result.success(
                "总账期间专项回退完成",
                financeGeneralLedgerMaintenanceService.rollbackPeriod(currentUserId, getCurrentUsername(request), dto)
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
