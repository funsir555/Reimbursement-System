package com.finex.auth.service.impl.voucher;

import com.finex.auth.dto.FinanceBalanceSheetRowVO;
import com.finex.auth.entity.FinanceAccountSubject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Builds category totals from the highest selected subjects, without counting descendants twice. */
final class BalanceCategoryTotalSupport {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private BalanceCategoryTotalSupport() {
    }

    static List<FinanceBalanceSheetRowVO> build(
            List<FinanceBalanceSheetRowVO> subjectRows,
            Map<String, FinanceAccountSubject> subjectMap
    ) {
        Map<String, Total> totals = new HashMap<>();
        Set<String> selectedCodes = subjectRows.stream()
                .filter(row -> "SUBJECT".equals(row.getRowType()))
                .map(FinanceBalanceSheetRowVO::getSubjectCode)
                .collect(Collectors.toSet());
        for (FinanceBalanceSheetRowVO row : subjectRows) {
            if (!selectedRoot(row, selectedCodes, subjectMap)) {
                continue;
            }
            String category = row.getSubjectCategory() == null ? "ASSET" : row.getSubjectCategory();
            totals.computeIfAbsent(category, unused -> new Total()).add(row);
        }
        return List.of(
                row("ASSET", "资产合计", totals.get("ASSET")),
                row("LIABILITY", "负债合计", totals.get("LIABILITY")),
                row("EQUITY", "权益合计", totals.get("EQUITY")),
                row("COST", "成本合计", totals.get("COST")),
                row("PROFIT", "损益合计", totals.get("PROFIT"))
        );
    }

    private static boolean selectedRoot(
            FinanceBalanceSheetRowVO row,
            Set<String> selectedCodes,
            Map<String, FinanceAccountSubject> subjectMap
    ) {
        if (row == null || !"SUBJECT".equals(row.getRowType())) {
            return false;
        }
        FinanceAccountSubject subject = subjectMap.get(row.getSubjectCode());
        Set<String> visited = new HashSet<>();
        while (subject != null && visited.add(subject.getSubjectCode())) {
            String parentCode = subject.getParentSubjectCode();
            if (parentCode == null || parentCode.isBlank()) {
                break;
            }
            if (selectedCodes.contains(parentCode)) {
                return false;
            }
            subject = subjectMap.get(parentCode);
        }
        return true;
    }

    private static FinanceBalanceSheetRowVO row(String category, String label, Total total) {
        FinanceBalanceSheetRowVO row = new FinanceBalanceSheetRowVO();
        row.setRowType("CATEGORY_TOTAL");
        row.setSubjectName(label);
        row.setSubjectCategory(category);
        Total amounts = total == null ? new Total() : total;
        row.setBeginDebit(amounts.beginDebit);
        row.setBeginCredit(amounts.beginCredit);
        row.setPeriodDebit(amounts.periodDebit);
        row.setPeriodCredit(amounts.periodCredit);
        row.setEndDebit(amounts.endDebit);
        row.setEndCredit(amounts.endCredit);
        return row;
    }

    private static final class Total {
        private BigDecimal beginDebit = ZERO;
        private BigDecimal beginCredit = ZERO;
        private BigDecimal periodDebit = ZERO;
        private BigDecimal periodCredit = ZERO;
        private BigDecimal endDebit = ZERO;
        private BigDecimal endCredit = ZERO;

        private void add(FinanceBalanceSheetRowVO row) {
            beginDebit = sum(beginDebit, row.getBeginDebit());
            beginCredit = sum(beginCredit, row.getBeginCredit());
            periodDebit = sum(periodDebit, row.getPeriodDebit());
            periodCredit = sum(periodCredit, row.getPeriodCredit());
            endDebit = sum(endDebit, row.getEndDebit());
            endCredit = sum(endCredit, row.getEndCredit());
        }

        private static BigDecimal sum(BigDecimal left, BigDecimal right) {
            return left.add(right == null ? ZERO : right).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
