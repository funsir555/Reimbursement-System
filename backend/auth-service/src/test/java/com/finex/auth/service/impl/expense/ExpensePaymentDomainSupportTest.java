package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpensePaymentDomainSupportTest {

    @Mock
    private ExpenseDocumentMutationSupport expenseDocumentMutationSupport;

    @Test
    void paymentReadAndActionMethodsDelegateToMutationSupport() {
        List<ExpensePaymentOrderVO> orders = List.of(new ExpensePaymentOrderVO());
        ExpenseApprovalActionDTO dto = new ExpenseApprovalActionDTO();
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        ExpensePaymentDomainSupport support = new ExpensePaymentDomainSupport(expenseDocumentMutationSupport);
        when(expenseDocumentMutationSupport.listPaymentOrders(1L, "PENDING")).thenReturn(orders);
        when(expenseDocumentMutationSupport.completePaymentTask(1L, "tester", 20L, dto)).thenReturn(detail);

        assertSame(orders, support.listPaymentOrders(1L, "PENDING"));
        assertSame(detail, support.completePaymentTask(1L, "tester", 20L, dto));

        verify(expenseDocumentMutationSupport).listPaymentOrders(1L, "PENDING");
        verify(expenseDocumentMutationSupport).completePaymentTask(1L, "tester", 20L, dto);
    }

    @Test
    void bankCallbacksDelegateToMutationSupport() {
        ExpenseBankCallbackDTO dto = new ExpenseBankCallbackDTO();
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        ExpensePaymentDomainSupport support = new ExpensePaymentDomainSupport(expenseDocumentMutationSupport);
        when(expenseDocumentMutationSupport.handleCmbCloudCallback(dto)).thenReturn(detail);

        ExpenseDocumentDetailVO actual = support.handleCmbCloudCallback(dto);
        support.runBankReceiptPolling();

        assertSame(detail, actual);
        verify(expenseDocumentMutationSupport).handleCmbCloudCallback(dto);
        verify(expenseDocumentMutationSupport).runBankReceiptPolling();
    }
}
