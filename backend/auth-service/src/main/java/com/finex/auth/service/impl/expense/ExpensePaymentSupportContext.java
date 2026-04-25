package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
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

final class ExpensePaymentSupportContext {

    final ExpenseDocumentReadSupport expenseDocumentReadSupport;
    final ExpenseSummaryAssembler expenseSummaryAssembler;
    final ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;
    final ExpenseRelationWriteOffService expenseRelationWriteOffService;
    final PmBankPaymentRecordMapper pmBankPaymentRecordMapper;
    final ProcessDocumentTaskMapper processDocumentTaskMapper;
    final ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    final SystemBankBranchCatalogMapper systemBankBranchCatalogMapper;
    final SystemCompanyBankAccountMapper systemCompanyBankAccountMapper;
    final SystemCompanyMapper systemCompanyMapper;
    final FinanceVendorMapper financeVendorMapper;
    final UserBankAccountMapper userBankAccountMapper;
    final ExpenseAttachmentService expenseAttachmentService;
    final ObjectMapper objectMapper;

    ExpensePaymentSupportContext(
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
        this.expenseDocumentReadSupport = expenseDocumentReadSupport;
        this.expenseSummaryAssembler = expenseSummaryAssembler;
        this.expenseWorkflowRuntimeSupport = expenseWorkflowRuntimeSupport;
        this.expenseRelationWriteOffService = expenseRelationWriteOffService;
        this.pmBankPaymentRecordMapper = pmBankPaymentRecordMapper;
        this.processDocumentTaskMapper = processDocumentTaskMapper;
        this.processDocumentExpenseDetailMapper = processDocumentExpenseDetailMapper;
        this.processDocumentInstanceMapper = processDocumentInstanceMapper;
        this.systemBankBranchCatalogMapper = systemBankBranchCatalogMapper;
        this.systemCompanyBankAccountMapper = systemCompanyBankAccountMapper;
        this.systemCompanyMapper = systemCompanyMapper;
        this.financeVendorMapper = financeVendorMapper;
        this.userBankAccountMapper = userBankAccountMapper;
        this.expenseAttachmentService = expenseAttachmentService;
        this.objectMapper = objectMapper;
    }
}
