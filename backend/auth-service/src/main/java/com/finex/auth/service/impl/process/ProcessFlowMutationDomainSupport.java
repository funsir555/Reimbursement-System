package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowDetailVO;
import com.finex.auth.dto.ProcessFlowSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneVO;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessFlow;
import com.finex.auth.entity.ProcessFlowNode;
import com.finex.auth.entity.ProcessFlowRoute;
import com.finex.auth.entity.ProcessFlowScene;
import com.finex.auth.entity.ProcessFlowVersion;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessFlowMapper;
import com.finex.auth.mapper.ProcessFlowNodeMapper;
import com.finex.auth.mapper.ProcessFlowRouteMapper;
import com.finex.auth.mapper.ProcessFlowSceneMapper;
import com.finex.auth.mapper.ProcessFlowVersionMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProcessFlowMutationDomainSupport extends AbstractProcessFlowDesignSupport {

    private final ProcessFlowStructureSupport structureSupport;
    private final ProcessFlowQuerySupport querySupport;

    public ProcessFlowMutationDomainSupport(
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
            ObjectMapper objectMapper,
            ProcessFlowStructureSupport structureSupport,
            ProcessFlowQuerySupport querySupport
    ) {
        super(
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
                objectMapper
        );
        this.structureSupport = structureSupport;
        this.querySupport = querySupport;
    }

    public ProcessFlowDetailVO createFlow(ProcessFlowSaveDTO dto) {
        structureSupport.validateFlowSave(dto);

        ProcessFlow flow = new ProcessFlow();
        flow.setFlowCode(buildFlowCode());
        flow.setFlowName(dto.getFlowName().trim());
        flow.setFlowDescription(trimToNull(dto.getFlowDescription()));
        flow.setStatus(FLOW_STATUS_DRAFT);
        processFlowMapper.insert(flow);

        ProcessFlowVersion version = structureSupport.createDraftVersion(flow.getId(), 1, dto);
        flow.setCurrentDraftVersionId(version.getId());
        processFlowMapper.updateById(flow);
        return querySupport.buildFlowDetail(requireFlow(flow.getId()));
    }

    public ProcessFlowDetailVO updateFlow(Long id, ProcessFlowSaveDTO dto) {
        structureSupport.validateFlowSave(dto);

        ProcessFlow flow = requireFlow(id);
        flow.setFlowName(dto.getFlowName().trim());
        flow.setFlowDescription(trimToNull(dto.getFlowDescription()));

        ProcessFlowVersion draftVersion = currentDraftVersion(flow);
        if (draftVersion == null) {
            draftVersion = structureSupport.createDraftVersion(flow.getId(), nextVersionNo(flow.getId()), dto);
            flow.setCurrentDraftVersionId(draftVersion.getId());
        } else {
            draftVersion.setSnapshotJson(writeSnapshot(dto));
            processFlowVersionMapper.updateById(draftVersion);
            structureSupport.replaceVersionNodesAndRoutes(draftVersion.getId(), dto);
        }

        if (flow.getCurrentPublishedVersionId() == null) {
            flow.setStatus(FLOW_STATUS_DRAFT);
        }
        processFlowMapper.updateById(flow);
        return querySupport.buildFlowDetail(requireFlow(id));
    }

    public ProcessFlowDetailVO publishFlow(Long id) {
        ProcessFlow flow = requireFlow(id);
        ProcessFlowVersion draftVersion = currentDraftVersion(flow);
        if (draftVersion == null) {
            if (currentPublishedVersion(flow) == null) {
                throw new IllegalStateException("当前流程没有可发布的草稿版本");
            }
            flow.setStatus(FLOW_STATUS_ENABLED);
            processFlowMapper.updateById(flow);
            return querySupport.buildFlowDetail(requireFlow(id));
        }

        ProcessFlowVersion publishedVersion = currentPublishedVersion(flow);
        if (publishedVersion != null) {
            publishedVersion.setVersionStatus(VERSION_STATUS_HISTORY);
            processFlowVersionMapper.updateById(publishedVersion);
        }

        draftVersion.setVersionStatus(VERSION_STATUS_PUBLISHED);
        draftVersion.setPublishedAt(LocalDateTime.now());
        processFlowVersionMapper.updateById(draftVersion);

        flow.setStatus(FLOW_STATUS_ENABLED);
        flow.setCurrentPublishedVersionId(draftVersion.getId());
        flow.setCurrentDraftVersionId(null);
        processFlowMapper.updateById(flow);
        return querySupport.buildFlowDetail(requireFlow(id));
    }

    public Boolean deleteFlow(Long id) {
        ProcessFlow flow = requireFlow(id);
        Long boundTemplateCount = processDocumentTemplateMapper.selectCount(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getApprovalFlow, flow.getFlowCode())
        );
        if (boundTemplateCount != null && boundTemplateCount > 0) {
            throw new IllegalStateException("当前审批流程仍被单据模板绑定，解绑后才可删除");
        }

        List<ProcessFlowVersion> versions = processFlowVersionMapper.selectList(
                Wrappers.<ProcessFlowVersion>lambdaQuery()
                        .eq(ProcessFlowVersion::getFlowId, id)
        );
        if (!versions.isEmpty()) {
            List<Long> versionIds = versions.stream()
                    .map(ProcessFlowVersion::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!versionIds.isEmpty()) {
                processFlowRouteMapper.delete(
                        Wrappers.<ProcessFlowRoute>lambdaQuery().in(ProcessFlowRoute::getVersionId, versionIds)
                );
                processFlowNodeMapper.delete(
                        Wrappers.<ProcessFlowNode>lambdaQuery().in(ProcessFlowNode::getVersionId, versionIds)
                );
                processFlowVersionMapper.delete(
                        Wrappers.<ProcessFlowVersion>lambdaQuery().in(ProcessFlowVersion::getId, versionIds)
                );
            }
        }

        processFlowMapper.deleteById(id);
        return Boolean.TRUE;
    }

    public Boolean updateFlowStatus(Long id, String status) {
        ProcessFlow flow = requireFlow(id);
        String targetStatus = normalizeFlowStatus(status);
        if (FLOW_STATUS_ENABLED.equals(targetStatus) && currentPublishedVersion(flow) == null) {
            throw new IllegalStateException("当前流程尚未发布，不能直接启用");
        }
        flow.setStatus(targetStatus);
        processFlowMapper.updateById(flow);
        return Boolean.TRUE;
    }

    public ProcessFlowSceneVO createFlowScene(ProcessFlowSceneSaveDTO dto) {
        String sceneName = trimToNull(dto.getSceneName());
        if (sceneName == null) {
            throw new IllegalStateException("场景名称不能为空");
        }
        validatePmNameLength(sceneName, "场景名称");

        ProcessFlowScene scene = new ProcessFlowScene();
        scene.setSceneCode(buildSceneCode());
        scene.setSceneName(sceneName);
        scene.setSceneDescription(trimToNull(dto.getSceneDescription()));
        scene.setStatus(dto.getStatus() == null ? 1 : (dto.getStatus() == 0 ? 0 : 1));
        processFlowSceneMapper.insert(scene);
        return toSceneVO(scene);
    }
}
