package com.finex.auth.service.impl;

import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseBankLinkConfigVO;
import com.finex.auth.dto.ExpenseBankLinkSaveDTO;
import com.finex.auth.dto.ExpenseBankLinkSummaryVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.service.impl.expense.ExpensePaymentDomainSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpensePaymentWorkflowService {

    private final ExpensePaymentDomainSupport expensePaymentDomainSupport;

    public List<ExpensePaymentOrderVO> listPaymentOrders(Long userId, String status) {
        return expensePaymentDomainSupport.listPaymentOrders(userId, status);
    }

    public List<ExpenseBankLinkSummaryVO> listBankLinks() {
        return expensePaymentDomainSupport.listBankLinks();
    }

    public ExpenseBankLinkConfigVO getBankLink(Long companyBankAccountId) {
        return expensePaymentDomainSupport.getBankLink(companyBankAccountId);
    }

    public ExpenseBankLinkConfigVO updateBankLink(Long companyBankAccountId, ExpenseBankLinkSaveDTO dto) {
        return expensePaymentDomainSupport.updateBankLink(companyBankAccountId, dto);
    }

    public ExpenseDocumentDetailVO handleCmbCloudCallback(ExpenseBankCallbackDTO dto) {
        return expensePaymentDomainSupport.handleCmbCloudCallback(dto);
    }

    public void runBankReceiptPolling() {
        expensePaymentDomainSupport.runBankReceiptPolling();
    }

    public ExpenseDocumentDetailVO startPaymentTask(Long userId, String username, Long taskId) {
        return expensePaymentDomainSupport.startPaymentTask(userId, username, taskId);
    }

    public ExpenseDocumentDetailVO completePaymentTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expensePaymentDomainSupport.completePaymentTask(userId, username, taskId, dto);
    }

    public ExpenseDocumentDetailVO markPaymentTaskException(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expensePaymentDomainSupport.markPaymentTaskException(userId, username, taskId, dto);
    }
}
