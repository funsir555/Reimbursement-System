package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ea_agent_run_artifact")
public class ArchiveAgentRunArtifact {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long runId;

    private String artifactKey;

    private String artifactType;

    private String artifactName;

    private String summary;

    private String contentJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
