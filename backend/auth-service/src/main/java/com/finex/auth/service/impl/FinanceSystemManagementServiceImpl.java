// 业务域：财务系统管理
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 财务系统设置和账套相关接口，下游会继续协调 账套、同步任务和财务上下文基础数据。
// 风险提醒：改坏后最容易影响 账套切换、基础数据同步和下游系统连接。

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

/**
 * FinanceSystemManagementServiceImpl：service 入口实现。
 * 接住上层请求，并把 财务系统管理相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 账套切换、基础数据同步和下游系统连接是否会被一起带坏。
 */
@Service
public class FinanceSystemManagementServiceImpl implements FinanceSystemManagementService {

    private final FinanceAccountSetMetaSupport financeAccountSetMetaSupport;
    private final FinanceAccountSetQueryDomainSupport financeAccountSetQueryDomainSupport;
    private final FinanceAccountSetTaskDomainSupport financeAccountSetTaskDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
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

    /**
     * 获取元数据。
     */
    @Override
    public FinanceAccountSetMetaVO getMeta() {
        return financeAccountSetMetaSupport.getMeta();
    }

    /**
     * 查询账户Sets列表。
     */
    @Override
    public List<FinanceAccountSetSummaryVO> listAccountSets() {
        return financeAccountSetQueryDomainSupport.listAccountSets();
    }

    /**
     * 提交创建任务。
     */
    @Override
    public FinanceAccountSetTaskStatusVO submitCreateTask(Long currentUserId, FinanceAccountSetCreateDTO dto) {
        return financeAccountSetTaskDomainSupport.submitCreateTask(currentUserId, dto);
    }

    /**
     * 获取任务Status。
     */
    @Override
    public FinanceAccountSetTaskStatusVO getTaskStatus(String taskNo) {
        return financeAccountSetTaskDomainSupport.getTaskStatus(taskNo);
    }
}
