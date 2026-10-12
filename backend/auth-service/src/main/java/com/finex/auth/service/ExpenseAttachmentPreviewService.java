package com.finex.auth.service;

public interface ExpenseAttachmentPreviewService {
    ExpenseAttachmentService.StoredExpenseAttachment load(Long userId, String attachmentId);
}
