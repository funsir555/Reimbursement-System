package com.finex.auth.service.impl.closeledger;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.FaAssetPeriodClose;
import com.finex.auth.mapper.FaAssetPeriodCloseMapper;
import java.util.Objects;

public class FixedAssetPeriodCloseChecker implements CloseLedgerExternalChecker {

    private static final String BOOK_CODE_FINANCE = "FINANCE";
    private static final String STATUS_CLOSED = "CLOSED";

    private final FaAssetPeriodCloseMapper faAssetPeriodCloseMapper;

    public FixedAssetPeriodCloseChecker(FaAssetPeriodCloseMapper faAssetPeriodCloseMapper) {
        this.faAssetPeriodCloseMapper = faAssetPeriodCloseMapper;
    }

    @Override
    public CloseLedgerExternalCheckResult check(String companyId, int iyear, int iperiod) {
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
