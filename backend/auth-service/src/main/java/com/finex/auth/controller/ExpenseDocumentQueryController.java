// 这里是 ExpenseDocumentQueryController 的后端接口入口。
// 它主要负责接收请求、校验权限并调用下游 Service。
// 如果改错，最容易影响这一组接口的查询、保存或状态流转。

package com.finex.auth.controller;

import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseDocumentCommentDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentNavigationVO;
import com.finex.auth.dto.ExpenseDocumentPickerVO;
import com.finex.auth.dto.ExpenseDocumentReminderDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.dto.ExpenseSummaryVO;
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
 * 这是 ExpenseDocumentQueryController 控制器。
 * 它主要负责接收请求、校验权限并调用下游 Service。
 * 具体业务规则以 Service 层为准。
 */
@RestController
@RequestMapping("/auth/expenses")
@RequiredArgsConstructor
public class ExpenseDocumentQueryController {

    private static final String EXPENSE_LIST_VIEW = "expense:list:view";
    private static final String EXPENSE_CREATE_CREATE = "expense:create:create";
    private static final String EXPENSE_CREATE_SUBMIT = "expense:create:submit";
    private static final String EXPENSE_APPROVAL_VIEW = "expense:approval:view";
    private static final String EXPENSE_DOCUMENTS_VIEW = "expense:documents:view";
    private static final String EXPENSE_CREATE_VIEW = "expense:create:view";

    private final ExpenseDocumentService expenseDocumentService;
    private final AccessControlService accessControlService;

    // 处理 queryDocuments 请求。
    @GetMapping("/query-documents")
    public Result<List<ExpenseSummaryVO>> queryDocuments(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, EXPENSE_DOCUMENTS_VIEW);
        return Result.success(expenseDocumentService.listQueryDocumentSummaries(userId));
    }

    // 处理 detail 请求。
    @GetMapping("/{documentCode}")
    public Result<ExpenseDocumentDetailVO> detail(@PathVariable String documentCode, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_APPROVAL_VIEW, EXPENSE_DOCUMENTS_VIEW);
        List<String> permissionCodes = accessControlService.getPermissionCodes(userId);
        boolean allowCrossView = permissionCodes.contains(EXPENSE_APPROVAL_VIEW) || permissionCodes.contains(EXPENSE_DOCUMENTS_VIEW);
        return Result.success(expenseDocumentService.getDocumentDetail(userId, documentCode, allowCrossView));
    }

    // 处理 expenseDetail 请求。
    @GetMapping("/{documentCode}/details/{detailNo}")
    public Result<ExpenseDetailInstanceDetailVO> expenseDetail(
            @PathVariable String documentCode,
            @PathVariable String detailNo,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_APPROVAL_VIEW, EXPENSE_DOCUMENTS_VIEW);
        List<String> permissionCodes = accessControlService.getPermissionCodes(userId);
        boolean allowCrossView = permissionCodes.contains(EXPENSE_APPROVAL_VIEW) || permissionCodes.contains(EXPENSE_DOCUMENTS_VIEW);
        return Result.success(expenseDocumentService.getExpenseDetail(userId, documentCode, detailNo, allowCrossView));
    }

    // 处理 documentPicker 请求。
    @GetMapping("/document-picker")
    public Result<ExpenseDocumentPickerVO> documentPicker(
            @RequestParam String relationType,
            @RequestParam(required = false) List<String> templateTypes,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String excludeDocumentCode,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(
                userId,
                EXPENSE_CREATE_VIEW,
                EXPENSE_CREATE_CREATE,
                EXPENSE_CREATE_SUBMIT,
                EXPENSE_LIST_VIEW,
                EXPENSE_APPROVAL_VIEW,
                EXPENSE_DOCUMENTS_VIEW
        );
        List<String> permissionCodes = accessControlService.getPermissionCodes(userId);
        boolean allowCrossView = permissionCodes.contains(EXPENSE_APPROVAL_VIEW) || permissionCodes.contains(EXPENSE_DOCUMENTS_VIEW);
        return Result.success(
                expenseDocumentService.getDocumentPicker(
                        userId,
                        relationType,
                        templateTypes,
                        keyword,
                        page,
                        pageSize,
                        excludeDocumentCode,
                        allowCrossView
                )
        );
    }

    // 处理 recall 请求。
    @PostMapping("/{documentCode}/recall")
    public Result<ExpenseDocumentDetailVO> recall(@PathVariable String documentCode, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_APPROVAL_VIEW, EXPENSE_DOCUMENTS_VIEW);
        return Result.success(
                "鍗曟嵁宸插彫鍥?",
                expenseDocumentService.recallDocument(userId, getCurrentUsername(request), documentCode)
        );
    }

    // 处理 comment 请求。
    @PostMapping("/{documentCode}/comments")
    public Result<ExpenseDocumentDetailVO> comment(
            @PathVariable String documentCode,
            @RequestBody(required = false) ExpenseDocumentCommentDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_APPROVAL_VIEW, EXPENSE_DOCUMENTS_VIEW);
        List<String> permissionCodes = accessControlService.getPermissionCodes(userId);
        boolean allowCrossView = permissionCodes.contains(EXPENSE_APPROVAL_VIEW) || permissionCodes.contains(EXPENSE_DOCUMENTS_VIEW);
        return Result.success(
                "璇勮宸插彂甯?",
                expenseDocumentService.commentOnDocument(
                        userId,
                        getCurrentUsername(request),
                        documentCode,
                        dto == null ? new ExpenseDocumentCommentDTO() : dto,
                        allowCrossView
                )
        );
    }

    // 处理 remind 请求。
    @PostMapping("/{documentCode}/reminders")
    public Result<ExpenseDocumentDetailVO> remind(
            @PathVariable String documentCode,
            @RequestBody(required = false) ExpenseDocumentReminderDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_DOCUMENTS_VIEW);
        return Result.success(
                "宸插悜褰撳墠瀹℃壒浜哄彂閫佸偓鍔?",
                expenseDocumentService.remindDocument(userId, getCurrentUsername(request), documentCode, dto == null ? new ExpenseDocumentReminderDTO() : dto)
        );
    }

    // 处理 navigation 请求。
    @GetMapping("/{documentCode}/navigation")
    public Result<ExpenseDocumentNavigationVO> navigation(@PathVariable String documentCode, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_APPROVAL_VIEW, EXPENSE_DOCUMENTS_VIEW);
        List<String> permissionCodes = accessControlService.getPermissionCodes(userId);
        boolean approvalViewer = permissionCodes.contains(EXPENSE_APPROVAL_VIEW);
        return Result.success(expenseDocumentService.getDocumentNavigation(userId, documentCode, approvalViewer));
    }

    // 处理 editContext 请求。
    @GetMapping("/{documentCode}/edit-context")
    public Result<ExpenseDocumentEditContextVO> editContext(@PathVariable String documentCode, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_APPROVAL_VIEW, EXPENSE_DOCUMENTS_VIEW);
        return Result.success(expenseDocumentService.getDocumentEditContext(userId, documentCode));
    }

    // 处理 resubmit 请求。
    @PutMapping("/{documentCode}/resubmit")
    public Result<ExpenseDocumentSubmitResultVO> resubmit(
            @PathVariable String documentCode,
            @Valid @RequestBody ExpenseDocumentUpdateDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_CREATE_CREATE, EXPENSE_CREATE_SUBMIT);
        return Result.success(
                "瀹℃壒鍗曞凡閲嶆柊鎻愪氦",
                expenseDocumentService.resubmitDocument(userId, getCurrentUsername(request), documentCode, dto)
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
