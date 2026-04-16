package com.finex.auth.controller;

import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseBankLinkConfigVO;
import com.finex.auth.dto.ExpenseBankLinkSaveDTO;
import com.finex.auth.dto.ExpenseBankLinkSummaryVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseExportSubmitDTO;
import com.finex.auth.dto.ExpensePaymentBatchTaskDTO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.AsyncTaskService;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.support.AsyncTaskSupport;
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

@RestController
@RequestMapping("/auth/expense-payment")
@RequiredArgsConstructor
public class ExpensePaymentController {

    private static final String EXPENSE_PAYMENT_VIEW = "expense:payment:payment_order:view";
    private static final String EXPENSE_PAYMENT_EXECUTE = "expense:payment:payment_order:execute";
    private static final String EXPENSE_BANK_LINK_VIEW = "expense:payment:bank_link:view";
    private static final String EXPENSE_BANK_LINK_EDIT = "expense:payment:bank_link:edit";

    private final ExpenseDocumentService expenseDocumentService;
    private final AsyncTaskService asyncTaskService;
    private final AccessControlService accessControlService;

    @GetMapping("/orders")
    public Result<List<ExpensePaymentOrderVO>> listOrders(
            @RequestParam(required = false) String status,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, EXPENSE_PAYMENT_VIEW);
        return Result.success(expenseDocumentService.listPaymentOrders(userId, status));
    }

    @PostMapping("/orders/export")
    public Result<AsyncTaskSubmitResultVO> exportOrders(
            @Valid @RequestBody ExpensePaymentBatchTaskDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, EXPENSE_PAYMENT_VIEW);
        ExpenseExportSubmitDTO payload = new ExpenseExportSubmitDTO();
        payload.setScene(AsyncTaskSupport.EXPENSE_EXPORT_SCENE_PAYMENT_PENDING);
        payload.setTaskIds(dto.getTaskIds());
        return Result.success(
                "导出任务已提交，请到下载中心查看进度",
                asyncTaskService.submitExpenseExport(userId, payload)
        );
    }

    @PostMapping("/tasks/{taskId}/start")
    public Result<ExpenseDocumentDetailVO> startTask(@PathVariable Long taskId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, EXPENSE_PAYMENT_EXECUTE);
        return Result.success(
                "付款任务已推送至银行",
                expenseDocumentService.startPaymentTask(userId, getCurrentUsername(request), taskId)
        );
    }

    @PostMapping("/tasks/{taskId}/complete")
    public Result<ExpenseDocumentDetailVO> completeTask(
            @PathVariable Long taskId,
            @RequestBody(required = false) ExpenseApprovalActionDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, EXPENSE_PAYMENT_EXECUTE);
        return Result.success(
                "付款任务已标记为已支付",
                expenseDocumentService.completePaymentTask(
                        userId,
                        getCurrentUsername(request),
                        taskId,
                        dto == null ? new ExpenseApprovalActionDTO() : dto
                )
        );
    }

    @PostMapping("/tasks/{taskId}/exception")
    public Result<ExpenseDocumentDetailVO> markException(
            @PathVariable Long taskId,
            @RequestBody(required = false) ExpenseApprovalActionDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, EXPENSE_PAYMENT_EXECUTE);
        return Result.success(
                "付款任务已标记异常",
                expenseDocumentService.markPaymentTaskException(
                        userId,
                        getCurrentUsername(request),
                        taskId,
                        dto == null ? new ExpenseApprovalActionDTO() : dto
                )
        );
    }

    @PostMapping("/tasks/reject")
    public Result<Boolean> rejectTasks(
            @Valid @RequestBody ExpensePaymentBatchTaskDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, EXPENSE_PAYMENT_EXECUTE);
        ExpenseApprovalActionDTO actionDTO = new ExpenseApprovalActionDTO();
        actionDTO.setComment(dto.getComment());
        return Result.success(
                "付款任务已驳回",
                expenseDocumentService.rejectPaymentTasks(
                        userId,
                        getCurrentUsername(request),
                        dto.getTaskIds(),
                        actionDTO
                )
        );
    }

    @GetMapping("/bank-links")
    public Result<List<ExpenseBankLinkSummaryVO>> listBankLinks(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), EXPENSE_BANK_LINK_VIEW);
        return Result.success(expenseDocumentService.listBankLinks());
    }

    @GetMapping("/bank-links/{companyBankAccountId}")
    public Result<ExpenseBankLinkConfigVO> getBankLink(
            @PathVariable Long companyBankAccountId,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), EXPENSE_BANK_LINK_VIEW);
        return Result.success(expenseDocumentService.getBankLink(companyBankAccountId));
    }

    @PutMapping("/bank-links/{companyBankAccountId}")
    public Result<ExpenseBankLinkConfigVO> updateBankLink(
            @PathVariable Long companyBankAccountId,
            @Valid @RequestBody ExpenseBankLinkSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), EXPENSE_BANK_LINK_EDIT);
        return Result.success("银企直连配置已保存", expenseDocumentService.updateBankLink(companyBankAccountId, dto));
    }

    @PostMapping("/banks/cmb-cloud/callback")
    public Result<ExpenseDocumentDetailVO> cmbCloudCallback(@RequestBody(required = false) ExpenseBankCallbackDTO dto) {
        ExpenseBankCallbackDTO payload = dto == null ? new ExpenseBankCallbackDTO() : dto;
        return Result.success("银行回调处理成功", expenseDocumentService.handleCmbCloudCallback(payload));
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
