package com.finex.auth.support;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinanceBalanceDirectionSupportTest {

    @Test
    void resolvesDebitSubjectDirectionsAndDisplayAmount() {
        assertEquals("借", FinanceBalanceDirectionSupport.resolveActualDirectionLabel("DEBIT", new BigDecimal("10.00")));
        assertEquals("贷", FinanceBalanceDirectionSupport.resolveActualDirectionLabel("DEBIT", new BigDecimal("-10.00")));
        assertEquals("借", FinanceBalanceDirectionSupport.resolveActualDirectionLabel("DEBIT", BigDecimal.ZERO));
        assertEquals("10.00", FinanceBalanceDirectionSupport.displayAmount(new BigDecimal("-10.00")).toPlainString());
    }

    @Test
    void resolvesCreditSubjectDirectionsAndDisplayAmount() {
        assertEquals("贷", FinanceBalanceDirectionSupport.resolveActualDirectionLabel("CREDIT", new BigDecimal("10.00")));
        assertEquals("借", FinanceBalanceDirectionSupport.resolveActualDirectionLabel("CREDIT", new BigDecimal("-10.00")));
        assertEquals("贷", FinanceBalanceDirectionSupport.resolveActualDirectionLabel("CREDIT", BigDecimal.ZERO));
        assertEquals("DEBIT", FinanceBalanceDirectionSupport.resolveActualDirectionCode("CREDIT", new BigDecimal("-10.00")));
    }
}
