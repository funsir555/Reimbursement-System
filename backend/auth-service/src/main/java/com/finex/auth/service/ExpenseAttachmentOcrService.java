package com.finex.auth.service;

import com.finex.auth.dto.ExpenseAttachmentOcrResultVO;

public interface ExpenseAttachmentOcrService {

    ExpenseAttachmentOcrResultVO recognizeAttachment(String attachmentId);
}
