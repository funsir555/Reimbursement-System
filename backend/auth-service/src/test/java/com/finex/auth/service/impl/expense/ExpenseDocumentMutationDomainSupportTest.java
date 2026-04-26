package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentMutationDomainSupportTest {

    @Mock private ExpenseDocumentSubmitBootstrapSupport submitBootstrapSupport;
    @Mock private ExpenseDocumentMutationApplySupport mutationApplySupport;

    @Test
    void submitAndResubmitDelegateToSharedSupport() {
        ExpenseDocumentMutationDomainSupport domainSupport = new ExpenseDocumentMutationDomainSupport(
                submitBootstrapSupport,
                mutationApplySupport
        );
        ExpenseDocumentSubmitDTO submitDto = new ExpenseDocumentSubmitDTO();
        ExpenseDocumentUpdateDTO updateDto = new ExpenseDocumentUpdateDTO();
        ExpenseDocumentSubmitResultVO submitResult = new ExpenseDocumentSubmitResultVO();
        ExpenseDocumentSubmitResultVO resubmitResult = new ExpenseDocumentSubmitResultVO();
        ProcessDocumentInstance savedDraft = new ProcessDocumentInstance();
        when(submitBootstrapSupport.submitDocument(1L, "tester", submitDto)).thenReturn(submitResult);
        when(submitBootstrapSupport.saveDraftDocument(1L, "DOC-1", updateDto)).thenReturn(savedDraft);
        when(submitBootstrapSupport.resubmitDocument(1L, "tester", "DOC-1", updateDto)).thenReturn(resubmitResult);

        assertSame(submitResult, domainSupport.submitDocument(1L, "tester", submitDto));
        assertSame(savedDraft, domainSupport.saveDraftDocument(1L, "DOC-1", updateDto));
        assertSame(resubmitResult, domainSupport.resubmitDocument(1L, "tester", "DOC-1", updateDto));
    }

    @Test
    void mutationContextOperationsDelegateToSharedSupport() {
        ExpenseDocumentMutationDomainSupport domainSupport = new ExpenseDocumentMutationDomainSupport(
                submitBootstrapSupport,
                mutationApplySupport
        );
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        ExpenseDocumentUpdateDTO dto = new ExpenseDocumentUpdateDTO();
        AbstractExpenseDocumentSupport.DocumentMutationContext context =
                new AbstractExpenseDocumentSupport.DocumentMutationContext(null, null, null, Map.of(), List.of(), null, Map.of(), null, null, null);
        when(mutationApplySupport.buildMutationContext(instance, dto, true)).thenReturn(context);

        assertSame(context, domainSupport.buildMutationContext(instance, dto, true));
        domainSupport.applyDocumentMutation(instance, context, true);

        verify(mutationApplySupport).applyDocumentMutation(instance, context, true);
    }
}
