package com.finex.auth.dto;

import lombok.Data;

@Data
public class ArchiveAgentRunVO {

    private Long id;

    private String runNo;

    private Long agentId;

    private String triggerType;

    private String triggerSource;

    private String status;

    private String summary;

    private String errorMessage;

    private String startedAt;

    private String finishedAt;

    private Long durationMs;
}
