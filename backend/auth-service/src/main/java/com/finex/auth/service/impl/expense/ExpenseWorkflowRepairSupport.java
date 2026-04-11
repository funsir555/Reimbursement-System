package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessDocumentInstance;

class ExpenseWorkflowRepairSupport {

    private final AbstractExpenseWorkflowSupport support;
    private final ExpenseWorkflowExecutionSupport executionSupport;

    ExpenseWorkflowRepairSupport(AbstractExpenseWorkflowSupport support, ExpenseWorkflowExecutionSupport executionSupport) {
        this.support = support;
        this.executionSupport = executionSupport;
    }

    boolean isMisapprovedByBlankRootBug(String documentCode) {
        return support.isMisapprovedByBlankRootBug(documentCode);
    }

    void rebuildMisapprovedRuntime(ProcessDocumentInstance instance) {
        support.rebuildMisapprovedRuntime(instance);
    }
}
