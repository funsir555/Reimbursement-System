package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExpenseAmountResolverTest {

    @Test
    void resolveBusinessScenarioReturnsNullForEnterpriseWhenNothingIsExplicitlySelected() {
        assertNull(ExpenseAmountResolver.resolveBusinessScenario(
                ExpenseAmountResolver.DETAIL_TYPE_ENTERPRISE,
                null,
                null
        ));
    }

    @Test
    void resolveExpenseDetailAmountReturnsNullForEnterpriseWhenScenarioIsMissing() {
        BigDecimal resolved = ExpenseAmountResolver.resolveExpenseDetailAmount(
                Map.of(
                        ExpenseAmountResolver.FIELD_DETAIL_AMOUNT, "88.50",
                        ExpenseAmountResolver.FIELD_ACTUAL_PAYMENT_AMOUNT, "120.00"
                ),
                ExpenseAmountResolver.DETAIL_TYPE_ENTERPRISE,
                null
        );

        assertNull(resolved);
    }

    @Test
    void resolveExpenseDetailAmountFallsBackToDetailAmountForExplicitPrepayScenario() {
        BigDecimal resolved = ExpenseAmountResolver.resolveExpenseDetailAmount(
                Map.of(
                        ExpenseAmountResolver.FIELD_BUSINESS_SCENARIO, ExpenseAmountResolver.MODE_PREPAY_UNBILLED,
                        ExpenseAmountResolver.FIELD_DETAIL_AMOUNT, "88.50"
                ),
                ExpenseAmountResolver.DETAIL_TYPE_ENTERPRISE,
                null
        );

        assertEquals(new BigDecimal("88.50"), resolved);
    }

    @Test
    void sumExpenseDetailAmountsSkipsEnterpriseDetailsWhoseScenarioIsStillMissing() {
        ExpenseDetailInstanceDTO detail = new ExpenseDetailInstanceDTO();
        detail.setDetailType(ExpenseAmountResolver.DETAIL_TYPE_ENTERPRISE);
        detail.setFormData(Map.of(
                ExpenseAmountResolver.FIELD_DETAIL_AMOUNT, "88.50",
                ExpenseAmountResolver.FIELD_ACTUAL_PAYMENT_AMOUNT, "99.99"
        ));

        assertNull(ExpenseAmountResolver.sumExpenseDetailAmounts(List.of(detail), null));
    }
}
