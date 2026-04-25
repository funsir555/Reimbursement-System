package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseBankLinkConfigVO;
import com.finex.auth.dto.ExpenseBankLinkSaveDTO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseBankLinkDomainSupportTest {

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
    void updateBankLinkDisablesSiblingAccountsAndReturnsConfig() {
        ExpensePaymentSupportContext context = newContext();
        ExpensePaymentRecordSupport recordSupport = new ExpensePaymentRecordSupport(context);
        ExpenseBankLinkDomainSupport support = new ExpenseBankLinkDomainSupport(context, recordSupport);

        SystemCompanyBankAccount current = new SystemCompanyBankAccount();
        current.setId(10L);
        current.setCompanyId("C1");
        current.setAccountName("Main");
        current.setAccountNo("6222");

        SystemCompanyBankAccount sibling = new SystemCompanyBankAccount();
        sibling.setId(11L);
        sibling.setCompanyId("C1");
        sibling.setDirectConnectEnabled(1);
        sibling.setDirectConnectProvider("CMB");
        sibling.setDirectConnectChannel("CMB_CLOUD");

        ExpenseBankLinkSaveDTO dto = new ExpenseBankLinkSaveDTO();
        dto.setEnabled(true);
        dto.setDirectConnectProvider("CMB");
        dto.setDirectConnectChannel("CMB_CLOUD");
        dto.setOperatorKey("op-key");
        dto.setCallbackSecret("secret");

        when(systemCompanyBankAccountMapper.selectById(10L)).thenReturn(current, current);
        when(systemCompanyBankAccountMapper.selectList(any())).thenReturn(List.of(sibling));

        ExpenseBankLinkConfigVO actual = support.updateBankLink(10L, dto);

        assertEquals(10L, actual.getCompanyBankAccountId());
        assertEquals("op-key", actual.getOperatorKey());
        verify(systemCompanyBankAccountMapper).updateById(current);
        verify(systemCompanyBankAccountMapper).updateById(sibling);
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
