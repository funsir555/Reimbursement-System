package com.finex.auth.service.impl;

import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseDetailDesignMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessExpenseDetailDesignServiceImplTest {

    @Mock
    private ProcessExpenseDetailDesignMapper processExpenseDetailDesignMapper;
    @Mock
    private ProcessDocumentTemplateMapper processDocumentTemplateMapper;
    @Mock
    private ExpenseDetailSystemFieldSupport expenseDetailSystemFieldSupport;

    private ProcessExpenseDetailDesignServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProcessExpenseDetailDesignServiceImpl(
                processExpenseDetailDesignMapper,
                processDocumentTemplateMapper,
                expenseDetailSystemFieldSupport
        );
    }

    @Test
    void listExpenseDetailDesignsReturnsReadableChineseTypeLabels() {
        ProcessExpenseDetailDesign normal = new ProcessExpenseDetailDesign();
        normal.setId(1L);
        normal.setDetailCode("EDD-001");
        normal.setDetailName("交通费明细");
        normal.setDetailType(ExpenseDetailSystemFieldSupport.DETAIL_TYPE_NORMAL);
        normal.setUpdatedAt(LocalDateTime.of(2026, 4, 8, 10, 0));

        ProcessExpenseDetailDesign enterprise = new ProcessExpenseDetailDesign();
        enterprise.setId(2L);
        enterprise.setDetailCode("EDD-002");
        enterprise.setDetailName("对公付款明细");
        enterprise.setDetailType(ExpenseDetailSystemFieldSupport.DETAIL_TYPE_ENTERPRISE);
        enterprise.setUpdatedAt(LocalDateTime.of(2026, 4, 8, 11, 0));

        when(processExpenseDetailDesignMapper.selectList(any())).thenReturn(List.of(normal, enterprise));
        when(expenseDetailSystemFieldSupport.normalizeDetailType(ExpenseDetailSystemFieldSupport.DETAIL_TYPE_NORMAL))
                .thenReturn(ExpenseDetailSystemFieldSupport.DETAIL_TYPE_NORMAL);
        when(expenseDetailSystemFieldSupport.normalizeDetailType(ExpenseDetailSystemFieldSupport.DETAIL_TYPE_ENTERPRISE))
                .thenReturn(ExpenseDetailSystemFieldSupport.DETAIL_TYPE_ENTERPRISE);

        var result = service.listExpenseDetailDesigns();

        assertEquals(2, result.size());
        assertEquals("普通报销", result.get(0).getDetailTypeLabel());
        assertEquals("企业往来", result.get(1).getDetailTypeLabel());
    }

    @Test
    void resolveExpenseDetailDesignCodeRejectsBlankCodeWithReadableMessage() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.resolveExpenseDetailDesignCode("   ")
        );

        assertEquals("费用明细设计编码不能为空", error.getMessage());
    }

    @Test
    void deleteExpenseDetailDesignRejectsReferencedTemplateWithReadableMessage() {
        ProcessExpenseDetailDesign detail = new ProcessExpenseDetailDesign();
        detail.setId(1L);
        detail.setDetailCode("EDD-001");
        when(processExpenseDetailDesignMapper.selectById(1L)).thenReturn(detail);
        when(processDocumentTemplateMapper.selectCount(any())).thenReturn(1L);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.deleteExpenseDetailDesign(1L)
        );

        assertEquals("费用明细表单已被模板引用，不能删除", error.getMessage());
    }
}
