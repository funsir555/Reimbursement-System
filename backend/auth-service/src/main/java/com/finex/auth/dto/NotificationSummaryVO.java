package com.finex.auth.dto;

import lombok.Data;

@Data
public class NotificationSummaryVO {

    private Long unreadCount;

    private String latestTitle;

    private String latestContent;

    private String latestCreatedAt;
}
