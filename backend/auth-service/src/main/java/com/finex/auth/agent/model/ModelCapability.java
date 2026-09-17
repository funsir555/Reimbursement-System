package com.finex.auth.agent.model;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * 模型能力声明
 */
@Data
@Builder
public class ModelCapability {

    /**
     * 功能能力
     */
    private boolean chatCompletion;
    private boolean streamingSupport;
    private boolean jsonMode;
    private boolean toolCalling;

    /**
     * 上下文能力
     */
    private int maxContextTokens;
    private int maxOutputTokens;

    /**
     * 部署方式
     */
    private ModelDeployment deployment;

    /**
     * 数据驻留要求
     */
    private DataResidency dataResidency;

    /**
     * 性能指标
     */
    private int avgLatencyMs;
    private double costPerMillion;

    /**
     * 适用场景
     */
    private Set<ModelUseCase> useCases;

    /**
     * 部署方式枚举
     */
    public enum ModelDeployment {
        CLOUD,       // 云端部署
        LOCAL,       // 本地部署
        ON_PREMISE   // 私有化部署
    }

    /**
     * 数据驻留枚举
     */
    public enum DataResidency {
        CHINA,       // 中国大陆
        GLOBAL,      // 全球
        ON_PREMISE   // 本地不出域
    }

    /**
     * 使用场景枚举
     */
    public enum ModelUseCase {
        CHAT_QUERY,      // 普通对话查询
        DEEP_ANALYSIS,   // 深度分析推理
        DATA_MASKING,    // 数据脱敏
        SUMMARIZATION    // 内容摘要
    }
}
