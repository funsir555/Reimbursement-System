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

    @Mock
    private AbstractExpenseDocumentSupport support;

    @Test
    void submitAndResubmitDelegateToSharedSupport() {
        ExpenseDocumentMutationDomainSupport domainSupport = new ExpenseDocumentMutationDomainSupport(support);
        ExpenseDocumentSubmitDTO submitDto = new ExpenseDocumentSubmitDTO();
        ExpenseDocumentUpdateDTO updateDto = new ExpenseDocumentUpdateDTO();
        ExpenseDocumentSubmitResultVO submitResult = new ExpenseDocumentSubmitResultVO();
        ExpenseDocumentSubmitResultVO resubmitResult = new ExpenseDocumentSubmitResultVO();
        when(support.submitDocument(1L, "tester", submitDto)).thenReturn(submitResult);
        when(support.resubmitDocument(1L, "tester", "DOC-1", updateDto)).thenReturn(resubmitResult);

        assertSame(submitResult, domainSupport.submitDocument(1L, "tester", submitDto));
        assertSame(resubmitResult, domainSupport.resubmitDocument(1L, "tester", "DOC-1", updateDto));
    }

    @Test
    void mutationContextOperationsDelegateToSharedSupport() {
        ExpenseDocumentMutationDomainSupport domainSupport = new ExpenseDocumentMutationDomainSupport(support);
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        ExpenseDocumentUpdateDTO dto = new ExpenseDocumentUpdateDTO();
        AbstractExpenseDocumentSupport.DocumentMutationContext context =
                new AbstractExpenseDocumentSupport.DocumentMutationContext(null, null, null, Map.of(), List.of(), Map.of(), null, null, null);
        when(support.buildMutationContext(instance, dto, true)).thenReturn(context);

        assertSame(context, domainSupport.buildMutationContext(instance, dto, true));
        domainSupport.applyDocumentMutation(instance, context, true);

        verify(support).applyDocumentMutation(instance, context, true);
    }
}