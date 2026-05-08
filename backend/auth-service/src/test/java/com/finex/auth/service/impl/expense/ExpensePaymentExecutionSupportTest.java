package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpensePaymentExecutionSupportTest {

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
    void startPaymentTaskPushesToBankThroughRuntime() {
        ExpensePaymentExecutionSupport support = newSupport();

        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(1L);
        task.setDocumentCode("DOC-200");
        task.setAssigneeUserId(9L);
        task.setNodeType("PAYMENT");
        task.setStatus("PENDING");

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-200");
        instance.setStatus("PENDING_PAYMENT");

        ProcessDocumentInstance refreshed = new ProcessDocumentInstance();
        refreshed.setDocumentCode("DOC-200");

        SystemCompanyBankAccount account = new SystemCompanyBankAccount();
        account.setId(8L);
        account.setCompanyId("C1");
        account.setStatus(1);
        account.setDirectConnectEnabled(1);
        account.setDirectConnectProvider("CMB");
        account.setDirectConnectChannel("CMB_CLOUD");
        account.setAccountName("Main");
        account.setAccountNo("6222333344445555");

        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();

        when(processDocumentTaskMapper.selectById(1L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-200")).thenReturn(instance, refreshed);
        when(expenseSummaryAssembler.buildSummaryEnrichmentData(any())).thenReturn(enrichmentData);
        when(enrichmentData.metadata("DOC-200")).thenReturn(metadata);
        when(metadata.paymentCompanyId()).thenReturn("C1");
        when(systemCompanyBankAccountMapper.selectList(any())).thenReturn(List.of(account));
        when(expenseDocumentReadSupport.buildDocumentDetail(refreshed)).thenReturn(detail);

        ExpenseDocumentDetailVO actual = support.startPaymentTask(9L, "tester", 1L);

        assertSame(detail, actual);
        verify(expenseWorkflowRuntimeSupport).markPaymentStarted(eq(instance), eq(task), eq(9L), eq("tester"), eq(false), eq(8L), eq("Main（尾号 5555）"), any());
    }

    @Test
    void completePaymentTaskFinalizesWriteOffs() {
        ExpensePaymentExecutionSupport support = newSupport();
        ExpenseApprovalActionDTO dto = new ExpenseApprovalActionDTO();
        dto.setComment("done");

        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(2L);
        task.setDocumentCode("DOC-201");
        task.setAssigneeUserId(9L);
        task.setNodeType("PAYMENT");
        task.setStatus("PENDING");

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-201");
        instance.setStatus("PAYING");

        ProcessDocumentInstance refreshed = new ProcessDocumentInstance();
        refreshed.setDocumentCode("DOC-201");
        refreshed.setStatus("PAYMENT_COMPLETED");

        SystemCompanyBankAccount account = new SystemCompanyBankAccount();
        account.setId(8L);
        account.setCompanyId("C1");
        account.setStatus(1);
        account.setDirectConnectEnabled(1);
        account.setDirectConnectProvider("CMB");
        account.setDirectConnectChannel("CMB_CLOUD");

        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();

        when(processDocumentTaskMapper.selectById(2L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-201")).thenReturn(instance, refreshed, refreshed);
        when(expenseSummaryAssembler.buildSummaryEnrichmentData(any())).thenReturn(enrichmentData);
        when(enrichmentData.metadata("DOC-201")).thenReturn(metadata);
        when(metadata.paymentCompanyId()).thenReturn("C1");
        when(systemCompanyBankAccountMapper.selectList(any())).thenReturn(List.of(account));
        when(pmBankPaymentRecordMapper.selectOne(any())).thenReturn(null);
        when(expenseDocumentReadSupport.buildDocumentDetail(refreshed)).thenReturn(detail);

        ExpenseDocumentDetailVO actual = support.completePaymentTask(9L, "tester", 2L, dto);

        assertSame(detail, actual);
        verify(expenseWorkflowRuntimeSupport).completePaymentRuntime(eq(instance), eq(task), eq(9L), eq("tester"), eq("done"), eq(true), any());
        verify(expenseRelationWriteOffService).finalizeEffectiveWriteOffs("DOC-201");
    }

    @Test
    void completePaymentTaskDoesNotRequireDirectConnectAccountForManualPaid() {
        ExpensePaymentExecutionSupport support = newSupport();
        PmBankPaymentRecord[] insertedRecord = new PmBankPaymentRecord[1];

        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(3L);
        task.setDocumentCode("DOC-202");
        task.setAssigneeUserId(9L);
        task.setNodeType("PAYMENT");
        task.setStatus("PENDING");

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-202");
        instance.setStatus("PAYING");

        ProcessDocumentInstance refreshed = new ProcessDocumentInstance();
        refreshed.setDocumentCode("DOC-202");
        refreshed.setStatus("PAYMENT_COMPLETED");

        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();

        when(processDocumentTaskMapper.selectById(3L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-202")).thenReturn(instance, refreshed, refreshed);
        when(expenseSummaryAssembler.buildSummaryEnrichmentData(any())).thenReturn(enrichmentData);
        when(enrichmentData.metadata("DOC-202")).thenReturn(metadata);
        when(metadata.paymentCompanyId()).thenReturn(null);
        when(pmBankPaymentRecordMapper.selectOne(any())).thenReturn(null);
        when(expenseDocumentReadSupport.buildDocumentDetail(refreshed)).thenReturn(detail);
        doAnswer(invocation -> {
            insertedRecord[0] = invocation.getArgument(0);
            return 1;
        }).when(pmBankPaymentRecordMapper).insert(any(PmBankPaymentRecord.class));

        ExpenseDocumentDetailVO actual = support.completePaymentTask(9L, "tester", 3L, new ExpenseApprovalActionDTO());

        assertSame(detail, actual);
        assertEquals("MANUAL", insertedRecord[0].getBankProvider());
        assertEquals("MANUAL_CONFIRM", insertedRecord[0].getBankChannel());
        verify(expenseWorkflowRuntimeSupport).completePaymentRuntime(eq(instance), eq(task), eq(9L), eq("tester"), eq(null), eq(true), any());
        verify(expenseRelationWriteOffService).finalizeEffectiveWriteOffs("DOC-202");
    }

    @Test
    void validatePaymentTasksExportableAllowsCompletedAndFinishedStatuses() {
        ExpensePaymentExecutionSupport support = newSupport();

        ProcessDocumentTask completedTask = new ProcessDocumentTask();
        completedTask.setId(4L);
        completedTask.setDocumentCode("DOC-203");
        completedTask.setAssigneeUserId(9L);
        completedTask.setNodeType("PAYMENT");
        completedTask.setStatus("APPROVED");

        ProcessDocumentTask finishedTask = new ProcessDocumentTask();
        finishedTask.setId(5L);
        finishedTask.setDocumentCode("DOC-204");
        finishedTask.setAssigneeUserId(9L);
        finishedTask.setNodeType("PAYMENT");
        finishedTask.setStatus("APPROVED");

        ProcessDocumentInstance completedInstance = new ProcessDocumentInstance();
        completedInstance.setDocumentCode("DOC-203");
        completedInstance.setStatus("PAYMENT_COMPLETED");

        ProcessDocumentInstance finishedInstance = new ProcessDocumentInstance();
        finishedInstance.setDocumentCode("DOC-204");
        finishedInstance.setStatus("PAYMENT_FINISHED");

        when(processDocumentTaskMapper.selectById(4L)).thenReturn(completedTask);
        when(processDocumentTaskMapper.selectById(5L)).thenReturn(finishedTask);
        when(expenseDocumentReadSupport.requireDocument("DOC-203")).thenReturn(completedInstance);
        when(expenseDocumentReadSupport.requireDocument("DOC-204")).thenReturn(finishedInstance);

        assertEquals(true, support.validatePaymentTasksExportable(9L, List.of(4L, 5L)));
    }

    @Test
    void voidPaymentTasksReopensCompletedTaskAndReturnsToPreviousStatus() {
        ExpensePaymentExecutionSupport support = newSupport();
        PmBankPaymentRecord record = new PmBankPaymentRecord();
        record.setReceiptStatus("FAILED");
        record.setManualPaid(1);

        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(6L);
        task.setDocumentCode("DOC-205");
        task.setAssigneeUserId(9L);
        task.setNodeType("PAYMENT");
        task.setStatus("APPROVED");

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-205");
        instance.setStatus("PAYMENT_COMPLETED");

        when(processDocumentTaskMapper.selectById(6L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-205")).thenReturn(instance);
        when(expenseWorkflowRuntimeSupport.resolvePaymentVoidTargetStatus(instance)).thenReturn("PAYING");
        when(pmBankPaymentRecordMapper.selectOne(any())).thenReturn(record);

        assertEquals(true, support.voidPaymentTasks(9L, "tester", List.of(6L)));

        assertEquals("PENDING", task.getStatus());
        assertEquals(null, task.getHandledAt());
        assertEquals(null, task.getActionComment());
        assertEquals(Integer.valueOf(0), record.getManualPaid());
        assertEquals(null, record.getPaidAt());
        assertEquals("PENDING", record.getReceiptStatus());
        assertEquals(null, record.getLastErrorMessage());
        verify(processDocumentTaskMapper).updateById(task);
        verify(expenseWorkflowRuntimeSupport).revertPaymentToStatus(
                instance,
                task,
                9L,
                "tester",
                "作废后返回支付中",
                "PAYMENT_COMPLETED",
                "PAYING"
        );
        verify(pmBankPaymentRecordMapper).updateById(record);
    }

    private ExpensePaymentExecutionSupport newSupport() {
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
        return new ExpensePaymentExecutionSupport(context, new ExpensePaymentRecordSupport(context));
    }
}
