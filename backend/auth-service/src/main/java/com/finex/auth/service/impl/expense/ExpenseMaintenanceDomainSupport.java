package com.finex.auth.service.impl.expense;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseMaintenanceDomainSupport {

    private final ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;

    public List<String> repairMisapprovedDocumentsByRootContainerBug() {
        return expenseWorkflowRuntimeSupport.repairMisapprovedDocumentsByRootContainerBug();
    }
}
