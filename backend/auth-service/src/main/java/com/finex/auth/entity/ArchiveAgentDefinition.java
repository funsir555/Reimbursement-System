package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ea_agent_definition")
public class ArchiveAgentDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String agentCode;

    private Long ownerUserId;

    private String agentName;

    private String agentDescription;

    private String iconKey;

    private String themeKey;

    private String coverColor;

    private String tagsJson;

    private String status;

    private Integer latestVersionNo;

    private Long publishedVersionId;

    private Long lastRunId;

    private String lastRunStatus;

    private String lastRunSummary;

    private LocalDateTime lastRunAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
