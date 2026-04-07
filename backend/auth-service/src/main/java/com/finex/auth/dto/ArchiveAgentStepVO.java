package com.finex.auth.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ArchiveAgentStepVO {

    private Integer stepNo;

    private String nodeKey;

    private String nodeType;

    private String nodeLabel;

    private String status;

    private String errorMessage;

    private String startedAt;

    private String finishedAt;

    private Long durationMs;

    private Map<String, Object> inputPayload = new LinkedHashMap<>();

    private Map<String, Object> outputPayload = new LinkedHashMap<>();
}
