package com.finex.auth.controller;

import com.finex.auth.dto.ExpenseAttachmentVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseAttachmentService;
import com.finex.common.JwtUtil;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/auth/expenses/attachments")
@RequiredArgsConstructor
public class ExpenseAttachmentController {

    private static final String EXPENSE_CREATE_VIEW = "expense:create:view";
    private static final String EXPENSE_CREATE_CREATE = "expense:create:create";
    private static final String EXPENSE_CREATE_SUBMIT = "expense:create:submit";
    private static final String EXPENSE_APPROVAL_VIEW = "expense:approval:view";
    private static final String EXPENSE_APPROVAL_APPROVE = "expense:approval:approve";
    private static final String EXPENSE_APPROVAL_REJECT = "expense:approval:reject";
    private static final String EXPENSE_LIST_VIEW = "expense:list:view";
    private static final String EXPENSE_DOCUMENTS_VIEW = "expense:documents:view";

    private final ExpenseAttachmentService expenseAttachmentService;
    private final AccessControlService accessControlService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ExpenseAttachmentVO> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(
                getCurrentUserId(request),
                EXPENSE_CREATE_VIEW,
                EXPENSE_CREATE_CREATE,
                EXPENSE_CREATE_SUBMIT,
                EXPENSE_APPROVAL_VIEW,
                EXPENSE_APPROVAL_APPROVE
        );
        return Result.success("附件上传成功", expenseAttachmentService.uploadAttachment(file));
    }

    @GetMapping("/{attachmentId}/content")
    public ResponseEntity<?> previewAttachment(
            @PathVariable String attachmentId,
            @RequestParam("token") String token
    ) {
        if (!JwtUtil.verify(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.unauthorized("未登录或登录已过期"));
        }

        accessControlService.requireAnyPermission(
                JwtUtil.getUserId(token),
                EXPENSE_CREATE_VIEW,
                EXPENSE_CREATE_CREATE,
                EXPENSE_CREATE_SUBMIT,
                EXPENSE_APPROVAL_VIEW,
                EXPENSE_APPROVAL_APPROVE,
                EXPENSE_APPROVAL_REJECT,
                EXPENSE_LIST_VIEW,
                EXPENSE_DOCUMENTS_VIEW
        );

        ExpenseAttachmentService.StoredExpenseAttachment attachment = expenseAttachmentService.loadAttachment(attachmentId);
        Resource resource = attachment.resource();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            mediaType = MediaType.parseMediaType(attachment.contentType());
        } catch (Exception ignored) {
            // Fallback to octet-stream when the stored content type is invalid.
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(attachment.fileSize())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(attachment.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(resource);
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
}
