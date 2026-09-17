package com.finex.auth.agent.model.impl;

import com.finex.auth.agent.model.ChatModelProvider;
import com.finex.auth.agent.model.ModelCapability;
import com.finex.auth.support.archiveagent.ModelProviderAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * DeepSeek Provider 实现
 * 适配现有的 ModelProviderAdapter 接口
 */
@Slf4j
@Component("deepseekProvider")
public class DeepSeekProvider implements ChatModelProvider {

    private final ModelProviderAdapter modelProviderAdapter;

    public DeepSeekProvider(@Qualifier("deepSeekModelProviderAdapter") ModelProviderAdapter modelProviderAdapter) {
        this.modelProviderAdapter = modelProviderAdapter;
    }

    @Override
    public String getProviderCode() {
        return "DEEPSEEK";
    }

    @Override
    public ModelCapability getCapability() {
        return ModelCapability.builder()
                .chatCompletion(true)
                .streamingSupport(true)
                .maxContextTokens(64000)
                .maxOutputTokens(4000)
                .deployment(ModelCapability.ModelDeployment.CLOUD)
                .dataResidency(ModelCapability.DataResidency.CHINA)
                .avgLatencyMs(1500)
                .costPerMillion(20.0)
                .useCases(Set.of(
                        ModelCapability.ModelUseCase.CHAT_QUERY,
                        ModelCapability.ModelUseCase.DEEP_ANALYSIS,
                        ModelCapability.ModelUseCase.SUMMARIZATION
                ))
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            long startTime = System.currentTimeMillis();

            // 构建 InvocationContext
            Map<String, Object> modelConfig = new HashMap<>();
            modelConfig.put("temperature", request.getTemperature() != null ? request.getTemperature() : 0.7);
            modelConfig.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 2000);
            String requestedModel = getStringExtraParam(request, "model");
            if (!requestedModel.isBlank()) {
                modelConfig.put("model", requestedModel);
            }

            Map<String, Object> promptConfig = new HashMap<>();
            if (request.getSystemPrompt() != null) {
                promptConfig.put("system_prompt", request.getSystemPrompt());
            }
            promptConfig.put("messages", convertMessages(request.getMessages()));

            ModelProviderAdapter.InvocationContext context = new ModelProviderAdapter.InvocationContext(
                    null,  // userId
                    "finwork-chat",
                    modelConfig,
                    promptConfig,
                    new HashMap<>(),
                    new HashMap<>()
            );

            // 调用现有的 ModelProviderAdapter
            ModelProviderAdapter.InvocationResult result = modelProviderAdapter.invoke(context);

            long latency = System.currentTimeMillis() - startTime;

            // 构建响应
            return ChatResponse.builder()
                    .content(result.content())
                    .model(requestedModel.isBlank() ? "deepseek-chat" : requestedModel)
                    .tokenUsage(extractTokenUsage(result.outputPayload()))
                    .success(true)
                    .latencyMs((int) latency)
                    .build();

        } catch (Exception e) {
            log.error("DeepSeek 调用失败", e);
            return ChatResponse.builder()
                    .success(false)
                    .errorMessage("DeepSeek 调用失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ChatResponse chatStream(ChatRequest request, StreamHandler handler) {
        // 流式实现可以后续添加
        log.warn("DeepSeek 流式调用暂未实现，回退到同步调用");
        return chat(request);
    }

    /**
     * 转换消息格式
     */
    private Object convertMessages(java.util.List<ChatMessage> messages) {
        if (messages == null) {
            return new java.util.ArrayList<>();
        }

        java.util.List<Map<String, String>> converted = new java.util.ArrayList<>();
        for (ChatMessage msg : messages) {
            Map<String, String> msgMap = new HashMap<>();
            msgMap.put("role", msg.getRole());
            msgMap.put("content", msg.getContent());
            converted.add(msgMap);
        }
        return converted;
    }

    private String getStringExtraParam(ChatRequest request, String key) {
        if (request == null || request.getExtraParams() == null) {
            return "";
        }
        Object value = request.getExtraParams().get(key);
        return value == null ? "" : value.toString().trim();
    }

    /**
     * 提取 Token 使用情况
     */
    private TokenUsage extractTokenUsage(Map<String, Object> outputPayload) {
        if (outputPayload == null) {
            return TokenUsage.builder()
                    .promptTokens(0)
                    .completionTokens(0)
                    .totalTokens(0)
                    .build();
        }

        int promptTokens = getIntValue(outputPayload, "prompt_tokens", 0);
        int completionTokens = getIntValue(outputPayload, "completion_tokens", 0);

        return TokenUsage.builder()
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .build();
    }

    /**
     * 从 Map 中安全获取 int 值
     */
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}
