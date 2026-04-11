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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArchiveAgentServiceImpl implements ArchiveAgentService {

    private final ArchiveAgentMetaSupport archiveAgentMetaSupport;
    private final ArchiveAgentDefinitionDomainSupport archiveAgentDefinitionDomainSupport;
    private final ArchiveAgentRunDomainSupport archiveAgentRunDomainSupport;
    private final ArchiveAgentScheduleDomainSupport archiveAgentScheduleDomainSupport;

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

    @Override
    public List<ArchiveAgentSummaryVO> listAgents(Long ownerUserId, String keyword, String status) {
        return archiveAgentDefinitionDomainSupport.listAgents(ownerUserId, keyword, status);
    }

    @Override
    public ArchiveAgentMetaVO getMeta() {
        return archiveAgentMetaSupport.getMeta();
    }

    @Override
    public ArchiveAgentDetailVO getAgentDetail(Long ownerUserId, Long id) {
        return archiveAgentDefinitionDomainSupport.getAgentDetail(ownerUserId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveAgentDetailVO createAgent(Long ownerUserId, String operatorName, ArchiveAgentSaveDTO dto) {
        return archiveAgentDefinitionDomainSupport.createAgent(ownerUserId, operatorName, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveAgentDetailVO updateAgent(Long ownerUserId, Long id, String operatorName, ArchiveAgentSaveDTO dto) {
        return archiveAgentDefinitionDomainSupport.updateAgent(ownerUserId, id, operatorName, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveAgentDetailVO publishAgent(Long ownerUserId, Long id, String operatorName) {
        return archiveAgentDefinitionDomainSupport.publishAgent(ownerUserId, id, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveAgentDetailVO updateAgentStatus(Long ownerUserId, Long id, String status) {
        return archiveAgentDefinitionDomainSupport.updateAgentStatus(ownerUserId, id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveAgentRunVO runAgent(Long ownerUserId, Long id, ArchiveAgentRunDTO dto) {
        return archiveAgentRunDomainSupport.runAgent(ownerUserId, id, dto);
    }

    @Override
    public List<ArchiveAgentRunVO> listRuns(Long ownerUserId, Long agentId) {
        return archiveAgentRunDomainSupport.listRuns(ownerUserId, agentId);
    }

    @Override
    public ArchiveAgentRunDetailVO getRunDetail(Long ownerUserId, Long runId) {
        return archiveAgentRunDomainSupport.getRunDetail(ownerUserId, runId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void runDueSchedules() {
        archiveAgentScheduleDomainSupport.runDueSchedules();
    }
}
