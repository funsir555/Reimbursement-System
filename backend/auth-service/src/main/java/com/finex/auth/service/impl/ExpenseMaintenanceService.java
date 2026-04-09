package com.finex.auth.service.impl;

import com.finex.auth.service.impl.expense.ExpenseMaintenanceDomainSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseMaintenanceService {

    private final ExpenseMaintenanceDomainSupport expenseMaintenanceDomainSupport;

    public List<String> repairMisapprovedDocumentsByRootContainerBug() {
        return expenseMaintenanceDomainSupport.repairMisapprovedDocumentsByRootContainerBug();
    }
}
