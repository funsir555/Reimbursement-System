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

    @Test
    void validateResolvedExpenseDetailAmountRuleRejectsPrepayMismatch() {
        assertEquals(
                "预付未到票场景下，【金额】必须等于【实际支付金额】",
                ExpenseAmountResolver.validateResolvedExpenseDetailAmountRule(
                        Map.of(
                                ExpenseAmountResolver.FIELD_DETAIL_AMOUNT, "88.50",
                                ExpenseAmountResolver.FIELD_ACTUAL_PAYMENT_AMOUNT, "66.00"
                        ),
                        ExpenseAmountResolver.DETAIL_TYPE_ENTERPRISE,
                        ExpenseAmountResolver.MODE_PREPAY_UNBILLED
                )
        );
    }

    @Test
    void validateResolvedExpenseDetailAmountRuleRejectsActualPaymentGreaterThanInvoice() {
        assertEquals(
                "全额付款场景下，【发票金额】必须大于或等于【实际支付金额】",
                ExpenseAmountResolver.validateResolvedExpenseDetailAmountRule(
                        Map.of(
                                ExpenseAmountResolver.FIELD_INVOICE_AMOUNT, "100.00",
                                ExpenseAmountResolver.FIELD_ACTUAL_PAYMENT_AMOUNT, "120.00"
                        ),
                        ExpenseAmountResolver.DETAIL_TYPE_ENTERPRISE,
                        ExpenseAmountResolver.MODE_INVOICE_FULL_PAYMENT
                )
        );
    }
}
