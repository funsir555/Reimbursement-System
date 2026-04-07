package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ea_agent_version")
public class ArchiveAgentVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentId;

    private Integer versionNo;

    private String versionLabel;

    private String configJson;

    private Integer published;

    private Long createdByUserId;

    private String createdByName;

    private LocalDateTime createdAt;
}
