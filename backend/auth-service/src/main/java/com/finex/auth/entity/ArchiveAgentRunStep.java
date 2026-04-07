package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ea_agent_run_step")
public class ArchiveAgentRunStep {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long runId;

    private Integer stepNo;

    private String nodeKey;

    private String nodeType;

    private String nodeLabel;

    private String status;

    private String errorMessage;

    private String inputJson;

    private String outputJson;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Long durationMs;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
