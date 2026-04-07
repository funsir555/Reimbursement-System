package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ArchiveAgentSummaryVO {

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

    private String runtimeStatus;

    private String lastRunStatus;

    private String lastRunSummary;

    private String lastRunAt;

    private Integer enabledTriggerCount;
}
