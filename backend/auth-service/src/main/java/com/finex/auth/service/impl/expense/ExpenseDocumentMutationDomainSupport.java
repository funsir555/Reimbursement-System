package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
import org.springframework.stereotype.Service;

@Service
class ExpenseDocumentMutationDomainSupport {

    private final AbstractExpenseDocumentSupport support;

    ExpenseDocumentMutationDomainSupport(AbstractExpenseDocumentSupport support) {
        this.support = support;
    }

    ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto) {
        return support.submitDocument(userId, username, dto);
    }

    ExpenseDocumentSubmitResultVO resubmitDocument(Long userId, String username, String documentCode, ExpenseDocumentUpdateDTO dto) {
        return support.resubmitDocument(userId, username, documentCode, dto);
    }

    AbstractExpenseDocumentSupport.DocumentMutationContext buildMutationContext(
            ProcessDocumentInstance instance,
            ExpenseDocumentUpdateDTO dto,
            boolean resetRuntime
    ) {
        return support.buildMutationContext(instance, dto, resetRuntime);
    }

    void applyDocumentMutation(
            ProcessDocumentInstance instance,
            AbstractExpenseDocumentSupport.DocumentMutationContext context,
            boolean resetRuntime
    ) {
        support.applyDocumentMutation(instance, context, resetRuntime);
    }
}
