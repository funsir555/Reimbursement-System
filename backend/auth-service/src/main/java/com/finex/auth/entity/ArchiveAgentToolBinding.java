package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ea_agent_tool_binding")
public class ArchiveAgentToolBinding {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentId;

    private String toolCode;

    private Integer enabled;

    private String credentialRefCode;

    private String configJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
