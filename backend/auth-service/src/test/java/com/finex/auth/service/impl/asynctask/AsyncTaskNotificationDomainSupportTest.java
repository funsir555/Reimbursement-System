package com.finex.auth.service.impl.asynctask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.NotificationItemVO;
import com.finex.auth.dto.NotificationSummaryVO;
import com.finex.auth.entity.NotificationRecord;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncTaskNotificationDomainSupportTest {

    @Mock
    private AsyncTaskRecordMapper asyncTaskRecordMapper;
    @Mock
    private DownloadRecordMapper downloadRecordMapper;
    @Mock
    private NotificationRecordMapper notificationRecordMapper;
    @Mock
    private AsyncTaskWorker asyncTaskWorker;

    private AsyncTaskNotificationDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new AsyncTaskNotificationDomainSupport(
                asyncTaskRecordMapper,
                downloadRecordMapper,
                notificationRecordMapper,
                asyncTaskWorker,
                new ObjectMapper()
        );
    }

    @Test
    void getNotificationSummaryReturnsUnreadCountAndLatestRecord() {
        NotificationRecord latest = new NotificationRecord();
        latest.setId(2L);
        latest.setUserId(9L);
        latest.setTitle("Export finished");
        latest.setContent("Task completed");
        latest.setCreatedAt(LocalDateTime.of(2026, 4, 10, 9, 30, 0));

        when(notificationRecordMapper.selectCount(any())).thenReturn(3L);
        when(notificationRecordMapper.selectList(any())).thenReturn(List.of(latest));

        NotificationSummaryVO summary = support.getNotificationSummary(9L);

        assertEquals(3L, summary.getUnreadCount());
        assertEquals("Export finished", summary.getLatestTitle());
        assertEquals("Task completed", summary.getLatestContent());
        assertEquals("2026-04-10 09:30:00", summary.getLatestCreatedAt());
    }

    @Test
    void listNotificationsReturnsLatestItemsForCurrentUser() {
        NotificationRecord unread = new NotificationRecord();
        unread.setId(2L);
        unread.setUserId(9L);
        unread.setTitle("Export finished");
        unread.setContent("Task completed");
        unread.setType(AsyncTaskSupport.NOTIFICATION_TYPE_TASK);
        unread.setStatus(AsyncTaskSupport.NOTIFICATION_STATUS_UNREAD);
        unread.setRelatedTaskNo("EXP-002");
        unread.setCreatedAt(LocalDateTime.of(2026, 4, 6, 11, 0, 0));

        NotificationRecord read = new NotificationRecord();
        read.setId(1L);
        read.setUserId(9L);
        read.setTitle("OCR finished");
        read.setContent("OCR success");
        read.setType(AsyncTaskSupport.NOTIFICATION_TYPE_TASK);
        read.setStatus(AsyncTaskSupport.NOTIFICATION_STATUS_READ);
        read.setRelatedTaskNo("OCR-001");
        read.setCreatedAt(LocalDateTime.of(2026, 4, 6, 10, 0, 0));
        read.setReadAt(LocalDateTime.of(2026, 4, 6, 10, 5, 0));

        when(notificationRecordMapper.selectList(any())).thenReturn(List.of(unread, read));

        List<NotificationItemVO> result = support.listNotifications(9L);

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals("Export finished", result.get(0).getTitle());
        assertEquals("UNREAD", result.get(0).getStatus());
        assertEquals("2026-04-06 11:00:00", result.get(0).getCreatedAt());
        assertEquals("READ", result.get(1).getStatus());
        assertEquals("2026-04-06 10:05:00", result.get(1).getReadAt());
    }

    @Test
    void markNotificationReadUpdatesUnreadRecord() {
        NotificationRecord record = new NotificationRecord();
        record.setId(8L);
        record.setUserId(9L);
        record.setStatus(AsyncTaskSupport.NOTIFICATION_STATUS_UNREAD);

        when(notificationRecordMapper.selectOne(any())).thenReturn(record);

        assertTrue(support.markNotificationRead(9L, 8L));

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordMapper).updateById(captor.capture());
        assertEquals(8L, captor.getValue().getId());
        assertEquals(AsyncTaskSupport.NOTIFICATION_STATUS_READ, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getReadAt());
    }

    @Test
    void markNotificationReadThrowsWhenRecordDoesNotBelongToUser() {
        when(notificationRecordMapper.selectOne(any())).thenReturn(null);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> support.markNotificationRead(9L, 8L)
        );

        assertEquals("通知不存在", error.getMessage());
    }
}
