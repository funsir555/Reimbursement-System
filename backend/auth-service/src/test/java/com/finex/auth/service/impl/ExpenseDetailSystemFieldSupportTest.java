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
import static org.mockito.Mockito.when;

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
        assertEquals("费用类型", blocks.get(0).get("label"));
        assertEquals("业务场景", blocks.get(1).get("label"));
        assertEquals("发票金额", blocks.get(2).get("label"));
        assertEquals("实际支付金额", blocks.get(3).get("label"));
        assertEquals("发票附件", blocks.get(4).get("label"));
        assertEquals("待核销金额", blocks.get(5).get("label"));

        @SuppressWarnings("unchecked")
        Map<String, Object> expenseTypeProps = (Map<String, Object>) blocks.get(0).get("props");
        @SuppressWarnings("unchecked")
        Map<String, Object> scenarioProps = (Map<String, Object>) blocks.get(1).get("props");
        @SuppressWarnings("unchecked")
        Map<String, Object> amountProps = (Map<String, Object>) blocks.get(2).get("props");

        assertEquals("请选择费用类型", expenseTypeProps.get("placeholder"));
        assertEquals("请选择业务场景", scenarioProps.get("placeholder"));
        assertEquals("请输入金额", amountProps.get("placeholder"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scenarioOptions = (List<Map<String, Object>>) scenarioProps.get("options");
        assertEquals("全额付款", scenarioOptions.get(0).get("label"));
        assertEquals("预付未到票", scenarioOptions.get(1).get("label"));
    }

    @Test
    void readSchemaThrowsReadableChineseMessageWhenJsonIsInvalid() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> support.readSchema("{bad-json", ExpenseDetailSystemFieldSupport.DETAIL_TYPE_NORMAL)
        );

        assertEquals("读取费用明细 schema 失败", error.getMessage());
    }
}
