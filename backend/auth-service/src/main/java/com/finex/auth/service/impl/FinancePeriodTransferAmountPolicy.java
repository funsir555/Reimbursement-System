package com.finex.auth.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Keeps the original voucher column when a red movement determines the net transfer. */
final class FinancePeriodTransferAmountPolicy {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private FinancePeriodTransferAmountPolicy() {
    }

    static Movement net(BigDecimal debit, BigDecimal credit) {
        BigDecimal signedDebit = money(debit);
        BigDecimal signedCredit = money(credit);
        BigDecimal net = signedDebit.subtract(signedCredit);
        if (net.signum() > 0) {
            return signedCredit.signum() < 0
                    ? new Movement("CREDIT", net.negate())
                    : new Movement("DEBIT", net);
        }
        if (net.signum() < 0) {
            return signedDebit.signum() < 0
                    ? new Movement("DEBIT", net)
                    : new Movement("CREDIT", net.negate());
        }
        return new Movement("DEBIT", ZERO);
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }

    record Movement(String direction, BigDecimal amount) {
    }
}
