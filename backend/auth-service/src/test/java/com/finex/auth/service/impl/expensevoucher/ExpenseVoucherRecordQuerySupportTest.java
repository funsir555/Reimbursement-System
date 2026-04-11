package com.finex.auth.service.impl.expensevoucher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseVoucherGeneratedRecordDetailVO;
import com.finex.auth.dto.FinanceVoucherDetailVO;
import com.finex.auth.entity.ExpVoucherPushDocument;
import com.finex.auth.entity.ExpVoucherPushEntry;
import com.finex.auth.mapper.ExpVoucherPushBatchMapper;
import com.finex.auth.mapper.ExpVoucherPushDocumentMapper;
import com.finex.auth.mapper.ExpVoucherPushEntryMapper;
import com.finex.auth.mapper.ExpVoucherSubjectMappingMapper;
import com.finex.auth.mapper.ExpVoucherTemplatePolicyMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinanceVoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseVoucherRecordQuerySupportTest {

    @Mock private ExpVoucherTemplatePolicyMapper templatePolicyMapper;
    @Mock private ExpVoucherSubjectMappingMapper subjectMappingMapper;
    @Mock private ExpVoucherPushBatchMapper pushBatchMapper;
    @Mock private ExpVoucherPushDocumentMapper pushDocumentMapper;
    @Mock private ExpVoucherPushEntryMapper pushEntryMapper;
    @Mock private ProcessDocumentInstanceMapper documentInstanceMapper;
    @Mock private ProcessDocumentExpenseDetailMapper expenseDetailMapper;
    @Mock private ProcessDocumentTemplateMapper documentTemplateMapper;
    @Mock private ProcessExpenseTypeMapper expenseTypeMapper;
    @Mock private SystemCompanyMapper systemCompanyMapper;
    @Mock private UserMapper userMapper;
    @Mock private GlAccvouchMapper glAccvouchMapper;
    @Mock private FinanceVoucherService financeVoucherService;

    private ExpenseVoucherRecordQuerySupport support;

    @BeforeEach
    void setUp() {
        support = new ExpenseVoucherRecordQuerySupport(AbstractExpenseVoucherGenerationSupport.dependencies(
                templatePolicyMapper,
                subjectMappingMapper,
                pushBatchMapper,
                pushDocumentMapper,
                pushEntryMapper,
                documentInstanceMapper,
                expenseDetailMapper,
                documentTemplateMapper,
                expenseTypeMapper,
                systemCompanyMapper,
                userMapper,
                glAccvouchMapper,
                financeVoucherService,
                new ObjectMapper()
        ));
    }

    @Test
    void getGeneratedVoucherDetailLoadsEntriesAndVoucherDetail() {
        ExpVoucherPushDocument pushDocument = new ExpVoucherPushDocument();
        pushDocument.setId(9L);
        pushDocument.setCompanyId("COMPANY_A");
        pushDocument.setBatchNo("VG001");
        pushDocument.setDocumentCode("DOC-001");
        pushDocument.setTemplateCode("TMP001");
        pushDocument.setTemplateName("Travel");
        pushDocument.setSubmitterName("alice");
        pushDocument.setTotalAmount(new BigDecimal("88.00"));
        pushDocument.setPushStatus("SUCCESS");
        pushDocument.setVoucherNo("记-0001");
        pushDocument.setVoucherType("GENERAL");
        pushDocument.setVoucherNumber(1);
        pushDocument.setBillDate(LocalDate.of(2026, 4, 11));
        pushDocument.setPushedAt(LocalDateTime.of(2026, 4, 11, 10, 30));

        ExpVoucherPushEntry entry = new ExpVoucherPushEntry();
        entry.setPushDocumentId(9L);
        entry.setEntryNo(1);
        entry.setDirection("DEBIT");
        entry.setDigest("Travel expense");
        entry.setAccountCode("660100");
        entry.setAccountName("Travel expense");
        entry.setExpenseTypeCode("TRAVEL");
        entry.setExpenseTypeName("Travel");
        entry.setAmount(new BigDecimal("88.00"));

        FinanceVoucherDetailVO voucherDetail = new FinanceVoucherDetailVO();

        when(pushDocumentMapper.selectById(9L)).thenReturn(pushDocument);
        when(pushEntryMapper.selectList(any())).thenReturn(List.of(entry));
        when(financeVoucherService.getDetail("COMPANY_A", "记-0001")).thenReturn(voucherDetail);
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of());

        ExpenseVoucherGeneratedRecordDetailVO detail = support.getGeneratedVoucherDetail(9L);

        assertNotNull(detail.getRecord());
        assertEquals("DOC-001", detail.getRecord().getDocumentCode());
        assertEquals(1, detail.getEntries().size());
        assertEquals("660100", detail.getEntries().get(0).getAccountCode());
        assertEquals(voucherDetail, detail.getVoucherDetail());
    }
}
