package com.finex.auth.service.impl.financesystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSetCreateDTO;
import com.finex.auth.dto.FinanceAccountSetTaskPayload;
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
import com.finex.auth.service.impl.FinanceAccountSetTaskWorker;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceAccountSetTaskDomainSupportTest {

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private FinanceAccountSetTaskDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceAccountSetTaskDomainSupport(
                financeAccountSetMapper,
                financeAccountSetCodeRuleMapper,
                financeAccountSetTemplateMapper,
                financeAccountSetTemplateSubjectMapper,
                systemCompanyMapper,
                userMapper,
                asyncTaskRecordMapper,
                financeAccountSetTaskWorker,
                objectMapper
        );
    }

    @Test
    void submitCreateTaskBuildsAsyncTaskForBlankAccountSet() throws Exception {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setCompanyName("骞垮窞娴嬭瘯鍏徃");
        company.setStatus(1);

        User supervisor = new User();
        supervisor.setId(2L);
        supervisor.setName("鏉庝細璁?");
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

        FinanceAccountSetTaskStatusVO result = support.submitCreateTask(1L, dto);

        assertNotNull(result.getTaskNo());
        assertEquals(AsyncTaskSupport.TASK_STATUS_PENDING, result.getStatus());
        assertEquals("COMPANY_A", result.getCompanyId());

        ArgumentCaptor<AsyncTaskRecord> captor = ArgumentCaptor.forClass(AsyncTaskRecord.class);
        verify(asyncTaskRecordMapper).insert(captor.capture());
        AsyncTaskRecord inserted = captor.getValue();
        assertEquals("COMPANY_A", inserted.getCompanyId());
        assertEquals(AsyncTaskSupport.TASK_TYPE_FINANCE_ACCOUNT_SET_CREATE, inserted.getTaskType());
        assertEquals(AsyncTaskSupport.BUSINESS_TYPE_FINANCE_ACCOUNT_SET, inserted.getBusinessType());

        FinanceAccountSetTaskPayload payload = objectMapper.readValue(inserted.getResultPayload(), FinanceAccountSetTaskPayload.class);
        assertEquals("COMPANY_A", payload.getTargetCompanyId());
        assertEquals("2026-04", payload.getEnabledYearMonth());
        assertEquals("BLANK", payload.getCreateMode());

        verify(financeAccountSetTaskWorker).runCreateAccountSetTask(9L);
    }

    @Test
    void getTaskStatusIncludesAccountSetStatus() {
        AsyncTaskRecord task = new AsyncTaskRecord();
        task.setTaskNo("TASK_1");
        task.setCompanyId("COMPANY_A");
        task.setTaskType(AsyncTaskSupport.TASK_TYPE_FINANCE_ACCOUNT_SET_CREATE);
        task.setStatus(AsyncTaskSupport.TASK_STATUS_SUCCESS);
        task.setProgress(100);

        FinanceAccountSet accountSet = new FinanceAccountSet();
        accountSet.setCompanyId("COMPANY_A");
        accountSet.setStatus("ACTIVE");

        when(asyncTaskRecordMapper.selectOne(any())).thenReturn(task);
        when(financeAccountSetMapper.selectById("COMPANY_A")).thenReturn(accountSet);

        FinanceAccountSetTaskStatusVO result = support.getTaskStatus("TASK_1");

        assertEquals("TASK_1", result.getTaskNo());
        assertEquals("ACTIVE", result.getAccountSetStatus());
        assertEquals(true, result.getFinished());
    }
}
