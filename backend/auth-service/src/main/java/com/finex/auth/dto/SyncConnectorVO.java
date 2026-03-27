package com.finex.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SyncConnectorVO {

    private Long id;

    private String platformCode;

    private String platformName;

    private Boolean enabled;

    private Boolean autoSyncEnabled;

    private Integer syncIntervalMinutes;

    private String appKey;

    private String appSecret;

    private String appId;

    private String corpId;

    private String agentId;

    private LocalDateTime lastSyncAt;

    private String lastSyncStatus;

    private String lastSyncMessage;
}
