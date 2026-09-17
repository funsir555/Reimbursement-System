package com.finex.auth.agent.desensitization;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 脱敏后的数据
 */
@Data
@Builder
public class DesensitizedData {

    /**
     * 脱敏后的文本
     */
    private String maskedText;

    /**
     * 映射表ID
     */
    private String mappingId;

    /**
     * 脱敏策略
     */
    private String strategy;

    /**
     * 脱敏实体映射（用于调试）
     */
    private Map<String, String> entityMappings;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;
}
