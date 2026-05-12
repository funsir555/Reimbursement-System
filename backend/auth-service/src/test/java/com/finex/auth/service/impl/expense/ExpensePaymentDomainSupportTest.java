package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseBankLinkConfigVO;
import com.finex.auth.dto.ExpenseBankLinkSummaryVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.PmBankPaymentRecord;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.SystemBankBranchCatalog;
import com.finex.auth.entity.SystemCompanyBankAccount;
import com.finex.auth.entity.UserBankAccount;
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

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpensePaymentDomainSupportTest {

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
    void listPaymentOrdersBuildsItemsLocally() {
        ExpensePaymentDomainSupport support = newSupport();
        ProcessDocumentTask task = paymentTask(20L, "DOC-001");
        ProcessDocumentInstance instance = paymentInstance("DOC-001", "Taxi", "PENDING_PAYMENT");

        stubListPaymentOrdersBase(task, instance);
        when(metadata.submitterDeptName()).thenReturn("Finance");
        when(metadata.paymentDate()).thenReturn("2026-04-10");
        when(metadata.paymentCompanyName()).thenReturn("HQ");
        when(metadata.counterpartyName()).thenReturn("供应商A");

        List<ExpensePaymentOrderVO> actual = support.listPaymentOrders(1L, "PENDING_PAYMENT");

        assertEquals(1, actual.size());
        assertEquals("DOC-001", actual.get(0).getDocumentCode());
        assertEquals("HQ", actual.get(0).getPaymentCompanyName());
        assertEquals("供应商A", actual.get(0).getPayeeOrCounterpartyName());
        assertEquals("待回单", actual.get(0).getReceiptStatusLabel());
        assertEquals(BigDecimal.ZERO, actual.get(0).getActualPaymentAmount());
    }

    @Test
    void listPaymentOrdersResolvesVendorReceiverInfoAndExportFields() {
        ExpensePaymentDomainSupport support = newSupport();
        ProcessDocumentTask task = paymentTask(21L, "DOC-002");
        ProcessDocumentInstance instance = paymentInstance("DOC-002", "Hotel", "PENDING_PAYMENT");
        instance.setFormSchemaSnapshotJson("""
                {"layoutMode":"TWO_COLUMN","blocks":[
                  {"kind":"BUSINESS_COMPONENT","fieldKey":"payeeAccountField","props":{"componentCode":"payee-account"}},
                  {"kind":"BUSINESS_COMPONENT","fieldKey":"bankSummaryField","props":{"componentCode":"bank-push-summary"}}
                ]}
                """);
        instance.setFormDataJson("""
                {"payeeAccountField":{"sourceType":"VENDOR","value":"VENDOR-001"},"bankSummaryField":"差旅付款摘要"}
                """);

        FinanceVendor vendor = new FinanceVendor();
        vendor.setCVenCode("VENDOR-001");
        vendor.setCompanyId("C1");
        vendor.setCVenName("上海供应商");
        vendor.setCVenAccount("6222000012345678");
        vendor.setCVenBank("招商银行");
        vendor.setReceiptBranchCode("BR-001");
        vendor.setReceiptBranchName("招商银行上海分行");
        vendor.setReceiptAccountName("上海供应商");
        vendor.setReceiptBankProvince("错误省份");
        vendor.setReceiptBankCity("错误城市");

        SystemBankBranchCatalog branch = new SystemBankBranchCatalog();
        branch.setBranchCode("BR-001");
        branch.setProvince("上海");
        branch.setCity("上海市");

        ProcessDocumentExpenseDetail detail1 = new ProcessDocumentExpenseDetail();
        detail1.setDocumentCode("DOC-002");
        detail1.setActualPaymentAmount(new BigDecimal("30.50"));
        ProcessDocumentExpenseDetail detail2 = new ProcessDocumentExpenseDetail();
        detail2.setDocumentCode("DOC-002");
        detail2.setActualPaymentAmount(new BigDecimal("19.50"));

        stubListPaymentOrdersBase(task, instance);
        when(metadata.paymentCompanyId()).thenReturn("C1");
        when(financeVendorMapper.selectOne(any())).thenReturn(vendor);
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of(detail1, detail2));
        when(systemBankBranchCatalogMapper.selectList(any())).thenReturn(List.of(branch));

        List<ExpensePaymentOrderVO> actual = support.listPaymentOrders(1L, "PENDING_PAYMENT");

        assertEquals(1, actual.size());
        ExpensePaymentOrderVO item = actual.get(0);
        assertEquals("上海供应商", item.getPayeeOrCounterpartyName());
        assertEquals("6222000012345678", item.getPayeeAccountNo());
        assertEquals("招商银行上海分行", item.getPayeeBankName());
        assertEquals("上海", item.getPayeeBankProvince());
        assertEquals("上海市", item.getPayeeBankCity());
        assertEquals("差旅付款摘要", item.getBankPushSummary());
        assertEquals(new BigDecimal("50.00"), item.getActualPaymentAmount());
    }

    @Test
    void listPaymentOrdersFallsBackToPersonalAccountInfo() {
        ExpensePaymentDomainSupport support = newSupport();
        ProcessDocumentTask task = paymentTask(22L, "DOC-USER");
        ProcessDocumentInstance instance = paymentInstance("DOC-USER", "Travel", "PENDING_PAYMENT");
        instance.setFormSchemaSnapshotJson("""
                {"layoutMode":"TWO_COLUMN","blocks":[
                  {"kind":"BUSINESS_COMPONENT","fieldKey":"payeeAccountField","props":{"componentCode":"payee-account"}}
                ]}
                """);
        instance.setFormDataJson("""
                {"payeeAccountField":{"sourceType":"USER","value":"USER_ACCOUNT:88"}}
                """);

        UserBankAccount account = new UserBankAccount();
        account.setId(88L);
        account.setAccountName("李四");
        account.setAccountNo("621700008888");
        account.setBranchName("中国银行北京东城支行");
        account.setBankName("中国银行");
        account.setBranchCode("NO-CATALOG");
        account.setProvince("北京");
        account.setCity("北京市");

        stubListPaymentOrdersBase(task, instance);
        when(userBankAccountMapper.selectById(88L)).thenReturn(account);

        List<ExpensePaymentOrderVO> actual = support.listPaymentOrders(1L, "PENDING_PAYMENT");

        ExpensePaymentOrderVO item = actual.get(0);
        assertEquals("李四", item.getPayeeOrCounterpartyName());
        assertEquals("621700008888", item.getPayeeAccountNo());
        assertEquals("中国银行北京东城支行", item.getPayeeBankName());
        assertEquals("北京", item.getPayeeBankProvince());
        assertEquals("北京市", item.getPayeeBankCity());
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
    void listPaymentOrdersResolvesReceiptStatusLabelsToChinese() {
        ExpensePaymentDomainSupport support = newSupport();
        ProcessDocumentTask task = paymentTask(23L, "DOC-RECEIPT");
        ProcessDocumentInstance instance = paymentInstance("DOC-RECEIPT", "Receipt", "PENDING_PAYMENT");

        PmBankPaymentRecord record = new PmBankPaymentRecord();
        record.setDocumentCode("DOC-RECEIPT");
        record.setReceiptStatus("FAILED");

        stubListPaymentOrdersBase(task, instance);
        when(pmBankPaymentRecordMapper.selectList(any())).thenReturn(List.of(record));

        List<ExpensePaymentOrderVO> actual = support.listPaymentOrders(1L, "PENDING_PAYMENT");

        assertEquals("回单失败", actual.get(0).getReceiptStatusLabel());
    }

    @Test
    void listPaymentOrdersTreatsManualPaidWithoutReceiptAsPendingReceipt() {
        ExpensePaymentDomainSupport support = newSupport();
        ProcessDocumentTask task = paymentTask(24L, "DOC-MANUAL");
        ProcessDocumentInstance instance = paymentInstance("DOC-MANUAL", "Manual", "PAYMENT_COMPLETED");

        PmBankPaymentRecord record = new PmBankPaymentRecord();
        record.setDocumentCode("DOC-MANUAL");
        record.setManualPaid(1);
        record.setReceiptStatus("RECEIVED");
        record.setReceiptAttachmentId(null);

        stubListPaymentOrdersBase(task, instance);
        when(pmBankPaymentRecordMapper.selectList(any())).thenReturn(List.of(record));

        List<ExpensePaymentOrderVO> actual = support.listPaymentOrders(1L, "PAYMENT_COMPLETED");

        assertEquals("待回单", actual.get(0).getReceiptStatusLabel());
    }

    @Test
    void listBankLinksUsesChineseStatusLabelsWithoutGarbledText() {
        ExpensePaymentDomainSupport support = newSupport();

        SystemCompanyBankAccount account = new SystemCompanyBankAccount();
        account.setId(11L);
        account.setCompanyId("C1");
        account.setAccountName("Main");
        account.setAccountNo("6222000012345678");
        account.setBankName("招商银行");
        account.setStatus(1);
        account.setDirectConnectEnabled(1);
        account.setDirectConnectProvider("CMB");
        account.setDirectConnectChannel("CMB_CLOUD");

        PmBankPaymentRecord latestRecord = new PmBankPaymentRecord();
        latestRecord.setCompanyBankAccountId(11L);
        latestRecord.setReceiptStatus("RECEIVED");

        when(systemCompanyBankAccountMapper.selectList(any())).thenReturn(List.of(account));
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of());
        when(pmBankPaymentRecordMapper.selectList(any())).thenReturn(List.of(latestRecord));

        List<ExpenseBankLinkSummaryVO> actual = support.listBankLinks();

        assertEquals(1, actual.size());
        assertEquals("已启用", actual.get(0).getDirectConnectStatusLabel());
        assertEquals("未推送", actual.get(0).getLastDirectConnectStatus());
        assertEquals("已收回单", actual.get(0).getLastReceiptStatus());
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
        when(expenseDocumentReadSupport.requireDocument("DOC-003")).thenReturn(instance, completed, completed, completed);
        when(expenseSummaryAssembler.buildSummaryEnrichmentData(any())).thenReturn(enrichmentData);
        when(enrichmentData.metadata("DOC-003")).thenReturn(metadata);
        when(metadata.paymentCompanyId()).thenReturn("C1");
        when(systemCompanyBankAccountMapper.selectList(any())).thenReturn(List.of(account));
        when(pmBankPaymentRecordMapper.selectOne(any())).thenReturn(null);
        when(expenseDocumentReadSupport.buildDocumentDetail(any())).thenReturn(detail);

        ExpenseDocumentDetailVO actual = support.completePaymentTask(1L, "tester", 30L, dto);

        assertSame(detail, actual);
        verify(expenseWorkflowRuntimeSupport).completePaymentRuntime(eq(instance), eq(task), eq(1L), eq("tester"), eq("done"), eq(true), any());
        verify(expenseRelationWriteOffService).finalizeEffectiveWriteOffs("DOC-003");
    }

    @Test
    void markPaymentTasksAsPayingMovesPendingTasksToPaying() {
        ExpensePaymentDomainSupport support = newSupport();

        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(60L);
        task.setDocumentCode("DOC-EXPORT");
        task.setNodeType("PAYMENT");
        task.setNodeName("Pay");
        task.setStatus("PENDING");
        task.setAssigneeUserId(1L);

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-EXPORT");
        instance.setStatus("PENDING_PAYMENT");

        when(processDocumentTaskMapper.selectById(60L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-EXPORT")).thenReturn(instance);

        boolean actual = support.markPaymentTasksAsPaying(1L, "tester", List.of(60L));

        assertEquals(true, actual);
        verify(expenseWorkflowRuntimeSupport).markPaymentStarted(
                eq(instance),
                eq(task),
                eq(1L),
                eq("tester"),
                eq(false),
                isNull(),
                isNull(),
                isNull()
        );
    }

    @Test
    void voidPaymentTasksReturnsPayingTasksToPending() {
        ExpensePaymentDomainSupport support = newSupport();

        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(61L);
        task.setDocumentCode("DOC-VOID");
        task.setNodeType("PAYMENT");
        task.setNodeName("Pay");
        task.setStatus("PENDING");
        task.setAssigneeUserId(1L);

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-VOID");
        instance.setStatus("PAYING");

        PmBankPaymentRecord record = new PmBankPaymentRecord();
        record.setId(6L);
        record.setDocumentCode("DOC-VOID");
        record.setManualPaid(1);
        record.setReceiptStatus("PENDING");

        when(processDocumentTaskMapper.selectById(61L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-VOID")).thenReturn(instance);
        when(expenseWorkflowRuntimeSupport.resolvePaymentVoidTargetStatus(instance)).thenReturn("PENDING_PAYMENT");
        when(pmBankPaymentRecordMapper.selectOne(any())).thenReturn(record);

        boolean actual = support.voidPaymentTasks(1L, "tester", List.of(61L));

        assertEquals(true, actual);
        verify(expenseWorkflowRuntimeSupport).revertPaymentToStatus(
                eq(instance),
                eq(task),
                eq(1L),
                eq("tester"),
                eq("作废后返回待支付"),
                eq("PAYING"),
                eq("PENDING_PAYMENT")
        );
        verify(pmBankPaymentRecordMapper).updateById(record);
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
        PmBankPaymentRecord record = new PmBankPaymentRecord();
        record.setId(1L);
        record.setTaskId(40L);
        record.setDocumentCode("DOC-004");

        SystemCompanyBankAccount account = new SystemCompanyBankAccount();
        account.setId(11L);
        record.setCompanyBankAccountId(11L);

        when(pmBankPaymentRecordMapper.selectOne(any())).thenReturn(record);
        when(processDocumentTaskMapper.selectById(40L)).thenReturn(task);
        when(systemCompanyBankAccountMapper.selectById(11L)).thenReturn(account);
        when(expenseDocumentReadSupport.requireDocument("DOC-004")).thenReturn(instance, instance, instance);
        when(expenseDocumentReadSupport.buildDocumentDetail(any())).thenReturn(detail);
        when(pmBankPaymentRecordMapper.selectList(any())).thenReturn(List.of(record));

        ExpenseDocumentDetailVO actual = support.handleCmbCloudCallback(dto);
        support.runBankReceiptPolling();

        assertSame(detail, actual);
        verify(pmBankPaymentRecordMapper).updateById(record);
    }

    private void stubListPaymentOrdersBase(ProcessDocumentTask task, ProcessDocumentInstance instance) {
        when(processDocumentTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(instance));
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of());
        when(expenseSummaryAssembler.buildSummaryEnrichmentData(any())).thenReturn(enrichmentData);
        when(enrichmentData.metadata(task.getDocumentCode())).thenReturn(metadata);
        when(pmBankPaymentRecordMapper.selectList(any())).thenReturn(List.of());
        when(expenseWorkflowRuntimeSupport.paymentTaskAllowsRetry(instance, task)).thenReturn(false);
    }

    private ProcessDocumentTask paymentTask(Long id, String documentCode) {
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(id);
        task.setDocumentCode(documentCode);
        task.setNodeType("PAYMENT");
        task.setNodeName("Pay");
        task.setAssigneeUserId(1L);
        return task;
    }

    private ProcessDocumentInstance paymentInstance(String documentCode, String title, String status) {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode(documentCode);
        instance.setDocumentTitle(title);
        instance.setTemplateName("Expense");
        instance.setStatus(status);
        return instance;
    }

    private ExpensePaymentDomainSupport newSupport() {
        return new ExpensePaymentDomainSupport(
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
    }
}
