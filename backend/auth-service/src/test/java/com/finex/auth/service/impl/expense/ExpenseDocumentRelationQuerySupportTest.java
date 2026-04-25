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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentRelationQuerySupportTest {

    @Mock
    private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock
    private ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    @Mock
    private ProcessDocumentRelationMapper processDocumentRelationMapper;
    @Mock
    private ProcessDocumentWriteOffMapper processDocumentWriteOffMapper;

    private ExpenseDocumentRelationQuerySupport querySupport;

    @BeforeEach
    void setUp() {
        ExpenseRelationWriteOffSupportContext context = new ExpenseRelationWriteOffSupportContext(
                processDocumentInstanceMapper,
                processDocumentExpenseDetailMapper,
                processDocumentRelationMapper,
                processDocumentWriteOffMapper,
                new ObjectMapper()
        );
        ExpenseWriteOffAmountSupport amountSupport = new ExpenseWriteOffAmountSupport(context);
        querySupport = new ExpenseDocumentRelationQuerySupport(context, amountSupport);
    }

    @Test
    void getDashboardWriteOffSourceReportPickerFiltersBoundAndZeroBalanceSources() {
        ProcessDocumentInstance target = createApprovedDocument("DOC-TARGET-001", "loan", BigDecimal.valueOf(400), 1L);
        ProcessDocumentInstance availableReport = createApprovedDocument("DOC-REPORT-001", "report", BigDecimal.valueOf(500), 1L);
        ProcessDocumentInstance boundReport = createApprovedDocument("DOC-REPORT-002", "report", BigDecimal.valueOf(600), 1L);
        ProcessDocumentInstance zeroReport = createApprovedDocument("DOC-REPORT-003", "report", BigDecimal.valueOf(400), 1L);
        ProcessDocumentWriteOff zeroBalance = createEffectiveWriteOff("DOC-REPORT-003", "DOC-OTHER-001", BigDecimal.valueOf(400));
        ProcessDocumentWriteOff bound = createEffectiveWriteOff("DOC-REPORT-002", "DOC-TARGET-001", BigDecimal.valueOf(50));

        when(processDocumentInstanceMapper.selectOne(any())).thenReturn(target);
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(availableReport, boundReport, zeroReport));
        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of(zeroBalance), List.of(bound));

        var result = querySupport.getDashboardWriteOffSourceReportPicker(1L, "DOC-TARGET-001", null, 1, 10);

        assertEquals(1, result.getGroups().size());
        assertEquals(1, result.getGroups().get(0).getItems().size());
        assertEquals("DOC-REPORT-001", result.getGroups().get(0).getItems().get(0).getDocumentCode());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(result.getGroups().get(0).getItems().get(0).getAvailableWriteOffAmount()));
    }

    private ProcessDocumentInstance createApprovedDocument(String documentCode, String templateType, BigDecimal totalAmount, Long submitterUserId) {
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

    private ProcessDocumentWriteOff createEffectiveWriteOff(String sourceDocumentCode, String targetDocumentCode, BigDecimal effectiveAmount) {
        ProcessDocumentWriteOff writeOff = new ProcessDocumentWriteOff();
        writeOff.setSourceDocumentCode(sourceDocumentCode);
        writeOff.setTargetDocumentCode(targetDocumentCode);
        writeOff.setEffectiveAmount(effectiveAmount);
        writeOff.setStatus("EFFECTIVE");
        return writeOff;
    }
}
