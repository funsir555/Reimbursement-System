package com.finex.auth.service.impl.closeledger;

import com.finex.auth.dto.FinanceCloseLedgerValidationResultVO;

public class CloseLedgerValidationSupport {

    private final SharedCloseLedgerSupport support;
    private final CloseLedgerReconcileSupport reconcileSupport;
    private final CloseLedgerExternalCheckerRegistry externalCheckerRegistry;

    public CloseLedgerValidationSupport(
            SharedCloseLedgerSupport support,
            CloseLedgerReconcileSupport reconcileSupport,
            CloseLedgerExternalCheckerRegistry externalCheckerRegistry
    ) {
        this.support = support;
        this.reconcileSupport = reconcileSupport;
        this.externalCheckerRegistry = externalCheckerRegistry;
    }

    public FinanceCloseLedgerValidationResultVO validate(
            Long currentUserId,
            String companyId,
            Integer iyear,
            Integer iperiod,
            String operatorName
    ) {
        var company = support.resolveEffectiveCompany(currentUserId, companyId);
        int effectiveYear = support.normalizeYear(iyear);
        int effectivePeriod = support.normalizePeriod(iperiod);
        var reconcileResult = reconcileSupport.reconcile(currentUserId, company.getCompanyId(), effectiveYear, effectivePeriod, operatorName);
        FinanceCloseLedgerValidationResultVO result = support.validateBeforeClose(
                company.getCompanyId(),
                effectiveYear,
                effectivePeriod,
                reconcileResult,
                externalCheckerRegistry.checkAll(company.getCompanyId(), effectiveYear, effectivePeriod)
        );
        support.logValidation(company.getCompanyId(), effectiveYear, effectivePeriod, operatorName, result);
        return result;
    }
}
