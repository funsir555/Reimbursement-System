package com.finex.auth.service.impl;

import com.finex.auth.service.impl.expense.ExpenseMaintenanceDomainSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseMaintenanceServiceTest {

    @Mock
    private ExpenseMaintenanceDomainSupport expenseMaintenanceDomainSupport;

    @Test
    void repairDelegatesToDomainSupport() {
        List<String> expected = List.of("DOC-001");
        ExpenseMaintenanceService service = new ExpenseMaintenanceService(expenseMaintenanceDomainSupport);
        when(expenseMaintenanceDomainSupport.repairMisapprovedDocumentsByRootContainerBug()).thenReturn(expected);

        List<String> actual = service.repairMisapprovedDocumentsByRootContainerBug();

        assertSame(expected, actual);
        verify(expenseMaintenanceDomainSupport).repairMisapprovedDocumentsByRootContainerBug();
    }
}
