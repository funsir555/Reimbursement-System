// 这里是 FinanceVoucherController 的后端接口入口。
// 它主要负责接收请求、校验权限并调用下游 Service。
// 如果改错，最容易影响这一组接口的查询、保存或状态流转。

package com.finex.auth.controller;

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

/**
 * 这是 FinanceVoucherController 控制器。
 * 它主要负责接收请求、校验权限并调用下游 Service。
 * 具体业务规则以 Service 层为准。
 */
@RestController
@RequestMapping("/auth/finance/vouchers")
@RequiredArgsConstructor
public class FinanceVoucherController {

    private static final String NEW_VOUCHER_VIEW = "finance:general_ledger:new_voucher:view";
    private static final String NEW_VOUCHER_CREATE = "finance:general_ledger:new_voucher:create";
    private static final String QUERY_VOUCHER_VIEW = "finance:general_ledger:query_voucher:view";
    private static final String QUERY_VOUCHER_EXPORT = "finance:general_ledger:query_voucher:export";
    private static final String QUERY_VOUCHER_EDIT = "finance:general_ledger:query_voucher:edit";
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final FinanceVoucherService financeVoucherService;
    private final AccessControlService accessControlService;

    // 处理 list 请求。
    @GetMapping
    public Result<FinanceVoucherPageVO<FinanceVoucherSummaryVO>> list(
            @RequestParam String companyId,
            @RequestParam(required = false) String voucherNo,
            @RequestParam(required = false) String csign,
            @RequestParam(required = false) String billMonth,
            @RequestParam(required = false) String billMonthFrom,
            @RequestParam(required = false) String billMonthTo,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), QUERY_VOUCHER_VIEW);
        FinanceVoucherQueryDTO dto = new FinanceVoucherQueryDTO();
        dto.setCompanyId(companyId);
        dto.setVoucherNo(voucherNo);
        dto.setCsign(csign);
        dto.setBillMonth(billMonth);
        dto.setBillMonthFrom(billMonthFrom);
        dto.setBillMonthTo(billMonthTo);
        dto.setSummary(summary);
        dto.setPage(page);
        dto.setPageSize(pageSize);
        return Result.success(financeVoucherService.queryVouchers(dto));
    }

    // 处理 meta 请求。
    @GetMapping("/meta")
    public Result<FinanceVoucherMetaVO> meta(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String billDate,
            @RequestParam(required = false) String csign,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(currentUserId, NEW_VOUCHER_VIEW, QUERY_VOUCHER_VIEW);
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

    // 处理 export 请求。
    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> export(
            @RequestParam String companyId,
            @RequestParam(required = false) String voucherNo,
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
        dto.setCsign(csign);
        dto.setBillMonth(billMonth);
        dto.setBillMonthFrom(billMonthFrom);
        dto.setBillMonthTo(billMonthTo);
        dto.setSummary(summary);

        byte[] content = financeVoucherService.exportVouchers(dto);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("凭证查询-" + LocalDateTime.now().format(EXPORT_TIME_FORMATTER) + ".csv", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .contentLength(content.length)
                .body(new ByteArrayResource(content));
    }

    // 处理 detail 请求。
    @GetMapping("/{voucherNo}")
    public Result<FinanceVoucherDetailVO> detail(
            @PathVariable String voucherNo,
            @RequestParam String companyId,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), QUERY_VOUCHER_VIEW);
        return Result.success(financeVoucherService.getDetail(companyId, voucherNo));
    }

    // 处理 createVoucher 请求。
    @PostMapping
    public Result<FinanceVoucherSaveResultVO> createVoucher(
            @Valid @RequestBody FinanceVoucherSaveDTO dto,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requirePermission(currentUserId, NEW_VOUCHER_CREATE);
        return Result.success(
                "凭证保存成功",
                financeVoucherService.saveVoucher(dto, currentUserId, getCurrentUsername(request))
        );
    }

    // 处理 updateVoucher 请求。
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
                "凭证更新成功",
                financeVoucherService.updateVoucher(companyId, voucherNo, dto, currentUserId, getCurrentUsername(request))
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
        throw new IllegalStateException("Missing current user id in request context");
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "system";
    }
}
