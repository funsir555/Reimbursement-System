package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentEditContextSupportTest {

    @Mock private ExpenseDocumentTemplateDetailSupport templateDetailSupport;
    @Mock private ExpenseDocumentReadSupport readSupport;
    @Mock private ProcessDocumentTaskMapper processDocumentTaskMapper;
    @Mock private ExpenseReadonlyPayeeAccountSnapshotEnhancer readonlyPayeeAccountSnapshotEnhancer;

    @Test
    void getDocumentEditContextBuildsFromTemplateAndReadSide() {
        ExpenseDocumentEditContextSupport support = newSupport();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-1");
        instance.setTemplateCode("TPL-1");
        instance.setStatus("DRAFT");
        instance.setFormDataJson("{}");
        ExpenseCreateTemplateDetailVO templateDetail = new ExpenseCreateTemplateDetailVO();
        templateDetail.setTemplateCode("TPL-1");
        templateDetail.setTemplateName("Travel");
        templateDetail.setTemplateType("report");
        templateDetail.setCurrentUserCompanyId("COMPANY_A");
        templateDetail.setCurrentUserCompanyName("Company A");
        ProcessDocumentExpenseDetail detail = new ProcessDocumentExpenseDetail();
        ExpenseDetailInstanceDTO runtimeDetail = new ExpenseDetailInstanceDTO();
        runtimeDetail.setDetailNo("D1");
        Map<String, Object> rawFormData = new LinkedHashMap<>(Map.of("reason", "trip"));
        Map<String, Object> enhancedFormData = new LinkedHashMap<>(rawFormData);
        enhancedFormData.put("payeeAccount", Map.of("value", "VENDOR_ACCOUNT:1", "ownerName", "Vendor Owner"));
        when(readSupport.requireDocument("DOC-1")).thenReturn(instance);
        when(templateDetailSupport.getDocumentTemplateDetail(1L, "TPL-1")).thenReturn(templateDetail);
        when(readSupport.readFormData("{}")).thenReturn(rawFormData);
        when(readonlyPayeeAccountSnapshotEnhancer.enhanceFormData(any(), any(), any())).thenReturn(enhancedFormData);
        when(readSupport.loadExpenseDetails("DOC-1")).thenReturn(List.of(detail));
        when(readSupport.toRuntimeExpenseDetailDTO(detail)).thenReturn(runtimeDetail);

        ExpenseDocumentEditContextVO actual = support.getDocumentEditContext(1L, "DOC-1");

        assertEquals("RESUBMIT", actual.getEditMode());
        assertEquals("DOC-1", actual.getDocumentCode());
        assertEquals("TPL-1", actual.getTemplateCode());
        assertEquals("Travel", actual.getTemplateName());
        assertEquals("COMPANY_A", actual.getCurrentUserCompanyId());
        assertEquals("Company A", actual.getCurrentUserCompanyName());
        assertEquals("trip", actual.getFormData().get("reason"));
        assertEquals("Vendor Owner", ((Map<?, ?>) actual.getFormData().get("payeeAccount")).get("ownerName"));
        assertEquals(1, actual.getExpenseDetails().size());
        assertSame(runtimeDetail, actual.getExpenseDetails().get(0));
        verify(readSupport).requireSubmitter(instance, 1L);
        verify(readonlyPayeeAccountSnapshotEnhancer).enhanceFormData(actual.getSchema(), rawFormData, "COMPANY_A");
    }

    @Test
    void getDocumentEditContextRejectsNonEditableStatuses() {
        ExpenseDocumentEditContextSupport support = newSupport();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-9");
        instance.setTemplateCode("TPL-9");
        instance.setStatus("PENDING_APPROVAL");
        when(readSupport.requireDocument("DOC-9")).thenReturn(instance);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> support.getDocumentEditContext(1L, "DOC-9")
        );

        assertEquals("\u5f53\u524d\u5355\u636e\u4e0d\u662f\u53ef\u7f16\u8f91\u72b6\u6001", error.getMessage());
        verify(readSupport).requireSubmitter(instance, 1L);
    }

    @Test
    void buildEditContextLoadsTaskLevelModifyCapabilitiesFromFlowSnapshot() throws Exception {
        ExpenseDocumentEditContextSupport support = newSupport();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-2");
        instance.setTemplateCode("TPL-2");
        instance.setFormDataJson("{}");
        instance.setFlowSnapshotJson(new ObjectMapper().writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "finance",
                                "nodeType", "APPROVAL",
                                "config", Map.of("specialSettings", List.of("ALLOW_EDIT_FORM_MODULE"))
                        )
                ),
                "routes", List.of()
        )));
        ExpenseCreateTemplateDetailVO templateDetail = new ExpenseCreateTemplateDetailVO();
        templateDetail.setTemplateCode("TPL-2");
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(10L);
        task.setDocumentCode("DOC-2");
        task.setNodeKey("finance");
        task.setNodeType("APPROVAL");
        task.setStatus("PENDING");
        when(templateDetailSupport.getDocumentTemplateDetail(1L, "TPL-2")).thenReturn(templateDetail);
        when(readSupport.readFormData("{}")).thenReturn(Map.of());
        when(readonlyPayeeAccountSnapshotEnhancer.enhanceFormData(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        when(readSupport.loadExpenseDetails("DOC-2")).thenReturn(List.of());
        when(processDocumentTaskMapper.selectOne(any())).thenReturn(task);

        ExpenseDocumentEditContextVO actual = support.buildEditContext(1L, instance, 10L, "MODIFY");

        assertEquals("finance", actual.getTaskNodeKey());
        assertEquals(Boolean.TRUE, actual.getAllowEditFormModule());
        assertEquals(Boolean.FALSE, actual.getAllowEditPayAccount());
    }

    private ExpenseDocumentEditContextSupport newSupport() {
        return new ExpenseDocumentEditContextSupport(
                templateDetailSupport,
                readSupport,
                processDocumentTaskMapper,
                readonlyPayeeAccountSnapshotEnhancer,
                new ObjectMapper()
        );
    }
}
