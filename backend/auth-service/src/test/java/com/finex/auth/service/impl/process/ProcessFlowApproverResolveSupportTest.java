package com.finex.auth.service.impl.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessFlowApproverResolveSupportTest {

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

    private ProcessFlowApproverResolveSupport support;

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
        support = new ProcessFlowApproverResolveSupport(
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
                structureSupport,
                userGroupResolverSupport
        );
    }

    @Test
    void resolveManagerMembersReturnsFirstToNthManagersInOrder() {
        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of(
                buildDepartment(3L, "Root Dept", null, 701L),
                buildDepartment(7L, "Parent Dept", 3L, 601L),
                buildDepartment(15L, "Undertake Dept", 7L, 501L)
        ));
        when(userMapper.selectBatchIds(List.of(501L))).thenReturn(List.of(createActiveUser(501L, "Leader L1")));
        when(userMapper.selectBatchIds(List.of(601L))).thenReturn(List.of(createActiveUser(601L, "Leader L2")));

        List<User> resolved = support.resolveManagerMembers(
                Map.of(
                        "managerConfig", Map.of(
                                "deptSource", "UNDERTAKE_DEPT",
                                "managerLevel", 2,
                                "orgTreeLookupEnabled", true,
                                "orgTreeLookupLevel", 1
                        )
                ),
                Map.of("undertakeDeptIds", List.of(15L)),
                new ArrayList<>()
        );

        assertEquals(List.of(501L, 601L), resolved.stream().map(User::getId).toList());
    }

    private SystemDepartment buildDepartment(Long id, String name, Long parentId, Long leaderUserId) {
        SystemDepartment department = new SystemDepartment();
        department.setId(id);
        department.setDeptName(name);
        department.setParentId(parentId);
        department.setLeaderUserId(leaderUserId);
        department.setStatus(1);
        return department;
    }

    private User createActiveUser(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setStatus(1);
        return user;
    }
}
