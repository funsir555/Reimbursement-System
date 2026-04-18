// 杩欓噷鏄?ExpenseDocumentQueryController 鐨勫悗绔帴鍙ｅ叆鍙ｃ€?
// 瀹冧富瑕佽礋璐ｆ帴鏀惰姹傘€佹牎楠屾潈闄愬苟璋冪敤涓嬫父 Service銆?
// 濡傛灉鏀归敊锛屾渶瀹规槗褰卞搷杩欎竴缁勬帴鍙ｇ殑鏌ヨ銆佷繚瀛樻垨鐘舵€佹祦杞€?

package com.finex.auth.controller;

import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseDocumentCommentDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentNavigationVO;
import com.finex.auth.dto.ExpenseDocumentPickerVO;
import com.finex.auth.dto.ExpenseDocumentReminderDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.impl.expense.ExpenseDocumentPrintOrientation;
import com.finex.auth.service.impl.expense.ExpenseDocumentPrintService;
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
import java.util.Arrays;
import java.util.List;

/**
 * 杩欐槸 ExpenseDocumentQueryController 鎺у埗鍣ㄣ€?
 * 瀹冧富瑕佽礋璐ｆ帴鏀惰姹傘€佹牎楠屾潈闄愬苟璋冪敤涓嬫父 Service銆?
 * 鍏蜂綋涓氬姟瑙勫垯浠?Service 灞備负鍑嗐€?
 */
@RestController
@RequestMapping("/auth/expenses")
@RequiredArgsConstructor
public class ExpenseDocumentQueryController {

    private static final String EXPENSE_LIST_VIEW = "expense:list:view";
    private static final String EXPENSE_CREATE_CREATE = "expense:create:create";
    private static final String EXPENSE_CREATE_SUBMIT = "expense:create:submit";
    private static final String EXPENSE_APPROVAL_VIEW = "expense:approval:view";
    private static final String EXPENSE_DOCUMENTS_VIEW = "expense:documents:view";
    private static final String EXPENSE_CREATE_VIEW = "expense:create:view";
    private static final String EXPENSE_PAYMENT_ORDER_VIEW = "expense:payment:payment_order:view";

    private final ExpenseDocumentService expenseDocumentService;
    private final ExpenseDocumentPrintService expenseDocumentPrintService;
    private final AccessControlService accessControlService;

    // 澶勭悊 queryDocuments 璇锋眰銆?
    @GetMapping("/query-documents")
    public Result<List<ExpenseSummaryVO>> queryDocuments(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requirePermission(userId, EXPENSE_DOCUMENTS_VIEW);
        return Result.success(expenseDocumentService.listQueryDocumentSummaries(userId));
    }

    // 澶勭悊 detail 璇锋眰銆?
    @GetMapping("/{documentCode}")
    public Result<ExpenseDocumentDetailVO> detail(@PathVariable String documentCode, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_APPROVAL_VIEW, EXPENSE_DOCUMENTS_VIEW);
        List<String> permissionCodes = accessControlService.getPermissionCodes(userId);
        boolean allowCrossView = permissionCodes.contains(EXPENSE_APPROVAL_VIEW) || permissionCodes.contains(EXPENSE_DOCUMENTS_VIEW);
        return Result.success(expenseDocumentService.getDocumentDetail(userId, documentCode, allowCrossView));
    }
    @GetMapping("/{documentCode}/print-pdf")
    public ResponseEntity<ByteArrayResource> printPdf(
            @PathVariable String documentCode,
            @RequestParam(required = false) String orientation,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_APPROVAL_VIEW, EXPENSE_DOCUMENTS_VIEW);
        List<String> permissionCodes = accessControlService.getPermissionCodes(userId);
        boolean allowCrossView = permissionCodes.contains(EXPENSE_APPROVAL_VIEW) || permissionCodes.contains(EXPENSE_DOCUMENTS_VIEW);
        ExpenseDocumentPrintService.ExpensePrintPdfResult result = expenseDocumentPrintService.generateSinglePdf(
                userId,
                documentCode,
                allowCrossView,
                ExpenseDocumentPrintOrientation.fromRequest(orientation)
        );
        return buildPdfResponse(result);
    }

    @GetMapping("/print-pdf/batch")
    public ResponseEntity<ByteArrayResource> batchPrintPdf(
            @RequestParam String documentCodes,
            @RequestParam(required = false) String orientation,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_APPROVAL_VIEW, EXPENSE_DOCUMENTS_VIEW, EXPENSE_PAYMENT_ORDER_VIEW);
        List<String> permissionCodes = accessControlService.getPermissionCodes(userId);
        boolean allowCrossView = permissionCodes.contains(EXPENSE_APPROVAL_VIEW)
                || permissionCodes.contains(EXPENSE_DOCUMENTS_VIEW)
                || permissionCodes.contains(EXPENSE_PAYMENT_ORDER_VIEW);
        ExpenseDocumentPrintService.ExpensePrintPdfResult result = expenseDocumentPrintService.generateBatchPdf(
                userId,
                normalizeDocumentCodes(documentCodes),
                allowCrossView,
                ExpenseDocumentPrintOrientation.fromRequest(orientation)
        );
        return buildPdfResponse(result);
    }


    // 澶勭悊 expenseDetail 璇锋眰銆?
    @GetMapping("/{documentCode}/details/{detailNo}")
    public Result<ExpenseDetailInstanceDetailVO> expenseDetail(
            @PathVariable String documentCode,
            @PathVariable String detailNo,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_APPROVAL_VIEW, EXPENSE_DOCUMENTS_VIEW);
        List<String> permissionCodes = accessControlService.getPermissionCodes(userId);
        boolean allowCrossView = permissionCodes.contains(EXPENSE_APPROVAL_VIEW) || permissionCodes.contains(EXPENSE_DOCUMENTS_VIEW);
        return Result.success(expenseDocumentService.getExpenseDetail(userId, documentCode, detailNo, allowCrossView));
    }

    // 澶勭悊 documentPicker 璇锋眰銆?
    @GetMapping("/document-picker")
    public Result<ExpenseDocumentPickerVO> documentPicker(
            @RequestParam String relationType,
            @RequestParam(required = false) List<String> templateTypes,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String excludeDocumentCode,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(
                userId,
                EXPENSE_CREATE_VIEW,
                EXPENSE_CREATE_CREATE,
                EXPENSE_CREATE_SUBMIT,
                EXPENSE_LIST_VIEW,
                EXPENSE_APPROVAL_VIEW,
                EXPENSE_DOCUMENTS_VIEW
        );
        List<String> permissionCodes = accessControlService.getPermissionCodes(userId);
        boolean allowCrossView = permissionCodes.contains(EXPENSE_APPROVAL_VIEW) || permissionCodes.contains(EXPENSE_DOCUMENTS_VIEW);
        return Result.success(
                expenseDocumentService.getDocumentPicker(
                        userId,
                        relationType,
                        templateTypes,
                        keyword,
                        page,
                        pageSize,
                        excludeDocumentCode,
                        allowCrossView
                )
        );
    }

    // 澶勭悊 recall 璇锋眰銆?
    @PostMapping("/{documentCode}/recall")
    public Result<ExpenseDocumentDetailVO> recall(@PathVariable String documentCode, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_APPROVAL_VIEW, EXPENSE_DOCUMENTS_VIEW);
        return Result.success(
                "单据已召回",
                expenseDocumentService.recallDocument(userId, getCurrentUsername(request), documentCode)
        );
    }

    // 澶勭悊 comment 璇锋眰銆?
    @PostMapping("/{documentCode}/comments")
    public Result<ExpenseDocumentDetailVO> comment(
            @PathVariable String documentCode,
            @RequestBody(required = false) ExpenseDocumentCommentDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_APPROVAL_VIEW, EXPENSE_DOCUMENTS_VIEW);
        List<String> permissionCodes = accessControlService.getPermissionCodes(userId);
        boolean allowCrossView = permissionCodes.contains(EXPENSE_APPROVAL_VIEW) || permissionCodes.contains(EXPENSE_DOCUMENTS_VIEW);
        return Result.success(
                "评论已发布",
                expenseDocumentService.commentOnDocument(
                        userId,
                        getCurrentUsername(request),
                        documentCode,
                        dto == null ? new ExpenseDocumentCommentDTO() : dto,
                        allowCrossView
                )
        );
    }

    // 澶勭悊 remind 璇锋眰銆?
    @PostMapping("/{documentCode}/reminders")
    public Result<ExpenseDocumentDetailVO> remind(
            @PathVariable String documentCode,
            @RequestBody(required = false) ExpenseDocumentReminderDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_DOCUMENTS_VIEW);
        return Result.success(
                "已向当前审批人发送催办",
                expenseDocumentService.remindDocument(userId, getCurrentUsername(request), documentCode, dto == null ? new ExpenseDocumentReminderDTO() : dto)
        );
    }

    // 澶勭悊 navigation 璇锋眰銆?
    @GetMapping("/{documentCode}/navigation")
    public Result<ExpenseDocumentNavigationVO> navigation(@PathVariable String documentCode, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_APPROVAL_VIEW, EXPENSE_DOCUMENTS_VIEW);
        List<String> permissionCodes = accessControlService.getPermissionCodes(userId);
        boolean approvalViewer = permissionCodes.contains(EXPENSE_APPROVAL_VIEW);
        return Result.success(expenseDocumentService.getDocumentNavigation(userId, documentCode, approvalViewer));
    }

    // 澶勭悊 editContext 璇锋眰銆?
    @GetMapping("/{documentCode}/edit-context")
    public Result<ExpenseDocumentEditContextVO> editContext(@PathVariable String documentCode, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_APPROVAL_VIEW, EXPENSE_DOCUMENTS_VIEW);
        return Result.success(expenseDocumentService.getDocumentEditContext(userId, documentCode));
    }

    // 澶勭悊 resubmit 璇锋眰銆?
    @PutMapping("/{documentCode}/resubmit")
    public Result<ExpenseDocumentSubmitResultVO> resubmit(
            @PathVariable String documentCode,
            @Valid @RequestBody ExpenseDocumentUpdateDTO dto,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        accessControlService.requireAnyPermission(userId, EXPENSE_LIST_VIEW, EXPENSE_CREATE_CREATE, EXPENSE_CREATE_SUBMIT);
        return Result.success(
                "审批单已重新提交",
                expenseDocumentService.resubmitDocument(userId, getCurrentUsername(request), documentCode, dto)
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
        throw new IllegalStateException("未找到当前登录用户信息");
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "当前用户";
    }
    private List<String> normalizeDocumentCodes(String rawDocumentCodes) {
        if (rawDocumentCodes == null || rawDocumentCodes.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawDocumentCodes.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private ResponseEntity<ByteArrayResource> buildPdfResponse(ExpenseDocumentPrintService.ExpensePrintPdfResult result) {
        ByteArrayResource resource = new ByteArrayResource(result.getContent());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(result.getContent().length);
        headers.setContentDisposition(ContentDisposition.inline().filename(result.getFileName(), StandardCharsets.UTF_8).build());
        headers.setCacheControl("no-store");
        return ResponseEntity.ok().headers(headers).body(resource);
    }

}
