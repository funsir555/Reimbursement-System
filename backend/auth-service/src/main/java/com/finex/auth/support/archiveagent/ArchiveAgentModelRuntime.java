package com.finex.auth.support.archiveagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.desensitization.DesensitizationService;
import com.finex.auth.agent.desensitization.DesensitizedData;
import com.finex.auth.agent.model.ChatModelProvider;
import com.finex.auth.agent.model.ModelCapability;
import com.finex.auth.agent.model.ModelRoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Executes workbench LLM nodes through the shared model router.
 */
@Component
@RequiredArgsConstructor
public class ArchiveAgentModelRuntime {

    private final ModelRoutingService modelRoutingService;
    private final DesensitizationService desensitizationService;
    private final ObjectMapper objectMapper;
    private final List<ModelProviderAdapter> modelProviderAdapters;

    public Map<String, Object> invoke(
            String sessionId,
            Long userId,
            String agentName,
            Map<String, Object> modelConfig,
            Map<String, Object> promptConfig,
            Map<String, Object> nodeConfig,
            Map<String, Object> runtimeContext
    ) {
        String providerCode = text(modelConfig.getOrDefault("provider", "MOCK")).toUpperCase(Locale.ROOT);
        if ("MOCK".equals(providerCode)) {
            return invokeMock(userId, agentName, modelConfig, promptConfig, nodeConfig, runtimeContext);
        }

        String contextJson = writeJson(runtimeContext);
        DesensitizedData desensitized = desensitizationService.desensitize(sessionId, contextJson);
        if (!desensitized.isSuccess() || desensitized.getMappingId() == null || desensitized.getMappingId().isBlank()) {
            throw new IllegalStateException("模型输入脱敏失败: " + desensitized.getErrorMessage());
        }

        String nodePrompt = text(nodeConfig.get("promptTemplate"));
        String systemPrompt = buildSystemPrompt(promptConfig);
        String userContent = nodePrompt.isBlank()
                ? desensitized.getMaskedText()
                : nodePrompt + "\n\n运行上下文：\n" + desensitized.getMaskedText();
        ChatModelProvider.ChatRequest request = ChatModelProvider.ChatRequest.builder()
                .systemPrompt(systemPrompt)
                .messages(List.of(ChatModelProvider.ChatMessage.builder()
                        .role("user")
                        .content(userContent)
                        .build()))
                .temperature(number(modelConfig.get("temperature"), 0.3D))
                .maxTokens(integer(modelConfig.get("maxTokens"), 1200))
                .extraParams(modelParams(modelConfig))
                .build();

        ChatModelProvider.ChatResponse response = modelRoutingService.chatWithPreferredProvider(
                providerCode,
                ModelCapability.ModelUseCase.CHAT_QUERY,
                ModelRoutingService.DataSensitivity.LOW,
                request
        );
        if (response == null || !response.isSuccess() || response.getContent() == null || response.getContent().isBlank()) {
            String error = response == null ? "模型未返回响应" : response.getErrorMessage();
            throw new IllegalStateException("模型调用失败: " + error);
        }

        String restoredContent = desensitizationService.restore(desensitized.getMappingId(), response.getContent());
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("provider", providerCode);
        output.put("model", response.getModel());
        output.put("content", restoredContent);
        output.put("mappingId", desensitized.getMappingId());
        output.put("latencyMs", response.getLatencyMs());
        return output;
    }

    private Map<String, Object> invokeMock(
            Long userId,
            String agentName,
            Map<String, Object> modelConfig,
            Map<String, Object> promptConfig,
            Map<String, Object> nodeConfig,
            Map<String, Object> runtimeContext
    ) {
        ModelProviderAdapter adapter = modelProviderAdapters.stream()
                .filter(item -> "MOCK".equalsIgnoreCase(item.getProviderCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Mock 模型提供商不可用"));
        return adapter.invoke(new ModelProviderAdapter.InvocationContext(
                userId,
                agentName,
                modelConfig,
                promptConfig,
                nodeConfig,
                runtimeContext
        )).outputPayload();
    }

    private String buildSystemPrompt(Map<String, Object> promptConfig) {
        return List.of(
                        text(promptConfig.get("systemPrompt")),
                        text(promptConfig.get("rolePrompt")),
                        text(promptConfig.get("guardrails")),
                        "只能基于传入的脱敏上下文回答，不得猜测或补造敏感信息。"
                ).stream()
                .filter(value -> !value.isBlank())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("只能基于传入的脱敏上下文回答。");
    }

    private Map<String, Object> modelParams(Map<String, Object> modelConfig) {
        String model = text(modelConfig.get("model"));
        return model.isBlank() ? Map.of() : Map.of("model", model);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("模型上下文序列化失败", ex);
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Double number(Object value, Double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private Integer integer(Object value, Integer fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }
}
