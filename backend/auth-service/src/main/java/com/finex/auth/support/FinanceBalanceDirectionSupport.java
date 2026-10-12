package com.finex.auth.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FinanceBalanceDirectionSupport {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private FinanceBalanceDirectionSupport() {
    }

    public static boolean isDebitDirection(String balanceDirection) {
        if (balanceDirection == null || balanceDirection.isBlank()) {
            return true;
        }
        String normalized = balanceDirection.trim();
        return normalized.toUpperCase().contains("DEBIT") || normalized.contains("借");
    }

    public static BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal displayAmount(BigDecimal amount) {
        return normalizeAmount(amount).abs();
    }

    /** Converts the opening balance entered relative to a subject's usual direction. */
    public static BigDecimal signedFromNatural(String balanceDirection, BigDecimal amount) {
        BigDecimal normalized = normalizeAmount(amount);
        return isDebitDirection(balanceDirection) ? normalized : normalized.negate();
    }

    /** Keeps the existing opening-balance API's input and output convention. */
    public static BigDecimal naturalFromSigned(String balanceDirection, BigDecimal amount) {
        return signedFromNatural(balanceDirection, amount);
    }

    public static BigDecimal ending(BigDecimal opening, BigDecimal debit, BigDecimal credit) {
        return normalizeAmount(opening).add(normalizeAmount(debit)).subtract(normalizeAmount(credit));
    }

    public static String resolveActualDirectionLabel(BigDecimal amount) {
        BigDecimal normalizedAmount = normalizeAmount(amount);
        return normalizedAmount.compareTo(BigDecimal.ZERO) < 0 ? "贷" : "借";
    }

    public static String resolveActualDirectionCode(BigDecimal amount) {
        return "借".equals(resolveActualDirectionLabel(amount)) ? "DEBIT" : "CREDIT";
    }
}
