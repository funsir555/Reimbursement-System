package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSetCreateDTO;
import com.finex.auth.dto.FinanceAccountSetSummaryVO;
import com.finex.auth.dto.FinanceAccountSetTaskStatusVO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.FinanceAccountSet;
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
import com.finex.auth.support.AsyncTaskSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceSystemManagementServiceImplTest {

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

    private FinanceSystemManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinanceSystemManagementServiceImpl(
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
    void submitCreateTaskBuildsAsyncTaskForBlankAccountSet() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setCompanyName("广州测试公司");
        company.setStatus(1);

        User supervisor = new User();
        supervisor.setId(2L);
        supervisor.setName("李会计");
        supervisor.setStatus(1);

        FinanceAccountSetTemplate template = new FinanceAccountSetTemplate();
        template.setTemplateCode("AS_2007_ENTERPRISE");
        template.setStatus(1);

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(userMapper.selectById(2L)).thenReturn(supervisor);
        when(financeAccountSetMapper.selectById("COMPANY_A")).thenReturn(null);
        when(asyncTaskRecordMapper.selectOne(any())).thenReturn(null);
        when(financeAccountSetTemplateMapper.selectById("AS_2007_ENTERPRISE")).thenReturn(template);
        doAnswer(invocation -> {
            AsyncTaskRecord task = invocation.getArgument(0);
            task.setId(9L);
            return 1;
        }).when(asyncTaskRecordMapper).insert(any(AsyncTaskRecord.class));

        FinanceAccountSetCreateDTO dto = new FinanceAccountSetCreateDTO();
        dto.setCreateMode("BLANK");
        dto.setTargetCompanyId("COMPANY_A");
        dto.setEnabledYearMonth("2026-04");
        dto.setTemplateCode("AS_2007_ENTERPRISE");
        dto.setSupervisorUserId(2L);
        dto.setSubjectCodeScheme("4-2-2-2");

        FinanceAccountSetTaskStatusVO result = service.submitCreateTask(1L, dto);

        assertNotNull(result.getTaskNo());
        assertEquals(AsyncTaskSupport.TASK_STATUS_PENDING, result.getStatus());
        assertEquals("COMPANY_A", result.getCompanyId());

        ArgumentCaptor<AsyncTaskRecord> captor = ArgumentCaptor.forClass(AsyncTaskRecord.class);
        verify(asyncTaskRecordMapper).insert(captor.capture());
        AsyncTaskRecord inserted = captor.getValue();
        assertEquals("COMPANY_A", inserted.getCompanyId());
        assertEquals(AsyncTaskSupport.TASK_TYPE_FINANCE_ACCOUNT_SET_CREATE, inserted.getTaskType());
        assertEquals(AsyncTaskSupport.BUSINESS_TYPE_FINANCE_ACCOUNT_SET, inserted.getBusinessType());
        verify(financeAccountSetTaskWorker).runCreateAccountSetTask(9L);
    }

    @Test
    void listAccountSetsIncludesCompanyCodeInSummary() {
        FinanceAccountSet accountSet = new FinanceAccountSet();
        accountSet.setCompanyId("COMPANY_A");
        accountSet.setStatus("ACTIVE");
        accountSet.setEnabledYear(2026);
        accountSet.setEnabledPeriod(4);
        accountSet.setTemplateCode("AS_2007_ENTERPRISE");
        accountSet.setSupervisorUserId(2L);
        accountSet.setCreateMode("BLANK");
        accountSet.setLastTaskNo("TASK_1");

        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setCompanyCode("COMP202604050001");
        company.setCompanyName("广州测试公司");
        company.setStatus(1);

        User supervisor = new User();
        supervisor.setId(2L);
        supervisor.setName("李会计");
        supervisor.setStatus(1);

        FinanceAccountSetTemplate template = new FinanceAccountSetTemplate();
        template.setTemplateCode("AS_2007_ENTERPRISE");
        template.setTemplateName("2007 企业会计制度");
        template.setStatus(1);

        AsyncTaskRecord task = new AsyncTaskRecord();
        task.setTaskNo("TASK_1");
        task.setStatus(AsyncTaskSupport.TASK_STATUS_SUCCESS);
        task.setProgress(100);
        task.setResultMessage("账套创建完成");

        when(financeAccountSetMapper.selectList(any())).thenReturn(java.util.List.of(accountSet));
        when(systemCompanyMapper.selectList(any())).thenReturn(java.util.List.of(company));
        when(userMapper.selectList(any())).thenReturn(java.util.List.of(supervisor));
        when(financeAccountSetTemplateMapper.selectList(any())).thenReturn(java.util.List.of(template));
        lenient().when(financeAccountSetCodeRuleMapper.selectList(any())).thenReturn(java.util.List.of());
        lenient().when(asyncTaskRecordMapper.selectList(any())).thenReturn(java.util.List.of(task));

        FinanceAccountSetSummaryVO summary = service.listAccountSets().get(0);

        assertEquals("COMPANY_A", summary.getCompanyId());
        assertEquals("COMP202604050001", summary.getCompanyCode());
        assertEquals("广州测试公司", summary.getCompanyName());
    }
}
