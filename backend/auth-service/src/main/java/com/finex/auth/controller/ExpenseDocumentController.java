package com.finex.auth.controller;

import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.FinanceVendorService;
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
@RequestMapping("/auth/expenses/create")
@RequiredArgsConstructor
public class ExpenseDocumentController {

    private static final String EXPENSE_CREATE_VIEW = "expense:create:view";
    private static final String EXPENSE_CREATE_CREATE = "expense:create:create";
    private static final String EXPENSE_CREATE_SUBMIT = "expense:create:submit";

    private final ExpenseDocumentService expenseDocumentService;
    private final FinanceVendorService financeVendorService;
    private final AccessControlService accessControlService;

    @GetMapping("/templates")
    public Result<List<ExpenseCreateTemplateSummaryVO>> listTemplates(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), EXPENSE_CREATE_VIEW, EXPENSE_CREATE_CREATE, EXPENSE_CREATE_SUBMIT);
        return Result.success(expenseDocumentService.listAvailableTemplates());
    }

    @GetMapping("/templates/{templateCode}")
    public Result<ExpenseCreateTemplateDetailVO> getTemplateDetail(
            @PathVariable String templateCode,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), EXPENSE_CREATE_VIEW, EXPENSE_CREATE_CREATE, EXPENSE_CREATE_SUBMIT);
        return Result.success(expenseDocumentService.getTemplateDetail(getCurrentUserId(request), templateCode));
    }

    @GetMapping("/documents/{documentCode}/details/{detailNo}")
    public Result<ExpenseDetailInstanceDetailVO> getExpenseDetail(
            @PathVariable String documentCode,
            @PathVariable String detailNo,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), EXPENSE_CREATE_VIEW, EXPENSE_CREATE_CREATE, EXPENSE_CREATE_SUBMIT);
        return Result.success(expenseDocumentService.getExpenseDetail(getCurrentUserId(request), documentCode, detailNo, false));
    }

    @GetMapping("/vendors/options")
    public Result<List<ExpenseCreateVendorOptionVO>> listVendorOptions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean includeDisabled,
            @RequestParam(required = false) String paymentCompanyId,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), EXPENSE_CREATE_VIEW, EXPENSE_CREATE_CREATE, EXPENSE_CREATE_SUBMIT);
        return Result.success(expenseDocumentService.listVendorOptions(
                getCurrentUserId(request),
                keyword,
                includeDisabled,
                paymentCompanyId
        ));
    }

    @GetMapping("/vendors/{vendorCode}")
    public Result<FinanceVendorDetailVO> getVendorDetail(
            @PathVariable String vendorCode,
            @RequestParam String paymentCompanyId,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), EXPENSE_CREATE_VIEW, EXPENSE_CREATE_CREATE, EXPENSE_CREATE_SUBMIT);
        return Result.success(financeVendorService.getVendorDetail(paymentCompanyId, vendorCode));
    }

    @PostMapping("/vendors")
    public Result<FinanceVendorDetailVO> createVendor(
            @RequestParam String paymentCompanyId,
            @Valid @RequestBody FinanceVendorSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), EXPENSE_CREATE_CREATE, EXPENSE_CREATE_SUBMIT);
        return Result.success("供应商已保存", financeVendorService.createVendor(paymentCompanyId, dto, getCurrentUsername(request), true));
    }

    @PutMapping("/vendors/{vendorCode}")
    public Result<FinanceVendorDetailVO> updateVendor(
            @PathVariable String vendorCode,
            @RequestParam String paymentCompanyId,
            @Valid @RequestBody FinanceVendorSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), EXPENSE_CREATE_CREATE, EXPENSE_CREATE_SUBMIT);
        return Result.success(
                "供应商已保存",
                financeVendorService.updateVendor(paymentCompanyId, vendorCode, dto, getCurrentUsername(request), true)
        );
    }

    @GetMapping("/payees/options")
    public Result<List<ExpenseCreatePayeeOptionVO>> listPayeeOptions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean personalOnly,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), EXPENSE_CREATE_VIEW, EXPENSE_CREATE_CREATE, EXPENSE_CREATE_SUBMIT);
        return Result.success(expenseDocumentService.listPayeeOptions(getCurrentUserId(request), keyword, personalOnly));
    }

    @GetMapping("/payee-accounts/options")
    public Result<List<ExpenseCreatePayeeAccountOptionVO>> listPayeeAccountOptions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String linkageMode,
            @RequestParam(required = false) String payeeName,
            @RequestParam(required = false) String counterpartyCode,
            @RequestParam(required = false) String paymentCompanyId,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), EXPENSE_CREATE_VIEW, EXPENSE_CREATE_CREATE, EXPENSE_CREATE_SUBMIT);
        return Result.success(expenseDocumentService.listPayeeAccountOptions(
                getCurrentUserId(request),
                keyword,
                linkageMode,
                payeeName,
                counterpartyCode,
                paymentCompanyId
        ));
    }

    @PostMapping("/documents")
    public Result<ExpenseDocumentSubmitResultVO> submitDocument(
            @Valid @RequestBody ExpenseDocumentSubmitDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), EXPENSE_CREATE_CREATE, EXPENSE_CREATE_SUBMIT);
        return Result.success(
                "提交成功",
                expenseDocumentService.submitDocument(getCurrentUserId(request), getCurrentUsername(request), dto)
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
        throw new IllegalStateException("未获取到当前登录用户");
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "系统";
    }
}
