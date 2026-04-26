package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentMutationApplySupportTest {

    @Mock private AbstractExpenseDocumentSupport support;
    @Mock private ExpenseDocumentMetadataSupport expenseDocumentMetadataSupport;
    @Mock private ExpenseDocumentTaskRuntimeSupport expenseDocumentTaskRuntimeSupport;
    @Mock private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock private ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    @Mock private ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;

    @Test
    void buildMutationContextMergesManualSelectionsAndRejectMetadata() {
        ExpenseDocumentMutationApplySupport applySupport = new ExpenseDocumentMutationApplySupport(
                support,
                expenseDocumentMetadataSupport,
                expenseDocumentTaskRuntimeSupport,
                processDocumentInstanceMapper,
                processDocumentExpenseDetailMapper,
                expenseWorkflowRuntimeSupport
        );
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setTemplateCode("TPL-1");
        instance.setSubmitterUserId(9L);
        instance.setSubmitterName("Tester");
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setExpenseDetailModeDefault("NORMAL");
        template.setTemplateName("Travel");
        template.setFlowName("Flow");
        ProcessFormDesign formDesign = new ProcessFormDesign();
        ProcessExpenseDetailDesign detailDesign = new ProcessExpenseDetailDesign();
        ExpenseDocumentUpdateDTO dto = new ExpenseDocumentUpdateDTO();
        dto.setFormData(Map.of("reason", "trip"));
        dto.setExpenseDetails(List.of(new ExpenseDetailInstanceDTO()));
        dto.setManualApproverSelections(Map.of("finance", List.of(1L, 2L)));
        Map<String, Object> runtimeContext = new LinkedHashMap<>();
        User submitter = new User();
        List<ExpenseDetailInstanceDTO> normalizedDetails = List.of(new ExpenseDetailInstanceDTO());
        when(support.requireTemplateForDocument("TPL-1")).thenReturn(template);
        when(support.loadFormDesign(null)).thenReturn(formDesign);
        when(support.loadExpenseDetailDesign(null)).thenReturn(detailDesign);
        when(support.normalizeExpenseDetails(dto.getExpenseDetails())).thenReturn(normalizedDetails);
        when(support.validateSubmitContext(eq(template), eq(formDesign), eq(detailDesign), any(), eq(normalizedDetails))).thenReturn("{}");
        when(support.loadActiveUser(9L)).thenReturn(submitter);
        when(expenseWorkflowRuntimeSupport.buildRuntimeFlowContext(eq(submitter), eq(template), eq(formDesign), any(), eq(detailDesign), eq(normalizedDetails)))
                .thenReturn(runtimeContext);
        when(support.normalizeManualApproverSelections(dto.getManualApproverSelections())).thenReturn(Map.of("finance", List.of(1L, 2L)));
        when(support.resolveRejectRuntimeMetadata(instance)).thenReturn(Map.of("resumeNodeKey", "finance"));
        when(expenseDocumentMetadataSupport.resolveDocumentTitle(eq(template), any(), eq("Tester"))).thenReturn("DOC");
        when(expenseDocumentMetadataSupport.resolveDocumentReason(eq(template), any())).thenReturn("Reason");
        when(support.resolveTotalAmount(any(), eq(normalizedDetails), eq("NORMAL"))).thenReturn(BigDecimal.TEN);

        AbstractExpenseDocumentSupport.DocumentMutationContext actual =
                applySupport.buildMutationContext(instance, dto, true);

        assertSame(template, actual.template());
        assertSame(formDesign, actual.formDesign());
        assertSame(detailDesign, actual.expenseDetailDesign());
        assertEquals("trip", actual.formData().get("reason"));
        assertEquals("{}", actual.flowSnapshotJson());
        assertEquals(Map.of("finance", List.of(1L, 2L)), actual.runtimeContext().get("manualApproverSelections"));
        assertEquals("finance", actual.runtimeContext().get("resumeNodeKey"));
        assertEquals("DOC", actual.documentTitle());
        assertEquals("Reason", actual.documentReason());
        assertEquals(BigDecimal.TEN, actual.totalAmount());
    }

    @Test
    void applyDocumentMutationReplacesDetailsAndPersistsRuntimeReset() {
        ExpenseDocumentMutationApplySupport applySupport = new ExpenseDocumentMutationApplySupport(
                support,
                expenseDocumentMetadataSupport,
                expenseDocumentTaskRuntimeSupport,
                processDocumentInstanceMapper,
                processDocumentExpenseDetailMapper,
                expenseWorkflowRuntimeSupport
        );
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-1");
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateName("Travel");
        template.setTemplateType("report");
        template.setFormDesignCode("FORM-1");
        template.setApprovalFlow("FLOW-1");
        template.setFlowName("Flow");
        ProcessFormDesign formDesign = new ProcessFormDesign();
        formDesign.setSchemaJson("{\"blocks\":[]}");
        ProcessExpenseDetailDesign detailDesign = new ProcessExpenseDetailDesign();
        ExpenseDetailInstanceDTO detail = new ExpenseDetailInstanceDTO();
        AbstractExpenseDocumentSupport.DocumentMutationContext context =
                new AbstractExpenseDocumentSupport.DocumentMutationContext(
                        template,
                        formDesign,
                        detailDesign,
                        Map.of("reason", "trip"),
                        List.of(detail),
                        "{}",
                        new LinkedHashMap<>(),
                        "DOC",
                        "Reason",
                        BigDecimal.ONE
                );
        when(support.toTemplateSnapshot(template)).thenReturn(Map.of("templateCode", "TPL-1"));
        when(support.writeJson(any())).thenAnswer(invocation -> String.valueOf((Object) invocation.getArgument(0)));
        when(expenseDocumentTaskRuntimeSupport.loadOpenTasks("DOC-1")).thenReturn(List.of());

        applySupport.applyDocumentMutation(instance, context, true);

        verify(processDocumentInstanceMapper).updateById(instance);
        verify(expenseDocumentTaskRuntimeSupport).cancelOpenTasks(List.of(), null, instance.getUpdatedAt());
        verify(support).persistDocumentRuntimeState(any(), any(), any(), any(), any(), any(), any());
        verify(processDocumentExpenseDetailMapper).delete(any());
        verify(support).saveExpenseDetailInstances("DOC-1", template, detailDesign, List.of(detail));
    }
}
