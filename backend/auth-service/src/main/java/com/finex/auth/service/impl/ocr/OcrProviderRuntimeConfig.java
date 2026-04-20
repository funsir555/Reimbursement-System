package com.finex.auth.service.impl.ocr;

public record OcrProviderRuntimeConfig(
        String providerCode,
        String providerName,
        String accessKeyId,
        String accessKeySecret,
        String endpoint,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}
