package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentTaskRuntimeSupportTest {

    @Mock
    private ProcessDocumentTaskMapper processDocumentTaskMapper;

    @Test
    void loadOpenTasksDelegatesToMapper() {
        ExpenseDocumentTaskRuntimeSupport support = new ExpenseDocumentTaskRuntimeSupport(processDocumentTaskMapper);
        ProcessDocumentTask task = new ProcessDocumentTask();
        when(processDocumentTaskMapper.selectList(any())).thenReturn(List.of(task));

        List<ProcessDocumentTask> actual = support.loadOpenTasks("DOC-1");

        assertSame(task, actual.get(0));
        verify(processDocumentTaskMapper).selectList(any());
    }

    @Test
    void cancelOpenTasksSkipsKeepTaskAndCancelsOthers() {
        ExpenseDocumentTaskRuntimeSupport support = new ExpenseDocumentTaskRuntimeSupport(processDocumentTaskMapper);
        ProcessDocumentTask keepTask = new ProcessDocumentTask();
        keepTask.setId(1L);
        keepTask.setStatus("PENDING");
        ProcessDocumentTask cancelTask = new ProcessDocumentTask();
        cancelTask.setId(2L);
        cancelTask.setStatus("PAUSED");
        LocalDateTime handledAt = LocalDateTime.of(2026, 4, 26, 12, 0);

        support.cancelOpenTasks(List.of(keepTask, cancelTask), 1L, handledAt);

        verify(processDocumentTaskMapper).updateById(cancelTask);
        verify(processDocumentTaskMapper, never()).updateById(keepTask);
        org.junit.jupiter.api.Assertions.assertEquals("CANCELLED", cancelTask.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(handledAt, cancelTask.getHandledAt());
    }
}
