package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_async_task")
public class AsyncTaskRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;

    private Long userId;

    private String taskType;

    private String businessType;

    private String businessKey;

    private String displayName;

    private String status;

    private Integer progress;

    private String resultMessage;

    private String resultPayload;

    private Long downloadRecordId;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
