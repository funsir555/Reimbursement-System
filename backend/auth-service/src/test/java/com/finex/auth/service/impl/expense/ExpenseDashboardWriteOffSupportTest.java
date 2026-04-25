package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentWriteOff;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentRelationMapper;
import com.finex.auth.mapper.ProcessDocumentWriteOffMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDashboardWriteOffSupportTest {

    @Mock
    private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock
    private ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    @Mock
    private ProcessDocumentRelationMapper processDocumentRelationMapper;
    @Mock
    private ProcessDocumentWriteOffMapper processDocumentWriteOffMapper;

    private ExpenseDashboardWriteOffSupport dashboardWriteOffSupport;

    @BeforeEach
    void setUp() {
        ExpenseRelationWriteOffSupportContext context = new ExpenseRelationWriteOffSupportContext(
                processDocumentInstanceMapper,
                processDocumentExpenseDetailMapper,
                processDocumentRelationMapper,
                processDocumentWriteOffMapper,
                new ObjectMapper()
        );
        dashboardWriteOffSupport = new ExpenseDashboardWriteOffSupport(context, new ExpenseWriteOffAmountSupport(context));
    }

    @Test
    void bindDashboardWriteOffPersistsEffectiveRecord() {
        ProcessDocumentInstance target = createApprovedDocument("DOC-TARGET-001", "loan", BigDecimal.valueOf(300), 1L);
        ProcessDocumentInstance sourceReport = createApprovedDocument("DOC-REPORT-001", "report", BigDecimal.valueOf(200), 1L);

        when(processDocumentInstanceMapper.selectOne(any())).thenReturn(target, sourceReport);
        when(processDocumentWriteOffMapper.selectCount(any())).thenReturn(0L);
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of(), List.of());

        boolean bound = dashboardWriteOffSupport.bindDashboardWriteOff(1L, "DOC-TARGET-001", "DOC-REPORT-001");

        assertTrue(bound);
        ArgumentCaptor<ProcessDocumentWriteOff> captor = ArgumentCaptor.forClass(ProcessDocumentWriteOff.class);
        verify(processDocumentWriteOffMapper).insert(captor.capture());
        assertEquals("DOC-REPORT-001", captor.getValue().getSourceDocumentCode());
        assertEquals("DOC-TARGET-001", captor.getValue().getTargetDocumentCode());
        assertEquals("EFFECTIVE", captor.getValue().getStatus());
    }

    private ProcessDocumentInstance createApprovedDocument(
            String documentCode,
            String templateType,
            BigDecimal totalAmount,
            Long submitterUserId
    ) {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode(documentCode);
        instance.setTemplateType(templateType);
        instance.setDocumentTitle(documentCode);
        instance.setTemplateName(documentCode + "-template");
        instance.setStatus("COMPLETED");
        instance.setSubmitterUserId(submitterUserId);
        instance.setTotalAmount(totalAmount);
        instance.setFinishedAt(LocalDateTime.of(2026, 4, 8, 18, 0));
        instance.setUpdatedAt(LocalDateTime.of(2026, 4, 8, 18, 0));
        return instance;
    }
}
