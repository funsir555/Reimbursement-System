package com.finex.auth.service;

import com.finex.auth.dto.ExpenseAttachmentVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ExpenseAttachmentService {

    ExpenseAttachmentVO uploadAttachment(MultipartFile file);

    StoredExpenseAttachment loadAttachment(String attachmentId);

    ExpenseAttachmentVO saveGeneratedAttachment(String fileName, String contentType, byte[] content);

    record StoredExpenseAttachment(Resource resource, String fileName, String contentType, long fileSize) {
    }
}
