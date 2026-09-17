package com.finex.auth.agent.desensitization;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 脱敏规则配置 DTO
 */
public class DesensitizationDTO {

    /**
     * 实体类型配置
     */
    @Data
    public static class EntityTypeDTO {
        private Long id;
        private String code;
        private String name;
        private String description;
        private String placeholderPrefix;
        private List<String> regexPatterns;
        private List<String> examples;
        private Boolean enabled;
    }

    /**
     * 实体类型保存请求
     */
    @Data
    public static class SaveEntityTypeRequest {
        private String code;
        private String name;
        private String description;
        private String placeholderPrefix;
        private List<String> regexPatterns;
        private List<String> examples;
        private Boolean enabled;
    }

    /**
     * 映射表查询响应
     */
    @Data
    public static class MappingTableDTO {
        private String id;
        private String sessionId;
        private Map<String, String> mappings;
        private java.time.Instant createdAt;
        private java.time.Instant expiresAt;
        private Boolean expired;
    }

    /**
     * 映射表保存请求
     */
    @Data
    public static class SaveMappingRequest {
        private String sessionId;
        private Map<String, String> mappings;
        private Integer ttlHours;
    }

    /**
     * 脱敏测试请求
     */
    @Data
    public static class TestRequest {
        private String text;
        private String strategy; // REGEX，保留字段用于接口兼容
    }

    /**
     * 脱敏测试响应
     */
    @Data
    public static class TestResponse {
        private String originalText;
        private String maskedText;
        private String aiResponse;
        private String restoredText;
        private String strategy;
        private String mappingId;
        private Map<String, String> mappings;
        private Boolean success;
        private String errorMessage;
    }

    /**
     * 映射表统计
     */
    @Data
    public static class MappingTableStats {
        private Long totalCount;
        private Long activeCount;
        private Long expiredCount;
        private Long last24hCount;
    }
}
