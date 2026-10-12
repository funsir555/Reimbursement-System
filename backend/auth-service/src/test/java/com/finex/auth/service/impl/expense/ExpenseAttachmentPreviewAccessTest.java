package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ExpenseAttachmentService;
import com.finex.auth.service.ExpenseDocumentService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpenseAttachmentPreviewAccessTest {
    private final ExpenseAttachmentService attachments = mock(ExpenseAttachmentService.class);
    private final ExpenseDocumentService documents = mock(ExpenseDocumentService.class);
    private final AccessControlService access = mock(AccessControlService.class);
    private final ExpenseDocumentReadAuthorization authorization = mock(ExpenseDocumentReadAuthorization.class);
    private final ProcessDocumentInstanceMapper instances = mock(ProcessDocumentInstanceMapper.class);
    private final ProcessDocumentExpenseDetailMapper details = mock(ProcessDocumentExpenseDetailMapper.class);
    private final ExpenseAttachmentPreviewAccess preview = new ExpenseAttachmentPreviewAccess(
            attachments, documents, access, authorization, instances, details);

    @org.junit.jupiter.api.BeforeEach
    void initMapping() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "preview-test");
        TableInfoHelper.initTableInfo(assistant, ProcessDocumentInstance.class);
        TableInfoHelper.initTableInfo(assistant, ProcessDocumentExpenseDetail.class);
    }

    @Test
    void detachedUploadCanOnlyUseUploaderBoundPreview() {
        when(instances.selectList(any())).thenReturn(List.of());
        when(details.selectList(any())).thenReturn(List.of());

        preview.load(7L, "ATT-1");

        verify(attachments).loadUploadedAttachment("ATT-1", 7L);
    }

    @Test
    void linkedAttachmentDoesNotFallBackToUploaderWhenDocumentAccessWasRevoked() {
        ProcessDocumentInstance linked = new ProcessDocumentInstance();
        linked.setDocumentCode("DOC-1");
        when(instances.selectList(any())).thenReturn(List.of(linked));
        when(details.selectList(any())).thenReturn(List.of());
        when(access.getPermissionCodes(7L)).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new SecurityException("无权"))
                .when(authorization).requireDocument(7L, "DOC-1", List.of());

        assertThatThrownBy(() -> preview.load(7L, "ATT-1"))
                .isInstanceOf(SecurityException.class);
        org.mockito.Mockito.verify(attachments, org.mockito.Mockito.never())
                .loadUploadedAttachment("ATT-1", 7L);
    }
}
