// 这里是 ExpenseApprovalController 的后端接口入口。
// 它主要负责接收请求、校验权限并调用下游 Service。
// 如果改错，最容易影响这一组接口的查询、保存或状态流转。

package com.finex.auth.controller;

import com.finex.auth.dto.ExpenseActionUserOptionVO;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.dto.ExpenseTaskAddSignDTO;
import com.finex.auth.dto.ExpenseTaskTransferDTO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 这是 ExpenseApprovalController 控制器。
 * 它主要负责接收请求、校验权限并调用下游 Service。
 * 具体业务规则以 Service 层为准。
 */
@RestController
@RequestMapping("/auth/expense-approval")
@RequiredArgsConstructor
public class ExpenseApprovalController {

    private static final String EXPENSE_APPROVAL_VIEW = "expense:approval:view";
    private static final String EXPENSE_APPROVAL_APPROVE = "expense:approval:approve";
    private static final String EXPENSE_APPROVAL_REJECT = "expense:approval:reject";

    private final ExpenseDocumentService expenseDocumentService;
    private final AccessControlService accessControlService;

    // 处理 pending 请求。
    @GetMapping("/pending")
    public Result<List<ExpenseApprovalPendingItemVO>> pending(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), EXPENSE_APPROVAL_VIEW);
        return Result.success(expenseDocumentService.listPendingApprovals(getCurrentUserId(request)));
    }

    // 处理 approve 请求。
    @PostMapping("/tasks/{taskId}/approve")
    public Result<ExpenseDocumentDetailVO> approve(
            @PathVariable Long taskId,
            @RequestBody(required = false) ExpenseApprovalActionDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), EXPENSE_APPROVAL_APPROVE);
        return Result.success(
                "瀹℃壒宸查€氳繃",
                expenseDocumentService.approveTask(getCurrentUserId(request), getCurrentUsername(request), taskId, dto == null ? new ExpenseApprovalActionDTO() : dto)
        );
    }

    // 处理 reject 请求。
    @PostMapping("/tasks/{taskId}/reject")
    public Result<ExpenseDocumentDetailVO> reject(
            @PathVariable Long taskId,
            @RequestBody(required = false) ExpenseApprovalActionDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), EXPENSE_APPROVAL_REJECT);
        return Result.success(
                "瀹℃壒宸查┏鍥?",
                expenseDocumentService.rejectTask(getCurrentUserId(request), getCurrentUsername(request), taskId, dto == null ? new ExpenseApprovalActionDTO() : dto)
        );
    }

    // 处理 modifyContext 请求。
    @GetMapping("/tasks/{taskId}/modify-context")
    public Result<ExpenseDocumentEditContextVO> modifyContext(@PathVariable Long taskId, HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), EXPENSE_APPROVAL_VIEW, EXPENSE_APPROVAL_APPROVE);
        return Result.success(expenseDocumentService.getTaskModifyContext(getCurrentUserId(request), taskId));
    }

    // 处理 modify 请求。
    @PutMapping("/tasks/{taskId}/modify")
    public Result<ExpenseDocumentDetailVO> modify(
            @PathVariable Long taskId,
            @Valid @RequestBody ExpenseDocumentUpdateDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), EXPENSE_APPROVAL_APPROVE);
        return Result.success(
                "瀹℃壒鍗曞凡鏇存柊",
                expenseDocumentService.modifyTaskDocument(getCurrentUserId(request), getCurrentUsername(request), taskId, dto)
        );
    }

    // 处理 addSign 请求。
    @PostMapping("/tasks/{taskId}/add-sign")
    public Result<ExpenseDocumentDetailVO> addSign(
            @PathVariable Long taskId,
            @Valid @RequestBody ExpenseTaskAddSignDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), EXPENSE_APPROVAL_APPROVE);
        return Result.success(
                "宸插彂璧峰姞绛?",
                expenseDocumentService.addSignTask(getCurrentUserId(request), getCurrentUsername(request), taskId, dto)
        );
    }

    // 处理 transfer 请求。
    @PostMapping("/tasks/{taskId}/transfer")
    public Result<ExpenseDocumentDetailVO> transfer(
            @PathVariable Long taskId,
            @Valid @RequestBody ExpenseTaskTransferDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), EXPENSE_APPROVAL_APPROVE);
        return Result.success(
                "瀹℃壒浠诲姟宸茶浆浜?",
                expenseDocumentService.transferTask(getCurrentUserId(request), getCurrentUsername(request), taskId, dto)
        );
    }

    // 处理 actionUsers 请求。
    @GetMapping("/action-users")
    public Result<List<ExpenseActionUserOptionVO>> actionUsers(
            @RequestParam(required = false) String keyword,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), EXPENSE_APPROVAL_VIEW);
        return Result.success(expenseDocumentService.searchActionUsers(getCurrentUserId(request), keyword));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId instanceof Long value) {
            return value;
        }
        if (userId instanceof Integer value) {
            return value.longValue();
        }
        throw new IllegalStateException("鏃犳硶鑾峰彇褰撳墠鐧诲綍鐢ㄦ埛");
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "褰撳墠鐢ㄦ埛";
    }
}
