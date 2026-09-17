package com.finex.auth.agent.model;

import com.finex.auth.agent.model.ModelCapability.DataResidency;
import com.finex.auth.agent.model.ModelCapability.ModelUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型路由服务
 * 负责根据场景和敏感度选择合适的模型 Provider
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRoutingService {

    private final ModelRoutingConfig config;
    private final Map<String, ChatModelProvider> providerMap = new HashMap<>();

    /**
     * 注册 Provider
     */
    public void registerProvider(ChatModelProvider provider) {
        providerMap.put(provider.getProviderCode(), provider);
        log.info("注册模型 Provider: {}", provider.getProviderCode());
    }

    /**
     * 根据使用场景和敏感度选择 Provider
     */
    public ChatModelProvider selectProvider(ModelUseCase useCase, DataSensitivity sensitivity) {
        // 查找匹配的路由规则
        ModelRoutingConfig.RoutingRule rule = findMatchingRule(useCase, sensitivity);

        if (rule == null) {
            log.warn("未找到匹配的路由规则，使用默认 Provider");
            return getDefaultProvider();
        }

        // 尝试主要 Provider
        String primaryCode = rule.getPrimary();
        ChatModelProvider primary = providerMap.get(primaryCode);

        if (primary != null && isProviderAvailable(primary, rule)) {
            log.debug("选择主要 Provider: {}", primaryCode);
            return primary;
        }

        // 尝试备用 Provider
        for (String fallbackCode : rule.getFallback()) {
            ChatModelProvider fallback = providerMap.get(fallbackCode);
            if (fallback != null && isProviderAvailable(fallback, rule)) {
                log.warn("主要 Provider 不可用，切换到备用 Provider: {}", fallbackCode);
                return fallback;
            }
        }

        log.error("所有配置的 Provider 都不可用");
        return null;
    }

    /**
     * 获取已注册 Provider。
     */
    public List<ChatModelProvider> getRegisteredProviders() {
        return List.copyOf(providerMap.values());
    }

    /**
     * 按用户指定的 Provider 优先调用，失败后仍按路由规则降级。
     */
    public ChatModelProvider.ChatResponse chatWithPreferredProvider(
            String preferredProviderCode,
            ModelUseCase useCase,
            DataSensitivity sensitivity,
            ChatModelProvider.ChatRequest request) {

        String providerCode = preferredProviderCode == null ? "" : preferredProviderCode.trim();
        if (providerCode.isEmpty()) {
            return chatWithFallback(useCase, sensitivity, request);
        }

        ChatModelProvider provider = providerMap.get(providerCode);
        if (provider == null) {
            log.warn("用户指定的模型 Provider 未注册，回退到自动路由: provider={}", providerCode);
            return chatWithFallback(useCase, sensitivity, request);
        }

        if (!supportsUseCase(provider, useCase)) {
            log.warn("用户指定的模型 Provider 不支持当前场景，回退到自动路由: provider={}, useCase={}",
                    providerCode, useCase);
            return chatWithFallback(useCase, sensitivity, request);
        }

        try {
            long startTime = System.currentTimeMillis();
            ChatModelProvider.ChatResponse response = provider.chat(request);
            long latency = System.currentTimeMillis() - startTime;
            response.setLatencyMs((int) latency);

            if (response.isSuccess()) {
                log.info("指定模型调用成功: provider={}, latency={}ms, tokens={}",
                        providerCode,
                        latency,
                        response.getTokenUsage() != null ? response.getTokenUsage().getTotalTokens() : 0);
                return response;
            }

            log.warn("指定模型调用失败，准备降级: provider={}, error={}",
                    providerCode, response.getErrorMessage());
            return fallbackToNextProvider(useCase, sensitivity, request, providerCode);

        } catch (Exception e) {
            log.error("指定模型调用异常，准备降级: provider={}, error={}", providerCode, e.getMessage());
            return fallbackToNextProvider(useCase, sensitivity, request, providerCode);
        }
    }

    /**
     * 带故障切换的聊天调用
     */
    public ChatModelProvider.ChatResponse chatWithFallback(
            ModelUseCase useCase,
            DataSensitivity sensitivity,
            ChatModelProvider.ChatRequest request) {

        ChatModelProvider provider = selectProvider(useCase, sensitivity);

        if (provider == null) {
            return ChatModelProvider.ChatResponse.builder()
                    .success(false)
                    .errorMessage("无可用的模型 Provider")
                    .build();
        }

        try {
            long startTime = System.currentTimeMillis();
            ChatModelProvider.ChatResponse response = provider.chat(request);
            long latency = System.currentTimeMillis() - startTime;

            response.setLatencyMs((int) latency);

            if (!response.isSuccess()) {
                log.warn("模型调用返回失败: provider={}, error={}",
                        provider.getProviderCode(), response.getErrorMessage());
                return fallbackToNextProvider(useCase, sensitivity, request, provider.getProviderCode());
            }

            log.info("模型调用成功: provider={}, latency={}ms, tokens={}",
                    provider.getProviderCode(),
                    latency,
                    response.getTokenUsage() != null ? response.getTokenUsage().getTotalTokens() : 0);

            return response;

        } catch (Exception e) {
            log.error("模型调用失败: provider={}, error={}",
                    provider.getProviderCode(), e.getMessage());

            // 尝试故障切换
            return fallbackToNextProvider(useCase, sensitivity, request, provider.getProviderCode());
        }
    }

    /**
     * 故障切换到下一个 Provider
     */
    private ChatModelProvider.ChatResponse fallbackToNextProvider(
            ModelUseCase useCase,
            DataSensitivity sensitivity,
            ChatModelProvider.ChatRequest request,
            String failedProviderCode) {

        ModelRoutingConfig.RoutingRule rule = findMatchingRule(useCase, sensitivity);

        if (rule == null || rule.getFallback().isEmpty()) {
            return ChatModelProvider.ChatResponse.builder()
                    .success(false)
                    .errorMessage("模型调用失败且无备用 Provider")
                    .build();
        }

        List<String> candidates = new ArrayList<>();
        candidates.add(rule.getPrimary());
        candidates.addAll(rule.getFallback());

        // 查找下一个可用 Provider
        for (String fallbackCode : candidates) {
            if (fallbackCode == null || fallbackCode.equals(failedProviderCode)) {
                continue; // 跳过已失败的 Provider
            }

            ChatModelProvider fallbackProvider = providerMap.get(fallbackCode);
            if (fallbackProvider == null) {
                continue;
            }

            try {
                log.warn("故障切换到备用 Provider: {}", fallbackCode);
                long startTime = System.currentTimeMillis();
                ChatModelProvider.ChatResponse response = fallbackProvider.chat(request);
                long latency = System.currentTimeMillis() - startTime;
                response.setLatencyMs((int) latency);
                if (response.isSuccess()) {
                    return response;
                }
                log.warn("备用 Provider 返回失败: provider={}, error={}",
                        fallbackCode, response.getErrorMessage());

            } catch (Exception e) {
                log.error("备用 Provider 调用也失败: provider={}, error={}",
                        fallbackCode, e.getMessage());
            }
        }

        return ChatModelProvider.ChatResponse.builder()
                .success(false)
                .errorMessage("所有 Provider 都调用失败")
                .build();
    }

    /**
     * 查找匹配的路由规则
     */
    private ModelRoutingConfig.RoutingRule findMatchingRule(
            ModelUseCase useCase,
            DataSensitivity sensitivity) {

        for (ModelRoutingConfig.RoutingRule rule : config.getRules()) {
            if (rule.getUseCase().equals(useCase.name()) &&
                rule.getSensitivity().equals(sensitivity.name())) {
                return rule;
            }
        }

        return null;
    }

    /**
     * 检查 Provider 是否可用
     */
    private boolean isProviderAvailable(
            ChatModelProvider provider,
            ModelRoutingConfig.RoutingRule rule) {

        // 检查必需约束
        if (rule.getRequired() != null && rule.getRequired().getDataResidency() != null) {
            String requiredResidency = rule.getRequired().getDataResidency();
            String providerResidency = provider.getCapability().getDataResidency().name();

            if (!providerResidency.equals(requiredResidency)) {
                log.debug("Provider {} 不满足数据驻留要求: required={}, actual={}",
                        provider.getProviderCode(), requiredResidency, providerResidency);
                return false;
            }
        }

        return true;
    }

    /**
     * 获取默认 Provider
     */
    private ChatModelProvider getDefaultProvider() {
        return providerMap.values().stream().findFirst().orElse(null);
    }

    private boolean supportsUseCase(ChatModelProvider provider, ModelUseCase useCase) {
        if (provider.getCapability() == null || provider.getCapability().getUseCases() == null) {
            return true;
        }
        return provider.getCapability().getUseCases().contains(useCase);
    }

    /**
     * 数据敏感度枚举
     */
    public enum DataSensitivity {
        LOW,    // 低敏感度
        HIGH    // 高敏感度
    }
}
