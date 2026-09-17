package com.finex.auth.agent.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型路由配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "finex.agent.model")
public class ModelRoutingConfig {

    /**
     * 路由规则列表
     */
    private List<RoutingRule> rules = new ArrayList<>();

    /**
     * Provider 配置列表
     */
    private List<ProviderConfig> providers = new ArrayList<>();

    /**
     * 路由规则
     */
    @Data
    public static class RoutingRule {
        /**
         * 使用场景
         */
        private String useCase;

        /**
         * 数据敏感度
         */
        private String sensitivity;

        /**
         * 主要 Provider
         */
        private String primary;

        /**
         * 备用 Provider 列表
         */
        private List<String> fallback = new ArrayList<>();

        /**
         * 必需约束
         */
        private RequiredConstraints required;
    }

    /**
     * 必需约束
     */
    @Data
    public static class RequiredConstraints {
        /**
         * 数据驻留要求
         */
        private String dataResidency;
    }

    /**
     * Provider 配置
     */
    @Data
    public static class ProviderConfig {
        /**
         * Provider 代码
         */
        private String code;

        /**
         * API Key
         */
        private String apiKey;

        /**
         * API 端点
         */
        private String endpoint;

        /**
         * 模型路径（本地模型）
         */
        private String modelPath;

        /**
         * 能力配置
         */
        private CapabilityConfig capabilities;
    }

    /**
     * 能力配置
     */
    @Data
    public static class CapabilityConfig {
        private boolean chatCompletion = true;
        private boolean streamingSupport = false;
        private int maxContextTokens = 8000;
        private String deployment = "CLOUD";
        private String dataResidency = "CHINA";
        private int avgLatencyMs = 2000;
        private double costPerMillion = 20.0;
        private List<String> useCases = new ArrayList<>();
    }
}
