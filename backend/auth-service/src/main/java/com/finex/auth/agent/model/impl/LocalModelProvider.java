package com.finex.auth.agent.model.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.model.ChatModelProvider;
import com.finex.auth.agent.model.ModelCapability;
import com.finex.auth.agent.model.ModelRoutingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Ollama 本地模型 Provider 实现
 * 支持通过 Ollama API 调用本地部署的大语言模型
 */
@Slf4j
@Component("localModelProvider")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "finex.agent.model.local", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LocalModelProvider implements ChatModelProvider {

    @Value("${LOCAL_MODEL_ENDPOINT:http://localhost:11434/api/chat}")
    private String endpoint;

    @Value("${LOCAL_MODEL_NAME:deepseek-r1:7b}")
    private String modelName;

    private final ModelRoutingConfig routingConfig;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String getProviderCode() {
        return "LOCAL_7B";
    }

    @Override
    public ModelCapability getCapability() {
        return ModelCapability.builder()
                .chatCompletion(true)
                .streamingSupport(true)
                .maxContextTokens(8192)
                .maxOutputTokens(2048)
                .deployment(ModelCapability.ModelDeployment.ON_PREMISE)
                .dataResidency(ModelCapability.DataResidency.ON_PREMISE)
                .avgLatencyMs(500)
                .costPerMillion(0.0)
                .useCases(Set.of(
                        ModelCapability.ModelUseCase.CHAT_QUERY,
                        ModelCapability.ModelUseCase.DATA_MASKING,
                        ModelCapability.ModelUseCase.SUMMARIZATION
                ))
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            String resolvedEndpoint = resolveEndpoint();
            String resolvedModelName = resolveModelName(request);

            log.debug("调用本地模型: endpoint={}, model={}", resolvedEndpoint, resolvedModelName);
            long startTime = System.currentTimeMillis();

            // 构建 Ollama API 请求
            Map<String, Object> requestBody = requestBody(request, resolvedModelName, false);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            log.debug("Ollama 请求体: {}", requestJson);

            // 发送 HTTP 请求
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(resolvedEndpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            long latency = System.currentTimeMillis() - startTime;

            if (response.statusCode() != 200) {
                log.error("Ollama API 调用失败: status={}, body={}", response.statusCode(), response.body());
                return ChatResponse.builder()
                        .success(false)
                        .errorMessage("Ollama API 返回错误: " + response.statusCode())
                        .build();
            }

            // 解析响应
            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            String content = extractContent(responseBody);

            log.info("本地模型调用成功: model={}, latency={}ms, contentLength={}",
                    resolvedModelName, latency, content != null ? content.length() : 0);

            return ChatResponse.builder()
                    .content(content)
                    .model(resolvedModelName)
                    .tokenUsage(extractTokenUsage(responseBody))
                    .success(true)
                    .latencyMs((int) latency)
                    .build();

        } catch (Exception e) {
            log.error("本地模型调用失败: endpoint={}, model={}, error={}",
                    resolveEndpoint(), resolveModelName(request), e.getMessage(), e);
            return ChatResponse.builder()
                    .success(false)
                    .errorMessage("本地模型调用失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ChatResponse chatStream(ChatRequest request, StreamHandler handler) {
        try {
            long started = System.currentTimeMillis();
            String model = resolveModelName(request);
            HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(resolveEndpoint()))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(requestBody(request, model, true)))).build();
            HttpResponse<java.io.InputStream> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                response.body().close();
                throw new IllegalStateException("本地模型返回错误: " + response.statusCode());
            }
            StringBuilder content = new StringBuilder();
            Map<String, Object> last = Map.of();
            boolean completed = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    Map<String, Object> event = objectMapper.readValue(line, Map.class);
                    last = event;
                    Object message = event.get("message");
                    if (message instanceof Map<?, ?> data && data.get("content") instanceof String part
                            && !part.isEmpty()) {
                        content.append(part);
                        handler.onChunk(part);
                    }
                    if (Boolean.TRUE.equals(event.get("done"))) {
                        completed = true;
                        break;
                    }
                }
            }
            if (!completed) throw new IllegalStateException("本地模型流式响应未完整结束");
            return ChatResponse.builder().content(content.toString()).model(model)
                    .tokenUsage(extractTokenUsage(last)).success(true)
                    .latencyMs((int) (System.currentTimeMillis() - started)).build();
        } catch (Exception error) {
            log.error("本地模型流式调用失败", error);
            return ChatResponse.builder().success(false).errorMessage("本地模型流式调用失败").build();
        }
    }

    private Map<String, Object> requestBody(ChatRequest request, String model, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", convertMessages(request));
        body.put("stream", stream);
        Map<String, Object> options = new HashMap<>();
        if (request.getTemperature() != null) options.put("temperature", request.getTemperature());
        if (request.getMaxTokens() != null) options.put("num_predict", request.getMaxTokens());
        if (!options.isEmpty()) body.put("options", options);
        return body;
    }

    private String resolveEndpoint() {
        return findProviderConfig()
                .map(ModelRoutingConfig.ProviderConfig::getEndpoint)
                .filter(value -> !value.isBlank())
                .orElse(endpoint);
    }

    private String resolveModelName(ChatRequest request) {
        String requestModel = getStringExtraParam(request, "model");
        if (!requestModel.isBlank()) {
            return requestModel;
        }

        return findProviderConfig()
                .map(ModelRoutingConfig.ProviderConfig::getModelPath)
                .filter(value -> !value.isBlank())
                .orElse(modelName);
    }

    private String getStringExtraParam(ChatRequest request, String key) {
        if (request == null || request.getExtraParams() == null) {
            return "";
        }
        Object value = request.getExtraParams().get(key);
        return value == null ? "" : value.toString().trim();
    }

    private Optional<ModelRoutingConfig.ProviderConfig> findProviderConfig() {
        if (routingConfig.getProviders() == null) {
            return Optional.empty();
        }
        return routingConfig.getProviders().stream()
                .filter(provider -> getProviderCode().equals(provider.getCode()))
                .findFirst();
    }

    /**
     * 转换消息格式
     */
    private List<Map<String, String>> convertMessages(ChatRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 添加系统提示词
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", request.getSystemPrompt());
            messages.add(systemMsg);
        }

        // 添加历史消息
        if (request.getMessages() != null) {
            for (ChatMessage msg : request.getMessages()) {
                Map<String, String> message = new HashMap<>();
                message.put("role", msg.getRole());
                message.put("content", msg.getContent());
                messages.add(message);
            }
        }

        return messages;
    }

    /**
     * 提取响应内容
     */
    private String extractContent(Map<String, Object> responseBody) {
        // Ollama 响应格式: {"message": {"role": "assistant", "content": "..."}}
        Object messageObj = responseBody.get("message");
        if (messageObj instanceof Map) {
            Map<?, ?> message = (Map<?, ?>) messageObj;
            Object content = message.get("content");
            if (content != null) {
                return content.toString();
            }
        }
        return "";
    }

    /**
     * 提取 Token 使用情况
     */
    private TokenUsage extractTokenUsage(Map<String, Object> responseBody) {
        // Ollama 可能返回 prompt_eval_count 和 eval_count
        int promptTokens = getIntValue(responseBody, "prompt_eval_count", 0);
        int completionTokens = getIntValue(responseBody, "eval_count", 0);

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
