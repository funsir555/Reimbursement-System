package com.finex.auth.agent.controller;

import com.finex.auth.agent.entity.AgentConversation;
import com.finex.auth.agent.entity.AgentMessage;
import com.finex.auth.agent.model.ChatModelProvider;
import com.finex.auth.agent.model.ModelCapability;
import com.finex.auth.agent.model.ModelRoutingConfig;
import com.finex.auth.agent.model.ModelRoutingService;
import com.finex.auth.agent.service.ConversationService;
import com.finex.auth.agent.workflow.AgentWorkflowRuntimeService;
import com.finex.auth.agent.workflow.FinWorkOrchestrationService;
import com.finex.auth.agent.workflow.FinWorkParameterService;
import com.finex.auth.service.digitalemployee.ControlledMessageAccessService;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import com.finex.auth.agent.workflow.WorkflowExecutionResult;
import com.finex.auth.util.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Agent 聊天 Controller
 * FinWork 的核心 API 入口
 *
 * 路径说明：
 * - 前端请求：/api/auth/agent/chat/**
 * - 网关转发：去掉 /api 前缀 -> /auth/agent/chat/**
 * - 后端监听：/auth/agent/chat/**
 */
@Slf4j
@RestController
@RequestMapping("/auth/agent/chat")
@RequiredArgsConstructor
public class AgentChatController {

    private final ConversationService conversationService;
    private final ModelRoutingService modelRoutingService;
    private final ModelRoutingConfig modelRoutingConfig;
    private final AgentWorkflowRuntimeService workflowRuntimeService;
    private final ControlledMessageAccessService controlledMessageAccessService;
    private final FinWorkOrchestrationService orchestrationService;

    /**
     * 创建新会话
     */
    @PostMapping("/conversations")
    public Result<ConversationVO> createConversation() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        AgentConversation conversation = conversationService.createConversation(userId);

        return Result.success(ConversationVO.from(conversation));
    }

    /**
     * 获取用户的会话列表
     */
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> getConversations() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        List<AgentConversation> conversations = conversationService.getUserConversations(userId, 20);
        List<ConversationVO> vos = conversations.stream()
                .map(ConversationVO::from)
                .toList();

        return Result.success(vos);
    }

    /**
     * 获取 FinWork 可选模型元数据。
     */
    @GetMapping("/meta")
    public Result<ChatMetaVO> getMeta() {
        List<ChatModelProvider> registeredProviders = modelRoutingService.getRegisteredProviders();
        Set<String> registeredCodes = registeredProviders.stream()
                .map(ChatModelProvider::getProviderCode)
                .collect(Collectors.toSet());

        List<ModelProviderVO> providers = modelRoutingConfig.getProviders().stream()
                .map(config -> toProviderVO(config, registeredCodes.contains(config.getCode())))
                .toList();

        List<ModelOptionVO> modelOptions = modelRoutingConfig.getProviders().stream()
                .map(config -> toModelOptionVO(config, registeredCodes.contains(config.getCode())))
                .toList();

        String defaultProviderCode = resolveDefaultProviderCode(providers);
        String defaultModel = modelOptions.stream()
                .filter(option -> defaultProviderCode.equals(option.getProviderCode()))
                .findFirst()
                .map(ModelOptionVO::getModel)
                .orElse("");

        return Result.success(ChatMetaVO.builder()
                .modelProviders(providers)
                .modelOptions(modelOptions)
                .defaultProviderCode(defaultProviderCode)
                .defaultModel(defaultModel)
                .build());
    }

    /**
     * 发送消息（核心聊天接口）
     */
    @PostMapping("/conversations/{sessionId}/messages")
    public Result<ChatResponseVO> sendMessage(
            @PathVariable String sessionId,
            @RequestBody ChatRequestDTO request) {

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        try {
            // 检查会话是否存在
            AgentConversation conversation = conversationService.getConversation(sessionId);
            if (conversation == null) {
                return Result.error("会话不存在");
            }

            // 检查权限
            if (!conversation.getUserId().equals(userId)) {
                return Result.error("无权访问此会话");
            }

            // 保存用户消息
            conversationService.addMessage(conversation, "user", request.getMessage(), null);

            // 获取上下文（最近10条消息）
            List<AgentMessage> recentMessages = conversationService.getRecentMessages(sessionId, 10);

            FinWorkOrchestrationService.Outcome outcome = orchestrationService.respond(
                    sessionId, userId, request.getMessage(), request.getDigitalEmployeeId(),
                    request.getWorkflowBindingId(), request.getSelectedOptions(), request.getSelectedAnswers());
            String intent = "ANSWER".equals(outcome.kind()) ? "controlled_query"
                    : "CLARIFY".equals(outcome.kind()) ? "clarification" : "chat";
            String responseContent = "CHAT".equals(outcome.kind())
                    ? callLLM(request, recentMessages) : outcome.content();

            // 保存 assistant 消息
            AgentMessage assistantMessage = conversationService.addMessage(conversation, "assistant", responseContent, intent);
            if ("ANSWER".equals(outcome.kind())) {
                controlledMessageAccessService.recordComposite(assistantMessage, outcome.executions());
                if (!controlledMessageAccessService.canRead(userId, assistantMessage)) {
                    responseContent = QueryAuthorizationService.NO_PERMISSION;
                }
            }

            return Result.success(ChatResponseVO.builder()
                    .sessionId(sessionId)
                    .content(responseContent)
                    .intent(intent)
                    .digitalEmployeeId(outcome.executions().isEmpty() ? null
                            : outcome.executions().get(0).result().getDigitalEmployeeId())
                    .kind(outcome.kind())
                    .options(outcome.options())
                    .clarifications(outcome.clarifications())
                    .sources(outcome.executions().stream().map(item -> new SourceVO(
                            item.result().getDigitalEmployeeId(), item.bindingId(),
                            item.result().getAgentVersionId(), item.result().getQueryPermissionCode())).toList())
                    .timestamp(Instant.now())
                    .build());
        } catch (SecurityException exception) {
            AgentConversation conversation = conversationService.getConversation(sessionId);
            if (conversation != null && conversation.getUserId().equals(userId)) {
                conversationService.addMessage(conversation, "assistant",
                        QueryAuthorizationService.NO_PERMISSION, "denied");
            }
            return Result.success(ChatResponseVO.builder().sessionId(sessionId)
                    .content(QueryAuthorizationService.NO_PERMISSION).kind("DENIED")
                    .options(List.of()).sources(List.of()).timestamp(Instant.now()).build());
        } catch (Exception e) {
            log.error("处理消息失败: sessionId={}", sessionId, e);
            return Result.error("处理消息失败，请稍后重试");
        }
    }

    /**
     * 获取会话历史消息
     */
    @GetMapping("/conversations/{sessionId}/messages")
    public Result<List<MessageVO>> getMessages(@PathVariable String sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        // 检查会话权限
        AgentConversation conversation = conversationService.getConversation(sessionId);
        if (conversation == null) {
            return Result.error("会话不存在");
        }

        if (!conversation.getUserId().equals(userId)) {
            return Result.error("无权访问此会话");
        }

        List<AgentMessage> messages = conversationService.getRecentMessages(sessionId, 50);
        List<MessageVO> vos = messages.stream()
                .map(message -> {
                    if ("assistant".equals(message.getRole()) && isProtectedIntent(message.getIntent())
                            && !controlledMessageAccessService.canRead(userId, message)) {
                        MessageVO masked = MessageVO.from(message);
                        masked.setContent(QueryAuthorizationService.NO_PERMISSION);
                        return masked;
                    }
                    return MessageVO.from(message);
                })
                .toList();

        List<FinWorkOrchestrationService.Option> pendingOptions = orchestrationService.pendingOptions(sessionId, userId);
        List<FinWorkParameterService.Group> pendingGroups = orchestrationService.pendingGroups(sessionId, userId);
        if (!pendingOptions.isEmpty() || !pendingGroups.isEmpty()) {
            for (int index = vos.size() - 1; index >= 0; index--) {
                MessageVO item = vos.get(index);
                if ("assistant".equals(item.getRole()) && "CLARIFY".equals(item.getKind())) {
                    item.setOptions(pendingOptions);
                    item.setClarifications(pendingGroups);
                    break;
                }
            }
        }

        return Result.success(vos);
    }

    /**
     * 简单的意图识别（基于关键词）
     */
    private String detectIntent(String message) {
        if (message == null) return "chat";
        String lower = message.toLowerCase();

        if (lower.contains("结账") || lower.contains("关账")) return "query_finance_close";

        if (lower.contains("报销单") || lower.contains("报销") || lower.contains("费用")) {
            if (lower.contains("查") || lower.contains("看") || lower.contains("显示")
                    || lower.contains("有哪些") || lower.contains("多少") || lower.contains("几个")
                    || lower.contains("我的") || lower.contains("当前") || lower.contains("本月")
                    || lower.contains("部门") || lower.contains("公司") || lower.contains("统计")) {
                return "query_expense";
            }
        }

        return "chat";
    }

    private boolean isProtectedIntent(String intent) {
        return "query_expense".equals(intent) || "query_finance_close".equals(intent)
                || "controlled_query".equals(intent);
    }

    /**
     * 调用大模型
     */
    private String callLLM(ChatRequestDTO chatRequest, List<AgentMessage> recentMessages) {
        // 构建消息列表
        List<ChatModelProvider.ChatMessage> messages = new ArrayList<>();

        // 添加历史消息
        for (AgentMessage msg : recentMessages) {
            if (isProtectedIntent(msg.getIntent())) continue;
            messages.add(ChatModelProvider.ChatMessage.builder()
                    .role(msg.getRole())
                    .content(msg.getContent())
                    .build());
        }

        // 构建请求
        ChatModelProvider.ChatRequest request = ChatModelProvider.ChatRequest.builder()
                .systemPrompt("你是 FinEx 财务助手，专注于帮助用户处理报销、费用管理相关的问题。回答要简洁、准确。")
                .messages(messages)
                .temperature(0.7)
                .maxTokens(1000)
                .extraParams(buildModelExtraParams(chatRequest))
                .build();

        // 调用模型路由服务
        ChatModelProvider.ChatResponse response = modelRoutingService.chatWithPreferredProvider(
                chatRequest.getProviderCode(),
                ModelCapability.ModelUseCase.CHAT_QUERY,
                ModelRoutingService.DataSensitivity.LOW,
                request
        );

        if (response.isSuccess()) {
            return response.getContent();
        } else {
            return "抱歉，当前 AI 服务暂时不可用，请稍后再试。";
        }
    }

    private Map<String, Object> buildModelExtraParams(ChatRequestDTO chatRequest) {
        Map<String, Object> extraParams = new HashMap<>();
        String model = trimToEmpty(chatRequest.getModel());
        if (!model.isEmpty()) {
            extraParams.put("model", model);
        }
        return extraParams;
    }

    private String resolveDefaultProviderCode(List<ModelProviderVO> providers) {
        return modelRoutingConfig.getRules().stream()
                .filter(rule -> ModelCapability.ModelUseCase.CHAT_QUERY.name().equals(rule.getUseCase()))
                .filter(rule -> ModelRoutingService.DataSensitivity.LOW.name().equals(rule.getSensitivity()))
                .map(ModelRoutingConfig.RoutingRule::getPrimary)
                .filter(primary -> providers.stream().anyMatch(provider -> primary.equals(provider.getCode()) && provider.isAvailable()))
                .findFirst()
                .orElseGet(() -> providers.stream()
                        .filter(ModelProviderVO::isAvailable)
                        .findFirst()
                        .map(ModelProviderVO::getCode)
                        .orElse(""));
    }

    private ModelProviderVO toProviderVO(ModelRoutingConfig.ProviderConfig config, boolean available) {
        ModelRoutingConfig.CapabilityConfig capability = config.getCapabilities();
        return ModelProviderVO.builder()
                .code(config.getCode())
                .label(resolveProviderLabel(config.getCode()))
                .available(available)
                .deployment(capability == null ? "" : capability.getDeployment())
                .dataResidency(capability == null ? "" : capability.getDataResidency())
                .maxContextTokens(capability == null ? 0 : capability.getMaxContextTokens())
                .defaultModel(resolveModelName(config))
                .build();
    }

    private ModelOptionVO toModelOptionVO(ModelRoutingConfig.ProviderConfig config, boolean available) {
        String model = resolveModelName(config);
        return ModelOptionVO.builder()
                .providerCode(config.getCode())
                .model(model)
                .label(resolveModelLabel(config.getCode(), model))
                .available(available)
                .build();
    }

    private String resolveProviderLabel(String code) {
        return switch (code) {
            case "DEEPSEEK" -> "DeepSeek";
            case "KIMI" -> "Kimi";
            case "LOCAL_7B" -> "本地模型";
            default -> code;
        };
    }

    private String resolveModelLabel(String providerCode, String model) {
        return switch (providerCode) {
            case "DEEPSEEK" -> "DeepSeek Chat";
            case "KIMI" -> "Moonshot v1";
            case "LOCAL_7B" -> "Ollama 本地模型（" + model + "）";
            default -> model;
        };
    }

    private String resolveModelName(ModelRoutingConfig.ProviderConfig config) {
        String modelPath = trimToEmpty(config.getModelPath());
        if (!modelPath.isEmpty()) {
            return modelPath;
        }
        return switch (config.getCode()) {
            case "DEEPSEEK" -> "deepseek-chat";
            case "KIMI" -> "moonshot-v1-8k";
            default -> config.getCode().toLowerCase();
        };
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    // ========== DTO/VO 类 ==========

    @Data
    public static class ChatRequestDTO {
        private String message;
        private String providerCode;
        private String model;
        private Long digitalEmployeeId;
        private Long workflowBindingId;
        private List<String> selectedOptions;
        private Map<String, String> selectedAnswers;
    }

    @Data
    @lombok.Builder
    public static class ChatResponseVO {
        private String sessionId;
        private String content;
        private String intent;
        private Instant timestamp;
        private Long digitalEmployeeId;
        private String kind;
        private List<FinWorkOrchestrationService.Option> options;
        private List<FinWorkParameterService.Group> clarifications;
        private List<SourceVO> sources;
    }

    public record SourceVO(Long digitalEmployeeId, Long workflowBindingId,
                           Long agentVersionId, String permissionCode) { }

    @Data
    @lombok.Builder
    public static class ChatMetaVO {
        private List<ModelProviderVO> modelProviders;
        private List<ModelOptionVO> modelOptions;
        private String defaultProviderCode;
        private String defaultModel;
    }

    @Data
    @lombok.Builder
    public static class ModelProviderVO {
        private String code;
        private String label;
        private boolean available;
        private String deployment;
        private String dataResidency;
        private int maxContextTokens;
        private String defaultModel;
    }

    @Data
    @lombok.Builder
    public static class ModelOptionVO {
        private String providerCode;
        private String model;
        private String label;
        private boolean available;
    }

    @Data
    @lombok.Builder
    public static class ConversationVO {
        private String sessionId;
        private Integer messageCount;
        private Instant lastMessageAt;
        private Instant createdAt;

        public static ConversationVO from(AgentConversation conversation) {
            return ConversationVO.builder()
                    .sessionId(conversation.getSessionId())
                    .messageCount(conversation.getMessageCount())
                    .lastMessageAt(conversation.getLastMessageAt())
                    .createdAt(conversation.getCreatedAt())
                    .build();
        }
    }

    @Data
    @lombok.Builder
    public static class MessageVO {
        private String role;
        private String content;
        private Instant createdAt;
        private String kind;
        private List<FinWorkOrchestrationService.Option> options;
        private List<FinWorkParameterService.Group> clarifications;

        public static MessageVO from(AgentMessage message) {
            return MessageVO.builder()
                    .role(message.getRole())
                    .content(message.getContent())
                    .createdAt(message.getCreatedAt())
                    .kind("clarification".equals(message.getIntent()) ? "CLARIFY"
                            : "controlled_query".equals(message.getIntent()) ? "ANSWER" : null)
                    .build();
        }
    }

    @Data
    @lombok.Builder
    public static class Result<T> {
        private Integer code;
        private String message;
        private T data;
        private Long timestamp;

        public static <T> Result<T> success(T data) {
            return Result.<T>builder()
                    .code(200)
                    .message("操作成功")
                    .data(data)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }

        public static <T> Result<T> error(String message) {
            return Result.<T>builder()
                    .code(500)
                    .message(message)
                    .data(null)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }
}
