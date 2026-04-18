package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowDetailVO;
import com.finex.auth.entity.ProcessFlow;
import com.finex.auth.entity.ProcessFlowNode;
import com.finex.auth.entity.ProcessFlowRoute;
import com.finex.auth.entity.ProcessFlowVersion;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessFlowMapper;
import com.finex.auth.mapper.ProcessFlowNodeMapper;
import com.finex.auth.mapper.ProcessFlowRouteMapper;
import com.finex.auth.mapper.ProcessFlowSceneMapper;
import com.finex.auth.mapper.ProcessFlowVersionMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessFlowDesignServiceImplTest {

    @Mock
    private ProcessFlowMapper processFlowMapper;
    @Mock
    private ProcessFlowVersionMapper processFlowVersionMapper;
    @Mock
    private ProcessFlowNodeMapper processFlowNodeMapper;
    @Mock
    private ProcessFlowRouteMapper processFlowRouteMapper;
    @Mock
    private ProcessFlowSceneMapper processFlowSceneMapper;
    @Mock
    private SystemCompanyMapper systemCompanyMapper;
    @Mock
    private SystemDepartmentMapper systemDepartmentMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ProcessExpenseTypeMapper processExpenseTypeMapper;
    @Mock
    private ProcessCustomArchiveDesignMapper processCustomArchiveDesignMapper;

    private ObjectMapper objectMapper;
    private ProcessFlowDesignServiceImpl service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ProcessFlowDesignServiceImpl(
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
                objectMapper
        );
    }

    @Test
    void getFlowMetaIncludesEnabledCompanyOptions() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setCompanyCode("A01");
        company.setCompanyName("广州远智教育科技有限公司");
        company.setStatus(1);
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(company));

        assertEquals(1, service.getFlowMeta().getCompanyOptions().size());
        assertEquals("COMPANY_A", service.getFlowMeta().getCompanyOptions().get(0).getValue());
        assertEquals("广州远智教育科技有限公司", service.getFlowMeta().getCompanyOptions().get(0).getLabel());
    }

    @Test
    void publishFlowUsesCurrentDraftVersionAndKeepsDesignatedMemberConfig() throws Exception {
        ProcessFlow flow = new ProcessFlow();
        flow.setId(1L);
        flow.setFlowCode("FLOW-001");
        flow.setFlowName("Flow Alpha");
        flow.setStatus("DRAFT");
        flow.setCurrentDraftVersionId(11L);

        ProcessFlowVersion draftVersion = new ProcessFlowVersion();
        draftVersion.setId(11L);
        draftVersion.setFlowId(1L);
        draftVersion.setVersionNo(3);
        draftVersion.setVersionStatus("DRAFT");

        Map<String, Object> designatedMemberConfig = new LinkedHashMap<>();
        designatedMemberConfig.put("userIds", List.of(101L, 202L));

        Map<String, Object> nodeConfig = new LinkedHashMap<>();
        nodeConfig.put("approverType", "DESIGNATED_MEMBER");
        nodeConfig.put("missingHandler", "AUTO_SKIP");
        nodeConfig.put("approvalMode", "OR_SIGN");
        nodeConfig.put("opinionDefaults", List.of("PASS"));
        nodeConfig.put("specialSettings", List.of());
        nodeConfig.put("managerConfig", Map.of());
        nodeConfig.put("designatedMemberConfig", designatedMemberConfig);
        nodeConfig.put("manualSelectConfig", Map.of("candidateScope", "ALL_ACTIVE_USERS"));

        ProcessFlowNode node = new ProcessFlowNode();
        node.setVersionId(11L);
        node.setNodeKey("approval-1");
        node.setNodeType("APPROVAL");
        node.setNodeName("Approval Node 1");
        node.setDisplayOrder(1);
        node.setConfigJson(objectMapper.writeValueAsString(nodeConfig));

        when(processFlowMapper.selectById(1L)).thenReturn(flow);
        when(processFlowVersionMapper.selectById(11L)).thenReturn(draftVersion);
        when(processFlowNodeMapper.selectList(any())).thenReturn(List.of(node));
        when(processFlowRouteMapper.selectList(any())).thenReturn(List.of());

        ProcessFlowDetailVO detail = service.publishFlow(1L);

        assertNotNull(detail);
        assertEquals("ENABLED", detail.getStatus());
        assertEquals(11L, detail.getPublishedVersionId());
        assertEquals(1, detail.getNodes().size());
        @SuppressWarnings("unchecked")
        Map<String, Object> returnedDesignatedMemberConfig =
                (Map<String, Object>) detail.getNodes().get(0).getConfig().get("designatedMemberConfig");
        assertEquals(List.of(101L, 202L), returnedDesignatedMemberConfig.get("userIds"));
        verify(processFlowMapper).updateById(flow);
        verify(processFlowVersionMapper).updateById(draftVersion);
    }

    @Test
    void createFlowNormalizesBlankRootParentNodeKeyInSnapshot() throws Exception {
        ArgumentCaptor<ProcessFlowVersion> versionCaptor = ArgumentCaptor.forClass(ProcessFlowVersion.class);
        doAnswer(invocation -> {
            ProcessFlow flow = invocation.getArgument(0);
            flow.setId(1L);
            return 1;
        }).when(processFlowMapper).insert(any(ProcessFlow.class));
        ProcessFlow storedFlow = new ProcessFlow();
        storedFlow.setId(1L);
        storedFlow.setFlowCode("FLOW-001");
        storedFlow.setFlowName("Flow Root Normalize");
        storedFlow.setStatus("DRAFT");
        storedFlow.setCurrentDraftVersionId(11L);
        when(processFlowMapper.selectById(1L)).thenReturn(storedFlow);
        doAnswer(invocation -> {
            ProcessFlowVersion version = invocation.getArgument(0);
            version.setId(11L);
            return 1;
        }).when(processFlowVersionMapper).insert(versionCaptor.capture());
        ProcessFlowVersion storedVersion = new ProcessFlowVersion();
        storedVersion.setId(11L);
        storedVersion.setFlowId(1L);
        storedVersion.setVersionNo(1);
        storedVersion.setVersionStatus("DRAFT");
        when(processFlowVersionMapper.selectById(11L)).thenReturn(storedVersion);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "approver")));

        com.finex.auth.dto.ProcessFlowSaveDTO dto = new com.finex.auth.dto.ProcessFlowSaveDTO();
        dto.setFlowName("Flow Root Normalize");

        com.finex.auth.dto.ProcessFlowNodeDTO node = new com.finex.auth.dto.ProcessFlowNodeDTO();
        node.setNodeKey("approval-1");
        node.setNodeType("APPROVAL");
        node.setNodeName("Approval Node 1");
        node.setParentNodeKey("");
        node.setDisplayOrder(1);
        node.setConfig(new LinkedHashMap<>(Map.of(
                "approverType", "DESIGNATED_MEMBER",
                "designatedMemberConfig", Map.of("userIds", List.of(101L))
        )));
        dto.setNodes(List.of(node));

        ProcessFlowDetailVO detail = service.createFlow(dto);

        assertNotNull(detail);
        Map<String, Object> snapshot = objectMapper.readValue(
                versionCaptor.getValue().getSnapshotJson(),
                new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>>() {}
        );
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) snapshot.get("nodes");
        assertEquals(1, nodes.size());
        assertEquals(null, nodes.get(0).get("parentNodeKey"));
    }

    @Test
    void createFlowMovesAttachedRouteToFirstPriorityAndPersistsFlag() {
        List<ProcessFlowNode> storedNodes = new java.util.ArrayList<>();
        List<ProcessFlowRoute> storedRoutes = new java.util.ArrayList<>();

        doAnswer(invocation -> {
            ProcessFlow flow = invocation.getArgument(0);
            flow.setId(1L);
            return 1;
        }).when(processFlowMapper).insert(any(ProcessFlow.class));
        doAnswer(invocation -> {
            ProcessFlowVersion version = invocation.getArgument(0);
            version.setId(11L);
            return 1;
        }).when(processFlowVersionMapper).insert(any(ProcessFlowVersion.class));
        doAnswer(invocation -> {
            ProcessFlowNode node = invocation.getArgument(0);
            node.setId((long) (storedNodes.size() + 1));
            storedNodes.add(node);
            return 1;
        }).when(processFlowNodeMapper).insert(any(ProcessFlowNode.class));
        doAnswer(invocation -> {
            ProcessFlowRoute route = invocation.getArgument(0);
            route.setId((long) (storedRoutes.size() + 1));
            storedRoutes.add(route);
            return 1;
        }).when(processFlowRouteMapper).insert(any(ProcessFlowRoute.class));

        ProcessFlow storedFlow = new ProcessFlow();
        storedFlow.setId(1L);
        storedFlow.setFlowCode("FLOW-001");
        storedFlow.setFlowName("Flow Attach Tail");
        storedFlow.setStatus("DRAFT");
        storedFlow.setCurrentDraftVersionId(11L);
        when(processFlowMapper.selectById(1L)).thenReturn(storedFlow);

        ProcessFlowVersion storedVersion = new ProcessFlowVersion();
        storedVersion.setId(11L);
        storedVersion.setFlowId(1L);
        storedVersion.setVersionNo(1);
        storedVersion.setVersionStatus("DRAFT");
        when(processFlowVersionMapper.selectById(11L)).thenReturn(storedVersion);
        when(processFlowSceneMapper.selectList(any())).thenReturn(List.of());
        when(processFlowNodeMapper.selectList(any())).thenReturn(storedNodes);
        when(processFlowRouteMapper.selectList(any())).thenReturn(storedRoutes);

        com.finex.auth.dto.ProcessFlowSaveDTO dto = new com.finex.auth.dto.ProcessFlowSaveDTO();
        dto.setFlowName("Flow Attach Tail");

        com.finex.auth.dto.ProcessFlowNodeDTO branchNode = new com.finex.auth.dto.ProcessFlowNodeDTO();
        branchNode.setNodeKey("branch-1");
        branchNode.setNodeType("BRANCH");
        branchNode.setNodeName("流程分支 1");
        branchNode.setDisplayOrder(1);
        branchNode.setConfig(new LinkedHashMap<>());
        dto.setNodes(List.of(branchNode));

        com.finex.auth.dto.ProcessFlowRouteDTO routeA = new com.finex.auth.dto.ProcessFlowRouteDTO();
        routeA.setRouteKey("route-1");
        routeA.setSourceNodeKey("branch-1");
        routeA.setRouteName("分支 A");
        routeA.setPriority(1);
        routeA.setAttachBelowNodes(false);
        routeA.setConditionGroups(List.of());

        com.finex.auth.dto.ProcessFlowRouteDTO routeB = new com.finex.auth.dto.ProcessFlowRouteDTO();
        routeB.setRouteKey("route-2");
        routeB.setSourceNodeKey("branch-1");
        routeB.setRouteName("分支 B");
        routeB.setPriority(2);
        routeB.setAttachBelowNodes(true);
        routeB.setConditionGroups(List.of());

        dto.setRoutes(List.of(routeA, routeB));

        ProcessFlowDetailVO detail = service.createFlow(dto);

        assertNotNull(detail);
        storedRoutes.sort(java.util.Comparator.comparing(ProcessFlowRoute::getPriority));
        assertEquals(List.of("route-2", "route-1"), storedRoutes.stream().map(ProcessFlowRoute::getRouteKey).toList());
        assertEquals(List.of(1, 0), storedRoutes.stream().map(ProcessFlowRoute::getAttachBelowNodes).toList());
        assertEquals(List.of(1, 2), storedRoutes.stream().map(ProcessFlowRoute::getPriority).toList());
        assertTrue(detail.getRoutes().stream().anyMatch(item -> "route-2".equals(item.getRouteKey()) && Boolean.TRUE.equals(item.getAttachBelowNodes())));
    }

    @Test
    void updateFlowPersistsAttachBelowNodesFalseWhenPreviouslyEnabled() {
        List<ProcessFlowNode> storedNodes = new java.util.ArrayList<>();
        List<ProcessFlowRoute> storedRoutes = new java.util.ArrayList<>();

        ProcessFlow flow = new ProcessFlow();
        flow.setId(1L);
        flow.setFlowCode("FLOW-001");
        flow.setFlowName("Flow Attach Tail");
        flow.setStatus("DRAFT");
        flow.setCurrentDraftVersionId(11L);
        when(processFlowMapper.selectById(1L)).thenReturn(flow);

        ProcessFlowVersion storedVersion = new ProcessFlowVersion();
        storedVersion.setId(11L);
        storedVersion.setFlowId(1L);
        storedVersion.setVersionNo(1);
        storedVersion.setVersionStatus("DRAFT");
        when(processFlowVersionMapper.selectById(11L)).thenReturn(storedVersion);

        ProcessFlowNode branchNode = new ProcessFlowNode();
        branchNode.setId(1L);
        branchNode.setVersionId(11L);
        branchNode.setNodeKey("branch-1");
        branchNode.setNodeType("BRANCH");
        branchNode.setNodeName("流程分支 1");
        branchNode.setDisplayOrder(1);
        branchNode.setConfigJson("{}");
        storedNodes.add(branchNode);

        ProcessFlowRoute routeA = new ProcessFlowRoute();
        routeA.setId(1L);
        routeA.setVersionId(11L);
        routeA.setRouteKey("route-1");
        routeA.setSourceNodeKey("branch-1");
        routeA.setRouteName("分支 A");
        routeA.setPriority(1);
        routeA.setDefaultRoute(0);
        routeA.setAttachBelowNodes(1);
        routeA.setConditionJson("[]");
        storedRoutes.add(routeA);

        ProcessFlowRoute routeB = new ProcessFlowRoute();
        routeB.setId(2L);
        routeB.setVersionId(11L);
        routeB.setRouteKey("route-2");
        routeB.setSourceNodeKey("branch-1");
        routeB.setRouteName("分支 B");
        routeB.setPriority(2);
        routeB.setDefaultRoute(0);
        routeB.setAttachBelowNodes(0);
        routeB.setConditionJson("[]");
        storedRoutes.add(routeB);

        when(processFlowSceneMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            storedNodes.clear();
            return 1;
        }).when(processFlowNodeMapper).delete(any());
        doAnswer(invocation -> {
            storedRoutes.clear();
            return 1;
        }).when(processFlowRouteMapper).delete(any());
        doAnswer(invocation -> {
            ProcessFlowNode node = invocation.getArgument(0);
            node.setId((long) (storedNodes.size() + 1));
            storedNodes.add(node);
            return 1;
        }).when(processFlowNodeMapper).insert(any(ProcessFlowNode.class));
        doAnswer(invocation -> {
            ProcessFlowRoute route = invocation.getArgument(0);
            route.setId((long) (storedRoutes.size() + 1));
            storedRoutes.add(route);
            return 1;
        }).when(processFlowRouteMapper).insert(any(ProcessFlowRoute.class));
        when(processFlowNodeMapper.selectList(any())).thenReturn(storedNodes);
        when(processFlowRouteMapper.selectList(any())).thenReturn(storedRoutes);

        com.finex.auth.dto.ProcessFlowSaveDTO dto = new com.finex.auth.dto.ProcessFlowSaveDTO();
        dto.setFlowName("Flow Attach Tail");

        com.finex.auth.dto.ProcessFlowNodeDTO branchNodeDto = new com.finex.auth.dto.ProcessFlowNodeDTO();
        branchNodeDto.setNodeKey("branch-1");
        branchNodeDto.setNodeType("BRANCH");
        branchNodeDto.setNodeName("流程分支 1");
        branchNodeDto.setDisplayOrder(1);
        branchNodeDto.setConfig(new LinkedHashMap<>());
        dto.setNodes(List.of(branchNodeDto));

        com.finex.auth.dto.ProcessFlowRouteDTO routeADto = new com.finex.auth.dto.ProcessFlowRouteDTO();
        routeADto.setRouteKey("route-1");
        routeADto.setSourceNodeKey("branch-1");
        routeADto.setRouteName("分支 A");
        routeADto.setPriority(1);
        routeADto.setAttachBelowNodes(false);
        routeADto.setConditionGroups(List.of());

        com.finex.auth.dto.ProcessFlowRouteDTO routeBDto = new com.finex.auth.dto.ProcessFlowRouteDTO();
        routeBDto.setRouteKey("route-2");
        routeBDto.setSourceNodeKey("branch-1");
        routeBDto.setRouteName("分支 B");
        routeBDto.setPriority(2);
        routeBDto.setAttachBelowNodes(false);
        routeBDto.setConditionGroups(List.of());

        dto.setRoutes(List.of(routeADto, routeBDto));

        ProcessFlowDetailVO detail = service.updateFlow(1L, dto);

        assertNotNull(detail);
        assertEquals(List.of(0, 0), storedRoutes.stream().map(ProcessFlowRoute::getAttachBelowNodes).toList());
        assertEquals(List.of("route-1", "route-2"), storedRoutes.stream().map(ProcessFlowRoute::getRouteKey).toList());
        assertTrue(detail.getRoutes().stream().allMatch(item -> !Boolean.TRUE.equals(item.getAttachBelowNodes())));
    }

    private com.finex.auth.entity.User createActiveUser(Long id, String name) {
        com.finex.auth.entity.User user = new com.finex.auth.entity.User();
        user.setId(id);
        user.setName(name);
        user.setStatus(1);
        return user;
    }
}
