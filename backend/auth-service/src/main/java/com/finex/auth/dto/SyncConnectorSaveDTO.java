package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SyncConnectorSaveDTO {

    @NotBlank(message = "同步平台不能为空")
    private String platformCode;

    private Integer enabled;

    private Integer autoSyncEnabled;

    private Integer syncIntervalMinutes;

    private String appKey;

    private String appSecret;

    private String appId;

    private String corpId;

    private String agentId;
}
