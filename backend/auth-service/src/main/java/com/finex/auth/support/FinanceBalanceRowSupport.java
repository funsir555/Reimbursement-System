package com.finex.auth.support;

import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccsum;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class FinanceBalanceRowSupport {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ZERO_QTY = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    private FinanceBalanceRowSupport() {
    }

    public static void fillBalanceRow(
            GlAccsum row,
            FinanceAccountSubject subject,
            BigDecimal mb,
            BigDecimal mbF,
            BigDecimal nbS,
            BigDecimal md,
            BigDecimal mc,
            BigDecimal mdF,
            BigDecimal mcF,
            BigDecimal ndS,
            BigDecimal ncS
    ) {
        row.setCexchName(resolveCurrencyName(subject));
        row.setCurrencyCode(resolveCurrencyCode(subject == null ? null : subject.getCexchName()));
        row.setMb(money(mb));
        row.setMbF(money(mbF));
        row.setNbS(qty(nbS));
        row.setMd(money(md));
        row.setMc(money(mc));
        row.setMdF(money(mdF));
        row.setMcF(money(mcF));
        row.setNdS(qty(ndS));
        row.setNcS(qty(ncS));
        row.setMe(row.getMb().add(row.getMd()).subtract(row.getMc()));
        row.setMeF(row.getMbF().add(row.getMdF()).subtract(row.getMcF()));
        row.setNeS(row.getNbS().add(row.getNdS()).subtract(row.getNcS()));
        applyDirection(subject, row.getMb(), row.getMe(), row::setCbegindC, row::setCbegindCEngl, row::setCenddC, row::setCenddCEngl);
    }

    public static void fillBalanceRow(
            GlAccass row,
            FinanceAccountSubject subject,
            BigDecimal mb,
            BigDecimal mbF,
            BigDecimal nbS,
            BigDecimal md,
            BigDecimal mc,
            BigDecimal mdF,
            BigDecimal mcF,
            BigDecimal ndS,
            BigDecimal ncS
    ) {
        row.setCexchName(resolveCurrencyName(subject));
        row.setCurrencyCode(resolveCurrencyCode(subject == null ? null : subject.getCexchName()));
        row.setMb(money(mb));
        row.setMbF(money(mbF));
        row.setNbS(qty(nbS));
        row.setMd(money(md));
        row.setMc(money(mc));
        row.setMdF(money(mdF));
        row.setMcF(money(mcF));
        row.setNdS(qty(ndS));
        row.setNcS(qty(ncS));
        row.setMe(row.getMb().add(row.getMd()).subtract(row.getMc()));
        row.setMeF(row.getMbF().add(row.getMdF()).subtract(row.getMcF()));
        row.setNeS(row.getNbS().add(row.getNdS()).subtract(row.getNcS()));
        applyDirection(subject, row.getMb(), row.getMe(), row::setCbegindC, row::setCbegindCEngl, row::setCenddC, row::setCenddCEngl);
    }

    public static BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal qty(BigDecimal value) {
        return value == null ? ZERO_QTY : value.setScale(6, RoundingMode.HALF_UP);
    }

    public static String resolveCurrencyCode(String cexchName) {
        String normalized = trimToNull(cexchName);
        if (normalized == null || normalized.contains("人民币")) {
            return "CNY";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static String resolveCurrencyName(FinanceAccountSubject subject) {
        String normalized = trimToNull(subject == null ? null : subject.getCexchName());
        return normalized == null ? "人民币" : normalized;
    }

    private static void applyDirection(
            FinanceAccountSubject subject,
            BigDecimal beginAmount,
            BigDecimal endAmount,
            java.util.function.Consumer<String> beginLabelSetter,
            java.util.function.Consumer<String> beginCodeSetter,
            java.util.function.Consumer<String> endLabelSetter,
            java.util.function.Consumer<String> endCodeSetter
    ) {
        String defaultDirection = subject == null ? null : subject.getBalanceDirection();
        beginLabelSetter.accept(FinanceBalanceDirectionSupport.resolveActualDirectionLabel(defaultDirection, beginAmount));
        beginCodeSetter.accept(FinanceBalanceDirectionSupport.resolveActualDirectionCode(defaultDirection, beginAmount));
        endLabelSetter.accept(FinanceBalanceDirectionSupport.resolveActualDirectionLabel(defaultDirection, endAmount));
        endCodeSetter.accept(FinanceBalanceDirectionSupport.resolveActualDirectionCode(defaultDirection, endAmount));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
