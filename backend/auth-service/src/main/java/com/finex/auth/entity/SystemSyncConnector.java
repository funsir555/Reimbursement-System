package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_sync_connector")
public class SystemSyncConnector {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platformCode;

    private String platformName;

    private Integer enabled;

    private Integer autoSyncEnabled;

    private Integer syncIntervalMinutes;

    private String configJson;

    private LocalDateTime lastSyncAt;

    private String lastSyncStatus;

    private String lastSyncMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
