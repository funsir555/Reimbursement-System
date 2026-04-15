// 业务域：档案代理与归档任务
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 档案代理配置接口和后台调度，下游会继续协调 归档规则、执行记录和调度计划。
// 风险提醒：改坏后最容易影响 档案归集效果、执行漏掉和后续追溯。

package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ArchiveAgentDetailVO;
import com.finex.auth.dto.ArchiveAgentMetaVO;
import com.finex.auth.dto.ArchiveAgentRunDTO;
import com.finex.auth.dto.ArchiveAgentRunDetailVO;
import com.finex.auth.dto.ArchiveAgentRunVO;
import com.finex.auth.dto.ArchiveAgentSaveDTO;
import com.finex.auth.dto.ArchiveAgentSummaryVO;
import com.finex.auth.mapper.ArchiveAgentDefinitionMapper;
import com.finex.auth.mapper.ArchiveAgentRunArtifactMapper;
import com.finex.auth.mapper.ArchiveAgentRunMapper;
import com.finex.auth.mapper.ArchiveAgentRunStepMapper;
import com.finex.auth.mapper.ArchiveAgentScheduleMapper;
import com.finex.auth.mapper.ArchiveAgentToolBindingMapper;
import com.finex.auth.mapper.ArchiveAgentTriggerMapper;
import com.finex.auth.mapper.ArchiveAgentVersionMapper;
import com.finex.auth.service.ArchiveAgentService;
import com.finex.auth.service.impl.archiveagent.AbstractArchiveAgentSupport;
import com.finex.auth.service.impl.archiveagent.ArchiveAgentDefinitionDomainSupport;
import com.finex.auth.service.impl.archiveagent.ArchiveAgentMetaSupport;
import com.finex.auth.service.impl.archiveagent.ArchiveAgentRunDomainSupport;
import com.finex.auth.service.impl.archiveagent.ArchiveAgentScheduleDomainSupport;
import com.finex.auth.support.archiveagent.TriggerDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ArchiveAgentServiceImpl：service 入口实现。
 * 接住上层请求，并把 档案代理相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 档案归集效果、执行漏掉和后续追溯是否会被一起带坏。
 */
@Service
public class ArchiveAgentServiceImpl implements ArchiveAgentService {

    private final ArchiveAgentMetaSupport archiveAgentMetaSupport;
    private final ArchiveAgentDefinitionDomainSupport archiveAgentDefinitionDomainSupport;
    private final ArchiveAgentRunDomainSupport archiveAgentRunDomainSupport;
    private final ArchiveAgentScheduleDomainSupport archiveAgentScheduleDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
    @Autowired
    public ArchiveAgentServiceImpl(
            ArchiveAgentDefinitionMapper archiveAgentDefinitionMapper,
            ArchiveAgentVersionMapper archiveAgentVersionMapper,
            ArchiveAgentTriggerMapper archiveAgentTriggerMapper,
            ArchiveAgentToolBindingMapper archiveAgentToolBindingMapper,
            ArchiveAgentRunMapper archiveAgentRunMapper,
            ArchiveAgentRunStepMapper archiveAgentRunStepMapper,
            ArchiveAgentRunArtifactMapper archiveAgentRunArtifactMapper,
            ArchiveAgentScheduleMapper archiveAgentScheduleMapper,
            ObjectMapper objectMapper,
            TriggerDispatcher triggerDispatcher
    ) {
        AbstractArchiveAgentSupport.Dependencies dependencies = AbstractArchiveAgentSupport.dependencies(
                archiveAgentDefinitionMapper,
                archiveAgentVersionMapper,
                archiveAgentTriggerMapper,
                archiveAgentToolBindingMapper,
                archiveAgentRunMapper,
                archiveAgentRunStepMapper,
                archiveAgentRunArtifactMapper,
                archiveAgentScheduleMapper,
                objectMapper,
                triggerDispatcher
        );
        this.archiveAgentMetaSupport = new ArchiveAgentMetaSupport(dependencies);
        this.archiveAgentDefinitionDomainSupport = new ArchiveAgentDefinitionDomainSupport(dependencies);
        this.archiveAgentRunDomainSupport = new ArchiveAgentRunDomainSupport(dependencies);
        this.archiveAgentScheduleDomainSupport = new ArchiveAgentScheduleDomainSupport(dependencies);
    }

    /**
     * 初始化这个类所需的依赖组件。
     */
    ArchiveAgentServiceImpl(
            ArchiveAgentMetaSupport archiveAgentMetaSupport,
            ArchiveAgentDefinitionDomainSupport archiveAgentDefinitionDomainSupport,
            ArchiveAgentRunDomainSupport archiveAgentRunDomainSupport,
            ArchiveAgentScheduleDomainSupport archiveAgentScheduleDomainSupport
    ) {
        this.archiveAgentMetaSupport = archiveAgentMetaSupport;
        this.archiveAgentDefinitionDomainSupport = archiveAgentDefinitionDomainSupport;
        this.archiveAgentRunDomainSupport = archiveAgentRunDomainSupport;
        this.archiveAgentScheduleDomainSupport = archiveAgentScheduleDomainSupport;
    }

    /**
     * 查询代理列表。
     */
    @Override
    public List<ArchiveAgentSummaryVO> listAgents(Long ownerUserId, String keyword, String status) {
        return archiveAgentDefinitionDomainSupport.listAgents(ownerUserId, keyword, status);
    }

    /**
     * 获取元数据。
     */
    @Override
    public ArchiveAgentMetaVO getMeta() {
        return archiveAgentMetaSupport.getMeta();
    }

    /**
     * 获取代理明细。
     */
    @Override
    public ArchiveAgentDetailVO getAgentDetail(Long ownerUserId, Long id) {
        return archiveAgentDefinitionDomainSupport.getAgentDetail(ownerUserId, id);
    }

    /**
     * 创建代理。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveAgentDetailVO createAgent(Long ownerUserId, String operatorName, ArchiveAgentSaveDTO dto) {
        return archiveAgentDefinitionDomainSupport.createAgent(ownerUserId, operatorName, dto);
    }

    /**
     * 更新代理。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveAgentDetailVO updateAgent(Long ownerUserId, Long id, String operatorName, ArchiveAgentSaveDTO dto) {
        return archiveAgentDefinitionDomainSupport.updateAgent(ownerUserId, id, operatorName, dto);
    }

    /**
     * 发布代理。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveAgentDetailVO publishAgent(Long ownerUserId, Long id, String operatorName) {
        return archiveAgentDefinitionDomainSupport.publishAgent(ownerUserId, id, operatorName);
    }

    /**
     * 更新代理Status。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveAgentDetailVO updateAgentStatus(Long ownerUserId, Long id, String status) {
        return archiveAgentDefinitionDomainSupport.updateAgentStatus(ownerUserId, id, status);
    }

    /**
     * 执行代理。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveAgentRunVO runAgent(Long ownerUserId, Long id, ArchiveAgentRunDTO dto) {
        return archiveAgentRunDomainSupport.runAgent(ownerUserId, id, dto);
    }

    /**
     * 查询执行列表。
     */
    @Override
    public List<ArchiveAgentRunVO> listRuns(Long ownerUserId, Long agentId) {
        return archiveAgentRunDomainSupport.listRuns(ownerUserId, agentId);
    }

    /**
     * 获取执行明细。
     */
    @Override
    public ArchiveAgentRunDetailVO getRunDetail(Long ownerUserId, Long runId) {
        return archiveAgentRunDomainSupport.getRunDetail(ownerUserId, runId);
    }

    /**
     * 执行Due调度。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void runDueSchedules() {
        archiveAgentScheduleDomainSupport.runDueSchedules();
    }
}
