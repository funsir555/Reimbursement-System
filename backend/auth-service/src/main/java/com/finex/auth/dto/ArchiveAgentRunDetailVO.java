package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ArchiveAgentRunDetailVO {

    private Long id;

    private String runNo;

    private Long agentId;

    private String agentName;

    private Integer agentVersionNo;

    private String triggerType;

    private String triggerSource;

    private String status;

    private String summary;

    private String errorMessage;

    private String startedAt;

    private String finishedAt;

    private Long durationMs;

    private Map<String, Object> inputPayload = new LinkedHashMap<>();

    private Map<String, Object> outputPayload = new LinkedHashMap<>();

    private List<ArchiveAgentStepVO> steps = new ArrayList<>();

    private List<Map<String, Object>> artifacts = new ArrayList<>();
}
