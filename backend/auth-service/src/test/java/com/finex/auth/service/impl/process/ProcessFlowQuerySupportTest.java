package com.finex.auth.service.impl.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowDetailVO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessFlowQuerySupportTest {

    @Mock private ProcessFlowMapper processFlowMapper;
    @Mock private ProcessFlowVersionMapper processFlowVersionMapper;
    @Mock private ProcessFlowNodeMapper processFlowNodeMapper;
    @Mock private ProcessFlowRouteMapper processFlowRouteMapper;
    @Mock private ProcessFlowSceneMapper processFlowSceneMapper;
    @Mock private SystemCompanyMapper systemCompanyMapper;
    @Mock private SystemDepartmentMapper systemDepartmentMapper;
    @Mock private UserMapper userMapper;
    @Mock private ProcessExpenseTypeMapper processExpenseTypeMapper;
    @Mock private ProcessCustomArchiveDesignMapper processCustomArchiveDesignMapper;
    @Mock private ProcessDocumentTemplateMapper processDocumentTemplateMapper;
    @Mock private ProcessUserGroupResolverSupport userGroupResolverSupport;

    private ProcessFlowQuerySupport support;

    @BeforeEach
    void setUp() {
        ProcessFlowStructureSupport structureSupport = new ProcessFlowStructureSupport(
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
                new ObjectMapper(),
                userGroupResolverSupport
        );
        support = new ProcessFlowQuerySupport(
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
                new ObjectMapper(),
                structureSupport
        );
    }

    @Test
    void getFlowDetailBuildsDraftNodesAndRoutes() throws Exception {
        ProcessFlow flow = new ProcessFlow();
        flow.setId(1L);
        flow.setFlowCode("FLOW-001");
        flow.setFlowName("Flow A");
        flow.setStatus("DRAFT");
        flow.setCurrentDraftVersionId(11L);

        ProcessFlowVersion draft = new ProcessFlowVersion();
        draft.setId(11L);
        draft.setVersionNo(2);
        draft.setVersionStatus("DRAFT");

        ProcessFlowNode node = new ProcessFlowNode();
        node.setVersionId(11L);
        node.setNodeKey("approval-1");
        node.setNodeType("APPROVAL");
        node.setNodeName("审批 1");
        node.setDisplayOrder(1);
        node.setConfigJson(new ObjectMapper().writeValueAsString(Map.of("approverType", "MANAGER")));

        ProcessFlowRoute route = new ProcessFlowRoute();
        route.setVersionId(11L);
        route.setRouteKey("route-1");
        route.setRouteName("分支 A");
        route.setPriority(1);
        route.setConditionJson("[]");

        when(processFlowMapper.selectById(1L)).thenReturn(flow);
        when(processFlowVersionMapper.selectById(11L)).thenReturn(draft);
        when(processFlowNodeMapper.selectList(any())).thenReturn(List.of(node));
        when(processFlowRouteMapper.selectList(any())).thenReturn(List.of(route));

        ProcessFlowDetailVO detail = support.getFlowDetail(1L);

        assertEquals(11L, detail.getEditableVersionId());
        assertEquals(1, detail.getNodes().size());
        assertEquals("approval-1", detail.getNodes().get(0).getNodeKey());
        assertEquals(1, detail.getRoutes().size());
        assertEquals("route-1", detail.getRoutes().get(0).getRouteKey());
    }
}
