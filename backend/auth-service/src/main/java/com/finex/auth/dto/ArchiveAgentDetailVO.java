package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ArchiveAgentDetailVO {

    private Long id;

    private String agentCode;

    private String agentName;

    private String agentDescription;

    private String iconKey;

    private String themeKey;

    private String coverColor;

    private List<String> tags = new ArrayList<>();

    private String status;

    private Integer latestVersionNo;

    private Integer publishedVersionNo;

    private Map<String, Object> promptConfig = new LinkedHashMap<>();

    private Map<String, Object> modelConfig = new LinkedHashMap<>();

    private List<Map<String, Object>> tools = new ArrayList<>();

    private Map<String, Object> workflow = new LinkedHashMap<>();

    private List<Map<String, Object>> triggers = new ArrayList<>();

    private Map<String, Object> inputSchema = new LinkedHashMap<>();

    private List<ArchiveAgentVersionVO> versions = new ArrayList<>();

    private String lastRunStatus;

    private String lastRunSummary;

    private String lastRunAt;
}
