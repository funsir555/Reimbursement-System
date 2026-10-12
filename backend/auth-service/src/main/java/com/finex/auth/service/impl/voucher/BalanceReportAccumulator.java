package com.finex.auth.service.impl.voucher;

import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.support.FinanceBalanceDirectionSupport;
import com.finex.auth.support.FinanceVoucherAmountSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Accumulates signed balances and gross movement for one report row. */
final class BalanceReportAccumulator {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final FinanceAccountSubject subject;
    private BigDecimal beginSigned = ZERO;
    private BigDecimal periodDebit = ZERO;
    private BigDecimal periodCredit = ZERO;
    private BigDecimal endSigned = ZERO;

    BalanceReportAccumulator(FinanceAccountSubject subject) {
        this.subject = subject;
    }

    static void addUnpostedHierarchy(
            GlAccvouch voucher,
            Map<String, FinanceAccountSubject> subjects,
            Map<String, BalanceReportAccumulator> rows,
            Predicate<FinanceAccountSubject> selected
    ) {
        FinanceAccountSubject current = subjects.get(voucher.getCcode());
        Set<String> visited = new HashSet<>();
        while (current != null && visited.add(current.getSubjectCode())) {
            if (selected.test(current)) {
                FinanceAccountSubject subject = current;
                rows.computeIfAbsent(subject.getSubjectCode(), unused -> new BalanceReportAccumulator(subject))
                        .addUnposted(voucher);
            }
            current = subjects.get(current.getParentSubjectCode());
        }
    }

    void addOpeningBase(BigDecimal amount) {
        beginSigned = add(beginSigned, amount);
    }

    void addPeriodBase(BigDecimal debit, BigDecimal credit) {
        periodDebit = add(periodDebit, debit);
        periodCredit = add(periodCredit, credit);
    }

    void addEndingBase(BigDecimal amount) {
        endSigned = add(endSigned, amount);
    }

    void addUnposted(GlAccvouch voucher) {
        BigDecimal debit = FinanceVoucherAmountSupport.zeroIfNull(voucher.getMd());
        BigDecimal credit = FinanceVoucherAmountSupport.zeroIfNull(voucher.getMc());
        periodDebit = add(periodDebit, debit);
        periodCredit = add(periodCredit, credit);
        endSigned = FinanceBalanceDirectionSupport.ending(endSigned, debit, credit);
    }

    FinanceAccountSubject subject() { return subject; }
    BigDecimal beginSigned() { return beginSigned; }
    BigDecimal periodDebit() { return periodDebit; }
    BigDecimal periodCredit() { return periodCredit; }
    BigDecimal endSigned() { return endSigned; }

    private static BigDecimal add(BigDecimal left, BigDecimal right) {
        return FinanceBalanceDirectionSupport.normalizeAmount(left)
                .add(FinanceBalanceDirectionSupport.normalizeAmount(right));
    }
}
