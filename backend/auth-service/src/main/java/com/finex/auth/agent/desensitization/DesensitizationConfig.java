package com.finex.auth.agent.desensitization;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 脱敏配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "desensitization")
public class DesensitizationConfig {

    /**
     * 敏感实体类型定义
     */
    private List<EntityType> entityTypes;

    /**
     * 脱敏策略
     */
    private List<Strategy> strategies;

    /**
     * 后验检测配置
     */
    private PostValidation postValidation;

    /**
     * 映射表配置
     */
    private MappingConfig mapping;

    @Data
    public static class EntityType {
        private String code;
        private String name;
        private String description;
        private String placeholderPrefix;
        private List<String> regexPatterns;
        private List<String> examples;
        private Boolean enabled = true;
    }

    @Data
    public static class Strategy {
        private String type;
        private Boolean enabled;
        private Integer priority;
    }

    @Data
    public static class PostValidation {
        private Boolean enabled;
        private List<LeakagePattern> leakagePatterns;
    }

    @Data
    public static class LeakagePattern {
        private String type;
        private String pattern;
        private String severity;
    }

    @Data
    public static class MappingConfig {
        private Integer ttlHours;
        private Boolean autoCleanup;
        private String cleanupCron;
    }
}
