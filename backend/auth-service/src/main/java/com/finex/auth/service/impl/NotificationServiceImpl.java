package com.finex.auth.service.impl;

import com.finex.auth.entity.NotificationRecord;
import com.finex.auth.mapper.NotificationRecordMapper;
import com.finex.auth.service.NotificationService;
import com.finex.auth.support.AsyncTaskSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRecordMapper notificationRecordMapper;

    @Override
    @Async("finexAsyncExecutor")
    public void sendAsyncNotification(Long userId, String type, String title, String content, String relatedTaskNo) {
        NotificationRecord record = new NotificationRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setTitle(title);
        record.setContent(content);
        record.setStatus(AsyncTaskSupport.NOTIFICATION_STATUS_UNREAD);
        record.setRelatedTaskNo(relatedTaskNo);
        notificationRecordMapper.insert(record);
    }
}
