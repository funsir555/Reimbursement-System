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
        assertEquals("\u53d1\u7968\u91d1\u989d", blocks.get(2).get("label"));
        assertEquals("\u5b9e\u9645\u652f\u4ed8\u91d1\u989d", blocks.get(3).get("label"));
        assertEquals("\u53d1\u7968\u9644\u4ef6", blocks.get(4).get("label"));
        assertEquals("\u5f85\u6838\u9500\u91d1\u989d", blocks.get(5).get("label"));

        @SuppressWarnings("unchecked")
        Map<String, Object> expenseTypeProps = (Map<String, Object>) blocks.get(0).get("props");
        @SuppressWarnings("unchecked")
        Map<String, Object> scenarioProps = (Map<String, Object>) blocks.get(1).get("props");
        @SuppressWarnings("unchecked")
        Map<String, Object> amountProps = (Map<String, Object>) blocks.get(2).get("props");
        @SuppressWarnings("unchecked")
        Map<String, Object> invoiceAttachmentProps = (Map<String, Object>) blocks.get(4).get("props");

        assertEquals("\u8bf7\u9009\u62e9\u8d39\u7528\u7c7b\u578b", expenseTypeProps.get("placeholder"));
        assertEquals("\u8bf7\u9009\u62e9\u4e1a\u52a1\u573a\u666f", scenarioProps.get("placeholder"));
        assertEquals("\u8bf7\u8f93\u5165\u91d1\u989d", amountProps.get("placeholder"));
        assertEquals(30, invoiceAttachmentProps.get("maxCount"));
        assertEquals(".pdf,.png,.jpg,.jpeg", invoiceAttachmentProps.get("accept"));

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
}
