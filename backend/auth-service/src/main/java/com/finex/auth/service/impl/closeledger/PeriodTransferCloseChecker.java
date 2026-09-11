package com.finex.auth.service.impl.closeledger;

import com.finex.auth.service.FinancePeriodTransferService;

public class PeriodTransferCloseChecker implements CloseLedgerExternalChecker {

    private final FinancePeriodTransferService financePeriodTransferService;

    public PeriodTransferCloseChecker(FinancePeriodTransferService financePeriodTransferService) {
        this.financePeriodTransferService = financePeriodTransferService;
    }

    @Override
    public CloseLedgerExternalCheckResult check(String companyId, int iyear, int iperiod) {
        boolean passed = financePeriodTransferService.hasCompletedRunForPeriod(companyId, iyear, iperiod);
        return new CloseLedgerExternalCheckResult(
                "period_transfer",
                "期末结转",
                passed,
                financePeriodTransferService.resolveValidationMessage(companyId, iyear, iperiod)
        );
    }
}
