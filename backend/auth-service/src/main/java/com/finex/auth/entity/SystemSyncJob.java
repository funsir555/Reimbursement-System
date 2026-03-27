package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_sync_job")
public class SystemSyncJob {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String jobNo;

    private String platformCode;

    private String triggerType;

    private String status;

    private Integer successCount;

    private Integer skippedCount;

    private Integer failedCount;

    private Integer deletedCount;

    private String summary;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;
}
