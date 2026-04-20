package com.finex.auth.service.impl.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.OcrProviderConfigVO;
import com.finex.auth.dto.OcrProviderSaveDTO;
import com.finex.auth.entity.SystemOcrProviderConfig;
import com.finex.auth.mapper.SystemOcrProviderConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsApiInterfaceDomainSupportTest {

    @Mock
    private SystemOcrProviderConfigMapper systemOcrProviderConfigMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SettingsApiInterfaceDomainSupport domainSupport;

    @BeforeEach
    void setUp() {
        domainSupport = new SettingsApiInterfaceDomainSupport(
                systemOcrProviderConfigMapper,
                objectMapper,
                List.of()
        );
        when(systemOcrProviderConfigMapper.selectCount(any())).thenReturn(1L);
    }

    @Test
    void updateOcrProviderAllowsBlankSecretWhenAccessKeyIdIsUnchanged() throws Exception {
        SystemOcrProviderConfig provider = existingAliyunProvider("old-ak", "old-secret");
        when(systemOcrProviderConfigMapper.selectOne(any())).thenReturn(provider);

        OcrProviderSaveDTO dto = new OcrProviderSaveDTO();
        dto.setEnabled(0);
        dto.setAccessKeyId("old-ak");
        dto.setAccessKeySecret("");
        dto.setEndpoint("ocr-api.cn-hangzhou.aliyuncs.com");
        dto.setConnectTimeoutMs(6000);
        dto.setReadTimeoutMs(20000);

        OcrProviderConfigVO result = domainSupport.updateOcrProvider("aliyun", dto);
        JsonNode savedConfig = objectMapper.readTree(provider.getConfigJson());

        assertEquals("old-ak", savedConfig.path("accessKeyId").asText());
        assertEquals("old-secret", savedConfig.path("accessKeySecret").asText());
        assertEquals("ocr-api.cn-hangzhou.aliyuncs.com", savedConfig.path("endpoint").asText());
        assertEquals(6000, savedConfig.path("connectTimeoutMs").asInt());
        assertEquals(20000, savedConfig.path("readTimeoutMs").asInt());
        assertEquals("old-ak", result.getAccessKeyId());
        assertTrue(Boolean.TRUE.equals(result.getHasSecret()));
        verify(systemOcrProviderConfigMapper).updateById(provider);
    }

    @Test
    void updateOcrProviderRejectsBlankSecretWhenAccessKeyIdChanges() {
        SystemOcrProviderConfig provider = existingAliyunProvider("old-ak", "old-secret");
        when(systemOcrProviderConfigMapper.selectOne(any())).thenReturn(provider);

        OcrProviderSaveDTO dto = new OcrProviderSaveDTO();
        dto.setEnabled(0);
        dto.setAccessKeyId("new-ak");
        dto.setAccessKeySecret("   ");
        dto.setEndpoint("ocr-api.cn-hangzhou.aliyuncs.com");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> domainSupport.updateOcrProvider("ALIYUN", dto)
        );

        assertEquals("更换 AccessKey ID 时，必须同时重新填写 AccessKey Secret", error.getMessage());
        verify(systemOcrProviderConfigMapper, never()).updateById(any(SystemOcrProviderConfig.class));
    }

    private SystemOcrProviderConfig existingAliyunProvider(String accessKeyId, String accessKeySecret) {
        SystemOcrProviderConfig provider = new SystemOcrProviderConfig();
        provider.setId(1L);
        provider.setProviderCode("ALIYUN");
        provider.setProviderName("阿里云");
        provider.setEnabled(0);
        try {
            provider.setConfigJson(objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "accessKeyId", accessKeyId,
                            "accessKeySecret", accessKeySecret,
                            "endpoint", "ocr-api.cn-hangzhou.aliyuncs.com",
                            "connectTimeoutMs", 5000,
                            "readTimeoutMs", 15000
                    )
            ));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        provider.setLastTestStatus("IDLE");
        provider.setLastTestMessage("尚未测试");
        return provider;
    }
}
