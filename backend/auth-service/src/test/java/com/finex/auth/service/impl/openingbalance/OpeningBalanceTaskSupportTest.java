package com.finex.auth.service.impl.openingbalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.OpeningBalanceTaskRequestDTO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.support.AsyncTaskSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpeningBalanceTaskSupportTest {

    @Mock
    private AsyncTaskRecordMapper asyncTaskRecordMapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private OpeningBalanceTaskWorker openingBalanceTaskWorker;

    private OpeningBalanceTaskSupport taskSupport;

    @BeforeEach
    void setUp() {
        taskSupport = new OpeningBalanceTaskSupport(asyncTaskRecordMapper, objectMapper, openingBalanceTaskWorker);
    }

    @Test
    void openBookCreatesTaskWithinTaskTypeLimit() throws Exception {
        OpeningBalanceTaskRequestDTO dto = buildRequest();
        when(asyncTaskRecordMapper.selectOne(any())).thenReturn(null);
        when(objectMapper.writeValueAsString(dto)).thenReturn("{\"companyId\":\"COMPANY_A\"}");
        doAnswer(invocation -> {
            AsyncTaskRecord record = invocation.getArgument(0);
            record.setId(99L);
            return 1;
        }).when(asyncTaskRecordMapper).insert(any(AsyncTaskRecord.class));

        AsyncTaskSubmitResultVO result = taskSupport.openBook(7L, "alice", dto);

        ArgumentCaptor<AsyncTaskRecord> captor = ArgumentCaptor.forClass(AsyncTaskRecord.class);
        verify(asyncTaskRecordMapper).insert(captor.capture());
        AsyncTaskRecord saved = captor.getValue();
        assertEquals(AsyncTaskSupport.TASK_TYPE_FINANCE_OPENING_BALANCE_OPEN_BOOK, saved.getTaskType());
        assertTrue(saved.getTaskType().length() <= 32);
        assertEquals("finance_opening_balance", saved.getBusinessType());
        assertEquals("COMPANY_A#2026#6#OPEN_BOOK", saved.getBusinessKey());
        assertEquals("任务已提交", saved.getResultMessage());
        assertEquals(AsyncTaskSupport.TASK_STATUS_PENDING, saved.getStatus());
        assertEquals(saved.getTaskType(), result.getTaskType());
        assertEquals("开账任务已提交", result.getMessage());
        verify(openingBalanceTaskWorker).runOpenBookTask(99L);
    }

    @Test
    void carryForwardCreatesTaskWithinTaskTypeLimit() throws Exception {
        OpeningBalanceTaskRequestDTO dto = buildRequest();
        when(asyncTaskRecordMapper.selectOne(any())).thenReturn(null);
        when(objectMapper.writeValueAsString(dto)).thenReturn("{\"companyId\":\"COMPANY_A\"}");
        doAnswer(invocation -> {
            AsyncTaskRecord record = invocation.getArgument(0);
            record.setId(101L);
            return 1;
        }).when(asyncTaskRecordMapper).insert(any(AsyncTaskRecord.class));

        AsyncTaskSubmitResultVO result = taskSupport.carryForward(7L, "alice", dto);

        ArgumentCaptor<AsyncTaskRecord> captor = ArgumentCaptor.forClass(AsyncTaskRecord.class);
        verify(asyncTaskRecordMapper).insert(captor.capture());
        AsyncTaskRecord saved = captor.getValue();
        assertEquals(AsyncTaskSupport.TASK_TYPE_FINANCE_OPENING_BALANCE_CARRY_FORWARD, saved.getTaskType());
        assertTrue(saved.getTaskType().length() <= 32);
        assertEquals("COMPANY_A#2026#6#CARRY_FORWARD", saved.getBusinessKey());
        assertEquals("结转任务已提交", result.getMessage());
        verify(openingBalanceTaskWorker).runCarryForwardTask(101L);
    }

    @Test
    void openBookReturnsActiveTaskWithoutCreatingDuplicate() {
        OpeningBalanceTaskRequestDTO dto = buildRequest();
        AsyncTaskRecord active = new AsyncTaskRecord();
        active.setId(12L);
        active.setTaskNo("FOB20260428010101001123");
        active.setTaskType(AsyncTaskSupport.TASK_TYPE_FINANCE_OPENING_BALANCE_OPEN_BOOK);
        active.setBusinessType("finance_opening_balance");
        active.setStatus(AsyncTaskSupport.TASK_STATUS_RUNNING);
        when(asyncTaskRecordMapper.selectOne(any())).thenReturn(active);

        AsyncTaskSubmitResultVO result = taskSupport.openBook(7L, "alice", dto);

        assertEquals(active.getTaskNo(), result.getTaskNo());
        assertEquals("当前已有相同开账任务在执行，请稍后查看结果", result.getMessage());
        verify(asyncTaskRecordMapper, never()).insert(any());
        verify(openingBalanceTaskWorker, never()).runOpenBookTask(any());
    }

    private OpeningBalanceTaskRequestDTO buildRequest() {
        OpeningBalanceTaskRequestDTO dto = new OpeningBalanceTaskRequestDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setIyear(2026);
        dto.setIperiod(6);
        return dto;
    }
}
