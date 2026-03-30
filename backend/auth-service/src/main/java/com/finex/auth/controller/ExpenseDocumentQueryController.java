package com.finex.auth.controller;

import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth/expenses")
@RequiredArgsConstructor
public class ExpenseDocumentQueryController {

    private static final String EXPENSE_LIST_VIEW = "expense:list:view";
    private static final String EXPENSE_APPROVAL_VIEW = "expense:approval:view";
    private static final String EXPENSE_DOCUMENTS_VIEW = "expense:documents:view";

    private final ExpenseDocumentService expenseDocumentService;
    private final AccessControlService accessControlService;

    @GetMapping("/{documentCode}")
    public Result<ExpenseDocumentDetailVO> detail(@PathVariable String documentCode, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_APPROVAL_VIEW, EXPENSE_DOCUMENTS_VIEW);
        List<String> permissionCodes = accessControlService.getPermissionCodes(userId);
        boolean allowCrossView = permissionCodes.contains(EXPENSE_APPROVAL_VIEW) || permissionCodes.contains(EXPENSE_DOCUMENTS_VIEW);
        return Result.success(expenseDocumentService.getDocumentDetail(userId, documentCode, allowCrossView));
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
