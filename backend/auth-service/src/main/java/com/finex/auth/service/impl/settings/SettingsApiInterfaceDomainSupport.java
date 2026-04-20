package com.finex.auth.service.impl.settings;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseAttachmentOcrResultVO;
import com.finex.auth.dto.OcrProviderConfigVO;
import com.finex.auth.dto.OcrProviderSaveDTO;
import com.finex.auth.entity.SystemOcrProviderConfig;
import com.finex.auth.mapper.SystemOcrProviderConfigMapper;
import com.finex.auth.service.impl.ocr.CloudInvoiceOcrProvider;
import com.finex.auth.service.impl.ocr.InvoiceOcrException;
import com.finex.auth.service.impl.ocr.InvoiceOcrRequest;
import com.finex.auth.service.impl.ocr.OcrProviderRuntimeConfig;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SettingsApiInterfaceDomainSupport {

    private static final String PROVIDER_ALIYUN = "ALIYUN";
    private static final String PROVIDER_TENCENT = "TENCENT";
    private static final String PROVIDER_BAIDU = "BAIDU";
    private static final String DEFAULT_ENDPOINT = "ocr-api.cn-hangzhou.aliyuncs.com";
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 15000;
    private static final String SAMPLE_RESOURCE_PATH = "fixtures/ocr/aliyun-invoice-sample.jpg";

    private static final String MESSAGE_NOT_TESTED = "尚未测试";
    private static final String MESSAGE_PENDING = "待接入";
    private static final String MESSAGE_TEST_PASSED = "测试通过";
    private static final String MESSAGE_TEST_FAILED = "测试失败";

    private final SystemOcrProviderConfigMapper systemOcrProviderConfigMapper;
    private final ObjectMapper objectMapper;
    private final Map<String, CloudInvoiceOcrProvider> providersByCode;

    public SettingsApiInterfaceDomainSupport(
            SystemOcrProviderConfigMapper systemOcrProviderConfigMapper,
            ObjectMapper objectMapper,
            List<CloudInvoiceOcrProvider> providers
    ) {
        this.systemOcrProviderConfigMapper = systemOcrProviderConfigMapper;
        this.objectMapper = objectMapper;
        this.providersByCode = providers.stream()
                .collect(Collectors.toMap(CloudInvoiceOcrProvider::getProviderCode, item -> item));
    }

    public List<OcrProviderConfigVO> listOcrProviders() {
        ensureDefaultProviders();
        return systemOcrProviderConfigMapper.selectList(
                Wrappers.<SystemOcrProviderConfig>lambdaQuery().orderByAsc(SystemOcrProviderConfig::getId)
        ).stream().map(this::toVo).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public OcrProviderConfigVO updateOcrProvider(String providerCode, OcrProviderSaveDTO dto) {
        String normalizedCode = normalizeProviderCode(providerCode);
        ensureEditableProvider(normalizedCode);
        ensureDefaultProviders();

        SystemOcrProviderConfig provider = requireProviderConfig(normalizedCode);
        OcrProviderSettings previous = readSettings(provider.getConfigJson());
        validateSecretRetentionRule(dto, previous);
        OcrProviderSettings next = mergeAliyunSettings(dto, previous);
        validateAliyunSettings(next);

        provider.setProviderName(resolveProviderName(normalizedCode));
        provider.setEnabled(dto.getEnabled() != null && dto.getEnabled() == 1 ? 1 : 0);
        provider.setConfigJson(writeSettings(next));
        systemOcrProviderConfigMapper.updateById(provider);
        if (provider.getEnabled() != null && provider.getEnabled() == 1) {
            disableOtherProviders(normalizedCode);
        }
        return toVo(requireProviderConfig(normalizedCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public OcrProviderConfigVO testOcrProvider(String providerCode) {
        String normalizedCode = normalizeProviderCode(providerCode);
        ensureEditableProvider(normalizedCode);
        ensureDefaultProviders();

        SystemOcrProviderConfig provider = requireProviderConfig(normalizedCode);
        OcrProviderRuntimeConfig runtimeConfig = buildRuntimeConfig(provider);
        CloudInvoiceOcrProvider ocrProvider = requireProvider(normalizedCode);

        LocalDateTime now = LocalDateTime.now();
        try {
            InvoiceOcrRequest request = buildConfigTestRequest();
            ExpenseAttachmentOcrResultVO result = ocrProvider.recognize(request, runtimeConfig);
            provider.setLastTestAt(now);
            provider.setLastTestStatus(result.getStatus());
            provider.setLastTestMessage(StrUtil.blankToDefault(result.getMessage(), MESSAGE_TEST_PASSED));
        } catch (InvoiceOcrException ex) {
            provider.setLastTestAt(now);
            provider.setLastTestStatus(ex.getStatus());
            provider.setLastTestMessage(StrUtil.blankToDefault(ex.getMessage(), MESSAGE_TEST_FAILED));
        } catch (Exception ex) {
            provider.setLastTestAt(now);
            provider.setLastTestStatus("FAILED");
            provider.setLastTestMessage(StrUtil.blankToDefault(ex.getMessage(), MESSAGE_TEST_FAILED));
        }
        systemOcrProviderConfigMapper.updateById(provider);
        return toVo(requireProviderConfig(normalizedCode));
    }

    public ResolvedOcrProvider resolveEnabledProvider() {
        ensureDefaultProviders();
        SystemOcrProviderConfig provider = systemOcrProviderConfigMapper.selectOne(
                Wrappers.<SystemOcrProviderConfig>lambdaQuery()
                        .eq(SystemOcrProviderConfig::getEnabled, 1)
                        .orderByAsc(SystemOcrProviderConfig::getId)
                        .last("limit 1")
        );
        if (provider == null) {
            return null;
        }
        CloudInvoiceOcrProvider ocrProvider = providersByCode.get(normalizeProviderCode(provider.getProviderCode()));
        if (ocrProvider == null) {
            return new ResolvedOcrProvider(provider.getProviderCode(), provider.getProviderName(), null);
        }
        return new ResolvedOcrProvider(
                provider.getProviderCode(),
                provider.getProviderName(),
                new ProviderRuntime(ocrProvider, buildRuntimeConfig(provider))
        );
    }

    private void ensureDefaultProviders() {
        ensureProvider(PROVIDER_ALIYUN);
        ensureProvider(PROVIDER_TENCENT);
        ensureProvider(PROVIDER_BAIDU);
    }

    private void ensureProvider(String providerCode) {
        boolean exists = systemOcrProviderConfigMapper.selectCount(
                Wrappers.<SystemOcrProviderConfig>lambdaQuery().eq(SystemOcrProviderConfig::getProviderCode, providerCode)
        ) > 0;
        if (exists) {
            return;
        }
        SystemOcrProviderConfig provider = new SystemOcrProviderConfig();
        provider.setProviderCode(providerCode);
        provider.setProviderName(resolveProviderName(providerCode));
        provider.setEnabled(0);
        provider.setConfigJson(writeSettings(defaultSettings()));
        provider.setLastTestStatus("IDLE");
        provider.setLastTestMessage(PROVIDER_ALIYUN.equals(providerCode) ? MESSAGE_NOT_TESTED : MESSAGE_PENDING);
        systemOcrProviderConfigMapper.insert(provider);
    }

    private SystemOcrProviderConfig requireProviderConfig(String providerCode) {
        SystemOcrProviderConfig provider = systemOcrProviderConfigMapper.selectOne(
                Wrappers.<SystemOcrProviderConfig>lambdaQuery()
                        .eq(SystemOcrProviderConfig::getProviderCode, providerCode)
                        .last("limit 1")
        );
        if (provider == null) {
            throw new IllegalArgumentException("OCR 厂商配置不存在");
        }
        return provider;
    }

    private void validateSecretRetentionRule(OcrProviderSaveDTO dto, OcrProviderSettings previous) {
        String submittedAccessKeyId = trimToNull(dto == null ? null : dto.getAccessKeyId());
        String submittedAccessKeySecret = trimToNull(dto == null ? null : dto.getAccessKeySecret());
        if (StrUtil.isBlank(submittedAccessKeyId)) {
            return;
        }
        String savedAccessKeyId = previous == null ? null : trimToNull(previous.accessKeyId);
        if (Objects.equals(submittedAccessKeyId, savedAccessKeyId)) {
            return;
        }
        if (StrUtil.isBlank(submittedAccessKeySecret)) {
            throw new IllegalArgumentException("更换 AccessKey ID 时，必须同时重新填写 AccessKey Secret");
        }
    }

    private OcrProviderSettings mergeAliyunSettings(OcrProviderSaveDTO dto, OcrProviderSettings previous) {
        OcrProviderSettings next = defaultSettings();
        next.accessKeyId = trimToNull(dto == null ? null : dto.getAccessKeyId());
        next.accessKeySecret = trimToNull(dto == null ? null : dto.getAccessKeySecret());
        next.endpoint = StrUtil.blankToDefault(trimToNull(dto == null ? null : dto.getEndpoint()), DEFAULT_ENDPOINT);
        next.connectTimeoutMs = dto == null || dto.getConnectTimeoutMs() == null || dto.getConnectTimeoutMs() <= 0
                ? DEFAULT_CONNECT_TIMEOUT_MS
                : dto.getConnectTimeoutMs();
        next.readTimeoutMs = dto == null || dto.getReadTimeoutMs() == null || dto.getReadTimeoutMs() <= 0
                ? DEFAULT_READ_TIMEOUT_MS
                : dto.getReadTimeoutMs();
        if (StrUtil.isBlank(next.accessKeyId) && previous != null) {
            next.accessKeyId = previous.accessKeyId;
        }
        if (StrUtil.isBlank(next.accessKeySecret) && previous != null) {
            next.accessKeySecret = previous.accessKeySecret;
        }
        return next;
    }

    private void validateAliyunSettings(OcrProviderSettings settings) {
        if (StrUtil.isBlank(settings.accessKeyId)) {
            throw new IllegalArgumentException("阿里云 AccessKey ID 不能为空");
        }
        if (StrUtil.isBlank(settings.accessKeySecret)) {
            throw new IllegalArgumentException("阿里云 AccessKey Secret 不能为空");
        }
        if (StrUtil.isBlank(settings.endpoint)) {
            throw new IllegalArgumentException("阿里云 Endpoint 不能为空");
        }
    }

    private void ensureEditableProvider(String providerCode) {
        if (!PROVIDER_ALIYUN.equals(providerCode)) {
            throw new IllegalArgumentException(resolveProviderName(providerCode) + " OCR 待接入，当前不支持保存或测试配置");
        }
    }

    private void disableOtherProviders(String activeProviderCode) {
        List<SystemOcrProviderConfig> others = systemOcrProviderConfigMapper.selectList(
                Wrappers.<SystemOcrProviderConfig>lambdaQuery().ne(SystemOcrProviderConfig::getProviderCode, activeProviderCode)
        );
        others.forEach(item -> {
            if (!Objects.equals(item.getEnabled(), 0)) {
                item.setEnabled(0);
                systemOcrProviderConfigMapper.updateById(item);
            }
        });
    }

    private OcrProviderConfigVO toVo(SystemOcrProviderConfig provider) {
        OcrProviderSettings settings = readSettings(provider.getConfigJson());
        OcrProviderConfigVO vo = new OcrProviderConfigVO();
        vo.setId(provider.getId());
        vo.setProviderCode(provider.getProviderCode());
        vo.setProviderName(provider.getProviderName());
        vo.setEnabled(provider.getEnabled() != null && provider.getEnabled() == 1);
        vo.setAccessKeyId(settings.accessKeyId);
        vo.setHasSecret(StrUtil.isNotBlank(settings.accessKeySecret));
        vo.setMaskedSecret(maskSecret(settings.accessKeySecret));
        vo.setEndpoint(StrUtil.blankToDefault(settings.endpoint, DEFAULT_ENDPOINT));
        vo.setConnectTimeoutMs(settings.connectTimeoutMs <= 0 ? DEFAULT_CONNECT_TIMEOUT_MS : settings.connectTimeoutMs);
        vo.setReadTimeoutMs(settings.readTimeoutMs <= 0 ? DEFAULT_READ_TIMEOUT_MS : settings.readTimeoutMs);
        vo.setLastTestAt(provider.getLastTestAt());
        vo.setLastTestStatus(provider.getLastTestStatus());
        vo.setLastTestMessage(provider.getLastTestMessage());
        return vo;
    }

    private OcrProviderSettings readSettings(String configJson) {
        if (StrUtil.isBlank(configJson)) {
            return defaultSettings();
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(configJson, new TypeReference<>() {
            });
            OcrProviderSettings settings = defaultSettings();
            settings.accessKeyId = trimToNull(asString(raw.get("accessKeyId")));
            settings.accessKeySecret = trimToNull(asString(raw.get("accessKeySecret")));
            settings.endpoint = StrUtil.blankToDefault(trimToNull(asString(raw.get("endpoint"))), DEFAULT_ENDPOINT);
            settings.connectTimeoutMs = asPositiveInt(raw.get("connectTimeoutMs"), DEFAULT_CONNECT_TIMEOUT_MS);
            settings.readTimeoutMs = asPositiveInt(raw.get("readTimeoutMs"), DEFAULT_READ_TIMEOUT_MS);
            return settings;
        } catch (Exception ex) {
            return defaultSettings();
        }
    }

    private String writeSettings(OcrProviderSettings settings) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("accessKeyId", settings.accessKeyId);
            payload.put("accessKeySecret", settings.accessKeySecret);
            payload.put("endpoint", StrUtil.blankToDefault(settings.endpoint, DEFAULT_ENDPOINT));
            payload.put("connectTimeoutMs", settings.connectTimeoutMs <= 0 ? DEFAULT_CONNECT_TIMEOUT_MS : settings.connectTimeoutMs);
            payload.put("readTimeoutMs", settings.readTimeoutMs <= 0 ? DEFAULT_READ_TIMEOUT_MS : settings.readTimeoutMs);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("OCR 厂商配置序列化失败，请检查输入内容", ex);
        }
    }

    private OcrProviderRuntimeConfig buildRuntimeConfig(SystemOcrProviderConfig provider) {
        OcrProviderSettings settings = readSettings(provider.getConfigJson());
        validateAliyunSettings(settings);
        return new OcrProviderRuntimeConfig(
                normalizeProviderCode(provider.getProviderCode()),
                resolveProviderName(provider.getProviderCode()),
                settings.accessKeyId,
                settings.accessKeySecret,
                StrUtil.blankToDefault(settings.endpoint, DEFAULT_ENDPOINT),
                settings.connectTimeoutMs <= 0 ? DEFAULT_CONNECT_TIMEOUT_MS : settings.connectTimeoutMs,
                settings.readTimeoutMs <= 0 ? DEFAULT_READ_TIMEOUT_MS : settings.readTimeoutMs
        );
    }

    private CloudInvoiceOcrProvider requireProvider(String providerCode) {
        return Optional.ofNullable(providersByCode.get(providerCode))
                .orElseThrow(() -> new IllegalArgumentException(resolveProviderName(providerCode) + " OCR 待接入"));
    }

    private InvoiceOcrRequest buildConfigTestRequest() throws Exception {
        byte[] bytes = loadSampleBytes();
        return new InvoiceOcrRequest("aliyun-invoice-sample.jpg", "image/jpeg", bytes, null);
    }

    private byte[] loadSampleBytes() throws Exception {
        ClassPathResource resource = new ClassPathResource(SAMPLE_RESOURCE_PATH);
        if (!resource.exists()) {
            throw new IllegalStateException("OCR 测试样张不存在，请检查资源文件");
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    private OcrProviderSettings defaultSettings() {
        OcrProviderSettings settings = new OcrProviderSettings();
        settings.endpoint = DEFAULT_ENDPOINT;
        settings.connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
        settings.readTimeoutMs = DEFAULT_READ_TIMEOUT_MS;
        return settings;
    }

    private String maskSecret(String secret) {
        if (StrUtil.isBlank(secret)) {
            return "";
        }
        String trimmed = secret.trim();
        if (trimmed.length() <= 6) {
            return "******";
        }
        return trimmed.substring(0, 3) + "******" + trimmed.substring(trimmed.length() - 3);
    }

    private String resolveProviderName(String providerCode) {
        return switch (normalizeProviderCode(providerCode)) {
            case PROVIDER_ALIYUN -> "阿里云";
            case PROVIDER_TENCENT -> "腾讯云";
            case PROVIDER_BAIDU -> "百度云";
            default -> providerCode == null ? "OCR" : providerCode.trim().toUpperCase(Locale.ROOT);
        };
    }

    private String normalizeProviderCode(String providerCode) {
        if (StrUtil.isBlank(providerCode)) {
            throw new IllegalArgumentException("OCR 厂商不能为空");
        }
        return providerCode.trim().toUpperCase(Locale.ROOT);
    }

    private int asPositiveInt(Object rawValue, int defaultValue) {
        if (rawValue instanceof Number number && number.intValue() > 0) {
            return number.intValue();
        }
        if (rawValue instanceof String text) {
            try {
                int parsed = Integer.parseInt(text.trim());
                return parsed > 0 ? parsed : defaultValue;
            } catch (Exception ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static final class OcrProviderSettings {
        private String accessKeyId;
        private String accessKeySecret;
        private String endpoint;
        private int connectTimeoutMs;
        private int readTimeoutMs;
    }

    public record ProviderRuntime(
            CloudInvoiceOcrProvider provider,
            OcrProviderRuntimeConfig config
    ) {
    }

    public record ResolvedOcrProvider(
            String providerCode,
            String providerName,
            ProviderRuntime runtime
    ) {
        public boolean isConfigured() {
            return runtime != null;
        }
    }
}
