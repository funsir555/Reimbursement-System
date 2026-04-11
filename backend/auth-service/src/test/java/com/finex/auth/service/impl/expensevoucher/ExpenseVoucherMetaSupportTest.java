package com.finex.auth.service.impl.expensevoucher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceVoucherOptionVO;
import com.finex.auth.dto.ExpenseVoucherGenerationMetaVO;
import com.finex.auth.entity.ExpVoucherPushBatch;
import com.finex.auth.entity.ExpVoucherPushDocument;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseVoucherMetaSupportTest {

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

    private ExpenseVoucherMetaSupport support;

    @BeforeEach
    void setUp() {
        support = new ExpenseVoucherMetaSupport(AbstractExpenseVoucherGenerationSupport.dependencies(
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
        )) {
            @Override
            protected java.util.List<FinanceVoucherOptionVO> loadAccountOptions() {
                return java.util.List.of();
            }
        };
    }

    @Test
    void getMetaBuildsCountsAndDefaultCompany() {
        SystemCompany companyA = new SystemCompany();
        companyA.setCompanyId("COMPANY_A");
        companyA.setCompanyCode("001");
        companyA.setCompanyName("Company A");
        companyA.setStatus(1);
        SystemCompany companyB = new SystemCompany();
        companyB.setCompanyId("COMPANY_B");
        companyB.setCompanyCode("002");
        companyB.setCompanyName("Company B");
        companyB.setStatus(1);

        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateCode("TMP001");
        template.setTemplateName("Travel");
        template.setEnabled(1);

        ProcessExpenseType expenseType = new ProcessExpenseType();
        expenseType.setExpenseCode("TRAVEL");
        expenseType.setExpenseName("Travel");
        expenseType.setStatus(1);

        ProcessDocumentInstance pendingDoc = new ProcessDocumentInstance();
        pendingDoc.setDocumentCode("DOC-001");
        pendingDoc.setStatus("APPROVED");
        pendingDoc.setTotalAmount(new BigDecimal("100.00"));
        pendingDoc.setFinishedAt(LocalDateTime.of(2026, 4, 11, 9, 0));

        ProcessDocumentInstance pushedDoc = new ProcessDocumentInstance();
        pushedDoc.setDocumentCode("DOC-002");
        pushedDoc.setStatus("APPROVED");
        pushedDoc.setTotalAmount(new BigDecimal("200.00"));
        pushedDoc.setFinishedAt(LocalDateTime.of(2026, 4, 11, 10, 0));

        ExpVoucherPushDocument success = new ExpVoucherPushDocument();
        success.setDocumentCode("DOC-002");
        success.setPushStatus("SUCCESS");

        ExpVoucherPushDocument failed = new ExpVoucherPushDocument();
        failed.setDocumentCode("DOC-003");
        failed.setPushStatus("FAILED");

        ExpVoucherPushBatch batch = new ExpVoucherPushBatch();
        batch.setBatchNo("VG-LATEST");

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setCompanyId("COMPANY_B");

        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(companyA, companyB));
        when(documentTemplateMapper.selectList(any())).thenReturn(List.of(template));
        when(expenseTypeMapper.selectList(any())).thenReturn(List.of(expenseType));
        when(documentInstanceMapper.selectList(any())).thenReturn(List.of(pushedDoc, pendingDoc));
        when(pushDocumentMapper.selectList(any())).thenReturn(List.of(success, failed));
        when(pushBatchMapper.selectOne(any())).thenReturn(batch);
        when(userMapper.selectById(1L)).thenReturn(currentUser);
        ExpenseVoucherGenerationMetaVO meta = support.getMeta(1L);

        assertEquals("COMPANY_B", meta.getDefaultCompanyId());
        assertEquals("VG-LATEST", meta.getLatestBatchNo());
        assertEquals(1, meta.getPendingPushCount());
        assertEquals("100.00", meta.getPendingPushAmount().toPlainString());
        assertEquals(1, meta.getPushedVoucherCount());
        assertEquals(1, meta.getPushFailureCount());
        assertEquals(2, meta.getCompanyOptions().size());
        assertEquals(1, meta.getTemplateOptions().size());
        assertEquals(1, meta.getExpenseTypeOptions().size());
    }
}
