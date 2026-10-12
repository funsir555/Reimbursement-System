package com.finex.auth.service.impl;

import com.finex.auth.entity.FinancePeriodTransferRun;
import com.finex.auth.entity.FinancePeriodTransferRunDetail;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Checks the saved preview before it is used to create a voucher. */
final class FinancePeriodTransferPreviewConsistency {

    private FinancePeriodTransferPreviewConsistency() {
    }

    static boolean matches(FinancePeriodTransferRun run, BigDecimal total,
                           List<FinancePeriodTransferRunDetail> saved, List<FinancePeriodTransferRunDetail> current) {
        if (run.getTotalAmount() == null || run.getTotalAmount().compareTo(total) != 0 || saved.size() != current.size()) {
            return false;
        }
        for (int i = 0; i < saved.size(); i++) {
            if (!same(saved.get(i), current.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean same(FinancePeriodTransferRunDetail a, FinancePeriodTransferRunDetail b) {
        return Objects.equals(a.getLineNo(), b.getLineNo())
                && Objects.equals(a.getRuleLineId(), b.getRuleLineId())
                && Objects.equals(a.getSourceSubjectCode(), b.getSourceSubjectCode())
                && Objects.equals(a.getSourceAssistKey(), b.getSourceAssistKey())
                && Objects.equals(a.getTargetSubjectCode(), b.getTargetSubjectCode())
                && Objects.equals(a.getTargetCdeptId(), b.getTargetCdeptId())
                && Objects.equals(a.getTargetCpersonId(), b.getTargetCpersonId())
                && Objects.equals(a.getTargetCcusId(), b.getTargetCcusId())
                && Objects.equals(a.getTargetCsupId(), b.getTargetCsupId())
                && Objects.equals(a.getTargetCitemClass(), b.getTargetCitemClass())
                && Objects.equals(a.getTargetCitemId(), b.getTargetCitemId())
                && Objects.equals(a.getMetricType(), b.getMetricType())
                && Objects.equals(a.getDirection(), b.getDirection())
                && Objects.equals(a.getSkipReason(), b.getSkipReason())
                && Objects.equals(a.getSourceTraceKey(), b.getSourceTraceKey())
                && a.getAmount() != null && b.getAmount() != null
                && a.getAmount().compareTo(b.getAmount()) == 0;
    }
}
