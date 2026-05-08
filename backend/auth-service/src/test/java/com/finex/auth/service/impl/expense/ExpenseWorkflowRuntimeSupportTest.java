package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowConditionDTO;
import com.finex.auth.dto.ProcessFlowConditionGroupDTO;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessUserGroup;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.mapper.ProcessUserGroupMapper;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private ProcessUserGroupMapper processUserGroupMapper;
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
        formDesign.setSchemaJson("""
                {
                  "blocks": [
                    {
                      "kind": "BUSINESS_COMPONENT",
                      "fieldKey": "paymentCompany",
                      "props": {
                        "componentCode": "payment-company"
                      }
                    },
                    {
                      "kind": "BUSINESS_COMPONENT",
                      "fieldKey": "undertakeDept",
                      "props": {
                        "componentCode": "undertake-department"
                      }
                    }
                  ]
                }
                """);
        ProcessExpenseDetailDesign expenseDetailDesign = new ProcessExpenseDetailDesign();
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("amount", new BigDecimal("123.45"));
        formData.put("paymentCompany", "COMPANY_A");
        formData.put("undertakeDept", List.of("15"));

        when(systemDepartmentMapper.selectList(any())).thenReturn(buildUndertakeDepartmentTree());

        com.finex.auth.dto.ExpenseDetailInstanceDTO detailA = new com.finex.auth.dto.ExpenseDetailInstanceDTO();
        detailA.setFormData(new LinkedHashMap<>(Map.of("actualPaymentAmount", "88.80")));
        com.finex.auth.dto.ExpenseDetailInstanceDTO detailB = new com.finex.auth.dto.ExpenseDetailInstanceDTO();
        detailB.setFormData(new LinkedHashMap<>(Map.of("actualPaymentAmount", "11.20")));

        Map<String, Object> actual = support.buildRuntimeFlowContext(
                submitter,
                template,
                formDesign,
                formData,
                expenseDetailDesign,
                List.of(detailA, detailB)
        );

        assertEquals(7L, actual.get("submitterUserId"));
        assertEquals(9L, actual.get("submitterDeptId"));
        assertEquals(new BigDecimal("100.00"), actual.get("amount"));
        assertEquals("COMPANY_A", actual.get("paymentCompanyId"));
        assertEquals(List.of("15"), actual.get("undertakeDeptIds"));
        assertEquals(List.of("15"), actual.get("undertakeDeptIdExact"));
        assertEquals(List.of(15L, 7L, 3L), actual.get("undertakeDeptIdWithChildren"));
        assertEquals("report", actual.get("documentType"));
        assertEquals("travel", actual.get("expenseTypeCode"));
    }

    @Test
    void buildRuntimeContextForInstanceKeepsDirectSubmitterDeptId() {
        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-CTX");
        instance.setSubmitterUserId(20L);
        instance.setFormDataJson("{}");

        User submitter = new User();
        submitter.setId(20L);
        submitter.setDeptId(15L);

        when(userMapper.selectById(20L)).thenReturn(submitter);
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> actual = support.buildRuntimeContextForInstance(instance);

        assertEquals(20L, actual.get("submitterUserId"));
        assertEquals(15L, actual.get("submitterDeptId"));
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
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "approver-A")));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildBranchSnapshot(true, "ATTACHED"));

        support.initializeRuntime(instance, Map.of("documentType", "ATTACHED"));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-route-a", insertedTasks.get(0).getNodeKey());

        ProcessDocumentTask firstTask = insertedTasks.get(0);
        firstTask.setId(1L);
        support.approvePendingTask(instance, firstTask, 101L, "approver-A", "agree");

        assertEquals(2, insertedTasks.size());
        assertEquals("approval-tail", insertedTasks.get(1).getNodeKey());
    }

    @Test
    void nonAttachedRouteSkipsSharedTailWhenSiblingLaneOwnsIt() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "approver-A")));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildBranchSnapshot(true, "B"));

        support.initializeRuntime(instance, Map.of("documentType", "B"));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-route-b", insertedTasks.get(0).getNodeKey());

        ProcessDocumentTask firstTask = insertedTasks.get(0);
        firstTask.setId(1L);
        support.approvePendingTask(instance, firstTask, 101L, "approver-A", "agree");

        assertEquals(1, insertedTasks.size());
        assertEquals("COMPLETED", instance.getStatus());
    }

    @Test
    void manualSelectApprovalNodeUsesRuntimeSelectedApprovers() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(201L, "finance-user")));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildManualSelectApprovalSnapshot());

        support.initializeRuntime(instance, Map.of(
                "manualApproverSelections", Map.of("approval-manual", List.of(201L))
        ));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-manual", insertedTasks.get(0).getNodeKey());
        assertEquals(201L, insertedTasks.get(0).getAssigneeUserId());
    }

    @Test
    void manualSelectPaymentNodeUsesRuntimeSelectedApprovers() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(301L, "cashier-user")));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildManualSelectPaymentSnapshot());

        support.initializeRuntime(instance, Map.of(
                "manualApproverSelections", Map.of("payment-manual", List.of(301L))
        ));

        assertEquals(1, insertedTasks.size());
        assertEquals("payment-manual", insertedTasks.get(0).getNodeKey());
        assertEquals(301L, insertedTasks.get(0).getAssigneeUserId());
        assertEquals("PENDING_PAYMENT", instance.getStatus());
    }


    @Test
    void manualSelectApprovalNodePausesRuntimeUntilSubmitterSelectsApprover() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildManualSelectApprovalSnapshot());

        support.initializeRuntime(instance, Map.of());

        assertTrue(insertedTasks.isEmpty());
        assertEquals("PENDING_APPROVAL", instance.getStatus());
        assertEquals("approval-manual", instance.getCurrentNodeKey());
        assertEquals("MANUAL_SELECT", instance.getCurrentTaskType());
    }

    @Test
    void manualSelectCcNodePausesRuntimeUntilSubmitterSelectsRecipient() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildManualSelectCcSnapshot());

        support.initializeRuntime(instance, Map.of());

        assertTrue(insertedTasks.isEmpty());
        assertEquals("cc-manual", instance.getCurrentNodeKey());
        assertEquals("MANUAL_SELECT", instance.getCurrentTaskType());
    }

    @Test
    void paymentExecutorSubmitterFallsBackToSubmitterContext() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(userMapper.selectById(301L)).thenReturn(createActiveUser(301L, "submitter"));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildSubmitterPaymentSnapshot());

        support.initializeRuntime(instance, Map.of("submitterUserId", 301L));

        assertEquals(1, insertedTasks.size());
        assertEquals("payment-1", insertedTasks.get(0).getNodeKey());
        assertEquals(301L, insertedTasks.get(0).getAssigneeUserId());
        assertEquals("PENDING_PAYMENT", instance.getStatus());
    }

    @Test
    void designatedMemberSubmitterFallsBackToSubmitterContext() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(userMapper.selectById(301L)).thenReturn(createActiveUser(301L, "submitter"));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildSubmitterDesignatedApprovalSnapshot());

        support.initializeRuntime(instance, Map.of("submitterUserId", 301L));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-submitter", insertedTasks.get(0).getNodeKey());
        assertEquals(301L, insertedTasks.get(0).getAssigneeUserId());
        assertEquals("PENDING_APPROVAL", instance.getStatus());
    }

    @Test
    void designatedUserGroupApprovalResolvesMatchedMembersAsAndSignTasks() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);

        ProcessUserGroup secondLevelGroup = new ProcessUserGroup();
        secondLevelGroup.setId(200L);
        secondLevelGroup.setCodeLevel(2);
        secondLevelGroup.setGroupName("二级分配组");

        ProcessUserGroup thirdLevelGroup = new ProcessUserGroup();
        thirdLevelGroup.setId(201L);
        thirdLevelGroup.setParentId(200L);
        thirdLevelGroup.setCodeLevel(3);
        thirdLevelGroup.setGroupName("三级功能组");
        thirdLevelGroup.setMemberUserIdsJson("[\"501\",\"502\"]");
        thirdLevelGroup.setScopeConditionGroupsJson("""
                [{"groupNo":1,"conditions":[{"fieldKey":"paymentCompanyId","operator":"IN","compareValue":["COMPANY_A"]}]}]
                """);

        when(processUserGroupMapper.selectById(200L)).thenReturn(secondLevelGroup);
        when(processUserGroupMapper.selectList(any())).thenReturn(List.of(secondLevelGroup, thirdLevelGroup));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                createActiveUser(501L, "group-user-a"),
                createActiveUser(502L, "group-user-b")
        ));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildUserGroupApprovalSnapshot());

        support.initializeRuntime(instance, Map.of("paymentCompanyId", "COMPANY_A"));

        assertEquals(2, insertedTasks.size());
        assertEquals(List.of(501L, 502L), insertedTasks.stream().map(ProcessDocumentTask::getAssigneeUserId).toList());
        assertTrue(insertedTasks.stream().allMatch(item -> "AND_SIGN".equals(item.getApprovalMode())));
    }

    @Test
    void designatedUserGroupPaymentResolvesMatchedMembersAsAndSignTasks() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);

        ProcessUserGroup secondLevelGroup = new ProcessUserGroup();
        secondLevelGroup.setId(200L);
        secondLevelGroup.setCodeLevel(2);
        secondLevelGroup.setGroupName("二级分配组");

        ProcessUserGroup thirdLevelGroup = new ProcessUserGroup();
        thirdLevelGroup.setId(201L);
        thirdLevelGroup.setParentId(200L);
        thirdLevelGroup.setCodeLevel(3);
        thirdLevelGroup.setGroupName("三级功能组");
        thirdLevelGroup.setMemberUserIdsJson("[\"501\",\"502\"]");
        thirdLevelGroup.setScopeConditionGroupsJson("""
                [{"groupNo":1,"conditions":[{"fieldKey":"paymentCompanyId","operator":"IN","compareValue":["COMPANY_A"]}]}]
                """);

        when(processUserGroupMapper.selectById(200L)).thenReturn(secondLevelGroup);
        when(processUserGroupMapper.selectList(any())).thenReturn(List.of(secondLevelGroup, thirdLevelGroup));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                createActiveUser(501L, "group-user-a"),
                createActiveUser(502L, "group-user-b")
        ));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildUserGroupPaymentSnapshot());

        support.initializeRuntime(instance, Map.of("paymentCompanyId", "COMPANY_A"));

        assertEquals(2, insertedTasks.size());
        assertEquals(List.of(501L, 502L), insertedTasks.stream().map(ProcessDocumentTask::getAssigneeUserId).toList());
        assertTrue(insertedTasks.stream().allMatch(item -> "AND_SIGN".equals(item.getApprovalMode())));
        assertEquals("PENDING_PAYMENT", instance.getStatus());
    }

    @Test
    void submitterDeptBranchMatchesAncestorDepartment() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "approver-A")));
        when(systemDepartmentMapper.selectList(any())).thenReturn(buildSubmitterDepartmentTree());

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildSubmitterDeptBranchSnapshot("IN", List.of("3")));

        support.initializeRuntime(instance, Map.of("submitterDeptId", 15L));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-route-a", insertedTasks.get(0).getNodeKey());
    }

    @Test
    void submitterDeptBranchMatchesParentDepartment() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "approver-A")));
        when(systemDepartmentMapper.selectList(any())).thenReturn(buildSubmitterDepartmentTree());

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildSubmitterDeptBranchSnapshot("IN", List.of("7")));

        support.initializeRuntime(instance, Map.of("submitterDeptId", 15L));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-route-a", insertedTasks.get(0).getNodeKey());
    }

    @Test
    void submitterDeptBranchSkipsUnrelatedDepartment() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "approver-A")));
        when(systemDepartmentMapper.selectList(any())).thenReturn(buildSubmitterDepartmentTree());

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildSubmitterDeptBranchSnapshot("IN", List.of("99")));

        support.initializeRuntime(instance, Map.of("submitterDeptId", 15L));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-route-b", insertedTasks.get(0).getNodeKey());
    }

    @Test
    void submitterDeptNotInDoesNotMatchAncestorDepartment() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "approver-A")));
        when(systemDepartmentMapper.selectList(any())).thenReturn(buildSubmitterDepartmentTree());

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildSubmitterDeptBranchSnapshot("NOT_IN", List.of("3")));

        support.initializeRuntime(instance, Map.of("submitterDeptId", 15L));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-route-b", insertedTasks.get(0).getNodeKey());
    }

    @Test
    void paymentCompanyBranchMatchesConfiguredCompany() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "approver-A")));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildBranchSnapshotForField("paymentCompanyId", "IN", List.of("COMPANY_A")));

        support.initializeRuntime(instance, Map.of("paymentCompanyId", "COMPANY_A"));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-route-a", insertedTasks.get(0).getNodeKey());
    }

    @Test
    void markPaymentStartedAllowsNullBankFieldsForExportFlow() {
        List<ProcessDocumentActionLog> insertedLogs = new ArrayList<>();
        doAnswer(invocation -> {
            ProcessDocumentActionLog log = invocation.getArgument(0);
            insertedLogs.add(log);
            return 1;
        }).when(processDocumentActionLogMapper).insert(any(ProcessDocumentActionLog.class));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-EXPORT-001");
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(99L);
        task.setDocumentCode("DOC-EXPORT-001");
        task.setNodeKey("payment-node");
        task.setNodeName("付款节点");

        support.markPaymentStarted(instance, task, 1L, "tester", false, null, null, null);

        assertEquals("PAYING", instance.getStatus());
        assertEquals(1, insertedLogs.size());
        assertNotNull(insertedLogs.get(0).getPayloadJson());
        assertTrue(insertedLogs.get(0).getPayloadJson().contains("\"taskId\":99"));
        assertFalse(insertedLogs.get(0).getPayloadJson().contains("companyBankAccountId"));
    }

    @Test
    void defaultRouteFallsBackOnlyAfterConfiguredConditionsDoNotMatch() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "approver-A")));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildDefaultRouteSnapshotForField("paymentCompanyId", "IN", List.of("COMPANY_A")));

        support.initializeRuntime(instance, Map.of("paymentCompanyId", "COMPANY_A"));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-route-a", insertedTasks.get(0).getNodeKey());
    }

    @Test
    void defaultRouteHandlesTheBranchWhenConfiguredConditionsDoNotMatch() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "approver-A")));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildDefaultRouteSnapshotForField("paymentCompanyId", "IN", List.of("COMPANY_A")));

        support.initializeRuntime(instance, Map.of("paymentCompanyId", "COMPANY_B"));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-route-b", insertedTasks.get(0).getNodeKey());
    }

    @Test
    void undertakeDeptExactBranchMatchesOnlyExactDepartment() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "approver-A")));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildBranchSnapshotForField("undertakeDeptIdExact", "IN", List.of("15")));

        support.initializeRuntime(instance, Map.of("undertakeDeptIdExact", List.of("15")));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-route-a", insertedTasks.get(0).getNodeKey());
    }

    @Test
    void undertakeDeptWithChildrenBranchMatchesAncestorDepartment() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "approver-A")));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildBranchSnapshotForField("undertakeDeptIdWithChildren", "IN", List.of("3")));

        support.initializeRuntime(instance, Map.of("undertakeDeptIdWithChildren", List.of(15L, 7L, 3L)));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-route-a", insertedTasks.get(0).getNodeKey());
    }
    @Test
    void undertakeDepartmentManagerResubmitPathCreatesApprovalTaskWhenLeaderExists() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(systemDepartmentMapper.selectList(any())).thenReturn(buildUndertakeDepartmentTree());
        when(userMapper.selectById(501L)).thenReturn(createActiveUser(501L, "dept-leader"));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildUndertakeDeptManagerSnapshot("BLOCK_SUBMIT"));

        support.initializeRuntime(instance, Map.of("undertakeDeptIds", List.of(15L)));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-manager", insertedTasks.get(0).getNodeKey());
        assertEquals(501L, insertedTasks.get(0).getAssigneeUserId());
    }

    @Test
    void undertakeDepartmentManagerResubmitPathReturnsReadableChineseErrorWhenLeaderMissing() throws Exception {
        when(systemDepartmentMapper.selectList(any())).thenReturn(buildSubmitterDepartmentTree());

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildUndertakeDeptManagerSnapshot("BLOCK_SUBMIT"));

        IllegalStateException error = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> support.initializeRuntime(instance, Map.of("undertakeDeptIds", List.of(15L)))
        );

        assertEquals("\u8282\u70b9\u3010\u9886\u5bfc\u5ba1\u6279\u3011\u627e\u4e0d\u5230\u5ba1\u6279\u4eba\uff0c\u5f53\u524d\u914d\u7f6e\u4e0d\u5141\u8bb8\u63d0\u4ea4", error.getMessage());
    }

    @Test
    void undertakeDepartmentMultiLevelManagerCreatesAndSignTasksForEachResolvedLevel() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(systemDepartmentMapper.selectList(any())).thenReturn(buildUndertakeDepartmentTree());
        when(userMapper.selectById(501L)).thenReturn(createActiveUser(501L, "leader-l1"));
        when(userMapper.selectById(701L)).thenReturn(createActiveUser(701L, "leader-l2"));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildUndertakeDeptManagerSnapshot("BLOCK_SUBMIT", 2, "OR_SIGN"));

        support.initializeRuntime(instance, Map.of("undertakeDeptIds", List.of(15L)));

        assertEquals(2, insertedTasks.size());
        assertEquals(List.of(501L, 701L), insertedTasks.stream().map(ProcessDocumentTask::getAssigneeUserId).toList());
        assertTrue(insertedTasks.stream().allMatch(item -> "AND_SIGN".equals(item.getApprovalMode())));
    }

    @Test
    void undertakeDepartmentMultiLevelManagerFailsWhenAnyRequiredLevelIsMissing() throws Exception {
        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of(
                buildDepartment(3L, "root-dept", null, null),
                buildDepartment(7L, "parent-dept", 3L, null),
                buildDepartment(15L, "undertake-dept", 7L, 501L)
        ));
        when(userMapper.selectById(501L)).thenReturn(createActiveUser(501L, "leader-l1"));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildUndertakeDeptManagerSnapshot("BLOCK_SUBMIT", 2, "OR_SIGN"));

        IllegalStateException error = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> support.initializeRuntime(instance, Map.of("undertakeDeptIds", List.of(15L)))
        );

        assertEquals("\u8282\u70b9\u3010\u9886\u5bfc\u5ba1\u6279\u3011\u627e\u4e0d\u5230\u5ba1\u6279\u4eba\uff0c\u5f53\u524d\u914d\u7f6e\u4e0d\u5141\u8bb8\u63d0\u4ea4", error.getMessage());
    }

    @Test
    void autoPassApprovedBeforeIgnoresPreviousRoundsWithoutCurrentRoundUpstreamApproval() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        mockTaskInsertions(insertedTasks);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "approver-A")));
        when(processDocumentActionLogMapper.selectList(any())).thenReturn(List.of(
                actionLog("SUBMIT", null, null),
                actionLog("APPROVE", "approval-start", 101L),
                actionLog("RESUBMIT", null, null)
        ));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildAutoPassSnapshot());

        support.initializeRuntime(instance, Map.of("resumeNodeKey", "approval-review"));

        assertEquals(1, insertedTasks.size());
        assertEquals("approval-review", insertedTasks.get(0).getNodeKey());
        assertEquals(101L, insertedTasks.get(0).getAssigneeUserId());
    }

    @Test
    void autoPassApprovedBeforeStillWorksForCurrentRoundUpstreamApproval() throws Exception {
        List<ProcessDocumentTask> insertedTasks = new ArrayList<>();
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createActiveUser(101L, "approver-A")));
        when(processDocumentActionLogMapper.selectList(any())).thenReturn(List.of(
                actionLog("RESUBMIT", null, null),
                actionLog("APPROVE", "approval-start", 101L)
        ));

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildAutoPassSnapshot());

        support.initializeRuntime(instance, Map.of("resumeNodeKey", "approval-review"));

        assertEquals("COMPLETED", instance.getStatus());
        assertTrue(insertedTasks.isEmpty());
    }

    @Test
    void rejectToAnyNodeRequiresSpecialSettingOnCurrentNode() throws Exception {

        ExpenseWorkflowRuntimeSupport support = newSupport();
        ProcessDocumentInstance instance = createRuntimeInstance(buildRejectWithoutAnyNodeSnapshot());
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(10L);
        task.setDocumentCode("DOC-001");
        task.setNodeKey("approval-review");
        task.setNodeName("review-node");
        task.setStatus("PENDING");

        IllegalStateException error = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> support.rejectPendingTask(instance, task, 101L, "approver-A", "agree", "approval-start")
        );

        assertEquals("\u5f53\u524d\u5ba1\u6279\u8282\u70b9\u672a\u5f00\u542f\u9a73\u56de\u81f3\u4efb\u610f\u8282\u70b9", error.getMessage());
    }

    private ExpenseWorkflowRuntimeSupport newSupport() {
        return new ExpenseWorkflowRuntimeSupport(
                processDocumentInstanceMapper,
                processDocumentTaskMapper,
                processDocumentActionLogMapper,
                processDocumentExpenseDetailMapper,
                processUserGroupMapper,
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
                "routeName", "route-a",
                "priority", 1,
                "attachBelowNodes", branchHasAttachedRoute,
                "conditionGroups", List.of()
        )));
        routes.add(new LinkedHashMap<>(Map.of(
                "routeKey", "route-b",
                "sourceNodeKey", "branch-1",
                "routeName", "route-b",
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
                                "nodeName", "branch-node",
                                "displayOrder", 1,
                                "config", Map.of()
                        ),
                        Map.of(
                                "nodeKey", "approval-route-a",
                                "nodeType", "APPROVAL",
                                "nodeName", "branch-route-a",
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
                                "nodeName", "branch-route-b",
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
                                "nodeName", "branch-tail",
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

    private String buildManualSelectApprovalSnapshot() throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "approval-manual",
                                "nodeType", "APPROVAL",
                                "nodeName", "manual-select-node",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "MANUAL_SELECT",
                                        "missingHandler", "BLOCK_SUBMIT",
                                        "approvalMode", "OR_SIGN"
                                )
                        )
                ),
                "routes", List.of()
        ));
    }

    private String buildManualSelectPaymentSnapshot() throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "payment-manual",
                                "nodeType", "PAYMENT",
                                "nodeName", "payment-manual-node",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "MANUAL_SELECT",
                                        "missingHandler", "BLOCK_SUBMIT",
                                        "approvalMode", "OR_SIGN",
                                        "manualSelectConfig", Map.of("candidateScope", "ALL_ACTIVE_USERS"),
                                        "paymentAction", "GENERATE_PAYMENT"
                                )
                        )
                ),
                "routes", List.of()
        ));
    }

    private String buildManualSelectCcSnapshot() throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "cc-manual",
                                "nodeType", "CC",
                                "nodeName", "cc-manual-node",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "MANUAL_SELECT",
                                        "missingHandler", "BLOCK_SUBMIT",
                                        "manualSelectConfig", Map.of("candidateScope", "ALL_ACTIVE_USERS"),
                                        "timing", "ON_ENTER"
                                )
                        )
                ),
                "routes", List.of()
        ));
    }

    private String buildSubmitterPaymentSnapshot() throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "payment-1",
                                "nodeType", "PAYMENT",
                                "nodeName", "payment-node",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "executorType", "SUBMITTER",
                                        "missingHandler", "AUTO_TRANSFER"
                                )
                        )
                ),
                "routes", List.of()
        ));
    }

    private String buildUserGroupPaymentSnapshot() throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "payment-user-group",
                                "nodeType", "PAYMENT",
                                "nodeName", "payment-user-group-node",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "DESIGNATED_USER_GROUP",
                                        "designatedUserGroupConfig", Map.of("groupId", 200L),
                                        "missingHandler", "BLOCK_SUBMIT",
                                        "approvalMode", "OR_SIGN",
                                        "paymentAction", "GENERATE_PAYMENT"
                                )
                        )
                ),
                "routes", List.of()
        ));
    }

    private String buildUserGroupApprovalSnapshot() throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "approval-user-group",
                                "nodeType", "APPROVAL",
                                "nodeName", "user-group-node",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "DESIGNATED_USER_GROUP",
                                        "designatedUserGroupConfig", Map.of("groupId", 200L),
                                        "missingHandler", "BLOCK_SUBMIT",
                                        "approvalMode", "OR_SIGN"
                                )
                        )
                ),
                "routes", List.of()
        ));
    }

    private String buildSubmitterDeptBranchSnapshot(String operator, Object compareValue) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProcessFlowConditionDTO routeCondition = new ProcessFlowConditionDTO();
        routeCondition.setFieldKey("submitterDeptId");
        routeCondition.setOperator(operator);
        routeCondition.setCompareValue(compareValue);

        ProcessFlowConditionGroupDTO routeGroup = new ProcessFlowConditionGroupDTO();
        routeGroup.setGroupNo(1);
        routeGroup.setConditions(List.of(routeCondition));

        return mapper.writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "branch-1",
                                "nodeType", "BRANCH",
                                "nodeName", "cc-node",
                                "displayOrder", 1,
                                "config", Map.of()
                        ),
                        Map.of(
                                "nodeKey", "approval-route-a",
                                "nodeType", "APPROVAL",
                                "nodeName", "cc-route-a",
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
                                "nodeName", "cc-route-b",
                                "parentNodeKey", "route-b",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "DESIGNATED_MEMBER",
                                        "designatedMemberConfig", Map.of("userIds", List.of(101L)),
                                        "missingHandler", "AUTO_SKIP",
                                        "approvalMode", "OR_SIGN"
                                )
                        )
                ),
                "routes", List.of(
                        Map.of(
                                "routeKey", "route-a",
                                "sourceNodeKey", "branch-1",
                                "routeName", "route-a",
                                "priority", 1,
                                "attachBelowNodes", false,
                                "conditionGroups", List.of(routeGroup)
                        ),
                        Map.of(
                                "routeKey", "route-b",
                                "sourceNodeKey", "branch-1",
                                "routeName", "route-b",
                                "priority", 2,
                                "attachBelowNodes", false,
                                "conditionGroups", List.of()
                        )
                )
        ));
    }

    private String buildSubmitterDesignatedApprovalSnapshot() throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "approval-submitter",
                                "nodeType", "APPROVAL",
                                "nodeName", "submitter-approval",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "DESIGNATED_MEMBER",
                                        "designatedMemberConfig", Map.of("userIds", List.of("SUBMITTER")),
                                        "missingHandler", "AUTO_SKIP",
                                        "approvalMode", "OR_SIGN"
                                )
                        )
                ),
                "routes", List.of()
        ));
    }

    private String buildBranchSnapshotForField(String fieldKey, String operator, Object compareValue) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProcessFlowConditionDTO routeCondition = new ProcessFlowConditionDTO();
        routeCondition.setFieldKey(fieldKey);
        routeCondition.setOperator(operator);
        routeCondition.setCompareValue(compareValue);

        ProcessFlowConditionGroupDTO routeGroup = new ProcessFlowConditionGroupDTO();
        routeGroup.setGroupNo(1);
        routeGroup.setConditions(List.of(routeCondition));

        return mapper.writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "branch-1",
                                "nodeType", "BRANCH",
                                "nodeName", "branch-node",
                                "displayOrder", 1,
                                "config", Map.of()
                        ),
                        Map.of(
                                "nodeKey", "approval-route-a",
                                "nodeType", "APPROVAL",
                                "nodeName", "route-a-approval",
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
                                "nodeName", "route-b-approval",
                                "parentNodeKey", "route-b",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "DESIGNATED_MEMBER",
                                        "designatedMemberConfig", Map.of("userIds", List.of(101L)),
                                        "missingHandler", "AUTO_SKIP",
                                        "approvalMode", "OR_SIGN"
                                )
                        )
                ),
                "routes", List.of(
                        Map.of(
                                "routeKey", "route-a",
                                "sourceNodeKey", "branch-1",
                                "routeName", "route-a",
                                "priority", 1,
                                "attachBelowNodes", false,
                                "conditionGroups", List.of(routeGroup)
                        ),
                        Map.of(
                                "routeKey", "route-b",
                                "sourceNodeKey", "branch-1",
                                "routeName", "route-b",
                                "priority", 2,
                                "attachBelowNodes", false,
                                "conditionGroups", List.of()
                        )
                )
        ));
    }

    private String buildDefaultRouteSnapshotForField(String fieldKey, String operator, Object compareValue) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProcessFlowConditionDTO routeCondition = new ProcessFlowConditionDTO();
        routeCondition.setFieldKey(fieldKey);
        routeCondition.setOperator(operator);
        routeCondition.setCompareValue(compareValue);

        ProcessFlowConditionGroupDTO routeGroup = new ProcessFlowConditionGroupDTO();
        routeGroup.setGroupNo(1);
        routeGroup.setConditions(List.of(routeCondition));

        return mapper.writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "branch-1",
                                "nodeType", "BRANCH",
                                "nodeName", "branch-node",
                                "displayOrder", 1,
                                "config", Map.of()
                        ),
                        Map.of(
                                "nodeKey", "approval-route-a",
                                "nodeType", "APPROVAL",
                                "nodeName", "route-a-approval",
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
                                "nodeName", "route-b-approval",
                                "parentNodeKey", "route-b",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "DESIGNATED_MEMBER",
                                        "designatedMemberConfig", Map.of("userIds", List.of(101L)),
                                        "missingHandler", "AUTO_SKIP",
                                        "approvalMode", "OR_SIGN"
                                )
                        )
                ),
                "routes", List.of(
                        Map.of(
                                "routeKey", "route-b",
                                "sourceNodeKey", "branch-1",
                                "routeName", "route-b",
                                "priority", 1,
                                "defaultRoute", true,
                                "attachBelowNodes", false,
                                "conditionGroups", List.of()
                        ),
                        Map.of(
                                "routeKey", "route-a",
                                "sourceNodeKey", "branch-1",
                                "routeName", "route-a",
                                "priority", 2,
                                "defaultRoute", false,
                                "attachBelowNodes", false,
                                "conditionGroups", List.of(routeGroup)
                        )
                )
        ));
    }

    private String buildUndertakeDeptManagerSnapshot(String missingHandler) throws Exception {
        return buildUndertakeDeptManagerSnapshot(missingHandler, 1, "OR_SIGN");
    }

    private String buildAutoPassSnapshot() throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "approval-start",
                                "nodeType", "APPROVAL",
                                "nodeName", "start-approval",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "DESIGNATED_MEMBER",
                                        "designatedMemberConfig", Map.of("userIds", List.of(101L)),
                                        "missingHandler", "AUTO_SKIP",
                                        "approvalMode", "OR_SIGN"
                                )
                        ),
                        Map.of(
                                "nodeKey", "approval-review",
                                "nodeType", "APPROVAL",
                                "nodeName", "review-approval",
                                "displayOrder", 2,
                                "config", Map.of(
                                        "approverType", "DESIGNATED_MEMBER",
                                        "designatedMemberConfig", Map.of("userIds", List.of(101L)),
                                        "missingHandler", "AUTO_SKIP",
                                        "approvalMode", "OR_SIGN",
                                        "specialSettings", List.of("AUTO_PASS_IF_APPROVED_BEFORE")
                                )
                        )
                ),
                "routes", List.of()
        ));
    }

    private String buildRejectWithoutAnyNodeSnapshot() throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "approval-start",
                                "nodeType", "APPROVAL",
                                "nodeName", "start-approval",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "DESIGNATED_MEMBER",
                                        "designatedMemberConfig", Map.of("userIds", List.of(101L)),
                                        "missingHandler", "AUTO_SKIP",
                                        "approvalMode", "OR_SIGN"
                                )
                        ),
                        Map.of(
                                "nodeKey", "approval-review",
                                "nodeType", "APPROVAL",
                                "nodeName", "review-approval",
                                "displayOrder", 2,
                                "config", Map.of(
                                        "approverType", "DESIGNATED_MEMBER",
                                        "designatedMemberConfig", Map.of("userIds", List.of(101L)),
                                        "missingHandler", "AUTO_SKIP",
                                        "approvalMode", "OR_SIGN"
                                )
                        )
                ),
                "routes", List.of()
        ));
    }

    private String buildUndertakeDeptManagerSnapshot(String missingHandler, int managerLevel, String approvalMode) throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "approval-manager",
                                "nodeType", "APPROVAL",
                                "nodeName", "\u9886\u5bfc\u5ba1\u6279",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "MANAGER",
                                        "missingHandler", missingHandler,
                                        "approvalMode", approvalMode,
                                        "managerConfig", Map.of(
                                                "deptSource", "UNDERTAKE_DEPT",
                                                "managerLevel", managerLevel,
                                                "orgTreeLookupEnabled", true,
                                                "orgTreeLookupLevel", 1
                                        )
                                )
                        )
                ),
                "routes", List.of()
        ));
    }

    private List<SystemDepartment> buildUndertakeDepartmentTree() {
        return List.of(
                buildDepartment(3L, "root-dept", null, 801L),
                buildDepartment(7L, "parent-dept", 3L, 701L),
                buildDepartment(15L, "undertake-dept", 7L, 501L)
        );
    }
    private List<SystemDepartment> buildSubmitterDepartmentTree() {
        return List.of(
                buildDepartment(3L, "submit-root", null),
                buildDepartment(7L, "submit-parent", 3L),
                buildDepartment(15L, "submit-dept", 7L)
        );
    }

    private SystemDepartment buildDepartment(Long id, String name, Long parentId) {
        return buildDepartment(id, name, parentId, null);
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

    private ProcessDocumentActionLog actionLog(String actionType, String nodeKey, Long actorUserId) {
        ProcessDocumentActionLog log = new ProcessDocumentActionLog();
        log.setActionType(actionType);
        log.setNodeKey(nodeKey);
        log.setActorUserId(actorUserId);
        return log;
    }
}


