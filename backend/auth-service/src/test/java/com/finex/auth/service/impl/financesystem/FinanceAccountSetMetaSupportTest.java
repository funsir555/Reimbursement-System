package com.finex.auth.service.impl.financesystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSetMetaVO;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinanceAccountSetCodeRule;
import com.finex.auth.entity.FinanceAccountSetTemplate;
import com.finex.auth.entity.FinanceAccountSetTemplateSubject;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.FinanceAccountSetCodeRuleMapper;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateSubjectMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.impl.FinanceAccountSetTaskWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceAccountSetMetaSupportTest {

    @Mock
    private FinanceAccountSetMapper financeAccountSetMapper;
    @Mock
    private FinanceAccountSetCodeRuleMapper financeAccountSetCodeRuleMapper;
    @Mock
    private FinanceAccountSetTemplateMapper financeAccountSetTemplateMapper;
    @Mock
    private FinanceAccountSetTemplateSubjectMapper financeAccountSetTemplateSubjectMapper;
    @Mock
    private SystemCompanyMapper systemCompanyMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AsyncTaskRecordMapper asyncTaskRecordMapper;
    @Mock
    private FinanceAccountSetTaskWorker financeAccountSetTaskWorker;

    private FinanceAccountSetMetaSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceAccountSetMetaSupport(
                financeAccountSetMapper,
                financeAccountSetCodeRuleMapper,
                financeAccountSetTemplateMapper,
                financeAccountSetTemplateSubjectMapper,
                systemCompanyMapper,
                userMapper,
                asyncTaskRecordMapper,
                financeAccountSetTaskWorker,
                new ObjectMapper()
        );
    }

    @Test
    void getMetaBuildsReferenceOptionsAndDefaults() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setCompanyCode("COMP001");
        company.setCompanyName("骞垮窞娴嬭瘯鍏徃");
        company.setStatus(1);

        User supervisor = new User();
        supervisor.setId(2L);
        supervisor.setUsername("alice");
        supervisor.setName("Alice");
        supervisor.setStatus(1);

        FinanceAccountSetTemplate template = new FinanceAccountSetTemplate();
        template.setTemplateCode("AS_2007_ENTERPRISE");
        template.setTemplateName("2007 浼佷笟浼氳鍒跺害");
        template.setAccountingStandard("PRC GAAP");
        template.setStatus(1);

        FinanceAccountSetTemplateSubject levelOne = new FinanceAccountSetTemplateSubject();
        levelOne.setTemplateCode("AS_2007_ENTERPRISE");
        levelOne.setSubjectLevel(1);
        FinanceAccountSetTemplateSubject detail = new FinanceAccountSetTemplateSubject();
        detail.setTemplateCode("AS_2007_ENTERPRISE");
        detail.setSubjectLevel(2);

        FinanceAccountSet accountSet = new FinanceAccountSet();
        accountSet.setCompanyId("COMPANY_A");
        accountSet.setStatus("ACTIVE");
        accountSet.setTemplateCode("AS_2007_ENTERPRISE");
        accountSet.setEnabledYear(2026);
        accountSet.setEnabledPeriod(4);
        accountSet.setUpdatedAt(LocalDateTime.of(2026, 4, 10, 8, 0));

        FinanceAccountSetCodeRule codeRule = new FinanceAccountSetCodeRule();
        codeRule.setCompanyId("COMPANY_A");
        codeRule.setScheme("4-2-2-2");

        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(company));
        when(userMapper.selectList(any())).thenReturn(List.of(supervisor));
        when(financeAccountSetTemplateMapper.selectList(any())).thenReturn(List.of(template));
        when(financeAccountSetTemplateSubjectMapper.selectList(any())).thenReturn(List.of(levelOne, detail));
        when(financeAccountSetMapper.selectList(any())).thenReturn(List.of(accountSet));
        when(financeAccountSetCodeRuleMapper.selectList(any())).thenReturn(List.of(codeRule));

        FinanceAccountSetMetaVO meta = support.getMeta();

        assertEquals("4-2-2-2", meta.getDefaultSubjectCodeScheme());
        assertEquals(1, meta.getCompanyOptions().size());
        assertEquals("COMPANY_A", meta.getCompanyOptions().get(0).getCompanyId());
        assertEquals(1, meta.getSupervisorOptions().size());
        assertEquals("2", meta.getSupervisorOptions().get(0).getValue());
        assertEquals(1, meta.getTemplateOptions().size());
        assertEquals(1, meta.getTemplateOptions().get(0).getLevel1SubjectCount());
        assertEquals(1, meta.getTemplateOptions().get(0).getCommonSubjectCount());
        assertEquals(1, meta.getReferenceOptions().size());
        assertEquals("2026-04", meta.getReferenceOptions().get(0).getEnabledYearMonth());
        assertEquals("4-2-2-2", meta.getReferenceOptions().get(0).getSubjectCodeScheme());
    }
}
