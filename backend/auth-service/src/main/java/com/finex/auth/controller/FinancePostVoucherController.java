package com.finex.auth.controller;

import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.FinancePostVoucherMetaVO;
import com.finex.auth.dto.FinancePostVoucherTaskRequestDTO;
import com.finex.auth.dto.FinancePostVoucherTaskStatusVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinancePostVoucherService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/finance/post-voucher")
@RequiredArgsConstructor
public class FinancePostVoucherController {

    private static final String VIEW_PERMISSION = "finance:general_ledger:post_voucher:view";
    private static final String RUN_PERMISSION = "finance:general_ledger:post_voucher:run";
    private static final String TASK_VIEW_PERMISSION = "finance:general_ledger:post_voucher:task:view";

    private final FinancePostVoucherService financePostVoucherService;
    private final AccessControlService accessControlService;

    @GetMapping("/meta")
    public Result<FinancePostVoucherMetaVO> meta(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) Integer iyear,
            @RequestParam(required = false) Integer iperiod,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, VIEW_PERMISSION);
        return Result.success(financePostVoucherService.getMeta(currentUserId, companyId, iyear, iperiod));
    }

    @PostMapping("/run")
    public Result<AsyncTaskSubmitResultVO> run(
            @Valid @RequestBody FinancePostVoucherTaskRequestDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, RUN_PERMISSION);
        return Result.success(
                "记账任务已提交",
                financePostVoucherService.runPosting(currentUserId, getCurrentUsername(request), dto)
        );
    }

    @GetMapping("/tasks/{taskNo}")
    public Result<FinancePostVoucherTaskStatusVO> taskStatus(
            @PathVariable String taskNo,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(currentUserId, TASK_VIEW_PERMISSION, VIEW_PERMISSION);
        return Result.success(financePostVoucherService.getTaskStatus(taskNo));
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
