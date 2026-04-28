package com.finex.auth.controller;

import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.OpeningAssistBalanceLineVO;
import com.finex.auth.dto.OpeningBalanceAssistSaveDTO;
import com.finex.auth.dto.OpeningBalanceCommitDTO;
import com.finex.auth.dto.OpeningBalanceCarryForwardPreviewVO;
import com.finex.auth.dto.OpeningBalanceMetaVO;
import com.finex.auth.dto.OpeningBalanceReconcileResultVO;
import com.finex.auth.dto.OpeningBalanceRowVO;
import com.finex.auth.dto.OpeningBalanceSaveDTO;
import com.finex.auth.dto.OpeningBalanceTaskRequestDTO;
import com.finex.auth.dto.OpeningBalanceTrialResultVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceOpeningBalanceService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/finance/opening-balance")
@RequiredArgsConstructor
public class FinanceOpeningBalanceController {

    private static final String VIEW = "finance:general_ledger:opening_balance:view";

    private final FinanceOpeningBalanceService financeOpeningBalanceService;
    private final AccessControlService accessControlService;

    @GetMapping("/meta")
    public Result<OpeningBalanceMetaVO> meta(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) Integer iyear,
            @RequestParam(required = false) Integer iperiod,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, VIEW);
        return Result.success(financeOpeningBalanceService.getMeta(currentUserId, getCurrentUsername(request), companyId, iyear, iperiod));
    }

    @GetMapping("/rows")
    public Result<List<OpeningBalanceRowVO>> rows(
            @RequestParam String companyId,
            @RequestParam Integer iyear,
            @RequestParam Integer iperiod,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success(financeOpeningBalanceService.listRows(companyId, iyear, iperiod));
    }

    @PutMapping("/rows")
    public Result<List<OpeningBalanceRowVO>> saveRows(
            @Valid @RequestBody OpeningBalanceSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success("期初余额保存成功", financeOpeningBalanceService.saveRows(dto, getCurrentUsername(request)));
    }

    @GetMapping("/{subjectCode}/assist-balances")
    public Result<List<OpeningAssistBalanceLineVO>> assistBalances(
            @PathVariable String subjectCode,
            @RequestParam String companyId,
            @RequestParam Integer iyear,
            @RequestParam Integer iperiod,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success(financeOpeningBalanceService.getAssistBalances(companyId, iyear, iperiod, subjectCode));
    }

    @PutMapping("/{subjectCode}/assist-balances")
    public Result<List<OpeningAssistBalanceLineVO>> saveAssistBalances(
            @PathVariable String subjectCode,
            @Valid @RequestBody OpeningBalanceAssistSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success("辅助期初保存成功", financeOpeningBalanceService.saveAssistBalances(subjectCode, dto, getCurrentUsername(request)));
    }

    @PutMapping("/commit")
    public Result<List<OpeningBalanceRowVO>> commit(
            @Valid @RequestBody OpeningBalanceCommitDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success("期初余额保存成功", financeOpeningBalanceService.commit(dto, getCurrentUsername(request)));
    }

    @PostMapping("/open-book")
    public Result<AsyncTaskSubmitResultVO> openBook(
            @Valid @RequestBody OpeningBalanceTaskRequestDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, VIEW);
        return Result.success("开账任务已提交", financeOpeningBalanceService.openBook(currentUserId, getCurrentUsername(request), dto));
    }

    @PostMapping("/carry-forward")
    public Result<AsyncTaskSubmitResultVO> carryForward(
            @Valid @RequestBody OpeningBalanceTaskRequestDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, VIEW);
        return Result.success("结转任务已提交", financeOpeningBalanceService.carryForward(currentUserId, getCurrentUsername(request), dto));
    }

    @PostMapping("/carry-forward-preview")
    public Result<OpeningBalanceCarryForwardPreviewVO> carryForwardPreview(
            @Valid @RequestBody OpeningBalanceTaskRequestDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success(financeOpeningBalanceService.carryForwardPreview(dto.getCompanyId(), dto.getIyear(), dto.getIperiod(), getCurrentUsername(request)));
    }

    @PostMapping("/trial-balance")
    public Result<OpeningBalanceTrialResultVO> trialBalance(
            @Valid @RequestBody OpeningBalanceTaskRequestDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success(financeOpeningBalanceService.trialBalance(dto.getCompanyId(), dto.getIyear(), dto.getIperiod(), getCurrentUsername(request)));
    }

    @PostMapping("/reconcile")
    public Result<OpeningBalanceReconcileResultVO> reconcile(
            @Valid @RequestBody OpeningBalanceTaskRequestDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success(financeOpeningBalanceService.reconcile(dto.getCompanyId(), dto.getIyear(), dto.getIperiod(), getCurrentUsername(request)));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId instanceof Long value) {
            return value;
        }
        if (userId instanceof Integer value) {
            return value.longValue();
        }
        throw new IllegalStateException("缺少当前用户ID");
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "system";
    }
}
