package com.finex.auth.controller;

import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth/expense-approval")
@RequiredArgsConstructor
public class ExpenseApprovalController {

    private static final String EXPENSE_APPROVAL_VIEW = "expense:approval:view";
    private static final String EXPENSE_APPROVAL_APPROVE = "expense:approval:approve";
    private static final String EXPENSE_APPROVAL_REJECT = "expense:approval:reject";

    private final ExpenseDocumentService expenseDocumentService;
    private final AccessControlService accessControlService;

    @GetMapping("/pending")
    public Result<List<ExpenseApprovalPendingItemVO>> pending(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), EXPENSE_APPROVAL_VIEW);
        return Result.success(expenseDocumentService.listPendingApprovals(getCurrentUserId(request)));
    }

    @PostMapping("/tasks/{taskId}/approve")
    public Result<ExpenseDocumentDetailVO> approve(
            @PathVariable Long taskId,
            @RequestBody(required = false) ExpenseApprovalActionDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), EXPENSE_APPROVAL_APPROVE);
        return Result.success(
                "审批已通过",
                expenseDocumentService.approveTask(getCurrentUserId(request), getCurrentUsername(request), taskId, dto == null ? new ExpenseApprovalActionDTO() : dto)
        );
    }

    @PostMapping("/tasks/{taskId}/reject")
    public Result<ExpenseDocumentDetailVO> reject(
            @PathVariable Long taskId,
            @RequestBody(required = false) ExpenseApprovalActionDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), EXPENSE_APPROVAL_REJECT);
        return Result.success(
                "审批已驳回",
                expenseDocumentService.rejectTask(getCurrentUserId(request), getCurrentUsername(request), taskId, dto == null ? new ExpenseApprovalActionDTO() : dto)
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
        return "当前用户";
    }
}
