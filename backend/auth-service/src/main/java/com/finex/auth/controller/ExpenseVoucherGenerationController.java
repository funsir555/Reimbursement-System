package com.finex.auth.controller;

import com.finex.auth.dto.ExpenseVoucherGeneratedRecordDetailVO;
import com.finex.auth.dto.ExpenseVoucherGeneratedRecordVO;
import com.finex.auth.dto.ExpenseVoucherGenerationMetaVO;
import com.finex.auth.dto.ExpenseVoucherPageVO;
import com.finex.auth.dto.ExpenseVoucherPushBatchResultVO;
import com.finex.auth.dto.ExpenseVoucherPushDTO;
import com.finex.auth.dto.ExpenseVoucherPushDocumentVO;
import com.finex.auth.dto.ExpenseVoucherSubjectMappingSaveDTO;
import com.finex.auth.dto.ExpenseVoucherSubjectMappingVO;
import com.finex.auth.dto.ExpenseVoucherTemplatePolicySaveDTO;
import com.finex.auth.dto.ExpenseVoucherTemplatePolicyVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseVoucherGenerationService;
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

@RestController
@RequestMapping("/auth/expenses/voucher-generation")
@RequiredArgsConstructor
public class ExpenseVoucherGenerationController {

    private static final String PAGE_VIEW = "expense:voucher_generation:view";
    private static final String MAPPING_VIEW = "expense:voucher_generation:mapping:view";
    private static final String MAPPING_EDIT = "expense:voucher_generation:mapping:edit";
    private static final String PUSH_VIEW = "expense:voucher_generation:push:view";
    private static final String PUSH_EXECUTE = "expense:voucher_generation:push:execute";
    private static final String QUERY_VIEW = "expense:voucher_generation:query:view";

    private final ExpenseVoucherGenerationService expenseVoucherGenerationService;
    private final AccessControlService accessControlService;

    @GetMapping("/meta")
    public Result<ExpenseVoucherGenerationMetaVO> meta(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PAGE_VIEW, MAPPING_VIEW, PUSH_VIEW, QUERY_VIEW);
        return Result.success(expenseVoucherGenerationService.getMeta(getCurrentUserId(request)));
    }

    @GetMapping("/mappings")
    public Result<?> getMappings(
            @RequestParam String type,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) String expenseTypeCode,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), MAPPING_VIEW);
        if ("template".equalsIgnoreCase(type)) {
            ExpenseVoucherPageVO<ExpenseVoucherTemplatePolicyVO> result = expenseVoucherGenerationService.getTemplatePolicies(
                    companyId,
                    templateCode,
                    enabled,
                    page,
                    pageSize
            );
            return Result.success(result);
        }
        ExpenseVoucherPageVO<ExpenseVoucherSubjectMappingVO> result = expenseVoucherGenerationService.getSubjectMappings(
                companyId,
                templateCode,
                expenseTypeCode,
                enabled,
                page,
                pageSize
        );
        return Result.success(result);
    }

    @PostMapping("/mappings/template-policy")
    public Result<ExpenseVoucherTemplatePolicyVO> createTemplatePolicy(
            @Valid @RequestBody ExpenseVoucherTemplatePolicySaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), MAPPING_EDIT);
        return Result.success(
                "??????????",
                expenseVoucherGenerationService.createTemplatePolicy(dto, getCurrentUserId(request), getCurrentUsername(request))
        );
    }

    @PutMapping("/mappings/template-policy/{id}")
    public Result<ExpenseVoucherTemplatePolicyVO> updateTemplatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseVoucherTemplatePolicySaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), MAPPING_EDIT);
        return Result.success(
                "??????????",
                expenseVoucherGenerationService.updateTemplatePolicy(id, dto, getCurrentUserId(request), getCurrentUsername(request))
        );
    }

    @PostMapping("/mappings/subject-lines")
    public Result<ExpenseVoucherSubjectMappingVO> createSubjectMapping(
            @Valid @RequestBody ExpenseVoucherSubjectMappingSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), MAPPING_EDIT);
        return Result.success(
                "????????????",
                expenseVoucherGenerationService.createSubjectMapping(dto, getCurrentUserId(request), getCurrentUsername(request))
        );
    }

    @PutMapping("/mappings/subject-lines/{id}")
    public Result<ExpenseVoucherSubjectMappingVO> updateSubjectMapping(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseVoucherSubjectMappingSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), MAPPING_EDIT);
        return Result.success(
                "????????????",
                expenseVoucherGenerationService.updateSubjectMapping(id, dto, getCurrentUserId(request), getCurrentUsername(request))
        );
    }

    @GetMapping("/push-documents")
    public Result<ExpenseVoucherPageVO<ExpenseVoucherPushDocumentVO>> getPushDocuments(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String pushStatus,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PUSH_VIEW);
        return Result.success(expenseVoucherGenerationService.getPushDocuments(
                companyId,
                templateCode,
                keyword,
                pushStatus,
                dateFrom,
                dateTo,
                page,
                pageSize
        ));
    }

    @PostMapping("/push")
    public Result<ExpenseVoucherPushBatchResultVO> push(
            @Valid @RequestBody ExpenseVoucherPushDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PUSH_EXECUTE);
        return Result.success(
                "??????",
                expenseVoucherGenerationService.pushDocuments(dto, getCurrentUserId(request), getCurrentUsername(request))
        );
    }

    @GetMapping("/vouchers")
    public Result<ExpenseVoucherPageVO<ExpenseVoucherGeneratedRecordVO>> getGeneratedVouchers(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) String documentCode,
            @RequestParam(required = false) String voucherNo,
            @RequestParam(required = false) String pushStatus,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), QUERY_VIEW);
        return Result.success(expenseVoucherGenerationService.getGeneratedVouchers(
                companyId,
                templateCode,
                documentCode,
                voucherNo,
                pushStatus,
                dateFrom,
                dateTo,
                page,
                pageSize
        ));
    }

    @GetMapping("/vouchers/{id}")
    public Result<ExpenseVoucherGeneratedRecordDetailVO> getGeneratedVoucherDetail(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), QUERY_VIEW);
        return Result.success(expenseVoucherGenerationService.getGeneratedVoucherDetail(id));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId instanceof Long value) {
            return value;
        }
        if (userId instanceof Integer value) {
            return value.longValue();
        }
        throw new IllegalStateException("??????????");
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "????";
    }
}
