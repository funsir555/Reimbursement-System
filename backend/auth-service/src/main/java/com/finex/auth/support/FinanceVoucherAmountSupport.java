package com.finex.auth.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FinanceVoucherAmountSupport {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private FinanceVoucherAmountSupport() {
    }

    public static BigDecimal normalizeSigned(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal effectiveDebit(BigDecimal debitAmount, BigDecimal creditAmount) {
        return positivePortion(debitAmount).add(negativePortionAsPositive(creditAmount));
    }

    public static BigDecimal effectiveCredit(BigDecimal debitAmount, BigDecimal creditAmount) {
        return positivePortion(creditAmount).add(negativePortionAsPositive(debitAmount));
    }

    public static boolean hasEffectiveDebit(BigDecimal debitAmount, BigDecimal creditAmount) {
        return effectiveDebit(debitAmount, creditAmount).compareTo(ZERO) > 0;
    }

    public static boolean hasEffectiveCredit(BigDecimal debitAmount, BigDecimal creditAmount) {
        return effectiveCredit(debitAmount, creditAmount).compareTo(ZERO) > 0;
    }

    private static BigDecimal positivePortion(BigDecimal value) {
        BigDecimal normalized = zeroIfNull(value);
        return normalized.compareTo(ZERO) > 0 ? normalized : ZERO;
    }

    private static BigDecimal negativePortionAsPositive(BigDecimal value) {
        BigDecimal normalized = zeroIfNull(value);
        return normalized.compareTo(ZERO) < 0 ? normalized.abs() : ZERO;
    }
}
