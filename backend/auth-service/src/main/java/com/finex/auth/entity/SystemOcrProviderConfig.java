package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_ocr_provider_config")
public class SystemOcrProviderConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String providerCode;

    private String providerName;

    private Integer enabled;

    private String configJson;

    private LocalDateTime lastTestAt;

    private String lastTestStatus;

    private String lastTestMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
