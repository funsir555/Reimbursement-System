package com.finex.auth.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ArchiveAgentRunDTO {

    private String triggerSource;

    /** Fixed version observed by the workbench preflight, when provided. */
    private Long expectedVersionId;

    /** Switching to another owned Agent must run its published snapshot. */
    private boolean usePublishedVersion;

    private Map<String, Object> inputPayload = new LinkedHashMap<>();
}
