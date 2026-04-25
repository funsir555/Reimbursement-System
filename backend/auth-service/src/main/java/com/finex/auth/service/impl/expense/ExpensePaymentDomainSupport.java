package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseBankLinkConfigVO;
import com.finex.auth.dto.ExpenseBankLinkSaveDTO;
import com.finex.auth.dto.ExpenseBankLinkSummaryVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.PmBankPaymentRecordMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.mapper.SystemBankBranchCatalogMapper;
import com.finex.auth.mapper.SystemCompanyBankAccountMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.service.ExpenseAttachmentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpensePaymentDomainSupport {

    private final ExpensePaymentOrderQuerySupport orderQuerySupport;
    private final ExpenseBankLinkDomainSupport bankLinkDomainSupport;
    private final ExpensePaymentExecutionSupport executionSupport;
    private final ExpensePaymentReceiptSupport receiptSupport;

    public ExpensePaymentDomainSupport(
            ExpenseDocumentReadSupport expenseDocumentReadSupport,
            ExpenseSummaryAssembler expenseSummaryAssembler,
            ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport,
            ExpenseRelationWriteOffService expenseRelationWriteOffService,
            PmBankPaymentRecordMapper pmBankPaymentRecordMapper,
            ProcessDocumentTaskMapper processDocumentTaskMapper,
            ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper,
            ProcessDocumentInstanceMapper processDocumentInstanceMapper,
            SystemBankBranchCatalogMapper systemBankBranchCatalogMapper,
            SystemCompanyBankAccountMapper systemCompanyBankAccountMapper,
            SystemCompanyMapper systemCompanyMapper,
            FinanceVendorMapper financeVendorMapper,
            UserBankAccountMapper userBankAccountMapper,
            ExpenseAttachmentService expenseAttachmentService,
            ObjectMapper objectMapper
    ) {
        ExpensePaymentSupportContext context = new ExpensePaymentSupportContext(
                expenseDocumentReadSupport,
                expenseSummaryAssembler,
                expenseWorkflowRuntimeSupport,
                expenseRelationWriteOffService,
                pmBankPaymentRecordMapper,
                processDocumentTaskMapper,
                processDocumentExpenseDetailMapper,
                processDocumentInstanceMapper,
                systemBankBranchCatalogMapper,
                systemCompanyBankAccountMapper,
                systemCompanyMapper,
                financeVendorMapper,
                userBankAccountMapper,
                expenseAttachmentService,
                objectMapper
        );
        ExpensePaymentRecordSupport recordSupport = new ExpensePaymentRecordSupport(context);
        this.orderQuerySupport = new ExpensePaymentOrderQuerySupport(context);
        this.bankLinkDomainSupport = new ExpenseBankLinkDomainSupport(context, recordSupport);
        this.executionSupport = new ExpensePaymentExecutionSupport(context, recordSupport);
        this.receiptSupport = new ExpensePaymentReceiptSupport(context, recordSupport, this.executionSupport);
    }

    public List<ExpensePaymentOrderVO> listPaymentOrders(Long userId, String status) {
        return orderQuerySupport.listPaymentOrders(userId, status);
    }

    public List<ExpenseBankLinkSummaryVO> listBankLinks() {
        return bankLinkDomainSupport.listBankLinks();
    }

    public ExpenseBankLinkConfigVO getBankLink(Long companyBankAccountId) {
        return bankLinkDomainSupport.getBankLink(companyBankAccountId);
    }

    public ExpenseBankLinkConfigVO updateBankLink(Long companyBankAccountId, ExpenseBankLinkSaveDTO dto) {
        return bankLinkDomainSupport.updateBankLink(companyBankAccountId, dto);
    }

    public ExpenseDocumentDetailVO handleCmbCloudCallback(ExpenseBankCallbackDTO dto) {
        return receiptSupport.handleCmbCloudCallback(dto);
    }

    public void runBankReceiptPolling() {
        receiptSupport.runBankReceiptPolling();
    }

    public ExpenseDocumentDetailVO startPaymentTask(Long userId, String username, Long taskId) {
        return executionSupport.startPaymentTask(userId, username, taskId);
    }

    public ExpenseDocumentDetailVO completePaymentTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return executionSupport.completePaymentTask(userId, username, taskId, dto);
    }

    public ExpenseDocumentDetailVO markPaymentTaskException(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return executionSupport.markPaymentTaskException(userId, username, taskId, dto);
    }

    public boolean rejectPaymentTasks(Long userId, String username, List<Long> taskIds, ExpenseApprovalActionDTO dto) {
        return executionSupport.rejectPaymentTasks(userId, username, taskIds, dto);
    }
}
