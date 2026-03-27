package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_sync_job_detail")
public class SystemSyncJobDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long jobId;

    private String detailType;

    private String actionType;

    private String businessKey;

    private String detailStatus;

    private String detailMessage;

    private LocalDateTime createdAt;
}
