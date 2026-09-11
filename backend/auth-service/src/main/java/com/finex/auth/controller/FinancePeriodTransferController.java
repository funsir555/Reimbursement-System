package com.finex.auth.controller;

import com.finex.auth.dto.FinancePeriodTransferGenerateDTO;
import com.finex.auth.dto.FinancePeriodTransferGenerateResultVO;
import com.finex.auth.dto.FinancePeriodTransferMetaVO;
import com.finex.auth.dto.FinancePeriodTransferPreviewDTO;
import com.finex.auth.dto.FinancePeriodTransferPreviewResultVO;
import com.finex.auth.dto.FinancePeriodTransferRuleDTO;
import com.finex.auth.dto.FinancePeriodTransferRuleVO;
import com.finex.auth.dto.FinancePeriodTransferRunDetailVO;
import com.finex.auth.dto.FinancePeriodTransferRunVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinancePeriodTransferService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/finance/period-transfer")
@RequiredArgsConstructor
public class FinancePeriodTransferController {

    private static final String VIEW_PERMISSION = "finance:general_ledger:period_transfer:view";
    private static final String MANAGE_PERMISSION = "finance:general_ledger:period_transfer:manage";
    private static final String GENERATE_PERMISSION = "finance:general_ledger:period_transfer:generate";

    private final FinancePeriodTransferService financePeriodTransferService;
    private final AccessControlService accessControlService;

    @GetMapping("/meta")
    public Result<FinancePeriodTransferMetaVO> meta(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) Integer iyear,
            @RequestParam(required = false) Integer iperiod,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, VIEW_PERMISSION);
        return Result.success(financePeriodTransferService.getMeta(currentUserId, companyId, iyear, iperiod));
    }

    @GetMapping("/rules")
    public Result<List<FinancePeriodTransferRuleVO>> rules(
            @RequestParam String companyId,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW_PERMISSION);
        return Result.success(financePeriodTransferService.listRules(companyId));
    }

    @PostMapping("/rules")
    public Result<FinancePeriodTransferRuleVO> saveRule(
            @Valid @RequestBody FinancePeriodTransferRuleDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, MANAGE_PERMISSION);
        return Result.success("期末结转规则已保存", financePeriodTransferService.saveRule(dto, currentUserId, getCurrentUsername(request)));
    }

    @PostMapping("/preview")
    public Result<FinancePeriodTransferPreviewResultVO> preview(
            @Valid @RequestBody FinancePeriodTransferPreviewDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, MANAGE_PERMISSION);
        return Result.success(financePeriodTransferService.preview(dto, currentUserId, getCurrentUsername(request)));
    }

    @PostMapping("/generate")
    public Result<FinancePeriodTransferGenerateResultVO> generate(
            @Valid @RequestBody FinancePeriodTransferGenerateDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, GENERATE_PERMISSION);
        return Result.success("期末结转凭证已生成", financePeriodTransferService.generate(dto, currentUserId, getCurrentUsername(request)));
    }

    @GetMapping("/runs")
    public Result<List<FinancePeriodTransferRunVO>> runs(
            @RequestParam String companyId,
            @RequestParam Integer iyear,
            @RequestParam Integer iperiod,
            @RequestParam(required = false) String ruleType,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW_PERMISSION);
        return Result.success(financePeriodTransferService.listRuns(companyId, iyear, iperiod, ruleType));
    }

    @GetMapping("/runs/{runId}")
    public Result<FinancePeriodTransferRunDetailVO> runDetail(
            @RequestParam String companyId,
            @PathVariable Long runId,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW_PERMISSION);
        return Result.success(financePeriodTransferService.getRunDetail(companyId, runId));
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
