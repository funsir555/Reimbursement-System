package com.finex.auth.controller;

import com.finex.auth.dto.FinanceVoucherDetailVO;
import com.finex.auth.dto.FinanceVoucherMetaVO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceVoucherService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/finance/vouchers")
@RequiredArgsConstructor
public class FinanceVoucherController {

    private static final String VOUCHER_VIEW = "finance:general_ledger:new_voucher:view";
    private static final String VOUCHER_CREATE = "finance:general_ledger:new_voucher:create";

    private final FinanceVoucherService financeVoucherService;
    private final AccessControlService accessControlService;

    @GetMapping("/meta")
    public Result<FinanceVoucherMetaVO> meta(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String billDate,
            @RequestParam(required = false) String csign,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), VOUCHER_VIEW);
        return Result.success(
                financeVoucherService.getMeta(
                        getCurrentUserId(request),
                        getCurrentUsername(request),
                        companyId,
                        billDate,
                        csign
                )
        );
    }

    @GetMapping("/{voucherNo}")
    public Result<FinanceVoucherDetailVO> detail(@PathVariable String voucherNo, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), VOUCHER_VIEW);
        return Result.success(financeVoucherService.getDetail(voucherNo));
    }

    @PostMapping
    public Result<FinanceVoucherSaveResultVO> createVoucher(
            @Valid @RequestBody FinanceVoucherSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), VOUCHER_CREATE);
        return Result.success(
                "凭证保存成功",
                financeVoucherService.saveVoucher(dto, getCurrentUserId(request), getCurrentUsername(request))
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
        return "财务制单员";
    }
}
