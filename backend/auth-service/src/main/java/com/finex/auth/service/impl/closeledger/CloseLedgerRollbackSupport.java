package com.finex.auth.service.impl.closeledger;

import com.finex.auth.dto.FinanceGeneralLedgerRollbackPeriodResultVO;

public class CloseLedgerRollbackSupport {

    private final SharedCloseLedgerSupport support;

    public CloseLedgerRollbackSupport(SharedCloseLedgerSupport support) {
        this.support = support;
    }

    public FinanceGeneralLedgerRollbackPeriodResultVO rollbackPeriod(
            Long currentUserId,
            String companyId,
            Integer iyear,
            Integer iperiod,
            String operatorName
    ) {
        var company = support.resolveEffectiveCompany(currentUserId, companyId);
        int effectiveYear = support.normalizeYear(iyear);
        int effectivePeriod = support.normalizePeriod(iperiod);
        var rollbackSnapshot = support.rollbackPostedPeriod(company.getCompanyId(), effectiveYear, effectivePeriod, operatorName);
        var postState = support.findPostState(company.getCompanyId(), effectiveYear, effectivePeriod);

        FinanceGeneralLedgerRollbackPeriodResultVO result = new FinanceGeneralLedgerRollbackPeriodResultVO();
        result.setCompanyId(company.getCompanyId());
        result.setIyear(effectiveYear);
        result.setIperiod(effectivePeriod);
        result.setIyperiod(support.buildYearPeriod(effectiveYear, effectivePeriod));
        result.setClosedPeriodDetected(rollbackSnapshot.closedPeriodDetected());
        result.setRolledBackVoucherCount(rollbackSnapshot.rolledBackVoucherCount());
        result.setRebuildAccsumCount(rollbackSnapshot.rebuildAccsumCount());
        result.setRebuildAccassCount(rollbackSnapshot.rebuildAccassCount());
        result.setPeriodStatus("OPEN");
        result.setPeriodStatusLabel("未结账");
        result.setPostStatus(postState == null ? "NOT_POSTED" : postState.getStatus());
        result.setPostStatusLabel(support.resolvePostStatusLabel(result.getPostStatus()));
        return result;
    }
}
