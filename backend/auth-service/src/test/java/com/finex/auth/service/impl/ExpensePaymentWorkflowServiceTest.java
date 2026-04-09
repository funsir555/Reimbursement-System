package com.finex.auth.service.impl;

import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseBankLinkConfigVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.service.impl.expense.ExpensePaymentDomainSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpensePaymentWorkflowServiceTest {

    @Mock
    private ExpensePaymentDomainSupport expensePaymentDomainSupport;

    @Test
    void listPaymentOrdersDelegatesToMutationSupport() {
        List<ExpensePaymentOrderVO> expected = List.of(new ExpensePaymentOrderVO());
        ExpensePaymentWorkflowService service = new ExpensePaymentWorkflowService(expensePaymentDomainSupport);
        when(expensePaymentDomainSupport.listPaymentOrders(1L, "PENDING")).thenReturn(expected);

        assertSame(expected, service.listPaymentOrders(1L, "PENDING"));
        verify(expensePaymentDomainSupport).listPaymentOrders(1L, "PENDING");
    }

    @Test
    void updateBankLinkAndCallbackDelegateToMutationSupport() {
        ExpenseBankLinkConfigVO config = new ExpenseBankLinkConfigVO();
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        ExpenseBankCallbackDTO callbackDTO = new ExpenseBankCallbackDTO();
        ExpensePaymentWorkflowService service = new ExpensePaymentWorkflowService(expensePaymentDomainSupport);
        when(expensePaymentDomainSupport.getBankLink(1L)).thenReturn(config);
        when(expensePaymentDomainSupport.handleCmbCloudCallback(callbackDTO)).thenReturn(detail);

        assertSame(config, service.getBankLink(1L));
        assertSame(detail, service.handleCmbCloudCallback(callbackDTO));

        verify(expensePaymentDomainSupport).getBankLink(1L);
        verify(expensePaymentDomainSupport).handleCmbCloudCallback(callbackDTO);
    }

    @Test
    void completePaymentAndPollingDelegateToMutationSupport() {
        ExpenseApprovalActionDTO dto = new ExpenseApprovalActionDTO();
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        ExpensePaymentWorkflowService service = new ExpensePaymentWorkflowService(expensePaymentDomainSupport);
        when(expensePaymentDomainSupport.completePaymentTask(1L, "tester", 5L, dto)).thenReturn(detail);

        assertSame(detail, service.completePaymentTask(1L, "tester", 5L, dto));
        service.runBankReceiptPolling();

        verify(expensePaymentDomainSupport).completePaymentTask(1L, "tester", 5L, dto);
        verify(expensePaymentDomainSupport).runBankReceiptPolling();
    }
}
