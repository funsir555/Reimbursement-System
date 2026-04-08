package com.finex.auth.controller;

import com.finex.auth.dto.FinanceBankBranchVO;
import com.finex.auth.dto.FinanceBankOptionVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceBankCatalogService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth/finance")
@RequiredArgsConstructor
public class FinanceBankCatalogController {

    private static final String EXPENSE_CREATE_VIEW = "expense:create:view";
    private static final String EXPENSE_CREATE_CREATE = "expense:create:create";
    private static final String EXPENSE_CREATE_SUBMIT = "expense:create:submit";
    private static final String PROFILE_VIEW = "profile:view";
    private static final String SUPPLIER_VIEW = "finance:archives:suppliers:view";
    private static final String SUPPLIER_CREATE = "finance:archives:suppliers:create";
    private static final String SUPPLIER_EDIT = "finance:archives:suppliers:edit";

    private final FinanceBankCatalogService financeBankCatalogService;
    private final AccessControlService accessControlService;

    @GetMapping("/banks")
    public Result<List<FinanceBankOptionVO>> listBanks(
            @RequestParam(required = false) String keyword,
            HttpServletRequest request
    ) {
        requireBankLookupPermission(request);
        return Result.success(financeBankCatalogService.listBanks(keyword));
    }

    @GetMapping("/bank-branches")
    public Result<List<FinanceBankBranchVO>> listBankBranches(
            @RequestParam(required = false) String bankCode,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request
    ) {
        requireBankLookupPermission(request);
        return Result.success(financeBankCatalogService.listBankBranches(bankCode, province, city, keyword));
    }

    @GetMapping("/bank-branches/lookup-by-cnaps")
    public Result<FinanceBankBranchVO> lookupByCnaps(
            @RequestParam String cnapsCode,
            HttpServletRequest request
    ) {
        requireBankLookupPermission(request);
        return Result.success(financeBankCatalogService.lookupBranchByCnaps(cnapsCode));
    }

    private void requireBankLookupPermission(HttpServletRequest request) {
        accessControlService.requireAnyPermission(
                getCurrentUserId(request),
                EXPENSE_CREATE_VIEW,
                EXPENSE_CREATE_CREATE,
                EXPENSE_CREATE_SUBMIT,
                PROFILE_VIEW,
                SUPPLIER_VIEW,
                SUPPLIER_CREATE,
                SUPPLIER_EDIT
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
}
