package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseAttachmentService;
import com.finex.auth.service.ExpenseAttachmentPreviewService;
import com.finex.auth.service.ExpenseDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

/** Binds generic previews to an uploaded owner or to a currently readable document. */
@Service
@RequiredArgsConstructor
public class ExpenseAttachmentPreviewAccess implements ExpenseAttachmentPreviewService {
    private final ExpenseAttachmentService attachments;
    private final ExpenseDocumentService documents;
    private final AccessControlService access;
    private final ExpenseDocumentReadAuthorization readAuthorization;
    private final ProcessDocumentInstanceMapper instances;
    private final ProcessDocumentExpenseDetailMapper details;

    public ExpenseAttachmentService.StoredExpenseAttachment load(Long userId, String attachmentId) {
        Set<String> documentCodes = new LinkedHashSet<>();
        instances.selectList(Wrappers.<ProcessDocumentInstance>lambdaQuery()
                .select(ProcessDocumentInstance::getDocumentCode)
                .like(ProcessDocumentInstance::getFormDataJson, attachmentId))
                .forEach(item -> documentCodes.add(item.getDocumentCode()));
        details.selectList(Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                .select(ProcessDocumentExpenseDetail::getDocumentCode)
                .like(ProcessDocumentExpenseDetail::getFormDataJson, attachmentId))
                .forEach(item -> documentCodes.add(item.getDocumentCode()));
        if (documentCodes.isEmpty()) return attachments.loadUploadedAttachment(attachmentId, userId);
        for (String documentCode : documentCodes) {
            try {
                readAuthorization.requireDocument(userId, documentCode, access.getPermissionCodes(userId));
                return documents.loadDocumentAttachment(userId, documentCode, attachmentId, true);
            } catch (SecurityException | IllegalArgumentException | IllegalStateException ignored) {
                // Another candidate can reference the same stored attachment.
            }
        }
        throw new SecurityException("无权查看该附件");
    }
}
