package com.finex.auth.service.impl.closeledger;

import com.finex.auth.dto.FinanceCloseLedgerMetaVO;

public class CloseLedgerMutationSupport {

    private final SharedCloseLedgerSupport support;
    private final CloseLedgerReconcileSupport reconcileSupport;
    private final CloseLedgerValidationSupport validationSupport;

    public CloseLedgerMutationSupport(
            SharedCloseLedgerSupport support,
            CloseLedgerReconcileSupport reconcileSupport,
            CloseLedgerValidationSupport validationSupport
    ) {
        this.support = support;
        this.reconcileSupport = reconcileSupport;
        this.validationSupport = validationSupport;
    }

    public String resolveOperatorName(Long currentUserId, String fallbackUsername) {
        return support.resolveOperatorName(currentUserId, fallbackUsername);
    }

    public FinanceCloseLedgerMetaVO close(
            Long currentUserId,
            String companyId,
            Integer iyear,
            Integer iperiod,
            String closeNote,
            String operatorName
    ) {
        var company = support.resolveEffectiveCompany(currentUserId, companyId);
        int effectiveYear = support.normalizeYear(iyear);
        int effectivePeriod = support.normalizePeriod(iperiod);
        var reconcileResult = reconcileSupport.reconcile(currentUserId, company.getCompanyId(), effectiveYear, effectivePeriod, operatorName);
        var validationResult = validationSupport.validate(currentUserId, company.getCompanyId(), effectiveYear, effectivePeriod, operatorName);
        if (!Boolean.TRUE.equals(validationResult.getPassed())) {
            throw new IllegalStateException(support.joinBlockingReasons(validationResult));
        }
        support.closePeriod(company.getCompanyId(), effectiveYear, effectivePeriod, closeNote, operatorName, reconcileResult);
        return support.buildMetaAfterClose(company.getCompanyId(), effectiveYear, effectivePeriod);
    }
}
