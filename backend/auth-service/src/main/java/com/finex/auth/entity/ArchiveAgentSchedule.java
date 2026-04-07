package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ea_agent_schedule")
public class ArchiveAgentSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long triggerId;

    private Long agentId;

    private String scheduleStatus;

    private LocalDateTime lastFireAt;

    private LocalDateTime nextFireAt;

    private Long lastRunId;

    private LocalDateTime lockedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
