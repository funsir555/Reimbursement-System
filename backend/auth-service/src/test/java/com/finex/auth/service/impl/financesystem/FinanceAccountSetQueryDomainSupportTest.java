package com.finex.auth.service.impl.financesystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSetSummaryVO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinanceAccountSetCodeRule;
import com.finex.auth.entity.FinanceAccountSetTemplate;
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
import com.finex.auth.support.AsyncTaskSupport;
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
class FinanceAccountSetQueryDomainSupportTest {

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

    private FinanceAccountSetQueryDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceAccountSetQueryDomainSupport(
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
    void listAccountSetsIncludesLoadedTaskAndSchemeContext() {
        FinanceAccountSet accountSet = new FinanceAccountSet();
        accountSet.setCompanyId("COMPANY_A");
        accountSet.setStatus("ACTIVE");
        accountSet.setEnabledYear(2026);
        accountSet.setEnabledPeriod(4);
        accountSet.setTemplateCode("AS_2007_ENTERPRISE");
        accountSet.setSupervisorUserId(2L);
        accountSet.setCreateMode("BLANK");
        accountSet.setSubjectCount(12);
        accountSet.setLastTaskNo("TASK_1");
        accountSet.setUpdatedAt(LocalDateTime.of(2026, 4, 10, 8, 30));

        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setCompanyCode("COMP202604050001");
        company.setCompanyName("骞垮窞娴嬭瘯鍏徃");
        company.setStatus(1);

        User supervisor = new User();
        supervisor.setId(2L);
        supervisor.setName("鏉庝細璁?");
        supervisor.setUsername("lee");
        supervisor.setStatus(1);

        FinanceAccountSetTemplate template = new FinanceAccountSetTemplate();
        template.setTemplateCode("AS_2007_ENTERPRISE");
        template.setTemplateName("2007 浼佷笟浼氳鍒跺害");
        template.setStatus(1);

        FinanceAccountSetCodeRule codeRule = new FinanceAccountSetCodeRule();
        codeRule.setCompanyId("COMPANY_A");
        codeRule.setScheme("4-2-2-2");

        AsyncTaskRecord task = new AsyncTaskRecord();
        task.setTaskNo("TASK_1");
        task.setStatus(AsyncTaskSupport.TASK_STATUS_SUCCESS);
        task.setProgress(100);
        task.setResultMessage("璐﹀鍒涘缓瀹屾垚");

        when(financeAccountSetMapper.selectList(any())).thenReturn(List.of(accountSet));
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(company));
        when(userMapper.selectList(any())).thenReturn(List.of(supervisor));
        when(financeAccountSetTemplateMapper.selectList(any())).thenReturn(List.of(template));
        when(financeAccountSetCodeRuleMapper.selectList(any())).thenReturn(List.of(codeRule));
        when(asyncTaskRecordMapper.selectList(any())).thenReturn(List.of(task));

        FinanceAccountSetSummaryVO summary = support.listAccountSets().get(0);

        assertEquals("COMPANY_A", summary.getCompanyId());
        assertEquals("COMP202604050001", summary.getCompanyCode());
        assertEquals("2026-04", summary.getEnabledYearMonth());
        assertEquals("4-2-2-2", summary.getSubjectCodeScheme());
        assertEquals(12, summary.getSubjectCount());
        assertEquals(AsyncTaskSupport.TASK_STATUS_SUCCESS, summary.getLastTaskStatus());
        assertEquals(100, summary.getLastTaskProgress());
    }
}
