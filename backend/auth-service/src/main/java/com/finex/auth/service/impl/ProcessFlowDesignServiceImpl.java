package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowDetailVO;
import com.finex.auth.dto.ProcessFlowMetaVO;
import com.finex.auth.dto.ProcessFlowResolveApproversDTO;
import com.finex.auth.dto.ProcessFlowResolveApproversVO;
import com.finex.auth.dto.ProcessFlowSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneVO;
import com.finex.auth.dto.ProcessFlowSummaryVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessFlowMapper;
import com.finex.auth.mapper.ProcessFlowNodeMapper;
import com.finex.auth.mapper.ProcessFlowRouteMapper;
import com.finex.auth.mapper.ProcessFlowSceneMapper;
import com.finex.auth.mapper.ProcessFlowVersionMapper;
import com.finex.auth.mapper.ProcessUserGroupMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.ProcessFlowDesignService;
import com.finex.auth.service.impl.process.ProcessFlowApproverResolveSupport;
import com.finex.auth.service.impl.process.ProcessFlowMetaSupport;
import com.finex.auth.service.impl.process.ProcessFlowMutationDomainSupport;
import com.finex.auth.service.impl.process.ProcessFlowQuerySupport;
import com.finex.auth.service.impl.process.ProcessFlowStructureSupport;
import com.finex.auth.service.impl.process.ProcessUserGroupResolverSupport;
import com.finex.auth.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ProcessFlowDesignServiceImpl implements ProcessFlowDesignService {

    private final ProcessFlowMetaSupport metaSupport;
    private final ProcessFlowMutationDomainSupport mutationSupport;
    private final ProcessFlowQuerySupport querySupport;
    private final ProcessFlowApproverResolveSupport approverResolveSupport;
    private final ProcessFlowStructureSupport structureSupport;

    public ProcessFlowDesignServiceImpl(
            ProcessFlowMapper processFlowMapper,
            ProcessFlowVersionMapper processFlowVersionMapper,
            ProcessFlowNodeMapper processFlowNodeMapper,
            ProcessFlowRouteMapper processFlowRouteMapper,
            ProcessFlowSceneMapper processFlowSceneMapper,
            SystemCompanyMapper systemCompanyMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper,
            ProcessExpenseTypeMapper processExpenseTypeMapper,
            ProcessCustomArchiveDesignMapper processCustomArchiveDesignMapper,
            ProcessDocumentTemplateMapper processDocumentTemplateMapper,
            ProcessUserGroupMapper processUserGroupMapper,
            ObjectMapper objectMapper
    ) {
        ProcessUserGroupResolverSupport userGroupResolverSupport = new ProcessUserGroupResolverSupport(
                processUserGroupMapper,
                processFlowNodeMapper,
                systemDepartmentMapper,
                userMapper,
                objectMapper
        );
        this.structureSupport = new ProcessFlowStructureSupport(
                processFlowMapper,
                processFlowVersionMapper,
                processFlowNodeMapper,
                processFlowRouteMapper,
                processFlowSceneMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper,
                processExpenseTypeMapper,
                processCustomArchiveDesignMapper,
                processDocumentTemplateMapper,
                objectMapper,
                userGroupResolverSupport
        );
        this.querySupport = new ProcessFlowQuerySupport(
                processFlowMapper,
                processFlowVersionMapper,
                processFlowNodeMapper,
                processFlowRouteMapper,
                processFlowSceneMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper,
                processExpenseTypeMapper,
                processCustomArchiveDesignMapper,
                processDocumentTemplateMapper,
                objectMapper,
                structureSupport
        );
        this.metaSupport = new ProcessFlowMetaSupport(
                processFlowMapper,
                processFlowVersionMapper,
                processFlowNodeMapper,
                processFlowRouteMapper,
                processFlowSceneMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper,
                processExpenseTypeMapper,
                processCustomArchiveDesignMapper,
                processDocumentTemplateMapper,
                objectMapper,
                userGroupResolverSupport
        );
        this.approverResolveSupport = new ProcessFlowApproverResolveSupport(
                processFlowMapper,
                processFlowVersionMapper,
                processFlowNodeMapper,
                processFlowRouteMapper,
                processFlowSceneMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper,
                processExpenseTypeMapper,
                processCustomArchiveDesignMapper,
                processDocumentTemplateMapper,
                objectMapper,
                structureSupport,
                userGroupResolverSupport
        );
        this.mutationSupport = new ProcessFlowMutationDomainSupport(
                processFlowMapper,
                processFlowVersionMapper,
                processFlowNodeMapper,
                processFlowRouteMapper,
                processFlowSceneMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper,
                processExpenseTypeMapper,
                processCustomArchiveDesignMapper,
                processDocumentTemplateMapper,
                objectMapper,
                structureSupport,
                querySupport
        );
    }

    @Override
    public List<ProcessFlowSummaryVO> listFlows() {
        return querySupport.listFlows();
    }

    @Override
    public ProcessFlowMetaVO getFlowMeta() {
        return metaSupport.getFlowMeta();
    }

    @Override
    public ProcessFlowDetailVO getFlowDetail(Long id) {
        return querySupport.getFlowDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowDetailVO createFlow(ProcessFlowSaveDTO dto) {
        return mutationSupport.createFlow(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowDetailVO updateFlow(Long id, ProcessFlowSaveDTO dto) {
        return mutationSupport.updateFlow(id, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowDetailVO publishFlow(Long id) {
        return mutationSupport.publishFlow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFlow(Long id) {
        return mutationSupport.deleteFlow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateFlowStatus(Long id, String status) {
        return mutationSupport.updateFlowStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFlowSceneVO createFlowScene(ProcessFlowSceneSaveDTO dto) {
        return mutationSupport.createFlowScene(dto);
    }

    @Override
    public ProcessFlowResolveApproversVO resolveApprovers(ProcessFlowResolveApproversDTO dto) {
        return approverResolveSupport.resolveApprovers(dto);
    }

    @Override
    public List<ProcessFormOptionVO> listPublishedFlowOptions() {
        return metaSupport.listPublishedFlowOptions();
    }

    @Override
    public Map<String, String> publishedFlowLabelMap() {
        return metaSupport.publishedFlowLabelMap();
    }

    Map<String, Object> normalizeNodeConfig(String nodeType, Map<String, Object> rawConfig, boolean strictValidation) {
        return structureSupport.normalizeNodeConfig(nodeType, rawConfig, strictValidation);
    }

    List<User> resolveManagerMembers(Map<String, Object> config, Map<String, Object> context, List<String> trace) {
        return approverResolveSupport.resolveManagerMembers(config, context, trace);
    }
}
