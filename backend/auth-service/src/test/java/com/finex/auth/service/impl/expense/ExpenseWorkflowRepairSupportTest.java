package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessDocumentInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseWorkflowRepairSupportTest {

    @Mock
    private AbstractExpenseWorkflowSupport support;
    @Mock
    private ExpenseWorkflowExecutionSupport executionSupport;

    @Test
    void repairQueriesDelegateToSharedSupport() {
        ExpenseWorkflowRepairSupport repairSupport = new ExpenseWorkflowRepairSupport(support, executionSupport);
        when(support.isMisapprovedByBlankRootBug("DOC-1")).thenReturn(true);

        assertTrue(repairSupport.isMisapprovedByBlankRootBug("DOC-1"));
    }

    @Test
    void rebuildDelegatesToSharedSupport() {
        ExpenseWorkflowRepairSupport repairSupport = new ExpenseWorkflowRepairSupport(support, executionSupport);
        ProcessDocumentInstance instance = new ProcessDocumentInstance();

        repairSupport.rebuildMisapprovedRuntime(instance);

        verify(support).rebuildMisapprovedRuntime(instance);
    }
}