package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ExpenseDetailSystemFieldSupportTest {

    @Mock
    private ProcessExpenseTypeMapper processExpenseTypeMapper;

    private ExpenseDetailSystemFieldSupport support;

    @BeforeEach
    void setUp() {
        support = new ExpenseDetailSystemFieldSupport(new ObjectMapper(), processExpenseTypeMapper);
        lenient().when(processExpenseTypeMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void normalizeSchemaBuildsReadableChineseLabelsAndPlaceholders() {
        Map<String, Object> schema = support.normalizeSchema(
                support.defaultSchema(),
                ExpenseDetailSystemFieldSupport.DETAIL_TYPE_ENTERPRISE
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) schema.get("blocks");
        assertEquals("\u8d39\u7528\u7c7b\u578b", blocks.get(0).get("label"));
        assertEquals("\u4e1a\u52a1\u573a\u666f", blocks.get(1).get("label"));
        assertEquals("\u91d1\u989d", blocks.get(2).get("label"));
        assertEquals("\u53d1\u7968\u91d1\u989d", blocks.get(3).get("label"));
        assertEquals("\u5b9e\u9645\u652f\u4ed8\u91d1\u989d", blocks.get(4).get("label"));
        assertEquals("\u53d1\u7968\u9644\u4ef6", blocks.get(5).get("label"));

        @SuppressWarnings("unchecked")
        Map<String, Object> expenseTypeProps = (Map<String, Object>) blocks.get(0).get("props");
        @SuppressWarnings("unchecked")
        Map<String, Object> scenarioProps = (Map<String, Object>) blocks.get(1).get("props");
        @SuppressWarnings("unchecked")
        Map<String, Object> detailAmountProps = (Map<String, Object>) blocks.get(2).get("props");
        @SuppressWarnings("unchecked")
        Map<String, Object> invoiceAmountProps = (Map<String, Object>) blocks.get(3).get("props");
        @SuppressWarnings("unchecked")
        Map<String, Object> invoiceAttachmentProps = (Map<String, Object>) blocks.get(5).get("props");

        assertEquals("\u8bf7\u9009\u62e9\u8d39\u7528\u7c7b\u578b", expenseTypeProps.get("placeholder"));
        assertEquals("\u8bf7\u9009\u62e9\u4e1a\u52a1\u573a\u666f", scenarioProps.get("placeholder"));
        assertEquals("\u8bf7\u8f93\u5165\u91d1\u989d", detailAmountProps.get("placeholder"));
        assertEquals(30, invoiceAttachmentProps.get("maxCount"));
        assertEquals(".pdf,.png,.jpg,.jpeg", invoiceAttachmentProps.get("accept"));
        assertEquals(List.of(ExpenseDetailSystemFieldSupport.MODE_PREPAY_UNBILLED), detailAmountProps.get("visibleSceneModes"));
        assertEquals(List.of(ExpenseDetailSystemFieldSupport.MODE_INVOICE_FULL_PAYMENT), invoiceAmountProps.get("visibleSceneModes"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scenarioOptions = (List<Map<String, Object>>) scenarioProps.get("options");
        assertEquals("\u5168\u989d\u4ed8\u6b3e", scenarioOptions.get(0).get("label"));
        assertEquals("\u9884\u4ed8\u672a\u5230\u7968", scenarioOptions.get(1).get("label"));
    }

    @Test
    void readSchemaThrowsReadableChineseMessageWhenJsonIsInvalid() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> support.readSchema("{bad-json", ExpenseDetailSystemFieldSupport.DETAIL_TYPE_NORMAL)
        );

        assertEquals("\u8bfb\u53d6\u8d39\u7528\u660e\u7ec6 schema \u5931\u8d25", error.getMessage());
    }

    @Test
    void normalizeSchemaKeepsOnlyOneFixedAmountBlockFromLegacyAmountControls() {
        Map<String, Object> schema = Map.of(
                "layoutMode", "TWO_COLUMN",
                "blocks", List.of(
                        Map.of(
                                "blockId", "legacy-amount-1",
                                "fieldKey", "legacyAmountOne",
                                "kind", "CONTROL",
                                "label", "旧金额1",
                                "props", Map.of("controlType", "AMOUNT")
                        ),
                        Map.of(
                                "blockId", "legacy-amount-2",
                                "fieldKey", "legacyAmountTwo",
                                "kind", "CONTROL",
                                "label", "旧金额2",
                                "props", Map.of("controlType", "AMOUNT")
                        )
                )
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) support.normalizeSchema(
                schema,
                ExpenseDetailSystemFieldSupport.DETAIL_TYPE_ENTERPRISE
        ).get("blocks");

        long amountBlockCount = blocks.stream()
                .filter(block -> ExpenseDetailSystemFieldSupport.FIELD_DETAIL_AMOUNT.equals(block.get("fieldKey")))
                .count();
        assertEquals(1L, amountBlockCount);
    }

    @Test
    void normalizeSchemaPreservesDraggedSystemFieldOrderWhileBackfillingMissingFields() {
        Map<String, Object> schema = Map.of(
                "layoutMode", "TWO_COLUMN",
                "blocks", List.of(
                        systemBlock("expense-type-block", ExpenseDetailSystemFieldSupport.FIELD_EXPENSE_TYPE_CODE),
                        genericBlock("remark-block", "remark"),
                        systemBlock("invoice-block", ExpenseDetailSystemFieldSupport.FIELD_INVOICE_AMOUNT),
                        systemBlock("attachment-block", ExpenseDetailSystemFieldSupport.FIELD_INVOICE_ATTACHMENTS)
                )
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) support.normalizeSchema(
                schema,
                ExpenseDetailSystemFieldSupport.DETAIL_TYPE_ENTERPRISE
        ).get("blocks");

        List<String> fieldKeys = blocks.stream()
                .map(block -> String.valueOf(block.get("fieldKey")))
                .collect(Collectors.toList());
        assertEquals(
                List.of(
                        ExpenseDetailSystemFieldSupport.FIELD_EXPENSE_TYPE_CODE,
                        "remark",
                        ExpenseDetailSystemFieldSupport.FIELD_BUSINESS_SCENARIO,
                        ExpenseDetailSystemFieldSupport.FIELD_DETAIL_AMOUNT,
                        ExpenseDetailSystemFieldSupport.FIELD_INVOICE_AMOUNT,
                        ExpenseDetailSystemFieldSupport.FIELD_ACTUAL_PAYMENT_AMOUNT,
                        ExpenseDetailSystemFieldSupport.FIELD_INVOICE_ATTACHMENTS
                ),
                fieldKeys
        );
    }

    @Test
    void normalizeSchemaBackfillsMissingRequiredFieldsIntoBaselineSlotsInsteadOfAppendingToTail() {
        Map<String, Object> schema = Map.of(
                "layoutMode", "TWO_COLUMN",
                "blocks", List.of(
                        genericBlock("remark-block", "remark"),
                        systemBlock("scenario-block", ExpenseDetailSystemFieldSupport.FIELD_BUSINESS_SCENARIO),
                        genericBlock("memo-block", "memo"),
                        systemBlock("actual-block", ExpenseDetailSystemFieldSupport.FIELD_ACTUAL_PAYMENT_AMOUNT)
                )
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) support.normalizeSchema(
                schema,
                ExpenseDetailSystemFieldSupport.DETAIL_TYPE_ENTERPRISE
        ).get("blocks");

        List<String> fieldKeys = blocks.stream()
                .map(block -> String.valueOf(block.get("fieldKey")))
                .collect(Collectors.toList());
        assertEquals(
                List.of(
                        "remark",
                        ExpenseDetailSystemFieldSupport.FIELD_EXPENSE_TYPE_CODE,
                        ExpenseDetailSystemFieldSupport.FIELD_BUSINESS_SCENARIO,
                        "memo",
                        ExpenseDetailSystemFieldSupport.FIELD_DETAIL_AMOUNT,
                        ExpenseDetailSystemFieldSupport.FIELD_INVOICE_AMOUNT,
                        ExpenseDetailSystemFieldSupport.FIELD_ACTUAL_PAYMENT_AMOUNT,
                        ExpenseDetailSystemFieldSupport.FIELD_INVOICE_ATTACHMENTS
                ),
                fieldKeys
        );
    }

    @Test
    void normalizeSchemaTreatsInvoiceAttachmentsAsRequiredProtectedSystemField() {
        Map<String, Object> schema = Map.of(
                "layoutMode", "TWO_COLUMN",
                "blocks", List.of(
                        systemBlock("expense-type-block", ExpenseDetailSystemFieldSupport.FIELD_EXPENSE_TYPE_CODE),
                        systemBlock("scenario-block", ExpenseDetailSystemFieldSupport.FIELD_BUSINESS_SCENARIO),
                        systemBlock("amount-block", ExpenseDetailSystemFieldSupport.FIELD_DETAIL_AMOUNT),
                        systemBlock("invoice-block", ExpenseDetailSystemFieldSupport.FIELD_INVOICE_AMOUNT),
                        systemBlock("actual-block", ExpenseDetailSystemFieldSupport.FIELD_ACTUAL_PAYMENT_AMOUNT)
                )
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) support.normalizeSchema(
                schema,
                ExpenseDetailSystemFieldSupport.DETAIL_TYPE_ENTERPRISE
        ).get("blocks");

        Map<String, Object> attachmentBlock = blocks.get(blocks.size() - 1);
        assertEquals(ExpenseDetailSystemFieldSupport.FIELD_INVOICE_ATTACHMENTS, attachmentBlock.get("fieldKey"));
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) attachmentBlock.get("props");
        assertEquals(Boolean.TRUE, props.get("locked"));
        assertEquals(ExpenseDetailSystemFieldSupport.SYSTEM_INVOICE_ATTACHMENTS, props.get("systemFieldCode"));
    }

    private Map<String, Object> systemBlock(String blockId, String fieldKey) {
        return Map.of(
                "blockId", blockId,
                "fieldKey", fieldKey,
                "kind", "CONTROL",
                "label", blockId,
                "props", Map.of()
        );
    }

    private Map<String, Object> genericBlock(String blockId, String fieldKey) {
        return Map.of(
                "blockId", blockId,
                "fieldKey", fieldKey,
                "kind", "CONTROL",
                "label", blockId,
                "props", Map.of("controlType", "TEXT")
        );
    }
}
