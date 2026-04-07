package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ea_agent_trigger")
public class ArchiveAgentTrigger {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentId;

    private String triggerType;

    private Integer enabled;

    private String scheduleMode;

    private String cronExpression;

    private Integer intervalMinutes;

    private String eventCode;

    private String configJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
