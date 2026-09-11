package com.finex.auth.controller;

import com.finex.auth.dto.FinanceBalanceSheetRowVO;
import com.finex.auth.dto.FinanceDetailLedgerRowVO;
import com.finex.auth.dto.FinanceGeneralLedgerSectionVO;
import com.finex.auth.dto.FinanceLedgerReportMetaVO;
import com.finex.auth.dto.FinanceLedgerReportPageVO;
import com.finex.auth.dto.FinanceLedgerReportQueryDTO;
import com.finex.auth.dto.FinanceSequenceLedgerRowVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceLedgerReportService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/finance/general-ledger/reports")
@RequiredArgsConstructor
public class FinanceLedgerReportController {

    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String MESSAGE_USER_MISSING = "当前登录用户不存在";

    private final FinanceLedgerReportService financeLedgerReportService;
    private final AccessControlService accessControlService;

    @GetMapping("/meta")
    public Result<FinanceLedgerReportMetaVO> meta(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) Integer iyear,
            @RequestParam(required = false) Integer iperiod,
            HttpServletRequest request
    ) {
        Long currentUserId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(
                currentUserId,
                "finance:general_ledger:balance_sheet:view",
                "finance:general_ledger:detail_ledger:view",
                "finance:general_ledger:general_ledger:view",
                "finance:general_ledger:project_detail_ledger:view",
                "finance:general_ledger:supplier_detail_ledger:view",
                "finance:general_ledger:customer_detail_ledger:view",
                "finance:general_ledger:personal_detail_ledger:view",
                "finance:general_ledger:quantity_amount_detail_ledger:view",
                "finance:general_ledger:sequence_ledger:view"
        );
        return Result.success(financeLedgerReportService.getMeta(currentUserId, companyId, iyear, iperiod));
    }

    @PostMapping("/balance-sheet/query")
    public Result<FinanceLedgerReportPageVO<FinanceBalanceSheetRowVO>> queryBalanceSheet(
            @Valid @RequestBody FinanceLedgerReportQueryDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), "finance:general_ledger:balance_sheet:view");
        return Result.success(financeLedgerReportService.queryBalanceSheet(getCurrentUserId(request), dto));
    }

    @PostMapping("/general-ledger/query")
    public Result<FinanceLedgerReportPageVO<FinanceGeneralLedgerSectionVO>> queryGeneralLedger(
            @Valid @RequestBody FinanceLedgerReportQueryDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), "finance:general_ledger:general_ledger:view");
        return Result.success(financeLedgerReportService.queryGeneralLedger(getCurrentUserId(request), dto));
    }

    @PostMapping("/detail-ledger/query")
    public Result<FinanceLedgerReportPageVO<FinanceDetailLedgerRowVO>> queryDetailLedger(
            @Valid @RequestBody FinanceLedgerReportQueryDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), resolveDetailViewPermission(dto.getLedgerKind()));
        return Result.success(financeLedgerReportService.queryDetailLedger(getCurrentUserId(request), dto));
    }

    @PostMapping("/sequence-ledger/query")
    public Result<FinanceLedgerReportPageVO<FinanceSequenceLedgerRowVO>> querySequenceLedger(
            @Valid @RequestBody FinanceLedgerReportQueryDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), "finance:general_ledger:sequence_ledger:view");
        return Result.success(financeLedgerReportService.querySequenceLedger(getCurrentUserId(request), dto));
    }

    @PostMapping("/balance-sheet/export")
    public ResponseEntity<ByteArrayResource> exportBalanceSheet(
            @Valid @RequestBody FinanceLedgerReportQueryDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), "finance:general_ledger:balance_sheet:export");
        return buildExcelResponse(
                "余额表",
                financeLedgerReportService.exportBalanceSheet(getCurrentUserId(request), dto)
        );
    }

    @PostMapping("/general-ledger/export")
    public ResponseEntity<ByteArrayResource> exportGeneralLedger(
            @Valid @RequestBody FinanceLedgerReportQueryDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), "finance:general_ledger:general_ledger:export");
        return buildExcelResponse(
                "总分类账",
                financeLedgerReportService.exportGeneralLedger(getCurrentUserId(request), dto)
        );
    }

    @PostMapping("/detail-ledger/export")
    public ResponseEntity<ByteArrayResource> exportDetailLedger(
            @Valid @RequestBody FinanceLedgerReportQueryDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), resolveDetailExportPermission(dto.getLedgerKind()));
        return buildExcelResponse(
                resolveDetailExportName(dto.getLedgerKind()),
                financeLedgerReportService.exportDetailLedger(getCurrentUserId(request), dto)
        );
    }

    @PostMapping("/sequence-ledger/export")
    public ResponseEntity<ByteArrayResource> exportSequenceLedger(
            @Valid @RequestBody FinanceLedgerReportQueryDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), "finance:general_ledger:sequence_ledger:export");
        return buildExcelResponse(
                "序时账",
                financeLedgerReportService.exportSequenceLedger(getCurrentUserId(request), dto)
        );
    }

    private ResponseEntity<ByteArrayResource> buildExcelResponse(String reportName, byte[] content) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(reportName + "-" + LocalDateTime.now().format(EXPORT_TIME_FORMATTER) + ".xlsx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(content.length)
                .body(new ByteArrayResource(content));
    }

    private String resolveDetailViewPermission(String ledgerKind) {
        String normalizedLedgerKind = normalizeLedgerKind(ledgerKind);
        return switch (normalizedLedgerKind) {
            case "PROJECT" -> "finance:general_ledger:project_detail_ledger:view";
            case "SUPPLIER" -> "finance:general_ledger:supplier_detail_ledger:view";
            case "CUSTOMER" -> "finance:general_ledger:customer_detail_ledger:view";
            case "PERSONAL" -> "finance:general_ledger:personal_detail_ledger:view";
            case "QUANTITY_AMOUNT" -> "finance:general_ledger:quantity_amount_detail_ledger:view";
            default -> "finance:general_ledger:detail_ledger:view";
        };
    }

    private String resolveDetailExportPermission(String ledgerKind) {
        String normalizedLedgerKind = normalizeLedgerKind(ledgerKind);
        return switch (normalizedLedgerKind) {
            case "PROJECT" -> "finance:general_ledger:project_detail_ledger:export";
            case "SUPPLIER" -> "finance:general_ledger:supplier_detail_ledger:export";
            case "CUSTOMER" -> "finance:general_ledger:customer_detail_ledger:export";
            case "PERSONAL" -> "finance:general_ledger:personal_detail_ledger:export";
            case "QUANTITY_AMOUNT" -> "finance:general_ledger:quantity_amount_detail_ledger:export";
            default -> "finance:general_ledger:detail_ledger:export";
        };
    }

    private String resolveDetailExportName(String ledgerKind) {
        String normalizedLedgerKind = normalizeLedgerKind(ledgerKind);
        return switch (normalizedLedgerKind) {
            case "PROJECT" -> "项目明细账";
            case "SUPPLIER" -> "供应商明细账";
            case "CUSTOMER" -> "客户明细账";
            case "PERSONAL" -> "个人明细账";
            case "QUANTITY_AMOUNT" -> "数量金额明细账";
            default -> "明细账";
        };
    }

    private String normalizeLedgerKind(String ledgerKind) {
        return ledgerKind == null ? "DETAIL" : ledgerKind.trim().toUpperCase();
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
}
