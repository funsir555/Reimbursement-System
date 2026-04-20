package com.finex.auth.dto;

import lombok.Data;

@Data
public class OcrProviderSaveDTO {

    private Integer enabled;

    private String accessKeyId;

    private String accessKeySecret;

    private String endpoint;

    private Integer connectTimeoutMs;

    private Integer readTimeoutMs;
}
