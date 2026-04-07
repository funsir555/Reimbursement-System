package com.finex.auth.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ArchiveAgentRunDTO {

    private String triggerSource;

    private Map<String, Object> inputPayload = new LinkedHashMap<>();
}
