package com.finex.auth.controller;

import com.finex.auth.dto.FinanceCashFlowItemSaveDTO;
import com.finex.auth.dto.FinanceCashFlowItemStatusDTO;
import com.finex.auth.dto.FinanceCashFlowItemSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceCashFlowArchiveService;
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
@RequestMapping("/auth/finance/archives/cash-flows")
@RequiredArgsConstructor
public class FinanceCashFlowArchiveController {

    private static final String ARCHIVE_VIEW = "finance:archives:projects:view";
    private static final String ARCHIVE_CREATE = "finance:archives:projects:create";
    private static final String ARCHIVE_EDIT = "finance:archives:projects:edit";
    private static final String ARCHIVE_DISABLE = "finance:archives:projects:disable";

    private final FinanceCashFlowArchiveService financeCashFlowArchiveService;
    private final AccessControlService accessControlService;

    @GetMapping
    public Result<List<FinanceCashFlowItemSummaryVO>> listCashFlows(
            @RequestParam String companyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), ARCHIVE_VIEW);
        return Result.success(financeCashFlowArchiveService.listCashFlows(companyId, keyword, direction, status));
    }

    @PostMapping
    public Result<FinanceCashFlowItemSummaryVO> createCashFlow(
            @RequestParam String companyId,
            @Valid @RequestBody FinanceCashFlowItemSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), ARCHIVE_CREATE, ARCHIVE_EDIT);
        return Result.success("现金流量创建成功", financeCashFlowArchiveService.createCashFlow(companyId, dto));
    }

    @PutMapping("/{id}")
    public Result<FinanceCashFlowItemSummaryVO> updateCashFlow(
            @RequestParam String companyId,
            @PathVariable Long id,
            @Valid @RequestBody FinanceCashFlowItemSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), ARCHIVE_EDIT);
        return Result.success("现金流量更新成功", financeCashFlowArchiveService.updateCashFlow(companyId, id, dto));
    }

    @PostMapping("/{id}/status")
    public Result<Boolean> updateCashFlowStatus(
            @RequestParam String companyId,
            @PathVariable Long id,
            @Valid @RequestBody FinanceCashFlowItemStatusDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), ARCHIVE_DISABLE, ARCHIVE_EDIT);
        return Result.success("现金流量状态更新成功", financeCashFlowArchiveService.updateCashFlowStatus(companyId, id, dto));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId instanceof Long value) {
            return value;
        }
        if (userId instanceof Integer value) {
            return value.longValue();
        }
        throw new IllegalStateException("当前用户信息缺失");
    }
}
