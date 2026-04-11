package com.finex.auth.service.impl.expensevoucher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseVoucherTemplatePolicySaveDTO;
import com.finex.auth.dto.ExpenseVoucherTemplatePolicyVO;
import com.finex.auth.entity.ExpVoucherTemplatePolicy;
import com.finex.auth.entity.ProcessDocumentTemplate;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseVoucherMappingDomainSupportTest {

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

    private ExpenseVoucherMappingDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new ExpenseVoucherMappingDomainSupport(AbstractExpenseVoucherGenerationSupport.dependencies(
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
    void createTemplatePolicyAppliesDefaultsAndResolvesTemplateName() {
        ExpenseVoucherTemplatePolicySaveDTO dto = new ExpenseVoucherTemplatePolicySaveDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setTemplateCode("TMP001");
        dto.setCreditAccountCode("2202");

        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateCode("TMP001");
        template.setTemplateName("Travel");

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(documentTemplateMapper.selectOne(any())).thenReturn(template);
        when(templatePolicyMapper.selectOne(any())).thenReturn(null);
        when(templatePolicyMapper.insert(any(ExpVoucherTemplatePolicy.class))).thenAnswer(invocation -> {
            ExpVoucherTemplatePolicy entity = invocation.getArgument(0);
            entity.setId(9L);
            return 1;
        });

        ExpenseVoucherTemplatePolicyVO result = support.createTemplatePolicy(dto, 1L, "tester");

        ArgumentCaptor<ExpVoucherTemplatePolicy> captor = ArgumentCaptor.forClass(ExpVoucherTemplatePolicy.class);
        verify(templatePolicyMapper).insert(captor.capture());
        assertEquals("Travel", captor.getValue().getTemplateName());
        assertEquals("GENERAL", captor.getValue().getVoucherType());
        assertEquals("tester", captor.getValue().getCreatedBy());
        assertEquals("tester", captor.getValue().getUpdatedBy());
        assertEquals("Travel", result.getTemplateName());
        assertEquals("GENERAL", result.getVoucherType());
        assertEquals(Boolean.TRUE, result.getEnabled());
    }

    @Test
    void createTemplatePolicyRejectsDuplicateCompanyTemplate() {
        ExpenseVoucherTemplatePolicySaveDTO dto = new ExpenseVoucherTemplatePolicySaveDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setTemplateCode("TMP001");
        dto.setCreditAccountCode("2202");

        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateCode("TMP001");
        ExpVoucherTemplatePolicy duplicate = new ExpVoucherTemplatePolicy();
        duplicate.setId(1L);

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(documentTemplateMapper.selectOne(any())).thenReturn(template);
        when(templatePolicyMapper.selectOne(any())).thenReturn(duplicate);

        assertThrows(IllegalArgumentException.class, () -> support.createTemplatePolicy(dto, 1L, "tester"));
    }
}
