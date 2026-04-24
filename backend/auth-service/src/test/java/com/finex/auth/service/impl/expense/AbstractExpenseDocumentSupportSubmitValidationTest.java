package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.mapper.ProcessFlowMapper;
import com.finex.auth.mapper.ProcessFlowVersionMapper;
import com.finex.auth.service.impl.ExpenseDetailSystemFieldSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractExpenseDocumentSupportSubmitValidationTest {

    @Mock
    private ProcessFlowMapper processFlowMapper;

    @Mock
    private ProcessFlowVersionMapper processFlowVersionMapper;

    @Mock
    private ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;

    @Mock
    private ExpenseDetailSystemFieldSupport expenseDetailSystemFieldSupport;

    @Mock
    private ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;

    private AbstractExpenseDocumentSupport support;

    @BeforeEach
    void setUp() {
        support = mock(AbstractExpenseDocumentSupport.class, Answers.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(support, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(support, "processFlowMapper", processFlowMapper);
        ReflectionTestUtils.setField(support, "processFlowVersionMapper", processFlowVersionMapper);
        ReflectionTestUtils.setField(support, "expenseWorkflowRuntimeSupport", expenseWorkflowRuntimeSupport);
        ReflectionTestUtils.setField(support, "expenseDetailSystemFieldSupport", expenseDetailSystemFieldSupport);
        ReflectionTestUtils.setField(support, "processDocumentExpenseDetailMapper", processDocumentExpenseDetailMapper);
    }

    @Test
    void validateRuntimeRequiredFieldsUsesReadableChinesePrompt() {
        Map<String, Object> schema = Map.of(
                "layoutMode", "TWO_COLUMN",
                "blocks", List.of(Map.of(
                        "kind", "CONTROL",
                        "fieldKey", "counterpartyName",
                        "label", "\u6536\u6b3e\u5355\u4f4d",
                        "required", true,
                        "props", Map.of("controlType", "TEXT")
                ))
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        support,
                        "validateRuntimeRequiredFields",
                        schema,
                        Collections.emptyMap(),
                        null,
                        null,
                        null
                )
        );

        assertEquals("\u8bf7\u5148\u586b\u5199\u3010\u6536\u6b3e\u5355\u4f4d\u3011", error.getMessage());
    }

    @Test
    void validateExpenseDetailRequiredFieldsUsesReadableChinesePrompt() {
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateType("report");

        ProcessExpenseDetailDesign expenseDetailDesign = new ProcessExpenseDetailDesign();
        expenseDetailDesign.setSchemaJson("{}");
        expenseDetailDesign.setDetailType("COMMON");

        when(expenseDetailSystemFieldSupport.readSchema(anyString(), anyString())).thenReturn(Map.of(
                "layoutMode", "TWO_COLUMN",
                "blocks", List.of(Map.of(
                        "kind", "CONTROL",
                        "fieldKey", "detailReason",
                        "label", "\u8d39\u7528\u8bf4\u660e",
                        "required", true,
                        "props", Map.of("controlType", "TEXT")
                ))
        ));

        ExpenseDetailInstanceDTO detail = new ExpenseDetailInstanceDTO();
        detail.setFormData(Collections.emptyMap());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        support,
                        "validateExpenseDetailRequiredFields",
                        template,
                        expenseDetailDesign,
                        List.of(detail)
                )
        );

        assertEquals("\u8bf7\u5148\u5b8c\u5584\u8d39\u7528\u660e\u7ec6\u201c\u8d39\u7528\u660e\u7ec6 1\u201d\uff1a\u8bf7\u5148\u586b\u5199\u3010\u8d39\u7528\u8bf4\u660e\u3011", error.getMessage());
    }

    @Test
    void validateFlowSnapshotForSubmitReportsMissingFlowInChinese() {
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setApprovalFlow("FLOW-001");
        when(processFlowMapper.selectOne(any())).thenReturn(null);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(support, "validateFlowSnapshotForSubmit", template)
        );

        assertEquals("\u5f53\u524d\u6a21\u677f\u7ed1\u5b9a\u7684\u5ba1\u6279\u6d41\u7a0b\u4e0d\u5b58\u5728\uff0c\u8bf7\u5148\u8865\u9f50\u914d\u7f6e\u540e\u518d\u63d0\u4ea4", error.getMessage());
    }

    @Test
    void validateSubmitContextAllowsCompleteDraftPayload() {
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateType("contract");
        template.setFormDesignCode("FORM-001");

        ProcessFormDesign formDesign = new ProcessFormDesign();
        formDesign.setSchemaJson("""
                {"layoutMode":"TWO_COLUMN","blocks":[
                  {"kind":"CONTROL","fieldKey":"counterpartyName","label":"\u6536\u6b3e\u5355\u4f4d","required":true,"props":{"controlType":"TEXT"}}
                ]}
                """);

        String snapshotJson = assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                support,
                "validateSubmitContext",
                template,
                formDesign,
                null,
                Map.of("counterpartyName", "\u4f9b\u5e94\u5546A"),
                Collections.emptyList()
        ));

        assertNull(snapshotJson);
    }

    @Test
    void saveExpenseDetailInstancesRejectsInvalidPrepayAmountRelation() {
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateCode("TPL-001");
        template.setTemplateType("report");
        template.setExpenseDetailModeDefault("PREPAY_UNBILLED");

        ProcessExpenseDetailDesign expenseDetailDesign = new ProcessExpenseDetailDesign();
        expenseDetailDesign.setDetailCode("DETAIL-001");
        expenseDetailDesign.setDetailType("ENTERPRISE_TRANSACTION");
        expenseDetailDesign.setSchemaJson("{}");

        ExpenseDetailInstanceDTO detail = new ExpenseDetailInstanceDTO();
        detail.setDetailNo("D-001");
        detail.setDetailTitle("费用明细 1");
        detail.setFormData(Map.of(
                "businessScenario", "PREPAY_UNBILLED",
                "amount", "88.50",
                "actualPaymentAmount", "66.00"
        ));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        support,
                        "saveExpenseDetailInstances",
                        "DOC-001",
                        template,
                        expenseDetailDesign,
                        List.of(detail)
                )
        );

        assertEquals("请先完善费用明细“费用明细 1”：预付未到票场景下，【金额】必须等于【实际支付金额】", error.getMessage());
    }
}
