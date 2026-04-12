// 这里是 FinanceCustomerArchiveController 的后端接口入口。
// 它主要负责接收请求、校验权限并调用下游 Service。
// 如果改错，最容易影响这一组接口的查询、保存或状态流转。

package com.finex.auth.controller;

import com.finex.auth.dto.FinanceCustomerDetailVO;
import com.finex.auth.dto.FinanceCustomerSaveDTO;
import com.finex.auth.dto.FinanceCustomerSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceCustomerService;
import com.finex.common.Result;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 这是 FinanceCustomerArchiveController 控制器。
 * 它主要负责接收请求、校验权限并调用下游 Service。
 * 具体业务规则以 Service 层为准。
 */
@RestController
@RequestMapping("/auth/finance/archives/customers")
@RequiredArgsConstructor
public class FinanceCustomerArchiveController {

    private static final String CUSTOMER_VIEW = "finance:archives:customers:view";
    private static final String CUSTOMER_CREATE = "finance:archives:customers:create";
    private static final String CUSTOMER_EDIT = "finance:archives:customers:edit";
    private static final String CUSTOMER_DELETE = "finance:archives:customers:delete";

    private final FinanceCustomerService financeCustomerService;
    private final AccessControlService accessControlService;

    // 处理 listCustomers 请求。
    @GetMapping
    public Result<List<FinanceCustomerSummaryVO>> listCustomers(
            @RequestParam String companyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean includeDisabled,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), CUSTOMER_VIEW);
        return Result.success(financeCustomerService.listCustomers(companyId, keyword, includeDisabled));
    }

    // 处理 getCustomerDetail 请求。
    @GetMapping("/{customerCode}")
    public Result<FinanceCustomerDetailVO> getCustomerDetail(
            @RequestParam String companyId,
            @PathVariable String customerCode,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), CUSTOMER_VIEW);
        return Result.success(financeCustomerService.getCustomerDetail(companyId, customerCode));
    }

    // 处理 createCustomer 请求。
    @PostMapping
    public Result<FinanceCustomerDetailVO> createCustomer(
            @RequestParam String companyId,
            @Valid @RequestBody FinanceCustomerSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), CUSTOMER_CREATE, CUSTOMER_EDIT);
        return Result.success("瀹㈡埛淇濆瓨鎴愬姛", financeCustomerService.createCustomer(companyId, dto, getCurrentUsername(request)));
    }

    // 处理 updateCustomer 请求。
    @PutMapping("/{customerCode}")
    public Result<FinanceCustomerDetailVO> updateCustomer(
            @RequestParam String companyId,
            @PathVariable String customerCode,
            @Valid @RequestBody FinanceCustomerSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), CUSTOMER_EDIT);
        return Result.success("瀹㈡埛鏇存柊鎴愬姛", financeCustomerService.updateCustomer(companyId, customerCode, dto, getCurrentUsername(request)));
    }

    // 处理 disableCustomer 请求。
    @DeleteMapping("/{customerCode}")
    public Result<Boolean> disableCustomer(
            @RequestParam String companyId,
            @PathVariable String customerCode,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), CUSTOMER_DELETE, CUSTOMER_EDIT);
        return Result.success("瀹㈡埛宸插仠鐢?", financeCustomerService.disableCustomer(companyId, customerCode, getCurrentUsername(request)));
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
        return "system";
    }
}
