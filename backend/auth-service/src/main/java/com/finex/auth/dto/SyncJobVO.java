package com.finex.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SyncJobVO {

    private Long id;

    private String jobNo;

    private String platformCode;

    private String triggerType;

    private String status;

    private Integer successCount;

    private Integer skippedCount;

    private Integer failedCount;

    private Integer deletedCount;

    private String summary;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
