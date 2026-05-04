package com.finex.auth.service.impl.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowRouteDTO;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ProcessFlowStructureSupportTest {

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

    private ProcessFlowStructureSupport support;

    @BeforeEach
    void setUp() {
        support = new ProcessFlowStructureSupport(
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
    }

    @Test
    void normalizeNodeConfigForcesAndSignWhenManagerLevelIsGreaterThanOne() {
        Map<String, Object> normalized = support.normalizeNodeConfig(
                "APPROVAL",
                new LinkedHashMap<>(Map.of(
                        "approverType", "MANAGER",
                        "approvalMode", "OR_SIGN",
                        "managerConfig", Map.of(
                                "deptSource", "UNDERTAKE_DEPT",
                                "managerLevel", 2,
                                "orgTreeLookupEnabled", true,
                                "orgTreeLookupLevel", 1
                        )
                )),
                false
        );

        assertEquals("AND_SIGN", normalized.get("approvalMode"));
    }

    @Test
    void normalizeBranchRoutePrioritiesMovesAttachedRouteFirst() {
        ProcessFlowRouteDTO routeA = new ProcessFlowRouteDTO();
        routeA.setRouteKey("route-1");
        routeA.setPriority(1);
        routeA.setAttachBelowNodes(false);

        ProcessFlowRouteDTO routeB = new ProcessFlowRouteDTO();
        routeB.setRouteKey("route-2");
        routeB.setPriority(2);
        routeB.setAttachBelowNodes(true);

        support.normalizeBranchRoutePriorities(List.of(routeA, routeB));

        assertEquals(2, routeA.getPriority());
        assertEquals(1, routeB.getPriority());
    }

    @Test
    void normalizeRoutesRejectsMultipleDefaultRoutesInSameBranch() {
        ProcessFlowRouteDTO routeA = new ProcessFlowRouteDTO();
        routeA.setRouteKey("route-1");
        routeA.setSourceNodeKey("branch-1");
        routeA.setPriority(1);
        routeA.setDefaultRoute(true);

        ProcessFlowRouteDTO routeB = new ProcessFlowRouteDTO();
        routeB.setRouteKey("route-2");
        routeB.setSourceNodeKey("branch-1");
        routeB.setPriority(2);
        routeB.setDefaultRoute(true);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> support.normalizeRoutes(List.of(routeA, routeB))
        );

        assertEquals("同一分支块最多只能有 1 条 else 分支", error.getMessage());
    }

    @Test
    void normalizeRoutesClearsConditionGroupsForDefaultRoute() {
        ProcessFlowRouteDTO route = new ProcessFlowRouteDTO();
        route.setRouteKey("route-1");
        route.setSourceNodeKey("branch-1");
        route.setPriority(1);
        route.setDefaultRoute(true);
        route.setConditionGroups(List.of(new com.finex.auth.dto.ProcessFlowConditionGroupDTO()));

        support.normalizeRoutes(List.of(route));

        assertEquals(List.of(), route.getConditionGroups());
    }
}
