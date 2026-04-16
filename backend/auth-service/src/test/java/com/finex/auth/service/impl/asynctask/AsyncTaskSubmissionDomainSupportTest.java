package com.finex.auth.service.impl.asynctask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.ExpenseExportSubmitDTO;
import com.finex.auth.dto.InvoiceTaskSubmitDTO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.DownloadRecord;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.NotificationRecordMapper;
import com.finex.auth.service.impl.AsyncTaskWorker;
import com.finex.auth.support.AsyncTaskSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncTaskSubmissionDomainSupportTest {

    @Mock
    private AsyncTaskRecordMapper asyncTaskRecordMapper;
    @Mock
    private DownloadRecordMapper downloadRecordMapper;
    @Mock
    private NotificationRecordMapper notificationRecordMapper;
    @Mock
    private AsyncTaskWorker asyncTaskWorker;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AsyncTaskSubmissionDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new AsyncTaskSubmissionDomainSupport(
                asyncTaskRecordMapper,
                downloadRecordMapper,
                notificationRecordMapper,
                asyncTaskWorker,
                objectMapper
        );
    }

    @Test
    void submitInvoiceVerifyReusesExistingActiveTask() {
        InvoiceTaskSubmitDTO dto = new InvoiceTaskSubmitDTO();
        dto.setCode("INV001");
        dto.setNumber("0001");

        AsyncTaskRecord existing = new AsyncTaskRecord();
        existing.setId(88L);
        existing.setTaskNo("VER-001");
        existing.setTaskType(AsyncTaskSupport.TASK_TYPE_INVOICE_VERIFY);
        existing.setBusinessType(AsyncTaskSupport.BUSINESS_TYPE_INVOICE);
        existing.setStatus(AsyncTaskSupport.TASK_STATUS_RUNNING);

        when(asyncTaskRecordMapper.selectList(any())).thenReturn(List.of(existing));

        AsyncTaskSubmitResultVO result = support.submitInvoiceVerify(9L, dto);

        assertEquals("VER-001", result.getTaskNo());
        assertEquals(AsyncTaskSupport.TASK_TYPE_INVOICE_VERIFY, result.getTaskType());
        assertEquals(AsyncTaskSupport.BUSINESS_TYPE_INVOICE, result.getBusinessType());
        assertEquals(AsyncTaskSupport.TASK_STATUS_RUNNING, result.getStatus());
        verify(asyncTaskRecordMapper, never()).insert(any());
        verify(asyncTaskWorker, never()).runInvoiceVerifyTask(any());
    }

    @Test
    void submitExpenseExportCreatesRecordsAndEnqueuesWorker() throws Exception {
        when(downloadRecordMapper.insert(any())).thenAnswer(invocation -> {
            DownloadRecord record = invocation.getArgument(0);
            record.setId(15L);
            return 1;
        });
        when(asyncTaskRecordMapper.insert(any())).thenAnswer(invocation -> {
            AsyncTaskRecord task = invocation.getArgument(0);
            task.setId(27L);
            return 1;
        });

        ExpenseExportSubmitDTO dto = new ExpenseExportSubmitDTO();
        dto.setScene(" my_expenses ");
        dto.setDocumentCodes(List.of("DOC-001", " DOC-001 ", "DOC-002", " "));

        AsyncTaskSubmitResultVO result = support.submitExpenseExport(9L, dto);

        ArgumentCaptor<DownloadRecord> downloadCaptor = ArgumentCaptor.forClass(DownloadRecord.class);
        verify(downloadRecordMapper).insert(downloadCaptor.capture());
        assertEquals(9L, downloadCaptor.getValue().getUserId());
        assertEquals(AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING, downloadCaptor.getValue().getStatus());
        assertEquals(0, downloadCaptor.getValue().getProgress());
        assertTrue(downloadCaptor.getValue().getFileName().endsWith(".xlsx"));

        ArgumentCaptor<AsyncTaskRecord> taskCaptor = ArgumentCaptor.forClass(AsyncTaskRecord.class);
        verify(asyncTaskRecordMapper).insert(taskCaptor.capture());
        AsyncTaskRecord createdTask = taskCaptor.getValue();
        assertEquals(AsyncTaskSupport.TASK_TYPE_EXPORT, createdTask.getTaskType());
        assertEquals(AsyncTaskSupport.BUSINESS_TYPE_EXPENSE_EXPORT, createdTask.getBusinessType());
        assertEquals(15L, createdTask.getDownloadRecordId());
        assertTrue(createdTask.getBusinessKey().startsWith("MY_EXPENSES#"));
        assertEquals("任务已提交", createdTask.getResultMessage());

        ExpenseExportSubmitDTO payload = objectMapper.readValue(createdTask.getResultPayload(), ExpenseExportSubmitDTO.class);
        assertEquals("MY_EXPENSES", payload.getScene());
        assertEquals(List.of("DOC-001", "DOC-002"), payload.getDocumentCodes());
        assertEquals(List.of(), payload.getTaskIds());

        verify(asyncTaskWorker).runExpenseExportTask(27L);
        assertEquals(AsyncTaskSupport.TASK_TYPE_EXPORT, result.getTaskType());
        assertEquals(15L, result.getDownloadRecordId());
        assertNotNull(result.getTaskNo());
    }

    @Test
    void submitPaymentPendingExportNormalizesTaskIds() throws Exception {
        when(downloadRecordMapper.insert(any())).thenAnswer(invocation -> {
            DownloadRecord record = invocation.getArgument(0);
            record.setId(18L);
            return 1;
        });
        when(asyncTaskRecordMapper.insert(any())).thenAnswer(invocation -> {
            AsyncTaskRecord task = invocation.getArgument(0);
            task.setId(29L);
            return 1;
        });

        ExpenseExportSubmitDTO dto = new ExpenseExportSubmitDTO();
        dto.setScene(" payment_pending ");
        dto.setTaskIds(List.of(9L, 9L, 10L));

        AsyncTaskSubmitResultVO result = support.submitExpenseExport(9L, dto);

        ArgumentCaptor<AsyncTaskRecord> taskCaptor = ArgumentCaptor.forClass(AsyncTaskRecord.class);
        verify(asyncTaskRecordMapper).insert(taskCaptor.capture());
        AsyncTaskRecord createdTask = taskCaptor.getValue();
        ExpenseExportSubmitDTO payload = objectMapper.readValue(createdTask.getResultPayload(), ExpenseExportSubmitDTO.class);

        assertEquals("PAYMENT_PENDING", payload.getScene());
        assertEquals(List.of(9L, 10L), payload.getTaskIds());
        assertEquals(18L, result.getDownloadRecordId());
        verify(asyncTaskWorker).runExpenseExportTask(29L);
    }
}
