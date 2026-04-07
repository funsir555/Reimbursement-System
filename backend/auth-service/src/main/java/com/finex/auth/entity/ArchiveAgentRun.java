package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ea_agent_run")
public class ArchiveAgentRun {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String runNo;

    private Long agentId;

    private Long agentVersionId;

    private Long ownerUserId;

    private String triggerType;

    private String triggerSource;

    private String status;

    private String errorMessage;

    private String summary;

    private String inputJson;

    private String outputJson;

    private LocalDateTime scheduledFireAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Long durationMs;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
