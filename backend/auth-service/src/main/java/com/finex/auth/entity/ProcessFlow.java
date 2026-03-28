package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_process_flow")
public class ProcessFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String flowCode;

    private String flowName;

    private String flowDescription;

    private String status;

    private Long currentDraftVersionId;

    private Long currentPublishedVersionId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
