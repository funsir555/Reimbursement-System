package com.finex.auth.support;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinanceBalanceDirectionSupportTest {

    @Test
    void resolvesDirectionFromSignedAmountAndDisplaysMagnitude() {
        assertEquals("借", FinanceBalanceDirectionSupport.resolveActualDirectionLabel(new BigDecimal("10.00")));
        assertEquals("贷", FinanceBalanceDirectionSupport.resolveActualDirectionLabel(new BigDecimal("-10.00")));
        assertEquals("借", FinanceBalanceDirectionSupport.resolveActualDirectionLabel(BigDecimal.ZERO));
        assertEquals("CREDIT", FinanceBalanceDirectionSupport.resolveActualDirectionCode(new BigDecimal("-10.00")));
        assertEquals("10.00", FinanceBalanceDirectionSupport.displayAmount(new BigDecimal("-10.00")).toPlainString());
    }

    @Test
    void convertsCreditUsualDirectionOnlyAtOpeningInputBoundary() {
        assertEquals(new BigDecimal("-1100000.00"), FinanceBalanceDirectionSupport.signedFromNatural("CREDIT", new BigDecimal("1100000.00")));
        assertEquals(new BigDecimal("1100000.00"), FinanceBalanceDirectionSupport.naturalFromSigned("CREDIT", new BigDecimal("-1100000.00")));
        assertEquals(new BigDecimal("-1616536.00"), FinanceBalanceDirectionSupport.ending(
                new BigDecimal("-1100000.00"), BigDecimal.ZERO, new BigDecimal("516536.00")));
    }

    @Test
    void creditsExceedingDebitsStayOnCreditSideRegardlessOfSubjectCategory() {
        BigDecimal ending = FinanceBalanceDirectionSupport.ending(
                BigDecimal.ZERO, new BigDecimal("2346232.71"), new BigDecimal("3883584.78"));
        assertEquals(new BigDecimal("-1537352.07"), ending);
        assertEquals("贷", FinanceBalanceDirectionSupport.resolveActualDirectionLabel(ending));
        assertEquals(new BigDecimal("1537352.07"), FinanceBalanceDirectionSupport.displayAmount(ending));
    }
}
