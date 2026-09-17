package com.finex.auth.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 安全配置
 * 支持 Demo 模式和 Enterprise 模式
 */
@Data
@Component
@ConfigurationProperties(prefix = "finex.agent.security")
public class AgentSecurityConfig {

    /**
     * 安全模式
     */
    private SecurityMode mode = SecurityMode.DEMO;

    /**
     * Demo 模式配置
     */
    private DemoConfig demo = new DemoConfig();

    @Data
    public static class DemoConfig {
        /**
         * 默认租户ID
         */
        private String defaultTenantId = "default";

        /**
         * 默认公司ID
         */
        private Long defaultCompanyId = 1L;

        /**
         * 回落时是否警告
         */
        private boolean warnOnFallback = true;
    }

    /**
     * 安全模式枚举
     */
    public enum SecurityMode {
        /**
         * Demo 模式：允许回落到默认租户
         */
        DEMO,

        /**
         * Enterprise 模式：缺失上下文直接拒绝
         */
        ENTERPRISE
    }
}
