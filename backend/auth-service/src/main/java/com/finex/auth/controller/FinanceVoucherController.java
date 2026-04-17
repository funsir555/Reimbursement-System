package com.finex.auth.controller;

import com.finex.auth.dto.FinanceVoucherActionResultVO;
import com.finex.auth.dto.FinanceVoucherBatchActionDTO;
import com.finex.auth.dto.FinanceVoucherBatchActionResultVO;
import com.finex.auth.dto.FinanceVoucherDetailVO;
import com.finex.auth.dto.FinanceVoucherMetaVO;
import com.finex.auth.dto.FinanceVoucherPageVO;
import com.finex.auth.dto.FinanceVoucherQueryDTO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.dto.FinanceVoucherSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceVoucherService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/auth/finance/vouchers")
@RequiredArgsConstructor
public class FinanceVoucherController {

    private static final String NEW_VOUCHER_VIEW = "finance:general_ledger:new_voucher:view";
    private static final String NEW_VOUCHER_CREATE = "finance:general_ledger:new_voucher:create";
    private static final String QUERY_VOUCHER_VIEW = "finance:general_ledger:query_voucher:view";
    private static final String QUERY_VOUCHER_EXPORT = "finance:general_ledger:query_voucher:export";
    private static final String QUERY_VOUCHER_EDIT = "finance:general_ledger:query_voucher:edit";
    private static final String REVIEW_VOUCHER_VIEW = "finance:general_ledger:review_voucher:view";
    private static final String REVIEW_VOUCHER_REVIEW = "finance:general_ledger:review_voucher:review";
    private static final String REVIEW_VOUCHER_UNREVIEW = "finance:general_ledger:review_voucher:unreview";
    private static final String REVIEW_VOUCHER_MARK_ERROR = "finance:general_ledger:review_voucher:mark_error";

    private static final String MESSAGE_SAVED = "凭证保存成功";
    private static final String MESSAGE_UPDATED = "凭证修改成功";
    private static final String MESSAGE_REVIEWED = "凭证审核成功";
    private static final String MESSAGE_UNREVIEWED = "凭证反审核成功";
    private static final String MESSAGE_MARKED_ERROR = "凭证已标记错误";
    private static final String MESSAGE_CLEARED_ERROR = "凭证错误标记已取消";
    private static final String MESSAGE_BATCH_UPDATED = "凭证批量状态更新成功";
    private static final String MESSAGE_USER_MISSING = "当前登录用户不存在";
    private static final String EXPORT_PREFIX = "凭证查询-";

    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final FinanceVoucherService financeVoucherService;
    private final AccessControlService accessControlService;

    @GetMapping
    public Result<FinanceVoucherPageVO<FinanceVoucherSummaryVO>> list(
            @RequestParam String companyId,
            @RequestParam(required = false) String voucherNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String csign,
            @RequestParam(required = false) String billMonth,
            @RequestParam(required = false) String billMonthFrom,
            @RequestParam(required = false) String billMonthTo,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), QUERY_VOUCHER_VIEW, REVIEW_VOUCHER_VIEW);
        FinanceVoucherQueryDTO dto = new FinanceVoucherQueryDTO();
        dto.setCompanyId(companyId);
        dto.setVoucherNo(voucherNo);
        dto.setStatus(status);
        dto.setCsign(csign);
        dto.setBillMonth(billMonth);
        dto.setBillMonthFrom(billMonthFrom);
        dto.setBillMonthTo(billMonthTo);
        dto.setSummary(summary);
        dto.setPage(page);
        dto.setPageSize(pageSize);
        return Result.success(financeVoucherService.queryVouchers(dto));
    }

    @GetMapping("/meta")
    public Result<FinanceVoucherMetaVO> meta(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String billDate,
            @RequestParam(required = false) String csign,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(currentUserId, NEW_VOUCHER_VIEW, QUERY_VOUCHER_VIEW, REVIEW_VOUCHER_VIEW);
        return Result.success(
                financeVoucherService.getMeta(
                        currentUserId,
                        getCurrentUsername(request),
                        companyId,
                        billDate,
                        csign
                )
        );
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> export(
            @RequestParam String companyId,
            @RequestParam(required = false) String voucherNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String csign,
            @RequestParam(required = false) String billMonth,
            @RequestParam(required = false) String billMonthFrom,
            @RequestParam(required = false) String billMonthTo,
            @RequestParam(required = false) String summary,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), QUERY_VOUCHER_EXPORT);

        FinanceVoucherQueryDTO dto = new FinanceVoucherQueryDTO();
        dto.setCompanyId(companyId);
        dto.setVoucherNo(voucherNo);
        dto.setStatus(status);
        dto.setCsign(csign);
        dto.setBillMonth(billMonth);
        dto.setBillMonthFrom(billMonthFrom);
        dto.setBillMonthTo(billMonthTo);
        dto.setSummary(summary);

        byte[] content = financeVoucherService.exportVouchers(dto);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(EXPORT_PREFIX + LocalDateTime.now().format(EXPORT_TIME_FORMATTER) + ".csv", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .contentLength(content.length)
                .body(new ByteArrayResource(content));
    }

    @GetMapping("/{voucherNo}")
    public Result<FinanceVoucherDetailVO> detail(
            @PathVariable String voucherNo,
            @RequestParam String companyId,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), QUERY_VOUCHER_VIEW, REVIEW_VOUCHER_VIEW);
        return Result.success(financeVoucherService.getDetail(companyId, voucherNo));
    }

    @PostMapping
    public Result<FinanceVoucherSaveResultVO> createVoucher(
            @Valid @RequestBody FinanceVoucherSaveDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, NEW_VOUCHER_CREATE);
        return Result.success(
                MESSAGE_SAVED,
                financeVoucherService.saveVoucher(dto, currentUserId, getCurrentUsername(request))
        );
    }

    @PutMapping("/{voucherNo}")
    public Result<FinanceVoucherSaveResultVO> updateVoucher(
            @PathVariable String voucherNo,
            @RequestParam String companyId,
            @Valid @RequestBody FinanceVoucherSaveDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, QUERY_VOUCHER_EDIT);
        return Result.success(
                MESSAGE_UPDATED,
                financeVoucherService.updateVoucher(companyId, voucherNo, dto, currentUserId, getCurrentUsername(request))
        );
    }

    @PostMapping("/{voucherNo}/review")
    public Result<FinanceVoucherActionResultVO> reviewVoucher(
            @PathVariable String voucherNo,
            @RequestParam String companyId,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, REVIEW_VOUCHER_REVIEW);
        return Result.success(
                MESSAGE_REVIEWED,
                financeVoucherService.reviewVoucher(companyId, voucherNo, currentUserId, getCurrentUsername(request))
        );
    }

    @PostMapping("/{voucherNo}/unreview")
    public Result<FinanceVoucherActionResultVO> unreviewVoucher(
            @PathVariable String voucherNo,
            @RequestParam String companyId,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), REVIEW_VOUCHER_UNREVIEW);
        return Result.success(MESSAGE_UNREVIEWED, financeVoucherService.unreviewVoucher(companyId, voucherNo));
    }

    @PostMapping("/{voucherNo}/mark-error")
    public Result<FinanceVoucherActionResultVO> markVoucherError(
            @PathVariable String voucherNo,
            @RequestParam String companyId,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), REVIEW_VOUCHER_MARK_ERROR);
        return Result.success(MESSAGE_MARKED_ERROR, financeVoucherService.markVoucherError(companyId, voucherNo));
    }

    @PostMapping("/{voucherNo}/clear-error")
    public Result<FinanceVoucherActionResultVO> clearVoucherError(
            @PathVariable String voucherNo,
            @RequestParam String companyId,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), REVIEW_VOUCHER_MARK_ERROR);
        return Result.success(MESSAGE_CLEARED_ERROR, financeVoucherService.clearVoucherError(companyId, voucherNo));
    }

    @PostMapping("/actions")
    public Result<FinanceVoucherBatchActionResultVO> batchUpdateVoucherState(
            @Valid @RequestBody FinanceVoucherBatchActionDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        requireBatchActionPermission(currentUserId, dto.getAction());
        return Result.success(
                MESSAGE_BATCH_UPDATED,
                financeVoucherService.batchUpdateVoucherState(dto, currentUserId, getCurrentUsername(request))
        );
    }

    private void requireBatchActionPermission(Long currentUserId, String action) {
        String normalizedAction = action == null ? "" : action.trim().toUpperCase();
        switch (normalizedAction) {
            case "REVIEW" -> accessControlService.requirePermission(currentUserId, REVIEW_VOUCHER_REVIEW);
            case "UNREVIEW" -> accessControlService.requirePermission(currentUserId, REVIEW_VOUCHER_UNREVIEW);
            case "MARK_ERROR", "CLEAR_ERROR" -> accessControlService.requirePermission(currentUserId, REVIEW_VOUCHER_MARK_ERROR);
            default -> throw new IllegalArgumentException("凭证动作不合法");
        }
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId instanceof Long value) {
            return value;
        }
        if (userId instanceof Integer value) {
            return value.longValue();
        }
        throw new IllegalStateException(MESSAGE_USER_MISSING);
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "system";
    }
}
