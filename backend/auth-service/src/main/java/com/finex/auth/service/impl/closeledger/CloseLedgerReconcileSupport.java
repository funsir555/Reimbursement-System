package com.finex.auth.service.impl.closeledger;

import com.finex.auth.dto.FinanceCloseLedgerReconcileResultVO;

public class CloseLedgerReconcileSupport {

    private final SharedCloseLedgerSupport support;

    public CloseLedgerReconcileSupport(SharedCloseLedgerSupport support) {
        this.support = support;
    }

    public FinanceCloseLedgerReconcileResultVO reconcile(
            Long currentUserId,
            String companyId,
            Integer iyear,
            Integer iperiod,
            String operatorName
    ) {
        var company = support.resolveEffectiveCompany(currentUserId, companyId);
        int effectiveYear = support.normalizeYear(iyear);
        int effectivePeriod = support.normalizePeriod(iperiod);
        FinanceCloseLedgerReconcileResultVO result = support.reconcilePeriod(company.getCompanyId(), effectiveYear, effectivePeriod);
        support.logReconcile(company.getCompanyId(), effectiveYear, effectivePeriod, operatorName, result);
        return result;
    }
}
