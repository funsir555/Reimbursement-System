package com.finex.auth.service.impl.expense;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseMaintenanceDomainSupportTest {

    @Mock
    private ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;

    @Test
    void repairDelegatesToRuntimeSupport() {
        List<String> expected = List.of("DOC-001");
        ExpenseMaintenanceDomainSupport support = new ExpenseMaintenanceDomainSupport(expenseWorkflowRuntimeSupport);
        when(expenseWorkflowRuntimeSupport.repairMisapprovedDocumentsByRootContainerBug()).thenReturn(expected);

        List<String> actual = support.repairMisapprovedDocumentsByRootContainerBug();

        assertSame(expected, actual);
        verify(expenseWorkflowRuntimeSupport).repairMisapprovedDocumentsByRootContainerBug();
    }
}
