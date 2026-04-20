package com.finex.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OcrProviderConfigVO {

    private Long id;

    private String providerCode;

    private String providerName;

    private Boolean enabled;

    private String accessKeyId;

    private Boolean hasSecret;

    private String maskedSecret;

    private String endpoint;

    private Integer connectTimeoutMs;

    private Integer readTimeoutMs;

    private LocalDateTime lastTestAt;

    private String lastTestStatus;

    private String lastTestMessage;
}
