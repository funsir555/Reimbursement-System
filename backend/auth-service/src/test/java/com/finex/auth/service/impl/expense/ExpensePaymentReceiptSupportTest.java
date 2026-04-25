package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseAttachmentVO;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.entity.PmBankPaymentRecord;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpensePaymentReceiptSupportTest {

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

    @Test
    void handleCallbackCompletesRuntimeThroughExecutionOwner() {
        ExpensePaymentReceiptSupport support = newSupport();

        ExpenseBankCallbackDTO dto = new ExpenseBankCallbackDTO();
        dto.setDocumentCode("DOC-300");
        dto.setSuccess(true);

        PmBankPaymentRecord record = new PmBankPaymentRecord();
        record.setId(1L);
        record.setTaskId(5L);
        record.setDocumentCode("DOC-300");
        record.setCompanyBankAccountId(11L);

        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(5L);
        task.setDocumentCode("DOC-300");

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-300");
        instance.setStatus("PAYING");

        ProcessDocumentInstance refreshed = new ProcessDocumentInstance();
        refreshed.setDocumentCode("DOC-300");
        refreshed.setStatus("PAYMENT_COMPLETED");

        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();

        SystemCompanyBankAccount account = new SystemCompanyBankAccount();
        account.setId(11L);

        when(pmBankPaymentRecordMapper.selectOne(any())).thenReturn(record, record);
        when(processDocumentTaskMapper.selectById(5L)).thenReturn(task);
        when(systemCompanyBankAccountMapper.selectById(11L)).thenReturn(account);
        when(expenseDocumentReadSupport.requireDocument("DOC-300")).thenReturn(instance, refreshed, refreshed);
        when(expenseDocumentReadSupport.buildDocumentDetail(refreshed)).thenReturn(detail);

        ExpenseDocumentDetailVO actual = support.handleCmbCloudCallback(dto);

        assertSame(detail, actual);
        verify(pmBankPaymentRecordMapper, atLeastOnce()).updateById(record);
        verify(expenseWorkflowRuntimeSupport).completePaymentRuntime(any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void runBankReceiptPollingAttachesReceiptWhenQueryEnabled() {
        ExpensePaymentReceiptSupport support = newSupport();

        PmBankPaymentRecord record = new PmBankPaymentRecord();
        record.setId(1L);
        record.setDocumentCode("DOC-301");
        record.setCompanyBankAccountId(12L);
        record.setPaidAt(java.time.LocalDateTime.now());

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-301");
        instance.setDocumentTitle("Trip");
        instance.setStatus("PAYMENT_COMPLETED");

        SystemCompanyBankAccount account = new SystemCompanyBankAccount();
        account.setId(12L);
        account.setAccountName("Main");
        account.setAccountNo("6222333344445555");
        account.setDirectConnectExtJson("{\"receiptQueryEnabled\":true}");

        ExpenseAttachmentVO attachment = new ExpenseAttachmentVO();
        attachment.setAttachmentId("ATT-1");
        attachment.setFileName("receipt.txt");

        when(pmBankPaymentRecordMapper.selectList(any())).thenReturn(List.of(record));
        when(expenseDocumentReadSupport.requireDocument("DOC-301")).thenReturn(instance);
        when(systemCompanyBankAccountMapper.selectById(12L)).thenReturn(account);
        when(expenseAttachmentService.saveGeneratedAttachment(any(), any(), any())).thenReturn(attachment);

        support.runBankReceiptPolling();

        verify(pmBankPaymentRecordMapper).updateById(record);
        verify(processDocumentInstanceMapper).updateById(instance);
    }

    private ExpensePaymentReceiptSupport newSupport() {
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
                new ObjectMapper()
        );
        ExpensePaymentRecordSupport recordSupport = new ExpensePaymentRecordSupport(context);
        ExpensePaymentExecutionSupport executionSupport = new ExpensePaymentExecutionSupport(context, recordSupport);
        return new ExpensePaymentReceiptSupport(context, recordSupport, executionSupport);
    }
}
