package com.finex.auth.agent.desensitization;

import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 脱敏映射表
 */
@Data
public class MappingTable {

    /**
     * 映射表ID
     */
    private String id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 正向映射：占位符 -> 原始值
     */
    private Map<String, String> placeholderToOriginal = new HashMap<>();

    /**
     * 反向映射：原始值 -> 占位符
     */
    private Map<String, String> originalToPlaceholder = new HashMap<>();

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 过期时间
     */
    private Instant expiresAt;

    /**
     * 添加映射
     */
    public void addMapping(String placeholder, String originalValue) {
        placeholderToOriginal.put(placeholder, originalValue);
        originalToPlaceholder.put(originalValue, placeholder);
    }

    /**
     * 获取原始值
     */
    public String getOriginal(String placeholder) {
        return placeholderToOriginal.get(placeholder);
    }

    /**
     * 获取占位符
     */
    public String getPlaceholder(String originalValue) {
        return originalToPlaceholder.get(originalValue);
    }

    /**
     * 还原文本
     */
    public String restore(String maskedText) {
        if (maskedText == null || maskedText.isEmpty()) {
            return maskedText;
        }

        String result = maskedText;

        // 替换所有占位符
        Pattern pattern = Pattern.compile("\\[(\\w+_\\d+)\\]");
        Matcher matcher = pattern.matcher(maskedText);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String original = placeholderToOriginal.get(placeholder);
            if (original != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(original));
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 是否过期
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
