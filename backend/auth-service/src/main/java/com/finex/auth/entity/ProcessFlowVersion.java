package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_process_flow_version")
public class ProcessFlowVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long flowId;

    private Integer versionNo;

    private String versionStatus;

    private String snapshotJson;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
