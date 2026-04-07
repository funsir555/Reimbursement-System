package com.finex.auth.dto;

import lombok.Data;

@Data
public class NotificationItemVO {

    private Long id;

    private String title;

    private String content;

    private String type;

    private String status;

    private String relatedTaskNo;

    private String createdAt;

    private String readAt;
}
