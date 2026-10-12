package com.finex.auth.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ArchiveAgentToolTestDTO {
    private String queryTemplateCode;
    private Map<String, Object> parameters;
}
