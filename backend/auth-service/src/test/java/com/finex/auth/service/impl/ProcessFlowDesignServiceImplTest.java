package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowDetailVO;
import com.finex.auth.entity.ProcessFlow;
import com.finex.auth.entity.ProcessFlowNode;
import com.finex.auth.entity.ProcessFlowVersion;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessFlowMapper;
import com.finex.auth.mapper.ProcessFlowNodeMapper;
import com.finex.auth.mapper.ProcessFlowRouteMapper;
import com.finex.auth.mapper.ProcessFlowSceneMapper;
import com.finex.auth.mapper.ProcessFlowVersionMapper;
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
                systemDepartmentMapper,
                userMapper,
                processExpenseTypeMapper,
                processCustomArchiveDesignMapper,
                objectMapper
        );
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

    private com.finex.auth.entity.User createActiveUser(Long id, String name) {
        com.finex.auth.entity.User user = new com.finex.auth.entity.User();
        user.setId(id);
        user.setName(name);
        user.setStatus(1);
        return user;
    }
}
