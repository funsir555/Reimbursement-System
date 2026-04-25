package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.SystemBankBranchCatalog;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpensePaymentOrderQuerySupportTest {

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
    void listPaymentOrdersResolvesVendorReceiverAndAmount() {
        ExpensePaymentOrderQuerySupport support = new ExpensePaymentOrderQuerySupport(newContext());

        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(1L);
        task.setDocumentCode("DOC-100");
        task.setNodeType("PAYMENT");
        task.setNodeName("Pay");

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-100");
        instance.setDocumentTitle("Hotel");
        instance.setTemplateName("Expense");
        instance.setStatus("PENDING_PAYMENT");
        instance.setFormSchemaSnapshotJson("""
                {"blocks":[{"kind":"BUSINESS_COMPONENT","fieldKey":"payeeField","props":{"componentCode":"payee-account"}}]}
                """);
        instance.setFormDataJson("""
                {"payeeField":{"sourceType":"VENDOR","value":"VENDOR-1"}}
                """);

        FinanceVendor vendor = new FinanceVendor();
        vendor.setCVenCode("VENDOR-1");
        vendor.setCompanyId("C1");
        vendor.setCVenName("Vendor A");
        vendor.setCVenAccount("6222000012345678");
        vendor.setReceiptBranchName("CMB Shanghai");
        vendor.setReceiptBranchCode("BR-1");

        SystemBankBranchCatalog branch = new SystemBankBranchCatalog();
        branch.setBranchCode("BR-1");
        branch.setProvince("Shanghai");
        branch.setCity("Shanghai");

        ProcessDocumentExpenseDetail detail1 = new ProcessDocumentExpenseDetail();
        detail1.setDocumentCode("DOC-100");
        detail1.setActualPaymentAmount(new BigDecimal("30.50"));
        ProcessDocumentExpenseDetail detail2 = new ProcessDocumentExpenseDetail();
        detail2.setDocumentCode("DOC-100");
        detail2.setActualPaymentAmount(new BigDecimal("19.50"));

        when(processDocumentTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(instance), List.of(instance));
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of(detail1, detail2));
        when(expenseSummaryAssembler.buildSummaryEnrichmentData(any())).thenReturn(enrichmentData);
        when(enrichmentData.metadata("DOC-100")).thenReturn(metadata);
        when(metadata.paymentCompanyId()).thenReturn("C1");
        when(metadata.paymentCompanyName()).thenReturn("HQ");
        when(financeVendorMapper.selectOne(any())).thenReturn(vendor);
        when(systemBankBranchCatalogMapper.selectList(any())).thenReturn(List.of(branch));
        when(pmBankPaymentRecordMapper.selectList(any())).thenReturn(List.of());
        when(expenseWorkflowRuntimeSupport.paymentTaskAllowsRetry(instance, task)).thenReturn(false);

        List<ExpensePaymentOrderVO> actual = support.listPaymentOrders(1L, "PENDING_PAYMENT");

        assertEquals(1, actual.size());
        assertEquals("Vendor A", actual.get(0).getPayeeOrCounterpartyName());
        assertEquals("CMB Shanghai", actual.get(0).getPayeeBankName());
        assertEquals("Shanghai", actual.get(0).getPayeeBankProvince());
        assertEquals(new BigDecimal("50.00"), actual.get(0).getActualPaymentAmount());
    }

    private ExpensePaymentSupportContext newContext() {
        return new ExpensePaymentSupportContext(
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
