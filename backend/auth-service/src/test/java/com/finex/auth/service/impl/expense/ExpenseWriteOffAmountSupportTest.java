package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseWriteOffAmountSupportTest {

    @Mock
    private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock
    private ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    @Mock
    private ProcessDocumentRelationMapper processDocumentRelationMapper;
    @Mock
    private ProcessDocumentWriteOffMapper processDocumentWriteOffMapper;

    private ExpenseWriteOffAmountSupport amountSupport;

    @BeforeEach
    void setUp() {
        amountSupport = new ExpenseWriteOffAmountSupport(new ExpenseRelationWriteOffSupportContext(
                processDocumentInstanceMapper,
                processDocumentExpenseDetailMapper,
                processDocumentRelationMapper,
                processDocumentWriteOffMapper,
                new ObjectMapper()
        ));
    }

    @Test
    void buildOutstandingAmountMapUsesLoanTotalAndPrepayAmount() {
        ProcessDocumentInstance loan = new ProcessDocumentInstance();
        loan.setDocumentCode("DOC-LOAN-001");
        loan.setTemplateType("loan");
        loan.setTotalAmount(BigDecimal.valueOf(500));

        ProcessDocumentInstance report = new ProcessDocumentInstance();
        report.setDocumentCode("DOC-REPORT-001");
        report.setTemplateType("report");
        report.setTotalAmount(BigDecimal.valueOf(700));

        ProcessDocumentExpenseDetail prepayDetail = new ProcessDocumentExpenseDetail();
        prepayDetail.setDocumentCode("DOC-REPORT-001");
        prepayDetail.setBusinessSceneMode("PREPAY_UNBILLED");
        prepayDetail.setActualPaymentAmount(BigDecimal.valueOf(240));

        ProcessDocumentWriteOff loanWriteOff = new ProcessDocumentWriteOff();
        loanWriteOff.setTargetDocumentCode("DOC-LOAN-001");
        loanWriteOff.setEffectiveAmount(BigDecimal.valueOf(120));
        loanWriteOff.setStatus("EFFECTIVE");
        ProcessDocumentWriteOff reportWriteOff = new ProcessDocumentWriteOff();
        reportWriteOff.setTargetDocumentCode("DOC-REPORT-001");
        reportWriteOff.setEffectiveAmount(BigDecimal.valueOf(40));
        reportWriteOff.setStatus("EFFECTIVE");

        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of(loanWriteOff, reportWriteOff));
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of(prepayDetail));

        var loanOutstanding = amountSupport.buildOutstandingAmountMap(List.of(loan), "LOAN");
        var reportOutstanding = amountSupport.buildOutstandingAmountMap(List.of(report), "PREPAY_REPORT");

        assertEquals(0, BigDecimal.valueOf(380).compareTo(loanOutstanding.get("DOC-LOAN-001")));
        assertEquals(0, BigDecimal.valueOf(200).compareTo(reportOutstanding.get("DOC-REPORT-001")));
    }
}
