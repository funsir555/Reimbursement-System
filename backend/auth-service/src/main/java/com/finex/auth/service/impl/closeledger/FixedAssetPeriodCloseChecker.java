package com.finex.auth.service.impl.closeledger;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.FaAssetPeriodClose;
import com.finex.auth.mapper.FinanceAccountSetModuleEnableMapper;
import com.finex.auth.mapper.FaAssetPeriodCloseMapper;
import com.finex.auth.support.FinanceModuleEnableSupport;
import java.util.Objects;

public class FixedAssetPeriodCloseChecker implements CloseLedgerExternalChecker {

    private static final String BOOK_CODE_FINANCE = "FINANCE";
    private static final String STATUS_CLOSED = "CLOSED";

    private final FaAssetPeriodCloseMapper faAssetPeriodCloseMapper;
    private final FinanceModuleEnableSupport financeModuleEnableSupport;

    public FixedAssetPeriodCloseChecker(
            FaAssetPeriodCloseMapper faAssetPeriodCloseMapper,
            FinanceAccountSetModuleEnableMapper financeAccountSetModuleEnableMapper
    ) {
        this.faAssetPeriodCloseMapper = faAssetPeriodCloseMapper;
        this.financeModuleEnableSupport = new FinanceModuleEnableSupport(financeAccountSetModuleEnableMapper);
    }

    @Override
    public CloseLedgerExternalCheckResult check(String companyId, int iyear, int iperiod) {
        if (!financeModuleEnableSupport.isEnabled(companyId, FinanceModuleEnableSupport.FIXED_ASSETS)) {
            return new CloseLedgerExternalCheckResult(
                    "fixed_assets",
                    "固定资产期间结账",
                    true,
                    "固定资产未启用，本期无需校验"
            );
        }
        FaAssetPeriodClose record = faAssetPeriodCloseMapper.selectOne(
                Wrappers.<FaAssetPeriodClose>lambdaQuery()
                        .eq(FaAssetPeriodClose::getCompanyId, companyId)
                        .eq(FaAssetPeriodClose::getBookCode, BOOK_CODE_FINANCE)
                        .eq(FaAssetPeriodClose::getFiscalYear, iyear)
                        .eq(FaAssetPeriodClose::getFiscalPeriod, iperiod)
                        .last("limit 1")
        );
        boolean passed = record != null && Objects.equals(STATUS_CLOSED, trimToNull(record.getStatus()));
        return new CloseLedgerExternalCheckResult(
                "fixed_assets",
                "固定资产期间结账",
                passed,
                passed ? "固定资产已完成期间结账" : "固定资产当前期间尚未结账"
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
