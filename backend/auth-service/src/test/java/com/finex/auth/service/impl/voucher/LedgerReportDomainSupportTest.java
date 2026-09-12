package com.finex.auth.service.impl.voucher;

import com.finex.auth.dto.FinanceBalanceSheetRowVO;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LedgerReportDomainSupportTest {

    @Test
    void balanceCategoryTotalsOnlyIncludeTopLevelSubjectRows() throws Exception {
        LedgerReportDomainSupport support = new LedgerReportDomainSupport(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        Method method = LedgerReportDomainSupport.class.getDeclaredMethod(
                "buildBalanceCategoryTotals",
                List.class
        );
        method.setAccessible(true);

        FinanceBalanceSheetRowVO assetLevelOne = row("SUBJECT", 1, "ASSET", "1001", 100);
        FinanceBalanceSheetRowVO assetLevelTwo = row("SUBJECT", 2, "ASSET", "100101", 200);
        FinanceBalanceSheetRowVO assetLevelThree = row("SUBJECT", 3, "ASSET", "10010101", 300);
        FinanceBalanceSheetRowVO assistRow = row("ASSIST", null, "ASSET", "1001", 400);
        FinanceBalanceSheetRowVO liabilityLevelOne = row("SUBJECT", 1, "LIABILITY", "2001", 500);

        @SuppressWarnings("unchecked")
        List<FinanceBalanceSheetRowVO> totals =
                (List<FinanceBalanceSheetRowVO>) method.invoke(
                        support,
                        List.of(
                                assetLevelOne,
                                assetLevelTwo,
                                assetLevelThree,
                                assistRow,
                                liabilityLevelOne
                        )
                );

        FinanceBalanceSheetRowVO assetTotal = totals.get(0);
        FinanceBalanceSheetRowVO liabilityTotal = totals.get(1);
        FinanceBalanceSheetRowVO equityTotal = totals.get(2);

        assertEquals("资产合计", assetTotal.getSubjectName());
        assertEquals(new BigDecimal("100.00"), assetTotal.getBeginDebit());
        assertEquals(new BigDecimal("0.00"), assetTotal.getPeriodDebit());
        assertEquals(new BigDecimal("100.00"), assetTotal.getEndDebit());

        assertEquals(new BigDecimal("500.00"), liabilityTotal.getBeginDebit());
        assertEquals(new BigDecimal("0.00"), equityTotal.getBeginDebit());
    }

    private FinanceBalanceSheetRowVO row(
            String rowType,
            Integer subjectLevel,
            String subjectCategory,
            String subjectCode,
            int amount
    ) {
        FinanceBalanceSheetRowVO row = new FinanceBalanceSheetRowVO();
        row.setRowType(rowType);
        row.setSubjectLevel(subjectLevel);
        row.setSubjectCategory(subjectCategory);
        row.setSubjectCode(subjectCode);
        row.setBeginDebit(new BigDecimal(amount).setScale(2));
        row.setBeginCredit(BigDecimal.ZERO.setScale(2));
        row.setPeriodDebit(BigDecimal.ZERO.setScale(2));
        row.setPeriodCredit(BigDecimal.ZERO.setScale(2));
        row.setEndDebit(new BigDecimal(amount).setScale(2));
        row.setEndCredit(BigDecimal.ZERO.setScale(2));
        return row;
    }
}
