package com.finex.auth.service.impl.closeledger;

import com.finex.auth.dto.FinanceCloseLedgerMetaVO;
import java.util.List;

public class CloseLedgerMetaSupport {

    private final SharedCloseLedgerSupport support;
    private final CloseLedgerExternalCheckerRegistry externalCheckerRegistry;

    public CloseLedgerMetaSupport(SharedCloseLedgerSupport support, CloseLedgerExternalCheckerRegistry externalCheckerRegistry) {
        this.support = support;
        this.externalCheckerRegistry = externalCheckerRegistry;
    }

    public FinanceCloseLedgerMetaVO getMeta(Long currentUserId, String companyId, Integer iyear, Integer iperiod) {
        var company = support.resolveEffectiveCompany(currentUserId, companyId);
        int effectiveYear = support.normalizeYear(iyear);
        int effectivePeriod = support.normalizePeriod(iperiod);
        var close = support.findPeriodClose(company.getCompanyId(), effectiveYear, effectivePeriod);
        var counts = support.summarizeVoucherGroups(support.loadVoucherGroups(company.getCompanyId(), effectiveYear, effectivePeriod));
        var postState = support.findPostState(company.getCompanyId(), effectiveYear, effectivePeriod);
        List<CloseLedgerExternalCheckResult> externalResults = externalCheckerRegistry.checkAll(company.getCompanyId(), effectiveYear, effectivePeriod);
        CloseLedgerExternalCheckResult fixedAssets = externalResults.stream()
                .filter(item -> "fixed_assets".equals(item.code()))
                .findFirst()
                .orElse(new CloseLedgerExternalCheckResult("fixed_assets", "固定资产期间结账", false, "固定资产当前期间尚未结账"));
        return support.buildMeta(company, effectiveYear, effectivePeriod, counts, postState, close, fixedAssets);
    }
}
