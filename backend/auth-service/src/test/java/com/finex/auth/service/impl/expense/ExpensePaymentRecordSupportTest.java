package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.entity.PmBankPaymentRecord;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.SystemCompanyBankAccount;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpensePaymentRecordSupportTest {

    @Mock private ExpenseDocumentReadSupport expenseDocumentReadSupport;
    @Mock private ExpenseSummaryAssembler expenseSummaryAssembler;
    @Mock private ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;
    @Mock private ExpenseRelationWriteOffService expenseRelationWriteOffService;
    @Mock private PmBankPaymentRecordMapper pmBankPaymentRecordMapper;
    @Mock private ProcessDocumentTaskMapper processDocumentTaskMapper;
    @Mock private ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    @Mock private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock private SystemBankBranchCatalogMapper systemBankBranchCatalogMapper;
    @Mock private SystemCompanyBankAccountMapper systemCompanyBankAccountMapper;
    @Mock private SystemCompanyMapper systemCompanyMapper;
    @Mock private FinanceVendorMapper financeVendorMapper;
    @Mock private UserBankAccountMapper userBankAccountMapper;
    @Mock private ExpenseAttachmentService expenseAttachmentService;
    @Mock private ExpenseSummaryAssembler.SummaryEnrichmentData enrichmentData;
    @Mock private ExpenseSummaryAssembler.SummaryMetadata metadata;

    @Test
    void requireBankPaymentRecordForCallbackFallsBackToDocumentCode() {
        ExpensePaymentRecordSupport support = newSupport();

        ExpenseBankCallbackDTO dto = new ExpenseBankCallbackDTO();
        dto.setDocumentCode("DOC-400");

        PmBankPaymentRecord record = new PmBankPaymentRecord();
        record.setDocumentCode("DOC-400");

        when(pmBankPaymentRecordMapper.selectOne(any())).thenReturn(record);

        assertSame(record, support.requireBankPaymentRecordForCallback(dto));
    }

    @Test
    void findActiveBankAccountForDocumentUsesPaymentCompanyMetadata() {
        ExpensePaymentRecordSupport support = newSupport();

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-401");

        SystemCompanyBankAccount account = new SystemCompanyBankAccount();
        account.setId(12L);
        account.setCompanyId("C1");

        when(expenseSummaryAssembler.buildSummaryEnrichmentData(any())).thenReturn(enrichmentData);
        when(enrichmentData.metadata("DOC-401")).thenReturn(metadata);
        when(metadata.paymentCompanyId()).thenReturn("C1");
        when(systemCompanyBankAccountMapper.selectList(any())).thenReturn(List.of(account));

        SystemCompanyBankAccount actual = support.findActiveBankAccountForDocument(instance);

        assertEquals(12L, actual.getId());
    }

    private ExpensePaymentRecordSupport newSupport() {
        return new ExpensePaymentRecordSupport(
                new ExpensePaymentSupportContext(
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
                        new ObjectMapper()
                )
        );
    }
}
