package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseBankLinkConfigVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.SystemCompanyBankAccount;
import com.finex.auth.mapper.PmBankPaymentRecordMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.mapper.SystemCompanyBankAccountMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpensePaymentDomainSupportTest {

    @Mock private ExpenseDocumentMutationSupport expenseDocumentMutationSupport;
    @Mock private ExpenseSummaryAssembler expenseSummaryAssembler;
    @Mock private ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;
    @Mock private ExpenseRelationWriteOffService expenseRelationWriteOffService;
    @Mock private PmBankPaymentRecordMapper pmBankPaymentRecordMapper;
    @Mock private ProcessDocumentTaskMapper processDocumentTaskMapper;
    @Mock private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock private SystemCompanyBankAccountMapper systemCompanyBankAccountMapper;
    @Mock private SystemCompanyMapper systemCompanyMapper;
    @Mock private ExpenseAttachmentService expenseAttachmentService;

    @Mock private ExpenseSummaryAssembler.SummaryEnrichmentData enrichmentData;
    @Mock private ExpenseSummaryAssembler.SummaryMetadata metadata;

    @Test
    void listPaymentOrdersBuildsItemsLocally() {
        ExpensePaymentDomainSupport support = newSupport();
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(20L);
        task.setDocumentCode("DOC-001");
        task.setNodeType("PAYMENT");
        task.setNodeName("Pay");
        task.setAssigneeUserId(1L);

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");
        instance.setDocumentTitle("Taxi");
        instance.setTemplateName("Expense");
        instance.setStatus("PENDING_PAYMENT");

        when(processDocumentTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(instance));
        when(expenseSummaryAssembler.buildSummaryEnrichmentData(any())).thenReturn(enrichmentData);
        when(enrichmentData.metadata("DOC-001")).thenReturn(metadata);
        when(metadata.submitterDeptName()).thenReturn("Finance");
        when(metadata.paymentDate()).thenReturn("2026-04-10");
        when(metadata.paymentCompanyName()).thenReturn("HQ");
        when(pmBankPaymentRecordMapper.selectList(any())).thenReturn(List.of());
        when(expenseWorkflowRuntimeSupport.paymentTaskAllowsRetry(instance, task)).thenReturn(false);

        List<ExpensePaymentOrderVO> actual = support.listPaymentOrders(1L, "PENDING_PAYMENT");

        assertEquals(1, actual.size());
        assertEquals("DOC-001", actual.get(0).getDocumentCode());
        assertEquals("HQ", actual.get(0).getPaymentCompanyName());
    }

    @Test
    void getBankLinkBuildsConfigLocally() {
        SystemCompanyBankAccount account = new SystemCompanyBankAccount();
        account.setId(9L);
        account.setCompanyId("C1");
        account.setAccountName("Main");
        account.setAccountNo("6222000012345678");
        account.setBankName("CMB");
        account.setDirectConnectEnabled(1);
        account.setDirectConnectProvider("CMB");
        account.setDirectConnectChannel("CMB_CLOUD");
        account.setDirectConnectExtJson("{\"operatorKey\":\"op-key\"}");
        ExpensePaymentDomainSupport support = newSupport();
        when(systemCompanyBankAccountMapper.selectById(9L)).thenReturn(account);
        when(systemCompanyMapper.selectOne(any())).thenReturn(null);

        ExpenseBankLinkConfigVO actual = support.getBankLink(9L);

        assertEquals(9L, actual.getCompanyBankAccountId());
        assertEquals("op-key", actual.getOperatorKey());
        assertEquals("Main", actual.getAccountName());
    }

    @Test
    void completePaymentTaskUsesLocalPaymentOwner() {
        ExpensePaymentDomainSupport support = newSupport();
        ExpenseApprovalActionDTO dto = new ExpenseApprovalActionDTO();
        dto.setComment("done");

        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(30L);
        task.setDocumentCode("DOC-003");
        task.setNodeType("PAYMENT");
        task.setStatus("PENDING");
        task.setAssigneeUserId(1L);

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-003");
        instance.setStatus("PENDING_PAYMENT");
        ProcessDocumentInstance completed = new ProcessDocumentInstance();
        completed.setDocumentCode("DOC-003");
        completed.setStatus("PAYMENT_COMPLETED");

        SystemCompanyBankAccount account = new SystemCompanyBankAccount();
        account.setId(8L);
        account.setCompanyId("C1");
        account.setStatus(1);
        account.setDirectConnectEnabled(1);
        account.setDirectConnectProvider("CMB");
        account.setDirectConnectChannel("CMB_CLOUD");

        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();

        when(processDocumentTaskMapper.selectById(30L)).thenReturn(task);
        when(expenseDocumentMutationSupport.requireDocument("DOC-003")).thenReturn(instance, completed, completed, completed);
        when(expenseSummaryAssembler.buildSummaryEnrichmentData(any())).thenReturn(enrichmentData);
        when(enrichmentData.metadata("DOC-003")).thenReturn(metadata);
        when(metadata.paymentCompanyId()).thenReturn("C1");
        when(systemCompanyBankAccountMapper.selectList(any())).thenReturn(List.of(account));
        when(pmBankPaymentRecordMapper.selectOne(any())).thenReturn(null);
        when(expenseDocumentMutationSupport.buildDocumentDetail(any())).thenReturn(detail);

        ExpenseDocumentDetailVO actual = support.completePaymentTask(1L, "tester", 30L, dto);

        assertSame(detail, actual);
        verify(expenseWorkflowRuntimeSupport).completePaymentRuntime(eq(instance), eq(task), eq(1L), eq("tester"), eq("done"), eq(true), any());
        verify(expenseRelationWriteOffService).finalizeEffectiveWriteOffs("DOC-003");
    }

    @Test
    void bankCallbacksAndPollingUsePaymentOwner() {
        ExpensePaymentDomainSupport support = newSupport();
        ExpenseBankCallbackDTO dto = new ExpenseBankCallbackDTO();
        dto.setDocumentCode("DOC-004");
        dto.setSuccess(true);

        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(40L);
        task.setDocumentCode("DOC-004");

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-004");
        instance.setStatus("PAYMENT_COMPLETED");

        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        com.finex.auth.entity.PmBankPaymentRecord record = new com.finex.auth.entity.PmBankPaymentRecord();
        record.setId(1L);
        record.setTaskId(40L);
        record.setDocumentCode("DOC-004");

        SystemCompanyBankAccount account = new SystemCompanyBankAccount();
        account.setId(11L);
        record.setCompanyBankAccountId(11L);

        when(pmBankPaymentRecordMapper.selectOne(any())).thenReturn(record);
        when(processDocumentTaskMapper.selectById(40L)).thenReturn(task);
        when(systemCompanyBankAccountMapper.selectById(11L)).thenReturn(account);
        when(expenseDocumentMutationSupport.requireDocument("DOC-004")).thenReturn(instance, instance, instance);
        when(expenseDocumentMutationSupport.buildDocumentDetail(any())).thenReturn(detail);
        when(pmBankPaymentRecordMapper.selectList(any())).thenReturn(List.of(record));

        ExpenseDocumentDetailVO actual = support.handleCmbCloudCallback(dto);
        support.runBankReceiptPolling();

        assertSame(detail, actual);
        verify(pmBankPaymentRecordMapper).updateById(record);
    }

    private ExpensePaymentDomainSupport newSupport() {
        return new ExpensePaymentDomainSupport(
                expenseDocumentMutationSupport,
                expenseSummaryAssembler,
                expenseWorkflowRuntimeSupport,
                expenseRelationWriteOffService,
                pmBankPaymentRecordMapper,
                processDocumentTaskMapper,
                processDocumentInstanceMapper,
                systemCompanyBankAccountMapper,
                systemCompanyMapper,
                expenseAttachmentService,
                new ObjectMapper()
        );
    }
}
