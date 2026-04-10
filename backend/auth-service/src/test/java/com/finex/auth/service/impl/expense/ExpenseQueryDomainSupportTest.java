package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDocumentCommentDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentReminderDTO;
import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseQueryDomainSupportTest {

    @Mock
    private ExpenseDocumentMutationSupport expenseDocumentMutationSupport;
    @Mock
    private ExpenseDocumentTemplateSupport expenseDocumentTemplateSupport;
    @Mock
    private ExpenseRelationWriteOffService expenseRelationWriteOffService;
    @Mock
    private ExpenseSummaryAssembler expenseSummaryAssembler;
    @Mock
    private ExpenseDocumentDetailAssembler expenseDocumentDetailAssembler;
    @Mock
    private ProcessDocumentTaskMapper processDocumentTaskMapper;
    @Mock
    private ProcessDocumentActionLogMapper processDocumentActionLogMapper;
    @Mock
    private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock
    private ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    @Mock
    private NotificationService notificationService;

    @Test
    void summaryAndDetailReadsUseDomainCollaborators() {
        List<ExpenseSummaryVO> summaries = List.of(new ExpenseSummaryVO());
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");
        ProcessDocumentExpenseDetail expenseDetail = new ProcessDocumentExpenseDetail();
        ExpenseDetailInstanceDetailVO expenseDetailVo = new ExpenseDetailInstanceDetailVO();
        ExpenseQueryDomainSupport support = new ExpenseQueryDomainSupport(
                expenseDocumentMutationSupport,
                expenseDocumentTemplateSupport,
                expenseRelationWriteOffService,
                expenseSummaryAssembler,
                expenseDocumentDetailAssembler,
                processDocumentTaskMapper,
                processDocumentActionLogMapper,
                processDocumentInstanceMapper,
                processDocumentExpenseDetailMapper,
                notificationService
        );
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(instance));
        when(expenseSummaryAssembler.toExpenseSummaries(List.of(instance))).thenReturn(summaries);
        when(expenseDocumentMutationSupport.requireDocument("DOC-001")).thenReturn(instance);
        when(expenseDocumentMutationSupport.buildDocumentDetail(instance)).thenReturn(detail);
        when(processDocumentExpenseDetailMapper.selectOne(any())).thenReturn(expenseDetail);
        when(expenseDocumentDetailAssembler.toExpenseDetailDetailVO(expenseDetail)).thenReturn(expenseDetailVo);

        assertSame(summaries, support.listExpenseSummaries(1L));
        assertSame(detail, support.getDocumentDetail(1L, "DOC-001", false));
        assertSame(expenseDetailVo, support.getExpenseDetail(1L, "DOC-001", "D1", false));
    }

    @Test
    void editContextUsesTemplateSupport() {
        ExpenseDocumentEditContextVO context = new ExpenseDocumentEditContextVO();
        ExpenseQueryDomainSupport support = new ExpenseQueryDomainSupport(
                expenseDocumentMutationSupport,
                expenseDocumentTemplateSupport,
                expenseRelationWriteOffService,
                expenseSummaryAssembler,
                expenseDocumentDetailAssembler,
                processDocumentTaskMapper,
                processDocumentActionLogMapper,
                processDocumentInstanceMapper,
                processDocumentExpenseDetailMapper,
                notificationService
        );
        when(expenseDocumentTemplateSupport.getDocumentEditContext(1L, "DOC-001")).thenReturn(context);

        ExpenseDocumentEditContextVO actual = support.getDocumentEditContext(1L, "DOC-001");

        assertSame(context, actual);
    }

    @Test
    void remindDocumentBuildsDetailAfterLogging() {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-001");
        instance.setSubmitterUserId(1L);
        instance.setStatus("PENDING_APPROVAL");
        instance.setCurrentNodeKey("N1");
        instance.setCurrentNodeName("审批");
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setAssigneeUserId(2L);
        task.setAssigneeName("审批人");
        task.setStatus("PENDING");
        task.setCreatedAt(LocalDateTime.now());
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        ExpenseDocumentReminderDTO dto = new ExpenseDocumentReminderDTO();
        dto.setRemark("请尽快处理");
        ExpenseQueryDomainSupport support = new ExpenseQueryDomainSupport(
                expenseDocumentMutationSupport,
                expenseDocumentTemplateSupport,
                expenseRelationWriteOffService,
                expenseSummaryAssembler,
                expenseDocumentDetailAssembler,
                processDocumentTaskMapper,
                processDocumentActionLogMapper,
                processDocumentInstanceMapper,
                processDocumentExpenseDetailMapper,
                notificationService
        );
        when(expenseDocumentMutationSupport.requireDocument("DOC-001")).thenReturn(instance, instance);
        when(processDocumentTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(processDocumentActionLogMapper.selectOne(any())).thenReturn(null);
        when(expenseDocumentMutationSupport.buildDocumentDetail(instance)).thenReturn(detail);

        ExpenseDocumentDetailVO actual = support.remindDocument(1L, "tester", "DOC-001", dto);

        assertSame(detail, actual);
        verify(expenseDocumentMutationSupport).appendLog(any(), any(), any(), any(), any(), any(), any(), anyMap());
    }
}
