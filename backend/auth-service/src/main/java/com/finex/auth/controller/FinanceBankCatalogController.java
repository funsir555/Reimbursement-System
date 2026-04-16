// 这里是 FinanceBankCatalogController 的后端接口入口。
// 它主要负责接收请求、校验权限并调用下游 Service。
// 如果改错，最容易影响这一组接口的查询、保存或状态流转。

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

/**
 * 这是 FinanceBankCatalogController 控制器。
 * 它主要负责接收请求、校验权限并调用下游 Service。
 * 具体业务规则以 Service 层为准。
 */
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
    private static final String COMPANY_ACCOUNT_VIEW = "settings:company_accounts:view";
    private static final String COMPANY_ACCOUNT_CREATE = "settings:company_accounts:create";
    private static final String COMPANY_ACCOUNT_EDIT = "settings:company_accounts:edit";

    private final FinanceBankCatalogService financeBankCatalogService;
    private final AccessControlService accessControlService;

    // 处理 listBanks 请求。
    @GetMapping("/banks")
    public Result<List<FinanceBankOptionVO>> listBanks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String businessScope,
            HttpServletRequest request
    ) {
        requireBankLookupPermission(request);
        return Result.success(financeBankCatalogService.listBanks(keyword, businessScope));
    }

    @GetMapping("/banks/provinces")
    public Result<List<String>> listBankProvinces(
            @RequestParam String bankCode,
            @RequestParam(required = false) String businessScope,
            HttpServletRequest request
    ) {
        requireBankLookupPermission(request);
        return Result.success(financeBankCatalogService.listProvinces(bankCode, businessScope));
    }

    @GetMapping("/banks/cities")
    public Result<List<String>> listBankCities(
            @RequestParam String bankCode,
            @RequestParam String province,
            @RequestParam(required = false) String businessScope,
            HttpServletRequest request
    ) {
        requireBankLookupPermission(request);
        return Result.success(financeBankCatalogService.listCities(bankCode, province, businessScope));
    }

    // 处理 listBankBranches 请求。
    @GetMapping("/bank-branches")
    public Result<List<FinanceBankBranchVO>> listBankBranches(
            @RequestParam(required = false) String bankCode,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String businessScope,
            HttpServletRequest request
    ) {
        requireBankLookupPermission(request);
        return Result.success(financeBankCatalogService.listBankBranches(bankCode, province, city, keyword, businessScope));
    }

    // 处理 lookupByCnaps 请求。
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
                SUPPLIER_EDIT,
                COMPANY_ACCOUNT_VIEW,
                COMPANY_ACCOUNT_CREATE,
                COMPANY_ACCOUNT_EDIT
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
