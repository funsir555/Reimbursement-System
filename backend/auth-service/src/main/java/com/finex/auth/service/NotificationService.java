package com.finex.auth.service;

public interface NotificationService {

    void sendAsyncNotification(Long userId, String type, String title, String content, String relatedTaskNo);
}
