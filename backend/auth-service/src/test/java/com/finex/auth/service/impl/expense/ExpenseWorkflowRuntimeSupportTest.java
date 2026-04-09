package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
