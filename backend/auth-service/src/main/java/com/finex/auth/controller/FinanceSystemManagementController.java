package com.finex.auth.controller;

import com.finex.auth.dto.FinanceAccountSetCreateDTO;
import com.finex.auth.dto.FinanceAccountSetMetaVO;
import com.finex.auth.dto.FinanceAccountSetSummaryVO;
import com.finex.auth.dto.FinanceAccountSetTaskStatusVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceSystemManagementService;
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

@RestController
@RequestMapping("/auth/finance/system-management")
@RequiredArgsConstructor
public class FinanceSystemManagementController {

    private static final String VIEW_PERMISSION = "finance:system_management:view";
    private static final String CREATE_PERMISSION = "finance:system_management:create";
    private static final String TASK_VIEW_PERMISSION = "finance:system_management:task:view";

    private final FinanceSystemManagementService financeSystemManagementService;
    private final AccessControlService accessControlService;

    @GetMapping("/meta")
    public Result<FinanceAccountSetMetaVO> meta(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW_PERMISSION);
        return Result.success(financeSystemManagementService.getMeta());
    }

    @GetMapping("/account-sets")
    public Result<List<FinanceAccountSetSummaryVO>> listAccountSets(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW_PERMISSION);
        return Result.success(financeSystemManagementService.listAccountSets());
    }

    @PostMapping("/account-sets/create")
    public Result<FinanceAccountSetTaskStatusVO> createAccountSet(
            @Valid @RequestBody FinanceAccountSetCreateDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, CREATE_PERMISSION);
        return Result.success(
                "账套创建任务已提交",
                financeSystemManagementService.submitCreateTask(userId, dto)
        );
    }

    @GetMapping("/tasks/{taskNo}")
    public Result<FinanceAccountSetTaskStatusVO> getTaskStatus(@PathVariable String taskNo, HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), TASK_VIEW_PERMISSION, VIEW_PERMISSION);
        return Result.success(financeSystemManagementService.getTaskStatus(taskNo));
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

