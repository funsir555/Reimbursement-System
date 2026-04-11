package com.finex.auth.service.impl.asynctask;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.NotificationItemVO;
import com.finex.auth.dto.NotificationSummaryVO;
import com.finex.auth.entity.NotificationRecord;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.NotificationRecordMapper;
import com.finex.auth.service.impl.AsyncTaskWorker;
import com.finex.auth.support.AsyncTaskSupport;

import java.time.LocalDateTime;
import java.util.List;

public final class AsyncTaskNotificationDomainSupport extends AbstractAsyncTaskDomainSupport {

    public AsyncTaskNotificationDomainSupport(
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            DownloadRecordMapper downloadRecordMapper,
            NotificationRecordMapper notificationRecordMapper,
            AsyncTaskWorker asyncTaskWorker,
            ObjectMapper objectMapper
    ) {
        super(asyncTaskRecordMapper, downloadRecordMapper, notificationRecordMapper, asyncTaskWorker, objectMapper);
    }

    public NotificationSummaryVO getNotificationSummary(Long userId) {
        Long unreadCount = notificationRecordMapper().selectCount(
                Wrappers.<NotificationRecord>lambdaQuery()
                        .eq(NotificationRecord::getUserId, userId)
                        .eq(NotificationRecord::getStatus, AsyncTaskSupport.NOTIFICATION_STATUS_UNREAD)
        );

        List<NotificationRecord> latestRecords = notificationRecordMapper().selectList(
                Wrappers.<NotificationRecord>lambdaQuery()
                        .eq(NotificationRecord::getUserId, userId)
                        .orderByDesc(NotificationRecord::getCreatedAt, NotificationRecord::getId)
                        .last("limit 1")
        );

        NotificationSummaryVO summary = new NotificationSummaryVO();
        summary.setUnreadCount(unreadCount == null ? 0L : unreadCount);
        if (!latestRecords.isEmpty()) {
            NotificationRecord latest = latestRecords.get(0);
            summary.setLatestTitle(latest.getTitle());
            summary.setLatestContent(latest.getContent());
            summary.setLatestCreatedAt(latest.getCreatedAt() == null ? "" : latest.getCreatedAt().format(DATE_TIME_FORMATTER));
        }
        return summary;
    }

    public List<NotificationItemVO> listNotifications(Long userId) {
        return notificationRecordMapper().selectList(
                        Wrappers.<NotificationRecord>lambdaQuery()
                                .eq(NotificationRecord::getUserId, userId)
                                .orderByDesc(NotificationRecord::getCreatedAt, NotificationRecord::getId)
                                .last("limit 50")
                ).stream()
                .map(this::toNotificationItem)
                .toList();
    }

    public boolean markNotificationRead(Long userId, Long notificationId) {
        NotificationRecord record = notificationRecordMapper().selectOne(
                Wrappers.<NotificationRecord>lambdaQuery()
                        .eq(NotificationRecord::getId, notificationId)
                        .eq(NotificationRecord::getUserId, userId)
                        .last("limit 1")
        );
        if (record == null) {
            throw new IllegalArgumentException("通知不存在");
        }
        if (AsyncTaskSupport.NOTIFICATION_STATUS_UNREAD.equalsIgnoreCase(record.getStatus())) {
            NotificationRecord update = new NotificationRecord();
            update.setId(record.getId());
            update.setStatus(AsyncTaskSupport.NOTIFICATION_STATUS_READ);
            update.setReadAt(LocalDateTime.now());
            notificationRecordMapper().updateById(update);
        }
        return true;
    }

    public boolean markAllNotificationsRead(Long userId) {
        NotificationRecord update = new NotificationRecord();
        update.setStatus(AsyncTaskSupport.NOTIFICATION_STATUS_READ);
        update.setReadAt(LocalDateTime.now());
        notificationRecordMapper().update(
                update,
                Wrappers.<NotificationRecord>lambdaUpdate()
                        .eq(NotificationRecord::getUserId, userId)
                        .eq(NotificationRecord::getStatus, AsyncTaskSupport.NOTIFICATION_STATUS_UNREAD)
        );
        return true;
    }

    private NotificationItemVO toNotificationItem(NotificationRecord record) {
        NotificationItemVO item = new NotificationItemVO();
        item.setId(record.getId());
        item.setTitle(record.getTitle());
        item.setContent(record.getContent());
        item.setType(record.getType());
        item.setStatus(record.getStatus());
        item.setRelatedTaskNo(record.getRelatedTaskNo());
        item.setCreatedAt(record.getCreatedAt() == null ? "" : record.getCreatedAt().format(DATE_TIME_FORMATTER));
        item.setReadAt(record.getReadAt() == null ? "" : record.getReadAt().format(DATE_TIME_FORMATTER));
        return item;
    }
}
