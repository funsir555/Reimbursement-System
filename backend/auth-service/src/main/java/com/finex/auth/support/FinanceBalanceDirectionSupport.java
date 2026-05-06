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

    public static String resolveActualDirectionLabel(String defaultBalanceDirection, BigDecimal amount) {
        boolean debitDirection = isDebitDirection(defaultBalanceDirection);
        BigDecimal normalizedAmount = normalizeAmount(amount);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) >= 0) {
            return debitDirection ? "借" : "贷";
        }
        return debitDirection ? "贷" : "借";
    }

    public static String resolveActualDirectionCode(String defaultBalanceDirection, BigDecimal amount) {
        return "借".equals(resolveActualDirectionLabel(defaultBalanceDirection, amount)) ? "DEBIT" : "CREDIT";
    }
}
