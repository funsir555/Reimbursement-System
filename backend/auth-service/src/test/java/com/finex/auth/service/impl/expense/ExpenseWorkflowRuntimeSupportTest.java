package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowConditionDTO;
import com.finex.auth.dto.ProcessFlowConditionGroupDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.SystemPermissionMapper;
import com.finex.auth.mapper.SystemRolePermissionMapper;
import com.finex.auth.mapper.SystemUserRoleMapper;
import com.finex.auth.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseWorkflowRuntimeSupportTest {

    @Mock
    private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock
    private ProcessDocumentTaskMapper processDocumentTaskMapper;
    @Mock
    private ProcessDocumentActionLogMapper processDocumentActionLogMapper;
    @Mock
    private ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    @Mock
    private SystemPermissionMapper systemPermissionMapper;
    @Mock
    private SystemDepartmentMapper systemDepartmentMapper;
    @Mock
    private SystemRolePermissionMapper systemRolePermissionMapper;
    @Mock
    private SystemUserRoleMapper systemUserRoleMapper;
    @Mock
    private UserMapper userMapper;

    @Test
    void buildRuntimeFlowContextIncludesSubmitterAmountAndType() {
        ExpenseWorkflowRuntimeSupport support = newSupport();
        User submitter = new User();
        submitter.setId(7L);
        submitter.setDeptId(9L);
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateType("report");
        template.setCategoryCode("travel");
        ProcessFormDesign formDesign = new ProcessFormDesign();
        ProcessExpenseDetailDesign expenseDetailDesign = new ProcessExpenseDetailDesign();
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("amount", new BigDecimal("123.45"));

        Map<String, Object> actual = support.buildRuntimeFlowContext(
                submitter,
                template,
                formDesign,
                formData,
                expenseDetailDesign,
                List.of()
        );

        assertEquals(7L, actual.get("submitterUserId"));
        assertEquals(9L, actual.get("submitterDeptId"));
        assertEquals(new BigDecimal("123.45"), actual.get("amount"));
        assertEquals("report", actual.get("documentType"));
        assertEquals("travel", actual.get("expenseTypeCode"));
    }

    @Test
    void inspectRawFlowSnapshotFlagsBlankAndNullRootSeparately() throws Exception {
        ExpenseWorkflowRuntimeSupport support = newSupport();
        ObjectMapper mapper = new ObjectMapper();
        String blankRootJson = mapper.writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "A1",
                                "nodeType", "APPROVAL",
                                "parentNodeKey", ""
                        )
                )
        ));
        String nullRootJson = mapper.writeValueAsString(Map.of(
                "nodes", List.of(
                        new LinkedHashMap<>(Map.of(
                                "nodeKey", "A1",
                                "nodeType", "APPROVAL"
                        ))
                )
        ));

        RawFlowSnapshotSignature blankRoot = support.inspectRawFlowSnapshot(blankRootJson);
        RawFlowSnapshotSignature nullRoot = support.inspectRawFlowSnapshot(nullRootJson);

        assertTrue(blankRoot.hasApprovalNode());
        assertTrue(blankRoot.hasBlankRootNode());
        assertFalse(blankRoot.hasNullRootNode());

        assertTrue(nullRoot.hasApprovalNode());
        assertFalse(nullRoot.hasBlankRootNode());
        assertTrue(nullRoot.hasNullRootNode());
    }

    @Test
    void paymentTaskAllowsRetryReadsFlowSnapshotSetting() throws Exception {
        ExpenseWorkflowRuntimeSupport support = newSupport();
        ObjectMapper mapper = new ObjectMapper();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setFlowSnapshotJson(mapper.writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "PAY-1",
                                "nodeType", "PAYMENT",
                                "parentNodeKey", "__ROOT__",
                                "config", Map.of("specialSettings", List.of("ALLOW_RETRY"))
                        )
                ),
                "routes", List.of()
        )));
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setNodeKey("PAY-1");

        assertTrue(support.paymentTaskAllowsRetry(instance, task));
    }

    @Test
    void attachedRouteContinuesToSharedTailAfterLaneTaskCompletes() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(processDocumentTaskMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "审批人A")));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildBranchSnapshot(true, "ATTACHED"));

        support.initializeRuntime(instance, Map.of("documentType", "ATTACHED"));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-route-a", insertedTasks.get(0).getNodeKey());

        ProcessDocumentTask firstTask = insertedTasks.get(0);
        firstTask.setId(1L);
        support.approvePendingTask(instance, firstTask, 101L, "审批人A", "同意");

        assertEquals(2, insertedTasks.size());
        assertEquals("approval-tail", insertedTasks.get(1).getNodeKey());
    }

    @Test
    void nonAttachedRouteSkipsSharedTailWhenSiblingLaneOwnsIt() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(processDocumentTaskMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "审批人A")));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildBranchSnapshot(true, "B"));

        support.initializeRuntime(instance, Map.of("documentType", "B"));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-route-b", insertedTasks.get(0).getNodeKey());

        ProcessDocumentTask firstTask = insertedTasks.get(0);
        firstTask.setId(1L);
        support.approvePendingTask(instance, firstTask, 101L, "审批人A", "同意");

        assertEquals(1, insertedTasks.size());
        assertEquals("COMPLETED", instance.getStatus());
    }

    private ExpenseWorkflowRuntimeSupport newSupport() {
        return new ExpenseWorkflowRuntimeSupport(
                processDocumentInstanceMapper,
                processDocumentTaskMapper,
                processDocumentActionLogMapper,
                processDocumentExpenseDetailMapper,
                systemPermissionMapper,
                systemDepartmentMapper,
                systemRolePermissionMapper,
                systemUserRoleMapper,
                userMapper,
                new ObjectMapper()
        );
    }

    private void mockTaskInsertions(List<ProcessDocumentTask> insertedTasks) {
        doAnswer(invocation -> {
            ProcessDocumentTask task = invocation.getArgument(0);
            task.setId((long) (insertedTasks.size() + 1));
            insertedTasks.add(task);
            return 1;
        }).when(processDocumentTaskMapper).insert(any(ProcessDocumentTask.class));
    }

    private ProcessDocumentInstance createRuntimeInstance(String snapshotJson) {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");
        instance.setApprovalFlowCode("FLOW-001");
        instance.setFlowSnapshotJson(snapshotJson);
        instance.setStatus("PENDING_APPROVAL");
        instance.setTemplateType("report");
        return instance;
    }

    private User createActiveUser(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setStatus(1);
        return user;
    }

    private String buildBranchSnapshot(boolean branchHasAttachedRoute, String matchedDocumentType) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProcessFlowConditionDTO routeBCondition = new ProcessFlowConditionDTO();
        routeBCondition.setFieldKey("documentType");
        routeBCondition.setOperator("EQ");
        routeBCondition.setCompareValue("B");

        ProcessFlowConditionGroupDTO routeBGroup = new ProcessFlowConditionGroupDTO();
        routeBGroup.setGroupNo(1);
        routeBGroup.setConditions(List.of(routeBCondition));

        List<Map<String, Object>> routes = new ArrayList<>();
        routes.add(new LinkedHashMap<>(Map.of(
                "routeKey", "route-a",
                "sourceNodeKey", "branch-1",
                "routeName", "分支A",
                "priority", 1,
                "attachBelowNodes", branchHasAttachedRoute,
                "conditionGroups", List.of()
        )));
        routes.add(new LinkedHashMap<>(Map.of(
                "routeKey", "route-b",
                "sourceNodeKey", "branch-1",
                "routeName", "分支B",
                "priority", 2,
                "attachBelowNodes", false,
                "conditionGroups", List.of(routeBGroup)
        )));
        if ("B".equals(matchedDocumentType)) {
            routes.get(0).put("priority", 2);
            routes.get(0).put("conditionGroups", List.of(new LinkedHashMap<>(Map.of(
                    "groupNo", 1,
                    "conditions", List.of(new LinkedHashMap<>(Map.of(
                            "fieldKey", "documentType",
                            "operator", "EQ",
                            "compareValue", "ATTACHED"
                    )))
            ))));
            routes.get(1).put("priority", 1);
        }

        return mapper.writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "branch-1",
                                "nodeType", "BRANCH",
                                "nodeName", "流程分支",
                                "displayOrder", 1,
                                "config", Map.of()
                        ),
                        Map.of(
                                "nodeKey", "approval-route-a",
                                "nodeType", "APPROVAL",
                                "nodeName", "分支A审批",
                                "parentNodeKey", "route-a",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "DESIGNATED_MEMBER",
                                        "designatedMemberConfig", Map.of("userIds", List.of(101L)),
                                        "missingHandler", "AUTO_SKIP",
                                        "approvalMode", "OR_SIGN"
                                )
                        ),
                        Map.of(
                                "nodeKey", "approval-route-b",
                                "nodeType", "APPROVAL",
                                "nodeName", "分支B审批",
                                "parentNodeKey", "route-b",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "DESIGNATED_MEMBER",
                                        "designatedMemberConfig", Map.of("userIds", List.of(101L)),
                                        "missingHandler", "AUTO_SKIP",
                                        "approvalMode", "OR_SIGN"
                                )
                        ),
                        Map.of(
                                "nodeKey", "approval-tail",
                                "nodeType", "APPROVAL",
                                "nodeName", "公共尾部审批",
                                "displayOrder", 2,
                                "config", Map.of(
                                        "approverType", "DESIGNATED_MEMBER",
                                        "designatedMemberConfig", Map.of("userIds", List.of(101L)),
                                        "missingHandler", "AUTO_SKIP",
                                        "approvalMode", "OR_SIGN"
                                )
                        )
                ),
                "routes", routes
        ));
    }
}
