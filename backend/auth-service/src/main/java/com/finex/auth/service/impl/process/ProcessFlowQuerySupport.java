package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowDetailVO;
import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;
import com.finex.auth.dto.ProcessFlowSummaryVO;
import com.finex.auth.entity.ProcessFlow;
import com.finex.auth.entity.ProcessFlowNode;
import com.finex.auth.entity.ProcessFlowRoute;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProcessFlowQuerySupport extends AbstractProcessFlowDesignSupport {

    private final ProcessFlowStructureSupport structureSupport;

    public ProcessFlowQuerySupport(
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
            ProcessFlowStructureSupport structureSupport
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
    }

    public List<ProcessFlowSummaryVO> listFlows() {
        List<ProcessFlow> flows = processFlowMapper.selectList(
                Wrappers.<ProcessFlow>lambdaQuery()
                        .orderByDesc(ProcessFlow::getUpdatedAt, ProcessFlow::getId)
        );
        if (flows.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> versionIds = new LinkedHashSet<>();
        flows.forEach(item -> {
            if (item.getCurrentDraftVersionId() != null) {
                versionIds.add(item.getCurrentDraftVersionId());
            }
            if (item.getCurrentPublishedVersionId() != null) {
                versionIds.add(item.getCurrentPublishedVersionId());
            }
        });
        Map<Long, ProcessFlowVersion> versionMap = loadVersionMap(versionIds);

        return flows.stream().map(flow -> {
            ProcessFlowVersion currentVersion = versionMap.get(
                    flow.getCurrentDraftVersionId() != null
                            ? flow.getCurrentDraftVersionId()
                            : flow.getCurrentPublishedVersionId()
            );
            ProcessFlowSummaryVO item = new ProcessFlowSummaryVO();
            item.setId(flow.getId());
            item.setFlowCode(flow.getFlowCode());
            item.setFlowName(flow.getFlowName());
            item.setFlowDescription(flow.getFlowDescription());
            item.setStatus(flow.getStatus());
            item.setStatusLabel(statusLabel(flow.getStatus()));
            item.setCurrentVersionNo(currentVersion == null ? null : currentVersion.getVersionNo());
            item.setUpdatedAt(formatTime(flow.getUpdatedAt()));
            return item;
        }).toList();
    }

    public ProcessFlowDetailVO getFlowDetail(Long id) {
        return buildFlowDetail(requireFlow(id));
    }

    public ProcessFlowDetailVO buildFlowDetail(ProcessFlow flow) {
        ProcessFlowDetailVO detail = new ProcessFlowDetailVO();
        detail.setId(flow.getId());
        detail.setFlowCode(flow.getFlowCode());
        detail.setFlowName(flow.getFlowName());
        detail.setFlowDescription(flow.getFlowDescription());
        detail.setStatus(flow.getStatus());
        detail.setStatusLabel(statusLabel(flow.getStatus()));

        ProcessFlowVersion draftVersion = currentDraftVersion(flow);
        ProcessFlowVersion publishedVersion = currentPublishedVersion(flow);
        ProcessFlowVersion dataVersion = draftVersion != null ? draftVersion : publishedVersion;

        detail.setEditableVersionId(dataVersion == null ? null : dataVersion.getId());
        detail.setEditableVersionNo(dataVersion == null ? null : dataVersion.getVersionNo());
        detail.setPublishedVersionId(publishedVersion == null ? null : publishedVersion.getId());
        detail.setPublishedVersionNo(publishedVersion == null ? null : publishedVersion.getVersionNo());
        detail.setHasDraftVersion(draftVersion != null);
        detail.setNodes(dataVersion == null ? new ArrayList<>() : loadVersionNodes(dataVersion.getId()));
        detail.setRoutes(dataVersion == null ? new ArrayList<>() : loadVersionRoutes(dataVersion.getId()));
        return detail;
    }

    public List<ProcessFlowNodeDTO> loadVersionNodes(Long versionId) {
        return processFlowNodeMapper.selectList(
                Wrappers.<ProcessFlowNode>lambdaQuery()
                        .eq(ProcessFlowNode::getVersionId, versionId)
                        .orderByAsc(ProcessFlowNode::getDisplayOrder, ProcessFlowNode::getId)
        ).stream().map(node -> {
            ProcessFlowNodeDTO item = new ProcessFlowNodeDTO();
            item.setNodeKey(node.getNodeKey());
            item.setNodeType(node.getNodeType());
            item.setNodeName(node.getNodeName());
            item.setSceneId(node.getSceneId());
            item.setParentNodeKey(node.getParentNodeKey());
            item.setDisplayOrder(node.getDisplayOrder());
            item.setConfig(structureSupport.normalizeNodeConfig(node.getNodeType(), readMap(node.getConfigJson()), false));
            return item;
        }).toList();
    }

    public List<ProcessFlowRouteDTO> loadVersionRoutes(Long versionId) {
        return processFlowRouteMapper.selectList(
                Wrappers.<ProcessFlowRoute>lambdaQuery()
                        .eq(ProcessFlowRoute::getVersionId, versionId)
                        .orderByAsc(ProcessFlowRoute::getPriority, ProcessFlowRoute::getId)
        ).stream().map(route -> {
            ProcessFlowRouteDTO item = new ProcessFlowRouteDTO();
            item.setRouteKey(route.getRouteKey());
            item.setSourceNodeKey(route.getSourceNodeKey());
            item.setTargetNodeKey(route.getTargetNodeKey());
            item.setRouteName(route.getRouteName());
            item.setPriority(route.getPriority());
            item.setDefaultRoute(route.getDefaultRoute() != null && route.getDefaultRoute() == 1);
            item.setAttachBelowNodes(route.getAttachBelowNodes() != null && route.getAttachBelowNodes() == 1);
            item.setConditionGroups(readConditionGroups(route.getConditionJson()));
            return item;
        }).toList();
    }
}
