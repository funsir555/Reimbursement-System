package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ArchiveAgentSaveDTO {

    @NotBlank(message = "Agent 名称不能为空")
    private String agentName;

    private String agentDescription;

    private String iconKey;

    private String themeKey;

    private String coverColor;

    private List<String> tags = new ArrayList<>();

    private Map<String, Object> promptConfig = new LinkedHashMap<>();

    private Map<String, Object> modelConfig = new LinkedHashMap<>();

    private List<Map<String, Object>> tools = new ArrayList<>();

    private Map<String, Object> workflow = new LinkedHashMap<>();

    private List<Map<String, Object>> triggers = new ArrayList<>();

    private Map<String, Object> inputSchema = new LinkedHashMap<>();
}
