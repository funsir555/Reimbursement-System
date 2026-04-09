package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseBankLinkConfigVO;
import com.finex.auth.dto.ExpenseBankLinkSaveDTO;
import com.finex.auth.dto.ExpenseBankLinkSummaryVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpensePaymentDomainSupport {

    private final ExpenseDocumentMutationSupport expenseDocumentMutationSupport;

    public List<ExpensePaymentOrderVO> listPaymentOrders(Long userId, String status) {
        return expenseDocumentMutationSupport.listPaymentOrders(userId, status);
    }

    public List<ExpenseBankLinkSummaryVO> listBankLinks() {
        return expenseDocumentMutationSupport.listBankLinks();
    }

    public ExpenseBankLinkConfigVO getBankLink(Long companyBankAccountId) {
        return expenseDocumentMutationSupport.getBankLink(companyBankAccountId);
    }

    public ExpenseBankLinkConfigVO updateBankLink(Long companyBankAccountId, ExpenseBankLinkSaveDTO dto) {
        return expenseDocumentMutationSupport.updateBankLink(companyBankAccountId, dto);
    }

    public ExpenseDocumentDetailVO handleCmbCloudCallback(ExpenseBankCallbackDTO dto) {
        return expenseDocumentMutationSupport.handleCmbCloudCallback(dto);
    }

    public void runBankReceiptPolling() {
        expenseDocumentMutationSupport.runBankReceiptPolling();
    }

    public ExpenseDocumentDetailVO startPaymentTask(Long userId, String username, Long taskId) {
        return expenseDocumentMutationSupport.startPaymentTask(userId, username, taskId);
    }

    public ExpenseDocumentDetailVO completePaymentTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expenseDocumentMutationSupport.completePaymentTask(userId, username, taskId, dto);
    }

    public ExpenseDocumentDetailVO markPaymentTaskException(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expenseDocumentMutationSupport.markPaymentTaskException(userId, username, taskId, dto);
    }
}
