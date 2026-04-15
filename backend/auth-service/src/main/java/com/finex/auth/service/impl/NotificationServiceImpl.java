// 业务域：异步任务
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 异步提交、查询和通知相关接口，下游会继续协调 任务记录、状态更新和通知消息。
// 风险提醒：改坏后最容易影响 任务重复提交、异步状态不准确和结果回传。

package com.finex.auth.service.impl;

import com.finex.auth.entity.NotificationRecord;
import com.finex.auth.mapper.NotificationRecordMapper;
import com.finex.auth.service.NotificationService;
import com.finex.auth.support.AsyncTaskSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * NotificationServiceImpl：service 入口实现。
 * 接住上层请求，并把 通知相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 任务重复提交、异步状态不准确和结果回传是否会被一起带坏。
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRecordMapper notificationRecordMapper;

    /**
     * 处理通知中的这一步。
     */
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
