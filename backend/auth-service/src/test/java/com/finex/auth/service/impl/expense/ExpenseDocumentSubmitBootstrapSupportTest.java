package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentSubmitBootstrapSupportTest {

    @Mock private AbstractExpenseDocumentSupport support;
    @Mock private ExpenseDocumentMetadataSupport expenseDocumentMetadataSupport;
    @Mock private ExpenseDocumentActionLogSupport expenseDocumentActionLogSupport;
    @Mock private ExpenseDocumentMutationApplySupport mutationApplySupport;
    @Mock private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock private UserMapper userMapper;
    @Mock private ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;
    @Mock private ExpenseManualApproverPreviewSupport expenseManualApproverPreviewSupport;
    @Mock private ExpenseDocumentCompanyResolver companyResolver;

    @Test
    void createDraftCapturesCompanyAtCreationInsteadOfFollowingUserTransfers() {
        ExpenseDocumentSubmitBootstrapSupport bootstrapSupport = new ExpenseDocumentSubmitBootstrapSupport(
                support, expenseDocumentMetadataSupport, expenseDocumentActionLogSupport,
                mutationApplySupport, processDocumentInstanceMapper, userMapper,
                expenseWorkflowRuntimeSupport, expenseManualApproverPreviewSupport, companyResolver
        );
        ExpenseDocumentSubmitDTO dto = new ExpenseDocumentSubmitDTO();
        dto.setTemplateCode("TPL-1");
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateCode("TPL-1");
        User submitter = new User();
        submitter.setCompanyId("COMPANY-A");
        when(support.requireTemplate("TPL-1")).thenReturn(template);
        when(support.normalizeExpenseDetails(null)).thenReturn(List.of());
        when(userMapper.selectById(1L)).thenReturn(submitter);
        when(companyResolver.resolve(any(), any(), any())).thenReturn("COMPANY-A");
        when(support.buildDocumentCode()).thenReturn("DOC-NEW");
        when(support.requireDocument("DOC-NEW")).thenAnswer(invocation -> {
            org.mockito.ArgumentCaptor<ProcessDocumentInstance> captor =
                    org.mockito.ArgumentCaptor.forClass(ProcessDocumentInstance.class);
            verify(processDocumentInstanceMapper).insert(captor.capture());
            assertEquals("COMPANY-A", captor.getValue().getCompanyId());
            submitter.setCompanyId("COMPANY-B");
            assertEquals("COMPANY-A", captor.getValue().getCompanyId());
            return captor.getValue();
        });

        ProcessDocumentInstance draft = bootstrapSupport.createDraftDocument(1L, "tester", dto);

        assertEquals("COMPANY-A", draft.getCompanyId());
    }

    @Test
    void saveDraftDocumentUsesMutationApplyOwnerAndReloadsDocument() {
        ExpenseDocumentSubmitBootstrapSupport bootstrapSupport = new ExpenseDocumentSubmitBootstrapSupport(
                support,
                expenseDocumentMetadataSupport,
                expenseDocumentActionLogSupport,
                mutationApplySupport,
                processDocumentInstanceMapper,
                userMapper,
                expenseWorkflowRuntimeSupport,
                expenseManualApproverPreviewSupport,
                companyResolver
        );
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-1");
        instance.setStatus("DRAFT");
        ExpenseDocumentUpdateDTO dto = new ExpenseDocumentUpdateDTO();
        AbstractExpenseDocumentSupport.DocumentMutationContext context =
                new AbstractExpenseDocumentSupport.DocumentMutationContext(null, null, null, Map.of(), List.of(), null, Map.of(), null, null, null);
        when(support.requireDocument("DOC-1")).thenReturn(instance);
        when(support.trimToNull("DRAFT")).thenReturn("DRAFT");
        when(mutationApplySupport.buildMutationContext(instance, dto, false)).thenReturn(context);

        ProcessDocumentInstance actual = bootstrapSupport.saveDraftDocument(1L, "DOC-1", dto);

        assertSame(instance, actual);
        verify(support).requireSubmitter(instance, 1L);
        verify(mutationApplySupport).applyDocumentMutation(instance, context, false);
    }

    @Test
    void resubmitDocumentReinitializesRuntimeAfterMutation() {
        ExpenseDocumentSubmitBootstrapSupport bootstrapSupport = new ExpenseDocumentSubmitBootstrapSupport(
                support,
                expenseDocumentMetadataSupport,
                expenseDocumentActionLogSupport,
                mutationApplySupport,
                processDocumentInstanceMapper,
                userMapper,
                expenseWorkflowRuntimeSupport,
                expenseManualApproverPreviewSupport,
                companyResolver
        );
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setId(10L);
        instance.setDocumentCode("DOC-2");
        instance.setStatus("REJECTED");
        instance.setTemplateCode("TPL-1");
        instance.setTemplateName("Travel");
        ExpenseDocumentUpdateDTO dto = new ExpenseDocumentUpdateDTO();
        Map<String, Object> runtimeContext = new LinkedHashMap<>();
        AbstractExpenseDocumentSupport.DocumentMutationContext context =
                new AbstractExpenseDocumentSupport.DocumentMutationContext(null, null, null, Map.of("reason", "trip"), List.of(), null, runtimeContext, null, null, null);
        when(support.requireDocument("DOC-2")).thenReturn(instance);
        when(support.trimToNull("REJECTED")).thenReturn("REJECTED");
        when(support.resolveUserDisplayName(1L, "tester")).thenReturn("Tester");
        when(mutationApplySupport.buildMutationContext(instance, dto, true)).thenReturn(context);
        when(support.isEffectiveApprovedStatus("REJECTED")).thenReturn(false);

        ExpenseDocumentSubmitResultVO actual = bootstrapSupport.resubmitDocument(1L, "tester", "DOC-2", dto);

        assertEquals(instance.getId(), actual.getId());
        verify(mutationApplySupport).applyDocumentMutation(instance, context, true);
        verify(expenseDocumentActionLogSupport).appendLog(any(), any(), any(), any(), any(), any(), any(), any());
        verify(support).syncDocumentBusinessRelations("DOC-2", null, Map.of("reason", "trip"));
        verify(expenseWorkflowRuntimeSupport).initializeRuntime(instance, runtimeContext);
    }
}
