package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSetCreateDTO;
import com.finex.auth.dto.FinanceAccountSetMetaVO;
import com.finex.auth.dto.FinanceAccountSetSummaryVO;
import com.finex.auth.dto.FinanceAccountSetTaskStatusVO;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.FinanceAccountSetCodeRuleMapper;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateSubjectMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinanceSystemManagementService;
import com.finex.auth.service.impl.financesystem.FinanceAccountSetMetaSupport;
import com.finex.auth.service.impl.financesystem.FinanceAccountSetQueryDomainSupport;
import com.finex.auth.service.impl.financesystem.FinanceAccountSetTaskDomainSupport;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FinanceSystemManagementServiceImpl implements FinanceSystemManagementService {

    private final FinanceAccountSetMetaSupport financeAccountSetMetaSupport;
    private final FinanceAccountSetQueryDomainSupport financeAccountSetQueryDomainSupport;
    private final FinanceAccountSetTaskDomainSupport financeAccountSetTaskDomainSupport;

    public FinanceSystemManagementServiceImpl(
            FinanceAccountSetMapper financeAccountSetMapper,
            FinanceAccountSetCodeRuleMapper financeAccountSetCodeRuleMapper,
            FinanceAccountSetTemplateMapper financeAccountSetTemplateMapper,
            FinanceAccountSetTemplateSubjectMapper financeAccountSetTemplateSubjectMapper,
            SystemCompanyMapper systemCompanyMapper,
            UserMapper userMapper,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            FinanceAccountSetTaskWorker financeAccountSetTaskWorker,
            ObjectMapper objectMapper
    ) {
        this.financeAccountSetMetaSupport = new FinanceAccountSetMetaSupport(
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
        this.financeAccountSetQueryDomainSupport = new FinanceAccountSetQueryDomainSupport(
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
        this.financeAccountSetTaskDomainSupport = new FinanceAccountSetTaskDomainSupport(
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

    @Override
    public FinanceAccountSetMetaVO getMeta() {
        return financeAccountSetMetaSupport.getMeta();
    }

    @Override
    public List<FinanceAccountSetSummaryVO> listAccountSets() {
        return financeAccountSetQueryDomainSupport.listAccountSets();
    }

    @Override
    public FinanceAccountSetTaskStatusVO submitCreateTask(Long currentUserId, FinanceAccountSetCreateDTO dto) {
        return financeAccountSetTaskDomainSupport.submitCreateTask(currentUserId, dto);
    }

    @Override
    public FinanceAccountSetTaskStatusVO getTaskStatus(String taskNo) {
        return financeAccountSetTaskDomainSupport.getTaskStatus(taskNo);
    }
}
