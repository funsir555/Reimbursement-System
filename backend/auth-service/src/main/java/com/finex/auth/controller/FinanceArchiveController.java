package com.finex.auth.controller;

import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.dto.FinanceVendorSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceVendorService;
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

@RestController
@RequestMapping("/auth/finance/archives/suppliers")
@RequiredArgsConstructor
public class FinanceArchiveController {

    private static final String SUPPLIER_VIEW = "finance:archives:suppliers:view";
    private static final String SUPPLIER_CREATE = "finance:archives:suppliers:create";
    private static final String SUPPLIER_EDIT = "finance:archives:suppliers:edit";
    private static final String SUPPLIER_DELETE = "finance:archives:suppliers:delete";

    private final FinanceVendorService financeVendorService;
    private final AccessControlService accessControlService;

    @GetMapping
    public Result<List<FinanceVendorSummaryVO>> listVendors(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean includeDisabled,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), SUPPLIER_VIEW);
        return Result.success(financeVendorService.listVendors(keyword, includeDisabled));
    }

    @GetMapping("/{vendorCode}")
    public Result<FinanceVendorDetailVO> getVendorDetail(
            @PathVariable String vendorCode,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), SUPPLIER_VIEW);
        return Result.success(financeVendorService.getVendorDetail(vendorCode));
    }

    @PostMapping
    public Result<FinanceVendorDetailVO> createVendor(
            @Valid @RequestBody FinanceVendorSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SUPPLIER_CREATE, SUPPLIER_EDIT);
        return Result.success("供应商保存成功", financeVendorService.createVendor(dto, getCurrentUsername(request)));
    }

    @PutMapping("/{vendorCode}")
    public Result<FinanceVendorDetailVO> updateVendor(
            @PathVariable String vendorCode,
            @Valid @RequestBody FinanceVendorSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), SUPPLIER_EDIT);
        return Result.success("供应商更新成功", financeVendorService.updateVendor(vendorCode, dto, getCurrentUsername(request)));
    }

    @DeleteMapping("/{vendorCode}")
    public Result<Boolean> disableVendor(
            @PathVariable String vendorCode,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SUPPLIER_DELETE, SUPPLIER_EDIT);
        return Result.success("供应商已停用", financeVendorService.disableVendor(vendorCode, getCurrentUsername(request)));
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
        return "system";
    }
}
