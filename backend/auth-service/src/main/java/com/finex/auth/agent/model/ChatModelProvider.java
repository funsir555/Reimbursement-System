package com.finex.auth.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 聊天模型 Provider 接口
 * 统一的模型调用抽象
 */
public interface ChatModelProvider {

    /**
     * 获取 Provider 代码
     */
    String getProviderCode();

    /**
     * 获取模型能力
     */
    ModelCapability getCapability();

    /**
     * 聊天补全（同步）
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 聊天补全（流式）
     */
    ChatResponse chatStream(ChatRequest request, StreamHandler handler);

    /**
     * 聊天请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ChatRequest {
        /**
         * 消息列表
         */
        private List<ChatMessage> messages;

        /**
         * 系统提示词
         */
        private String systemPrompt;

        /**
         * 温度（0-1）
         */
        private Double temperature;

        /**
         * 最大输出 Token
         */
        private Integer maxTokens;

        /**
         * 是否流式输出
         */
        private boolean stream;

        /**
         * 附加参数
         */
        private Map<String, Object> extraParams;
    }

    /**
     * 聊天消息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ChatMessage {
        /**
         * 角色（user/assistant/system）
         */
        private String role;

        /**
         * 内容
         */
        private String content;
    }

    /**
     * 聊天响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ChatResponse {
        /**
         * 响应内容
         */
        private String content;

        /**
         * 使用的模型
         */
        private String model;

        /**
         * Token 使用情况
         */
        private TokenUsage tokenUsage;

        /**
         * 是否成功
         */
        private boolean success;

        /**
         * 错误信息
         */
        private String errorMessage;

        /**
         * 延迟（毫秒）
         */
        private Integer latencyMs;
    }

    /**
     * Token 使用情况
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class TokenUsage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
    }

    /**
     * 流式处理器
     */
    @FunctionalInterface
    interface StreamHandler {
        /**
         * 处理流式数据块
         */
        void onChunk(String chunk);
    }
}
