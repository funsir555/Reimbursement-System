// 这里是 ExpenseVoucherGenerationController 的后端接口入口。
// 它主要负责接收请求、校验权限并调用下游 Service。
// 如果改错，最容易影响这一组接口的查询、保存或状态流转。

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

/**
 * 这是 ExpenseVoucherGenerationController 控制器。
 * 它主要负责接收请求、校验权限并调用下游 Service。
 * 具体业务规则以 Service 层为准。
 */
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

    // 处理 meta 请求。
    @GetMapping("/meta")
    public Result<ExpenseVoucherGenerationMetaVO> meta(HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PAGE_VIEW, MAPPING_VIEW, PUSH_VIEW, QUERY_VIEW);
        return Result.success(expenseVoucherGenerationService.getMeta(getCurrentUserId(request)));
    }

    // 处理 getMappings 请求。
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

    // 处理 createTemplatePolicy 请求。
    @PostMapping("/mappings/template-policy")
    public Result<ExpenseVoucherTemplatePolicyVO> createTemplatePolicy(
            @Valid @RequestBody ExpenseVoucherTemplatePolicySaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), MAPPING_EDIT);
        return Result.success(
                "模板策略新增成功",
                expenseVoucherGenerationService.createTemplatePolicy(dto, getCurrentUserId(request), getCurrentUsername(request))
        );
    }

    // 处理 updateTemplatePolicy 请求。
    @PutMapping("/mappings/template-policy/{id}")
    public Result<ExpenseVoucherTemplatePolicyVO> updateTemplatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseVoucherTemplatePolicySaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), MAPPING_EDIT);
        return Result.success(
                "模板策略更新成功",
                expenseVoucherGenerationService.updateTemplatePolicy(id, dto, getCurrentUserId(request), getCurrentUsername(request))
        );
    }

    // 处理 createSubjectMapping 请求。
    @PostMapping("/mappings/subject-lines")
    public Result<ExpenseVoucherSubjectMappingVO> createSubjectMapping(
            @Valid @RequestBody ExpenseVoucherSubjectMappingSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), MAPPING_EDIT);
        return Result.success(
                "科目映射新增成功",
                expenseVoucherGenerationService.createSubjectMapping(dto, getCurrentUserId(request), getCurrentUsername(request))
        );
    }

    // 处理 updateSubjectMapping 请求。
    @PutMapping("/mappings/subject-lines/{id}")
    public Result<ExpenseVoucherSubjectMappingVO> updateSubjectMapping(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseVoucherSubjectMappingSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), MAPPING_EDIT);
        return Result.success(
                "科目映射更新成功",
                expenseVoucherGenerationService.updateSubjectMapping(id, dto, getCurrentUserId(request), getCurrentUsername(request))
        );
    }

    // 处理 getPushDocuments 请求。
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

    // 处理 push 请求。
    @PostMapping("/push")
    public Result<ExpenseVoucherPushBatchResultVO> push(
            @Valid @RequestBody ExpenseVoucherPushDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PUSH_EXECUTE);
        return Result.success(
                "凭证推送已发起",
                expenseVoucherGenerationService.pushDocuments(dto, getCurrentUserId(request), getCurrentUsername(request))
        );
    }

    // 处理 getGeneratedVouchers 请求。
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

    // 处理 getGeneratedVoucherDetail 请求。
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
        throw new IllegalStateException("缺少当前用户信息");
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "system";
    }
}
