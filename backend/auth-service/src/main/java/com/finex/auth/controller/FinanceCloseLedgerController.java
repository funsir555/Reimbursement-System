package com.finex.auth.controller;

import com.finex.auth.dto.FinanceCloseLedgerMetaVO;
import com.finex.auth.dto.FinanceCloseLedgerReconcileResultVO;
import com.finex.auth.dto.FinanceCloseLedgerRequestDTO;
import com.finex.auth.dto.FinanceCloseLedgerValidationResultVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceCloseLedgerService;
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
@RequestMapping("/auth/finance/close-ledger")
@RequiredArgsConstructor
public class FinanceCloseLedgerController {

    private static final String VIEW_PERMISSION = "finance:general_ledger:close_ledger:view";
    private static final String CLOSE_PERMISSION = "finance:general_ledger:close_ledger:close";

    private final FinanceCloseLedgerService financeCloseLedgerService;
    private final AccessControlService accessControlService;

    @GetMapping("/meta")
    public Result<FinanceCloseLedgerMetaVO> meta(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) Integer iyear,
            @RequestParam(required = false) Integer iperiod,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, VIEW_PERMISSION);
        return Result.success(financeCloseLedgerService.getMeta(currentUserId, companyId, iyear, iperiod));
    }

    @PostMapping("/reconcile")
    public Result<FinanceCloseLedgerReconcileResultVO> reconcile(
            @Valid @RequestBody FinanceCloseLedgerRequestDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, CLOSE_PERMISSION);
        return Result.success(financeCloseLedgerService.reconcile(currentUserId, getCurrentUsername(request), dto));
    }

    @PostMapping("/validate")
    public Result<FinanceCloseLedgerValidationResultVO> validate(
            @Valid @RequestBody FinanceCloseLedgerRequestDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, CLOSE_PERMISSION);
        return Result.success(financeCloseLedgerService.validate(currentUserId, getCurrentUsername(request), dto));
    }

    @PostMapping("/close")
    public Result<FinanceCloseLedgerMetaVO> close(
            @Valid @RequestBody FinanceCloseLedgerRequestDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, CLOSE_PERMISSION);
        return Result.success("结账成功", financeCloseLedgerService.close(currentUserId, getCurrentUsername(request), dto));
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
