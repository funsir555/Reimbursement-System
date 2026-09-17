package com.finex.auth.support.archiveagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * DeepSeek API 真实实现
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "DEEPSEEK_API_KEY")
public class DeepSeekModelProviderAdapter implements ModelProviderAdapter {

    @Value("${DEEPSEEK_API_KEY}")
    private String apiKey;

    @Value("${DEEPSEEK_ENDPOINT:https://api.deepseek.com/v1/chat/completions}")
    private String endpoint;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String getProviderCode() {
        return "DEEPSEEK";
    }

    @Override
    public InvocationResult invoke(InvocationContext context) {
        try {
            log.debug("调用 DeepSeek API: endpoint={}", endpoint);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", context.modelConfig().getOrDefault("model", "deepseek-chat"));

            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();

            // 添加系统提示
            Object systemPrompt = context.promptConfig().get("system_prompt");
            if (systemPrompt != null && !systemPrompt.toString().isEmpty()) {
                Map<String, String> systemMsg = new HashMap<>();
                systemMsg.put("role", "system");
                systemMsg.put("content", systemPrompt.toString());
                messages.add(systemMsg);
            }

            // 添加历史消息
            Object messagesObj = context.promptConfig().get("messages");
            if (messagesObj instanceof List) {
                List<?> msgList = (List<?>) messagesObj;
                for (Object msgObj : msgList) {
                    if (msgObj instanceof Map) {
                        Map<?, ?> msg = (Map<?, ?>) msgObj;
                        Map<String, String> message = new HashMap<>();
                        message.put("role", String.valueOf(msg.get("role")));
                        message.put("content", String.valueOf(msg.get("content")));
                        messages.add(message);
                    }
                }
            }

            requestBody.put("messages", messages);

            // 添加可选参数
            requestBody.put("temperature", context.modelConfig().getOrDefault("temperature", 0.7));
            requestBody.put("max_tokens", context.modelConfig().getOrDefault("max_tokens", 2000));
            requestBody.put("stream", false);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            log.debug("DeepSeek 请求体: {}", requestJson);

            // 发送 HTTP 请求
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("DeepSeek API 调用失败: status={}, body={}", response.statusCode(), response.body());
                throw new RuntimeException("DeepSeek API 返回错误: " + response.statusCode());
            }

            // 解析响应
            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            log.debug("DeepSeek 响应: {}", responseBody);

            String content = extractContent(responseBody);
            Map<String, Object> outputPayload = extractOutputPayload(responseBody);

            log.info("DeepSeek API 调用成功: contentLength={}, tokens={}",
                    content != null ? content.length() : 0,
                    outputPayload.get("total_tokens"));

            return new InvocationResult(content, outputPayload);

        } catch (Exception e) {
            log.error("DeepSeek API 调用失败", e);
            throw new RuntimeException("DeepSeek API 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 提取响应内容
     */
    private String extractContent(Map<String, Object> responseBody) {
        // DeepSeek 响应格式: {"choices": [{"message": {"content": "..."}}]}
        Object choicesObj = responseBody.get("choices");
        if (choicesObj instanceof List) {
            List<?> choices = (List<?>) choicesObj;
            if (!choices.isEmpty() && choices.get(0) instanceof Map) {
                Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                Object messageObj = choice.get("message");
                if (messageObj instanceof Map) {
                    Map<?, ?> message = (Map<?, ?>) messageObj;
                    Object content = message.get("content");
                    if (content != null) {
                        return content.toString();
                    }
                }
            }
        }
        return "";
    }

    /**
     * 提取输出负载（包含 token 使用情况）
     */
    private Map<String, Object> extractOutputPayload(Map<String, Object> responseBody) {
        Map<String, Object> payload = new HashMap<>();

        // 提取 usage 信息
        Object usageObj = responseBody.get("usage");
        if (usageObj instanceof Map) {
            Map<?, ?> usage = (Map<?, ?>) usageObj;
            payload.put("prompt_tokens", getIntValue(usage, "prompt_tokens", 0));
            payload.put("completion_tokens", getIntValue(usage, "completion_tokens", 0));
            payload.put("total_tokens", getIntValue(usage, "total_tokens", 0));
        }

        // 提取模型信息
        payload.put("model", responseBody.get("model"));

        return payload;
    }

    /**
     * 从 Map 中安全获取 int 值
     */
    private int getIntValue(Map<?, ?> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}
