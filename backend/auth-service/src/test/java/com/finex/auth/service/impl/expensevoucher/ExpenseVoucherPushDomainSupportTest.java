package com.finex.auth.service.impl.expensevoucher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseVoucherPageVO;
import com.finex.auth.dto.ExpenseVoucherPushBatchResultVO;
import com.finex.auth.dto.ExpenseVoucherPushDTO;
import com.finex.auth.dto.ExpenseVoucherPushDocumentVO;
import com.finex.auth.entity.ExpVoucherSubjectMapping;
import com.finex.auth.entity.ExpVoucherTemplatePolicy;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.entity.SystemCompany;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseVoucherPushDomainSupportTest {

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

    private ExpenseVoucherPushDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new ExpenseVoucherPushDomainSupport(AbstractExpenseVoucherGenerationSupport.dependencies(
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
    void pushDocumentsRejectsEmptyDocumentCodes() {
        ExpenseVoucherPushDTO dto = new ExpenseVoucherPushDTO();
        dto.setDocumentCodes(List.of());

        assertThrows(IllegalArgumentException.class, () -> support.pushDocuments(dto, 1L, "tester"));
    }

    @Test
    void getPushDocumentsBuildsCompatibleReadModel() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setCompanyName("Company A");
        company.setCompanyCode("001");
        company.setStatus(1);

        ProcessExpenseType expenseType = new ProcessExpenseType();
        expenseType.setExpenseCode("TRAVEL");
        expenseType.setExpenseName("Travel");
        expenseType.setStatus(1);

        ProcessDocumentInstance document = new ProcessDocumentInstance();
        document.setDocumentCode("DOC-001");
        document.setTemplateCode("TMP001");
        document.setTemplateName("Travel");
        document.setSubmitterUserId(1L);
        document.setSubmitterName("alice");
        document.setDocumentTitle("Travel request");
        document.setStatus("APPROVED");
        document.setTotalAmount(new BigDecimal("88.00"));
        document.setFinishedAt(LocalDateTime.of(2026, 4, 11, 9, 0));
        document.setFormDataJson("{\"payment-company-1\":\"COMPANY_A\"}");
        document.setFormSchemaSnapshotJson("{}");

        ProcessDocumentExpenseDetail detail = new ProcessDocumentExpenseDetail();
        detail.setDocumentCode("DOC-001");
        detail.setExpenseTypeCode("TRAVEL");
        detail.setActualPaymentAmount(new BigDecimal("88.00"));

        ExpVoucherTemplatePolicy policy = new ExpVoucherTemplatePolicy();
        policy.setCompanyId("COMPANY_A");
        policy.setTemplateCode("TMP001");
        policy.setEnabled(1);

        ExpVoucherSubjectMapping mapping = new ExpVoucherSubjectMapping();
        mapping.setCompanyId("COMPANY_A");
        mapping.setTemplateCode("TMP001");
        mapping.setExpenseTypeCode("TRAVEL");
        mapping.setDebitAccountCode("660100");
        mapping.setEnabled(1);

        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(company));
        when(expenseTypeMapper.selectList(any())).thenReturn(List.of(expenseType));
        when(documentInstanceMapper.selectList(any())).thenReturn(List.of(document));
        when(expenseDetailMapper.selectList(any())).thenReturn(List.of(detail));
        when(pushDocumentMapper.selectList(any())).thenReturn(List.of());
        when(templatePolicyMapper.selectOne(any())).thenReturn(policy);
        when(subjectMappingMapper.selectList(any())).thenReturn(List.of(mapping));

        ExpenseVoucherPageVO<ExpenseVoucherPushDocumentVO> result = support.getPushDocuments(
                "COMPANY_A", "TMP001", "DOC", "UNPUSHED", "2026-04-01", "2026-04-30", 1, 10
        );

        assertEquals(1, result.getTotal());
        assertEquals("DOC-001", result.getItems().get(0).getDocumentCode());
        assertEquals("COMPANY_A", result.getItems().get(0).getCompanyId());
        assertEquals(Boolean.TRUE, result.getItems().get(0).getCanPush());
        assertEquals("UNPUSHED", result.getItems().get(0).getPushStatus());
    }

    @Test
    void pushDocumentsMarksMissingDocumentsAsFailures() {
        ExpenseVoucherPushDTO dto = new ExpenseVoucherPushDTO();
        dto.setDocumentCodes(List.of("DOC-404"));
        when(documentInstanceMapper.selectList(any())).thenReturn(List.of());

        ExpenseVoucherPushBatchResultVO result = support.pushDocuments(dto, 1L, "tester");

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getResults().size());
        assertEquals("FAILED", result.getResults().get(0).getPushStatus());
    }
}
