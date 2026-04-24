package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseApprovalDomainSupportTest {

    @Mock
    private ExpenseDocumentReadSupport expenseDocumentReadSupport;
    @Mock
    private ExpenseDocumentActionLogSupport expenseDocumentActionLogSupport;
    @Mock
    private ExpenseDocumentMutationDomainSupport expenseDocumentMutationDomainSupport;
    @Mock
    private ExpenseDocumentTemplateSupport expenseDocumentTemplateSupport;
    @Mock
    private ExpenseSummaryAssembler expenseSummaryAssembler;
    @Mock
    private ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;
    @Mock
    private ExpenseRelationWriteOffService expenseRelationWriteOffService;
    @Mock
    private ProcessDocumentTaskMapper processDocumentTaskMapper;
    @Mock
    private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SystemDepartmentMapper systemDepartmentMapper;

    @Test
    void listPendingApprovalsUsesSummaryAssembler() {
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(10L);
        task.setDocumentCode("DOC-001");
        task.setNodeType("APPROVAL");
        task.setStatus("PENDING");
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");
        List<ExpenseApprovalPendingItemVO> expected = List.of(new ExpenseApprovalPendingItemVO());
        ExpenseApprovalDomainSupport support = newSupport();
        when(processDocumentTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(instance));
        when(expenseSummaryAssembler.toPendingItems(anyList(), anyMap())).thenReturn(expected);

        List<ExpenseApprovalPendingItemVO> actual = support.listPendingApprovals(1L);

        assertSame(expected, actual);
        verify(expenseSummaryAssembler).toPendingItems(anyList(), anyMap());
    }

    @Test
    void getTaskModifyContextBuildsViaTemplateSupport() {
        ProcessDocumentTask task = pendingTask();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");
        ExpenseDocumentEditContextVO expected = new ExpenseDocumentEditContextVO();
        expected.setAllowEditFormModule(Boolean.TRUE);
        ExpenseApprovalDomainSupport support = newSupport();
        when(processDocumentTaskMapper.selectById(10L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-001")).thenReturn(instance);
        when(expenseDocumentTemplateSupport.buildEditContext(1L, instance, 10L, "MODIFY")).thenReturn(expected);

        ExpenseDocumentEditContextVO actual = support.getTaskModifyContext(1L, 10L);

        assertSame(expected, actual);
    }

    @Test
    void rejectTaskUsesRuntimeOwnerAndWriteOffOwner() {
        ProcessDocumentTask task = pendingTask();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        ExpenseApprovalActionDTO dto = new ExpenseApprovalActionDTO();
        dto.setTargetNodeKey("NODE-2");
        ExpenseApprovalDomainSupport support = newSupport();
        when(processDocumentTaskMapper.selectById(10L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-001")).thenReturn(instance, instance);
        when(expenseDocumentReadSupport.buildDocumentDetail(instance)).thenReturn(detail);

        ExpenseDocumentDetailVO actual = support.rejectTask(1L, "测试用户", 10L, dto);

        assertSame(detail, actual);
        verify(expenseWorkflowRuntimeSupport).rejectPendingTask(any(), any(), any(), any(), any(), eq("NODE-2"));
        verify(expenseRelationWriteOffService).voidPendingWriteOffs("DOC-001");
    }

    @Test
    void getTaskModifyContextRejectsWhenNodeHasNoEditPermission() {
        ProcessDocumentTask task = pendingTask();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");
        ExpenseDocumentEditContextVO context = new ExpenseDocumentEditContextVO();
        context.setAllowEditFormModule(Boolean.FALSE);
        context.setAllowEditPayAccount(Boolean.FALSE);
        ExpenseApprovalDomainSupport support = newSupport();
        when(processDocumentTaskMapper.selectById(10L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-001")).thenReturn(instance);
        when(expenseDocumentTemplateSupport.buildEditContext(1L, instance, 10L, "MODIFY")).thenReturn(context);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> support.getTaskModifyContext(1L, 10L));

        assertEquals("\u5f53\u524d\u5ba1\u6279\u8282\u70b9\u672a\u5f00\u542f\u5355\u636e\u4fee\u6539\u6743\u9650", error.getMessage());
    }

    @Test
    void modifyTaskDocumentAllowsPayeeAccountOnlyChangeWhenSpecialSettingEnabled() {
        ProcessDocumentTask task = pendingTask();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");
        instance.setFlowSnapshotJson("{\"routes\":[]}");

        ExpenseDocumentEditContextVO context = new ExpenseDocumentEditContextVO();
        context.setAllowEditPayAccount(Boolean.TRUE);
        context.setSchema(schema(
                block("paymentCompany", "BUSINESS_COMPONENT", "payment-company", "READONLY"),
                block("payeeAccount", "BUSINESS_COMPONENT", "payee-account", "READONLY")
        ));
        context.setFormData(mapOf(
                "paymentCompany", lookup("COMP-A"),
                "payeeAccount", lookup("ACC-OLD")
        ));

        ExpenseDocumentUpdateDTO dto = new ExpenseDocumentUpdateDTO();
        AbstractExpenseDocumentSupport.DocumentMutationContext mutation = createMutation(mapOf(
                "paymentCompany", lookup("COMP-A"),
                "payeeAccount", lookup("ACC-NEW")
        ), List.of());
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();

        ExpenseApprovalDomainSupport support = newSupport();
        when(processDocumentTaskMapper.selectById(10L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-001")).thenReturn(instance, instance);
        when(expenseDocumentTemplateSupport.buildEditContext(1L, instance, 10L, "MODIFY")).thenReturn(context);
        when(expenseDocumentMutationDomainSupport.buildMutationContext(instance, dto, false)).thenReturn(mutation);
        when(expenseWorkflowRuntimeSupport.buildRuntimeContextForInstance(instance)).thenReturn(Map.of());
        when(expenseWorkflowRuntimeSupport.buildRuntimeFlowContext(any(), any(), any(), anyMap(), any(), anyList())).thenReturn(Map.of());
        when(expenseDocumentReadSupport.buildDocumentDetail(instance)).thenReturn(detail);

        ExpenseDocumentDetailVO actual = support.modifyTaskDocument(1L, "测试用户", 10L, dto);

        assertSame(detail, actual);
        verify(expenseDocumentMutationDomainSupport).applyDocumentMutation(instance, mutation, false);
        verify(expenseRelationWriteOffService).syncDocumentBusinessRelations("DOC-001", mutation.formDesign(), mutation.formData());
    }

    @Test
    void modifyTaskDocumentRejectsNonPayeeChangeWhenOnlyPayAccountEnabled() {
        ProcessDocumentTask task = pendingTask();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");

        ExpenseDocumentEditContextVO context = new ExpenseDocumentEditContextVO();
        context.setAllowEditPayAccount(Boolean.TRUE);
        context.setSchema(schema(
                block("paymentCompany", "BUSINESS_COMPONENT", "payment-company", "READONLY"),
                block("payeeAccount", "BUSINESS_COMPONENT", "payee-account", "READONLY")
        ));
        context.setFormData(mapOf(
                "paymentCompany", lookup("COMP-A"),
                "payeeAccount", lookup("ACC-OLD")
        ));

        ExpenseDocumentUpdateDTO dto = new ExpenseDocumentUpdateDTO();
        AbstractExpenseDocumentSupport.DocumentMutationContext mutation = createMutation(mapOf(
                "paymentCompany", lookup("COMP-B"),
                "payeeAccount", lookup("ACC-OLD")
        ), List.of());

        ExpenseApprovalDomainSupport support = newSupport();
        when(processDocumentTaskMapper.selectById(10L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-001")).thenReturn(instance);
        when(expenseDocumentTemplateSupport.buildEditContext(1L, instance, 10L, "MODIFY")).thenReturn(context);
        when(expenseDocumentMutationDomainSupport.buildMutationContext(instance, dto, false)).thenReturn(mutation);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> support.modifyTaskDocument(1L, "测试用户", 10L, dto)
        );

        assertEquals("\u5f53\u524d\u5ba1\u6279\u8282\u70b9\u4ec5\u5141\u8bb8\u4fee\u6539\u6536\u6b3e\u8d26\u6237", error.getMessage());
        verify(expenseDocumentMutationDomainSupport, never()).applyDocumentMutation(any(), any(), eq(false));
    }

    @Test
    void modifyTaskDocumentRejectsPayeeAccountChangeWithoutDedicatedPermission() {
        ProcessDocumentTask task = pendingTask();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");

        ExpenseDocumentEditContextVO context = new ExpenseDocumentEditContextVO();
        context.setAllowEditFormModule(Boolean.TRUE);
        context.setSchema(schema(
                block("payeeAccount", "BUSINESS_COMPONENT", "payee-account", "EDITABLE")
        ));
        context.setFormData(mapOf("payeeAccount", lookup("ACC-OLD")));

        ExpenseDocumentUpdateDTO dto = new ExpenseDocumentUpdateDTO();
        AbstractExpenseDocumentSupport.DocumentMutationContext mutation = createMutation(mapOf(
                "payeeAccount", lookup("ACC-NEW")
        ), List.of());

        ExpenseApprovalDomainSupport support = newSupport();
        when(processDocumentTaskMapper.selectById(10L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-001")).thenReturn(instance);
        when(expenseDocumentTemplateSupport.buildEditContext(1L, instance, 10L, "MODIFY")).thenReturn(context);
        when(expenseDocumentMutationDomainSupport.buildMutationContext(instance, dto, false)).thenReturn(mutation);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> support.modifyTaskDocument(1L, "测试用户", 10L, dto)
        );

        assertEquals("\u5f53\u524d\u5ba1\u6279\u8282\u70b9\u4e0d\u5141\u8bb8\u4fee\u6539\u6240\u9009\u5b57\u6bb5\uff0c\u8bf7\u8c03\u6574\u540e\u91cd\u8bd5", error.getMessage());
        verify(expenseDocumentMutationDomainSupport, never()).applyDocumentMutation(any(), any(), eq(false));
    }

    @Test
    void modifyTaskDocumentRejectsWorkflowDrivingFieldChange() {
        ProcessDocumentTask task = pendingTask();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");
        instance.setFlowSnapshotJson("{\"routes\":[]}");

        ExpenseDocumentEditContextVO context = new ExpenseDocumentEditContextVO();
        context.setAllowEditFormModule(Boolean.TRUE);
        context.setSchema(schema(
                block("expenseTypeCode", "CONTROL", null, "EDITABLE")
        ));
        context.setFormData(mapOf("expenseTypeCode", "TRAVEL"));

        ExpenseDocumentUpdateDTO dto = new ExpenseDocumentUpdateDTO();
        AbstractExpenseDocumentSupport.DocumentMutationContext mutation = createMutation(mapOf(
                "expenseTypeCode", "MEETING"
        ), List.of());

        ExpenseApprovalDomainSupport support = newSupport();
        when(processDocumentTaskMapper.selectById(10L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-001")).thenReturn(instance);
        when(expenseDocumentTemplateSupport.buildEditContext(1L, instance, 10L, "MODIFY")).thenReturn(context);
        when(expenseDocumentMutationDomainSupport.buildMutationContext(instance, dto, false)).thenReturn(mutation);
        when(expenseWorkflowRuntimeSupport.buildRuntimeContextForInstance(instance)).thenReturn(Map.of("expenseTypeCode", "TRAVEL"));
        when(expenseWorkflowRuntimeSupport.buildRuntimeFlowContext(any(), any(), any(), anyMap(), any(), anyList()))
                .thenReturn(Map.of("expenseTypeCode", "MEETING"));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> support.modifyTaskDocument(1L, "测试用户", 10L, dto)
        );

        assertEquals("\u5f53\u524d\u5b57\u6bb5\u4f1a\u5f71\u54cd\u5ba1\u6279\u6d41\uff0c\u8bf7\u9a73\u56de\u540e\u4fee\u6539\u6216\u8054\u7cfb\u6d41\u7a0b\u7ba1\u7406\u5458", error.getMessage());
        verify(expenseDocumentMutationDomainSupport, never()).applyDocumentMutation(any(), any(), eq(false));
    }

    @Test
    void modifyTaskDocumentRejectsExpenseDetailChangesDuringApproval() {
        ProcessDocumentTask task = pendingTask();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");

        ExpenseDocumentEditContextVO context = new ExpenseDocumentEditContextVO();
        context.setAllowEditFormModule(Boolean.TRUE);
        context.setSchema(schema(block("remark", "CONTROL", null, "EDITABLE")));
        context.setFormData(mapOf("remark", "相同"));
        context.setExpenseDetails(List.of(expenseDetail("D1", "旧标题", mapOf("actualPaymentAmount", "100"))));

        ExpenseDocumentUpdateDTO dto = new ExpenseDocumentUpdateDTO();
        AbstractExpenseDocumentSupport.DocumentMutationContext mutation = createMutation(
                mapOf("remark", "相同"),
                List.of(expenseDetail("D1", "新标题", mapOf("actualPaymentAmount", "100")))
        );

        ExpenseApprovalDomainSupport support = newSupport();
        when(processDocumentTaskMapper.selectById(10L)).thenReturn(task);
        when(expenseDocumentReadSupport.requireDocument("DOC-001")).thenReturn(instance);
        when(expenseDocumentTemplateSupport.buildEditContext(1L, instance, 10L, "MODIFY")).thenReturn(context);
        when(expenseDocumentMutationDomainSupport.buildMutationContext(instance, dto, false)).thenReturn(mutation);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> support.modifyTaskDocument(1L, "测试用户", 10L, dto)
        );

        assertEquals("\u5f53\u524d\u5b57\u6bb5\u4f1a\u5f71\u54cd\u5ba1\u6279\u6d41\uff0c\u8bf7\u9a73\u56de\u540e\u4fee\u6539\u6216\u8054\u7cfb\u6d41\u7a0b\u7ba1\u7406\u5458", error.getMessage());
        verify(expenseDocumentMutationDomainSupport, never()).applyDocumentMutation(any(), any(), eq(false));
    }

    private ExpenseApprovalDomainSupport newSupport() {
        return new ExpenseApprovalDomainSupport(
                expenseDocumentReadSupport,
                expenseDocumentActionLogSupport,
                expenseDocumentMutationDomainSupport,
                expenseDocumentTemplateSupport,
                expenseSummaryAssembler,
                expenseWorkflowRuntimeSupport,
                expenseRelationWriteOffService,
                processDocumentTaskMapper,
                processDocumentInstanceMapper,
                userMapper,
                systemDepartmentMapper,
                new ObjectMapper()
        );
    }

    private ProcessDocumentTask pendingTask() {
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(10L);
        task.setDocumentCode("DOC-001");
        task.setAssigneeUserId(1L);
        task.setNodeType("APPROVAL");
        task.setStatus("PENDING");
        task.setNodeKey("NODE-001");
        task.setNodeName("审批");
        return task;
    }

    private AbstractExpenseDocumentSupport.DocumentMutationContext createMutation(
            Map<String, Object> formData,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateType("report");
        template.setCategoryCode("TRAVEL");
        ProcessFormDesign formDesign = new ProcessFormDesign();
        formDesign.setSchemaJson("{}");
        return new AbstractExpenseDocumentSupport.DocumentMutationContext(
                template,
                formDesign,
                null,
                formData,
                expenseDetails,
                null,
                Map.of(),
                "标题",
                "事由",
                null
        );
    }

    private Map<String, Object> schema(Map<String, Object>... blocks) {
        return mapOf("blocks", List.of(blocks));
    }

    private Map<String, Object> block(String fieldKey, String kind, String componentCode, String approvalPermission) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("fieldKey", fieldKey);
        block.put("kind", kind);
        if (componentCode != null) {
            block.put("props", mapOf("componentCode", componentCode));
        } else {
            block.put("props", mapOf());
        }
        block.put("permission", mapOf("fixedStages", mapOf("IN_APPROVAL", approvalPermission)));
        return block;
    }

    private ExpenseDetailInstanceDTO expenseDetail(String detailNo, String title, Map<String, Object> formData) {
        ExpenseDetailInstanceDTO detail = new ExpenseDetailInstanceDTO();
        detail.setDetailNo(detailNo);
        detail.setDetailTitle(title);
        detail.setFormData(formData);
        return detail;
    }

    private Map<String, Object> lookup(String value) {
        return mapOf("value", value, "label", value);
    }

    private final Map<String, Object> mapOf(Object... values) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }
}
