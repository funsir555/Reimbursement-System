package com.finex.auth.agent.controller;

import com.finex.auth.agent.entity.AgentConversation;
import com.finex.auth.agent.entity.AgentMessage;
import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.context.AgentExecutionContext;
import com.finex.auth.agent.model.ChatModelProvider;
import com.finex.auth.agent.model.ModelCapability;
import com.finex.auth.agent.model.ModelRoutingConfig;
import com.finex.auth.agent.model.ModelRoutingService;
import com.finex.auth.agent.service.ConversationService;
import com.finex.auth.agent.workflow.FinWorkOrchestrationService;
import com.finex.auth.agent.workflow.FinWorkParameterService;
import com.finex.auth.service.digitalemployee.ControlledMessageAccessService;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import com.finex.auth.agent.workflow.WorkflowExecutionResult;
import com.finex.auth.util.SecurityUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
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
public class AgentChatController {

    private final ConversationService conversationService;
    private final ModelRoutingService modelRoutingService;
    private final ModelRoutingConfig modelRoutingConfig;
    private final ControlledMessageAccessService controlledMessageAccessService;
    private final FinWorkOrchestrationService orchestrationService;
    private final Executor streamExecutor;

    public AgentChatController(ConversationService conversationService,
                               ModelRoutingService modelRoutingService,
                               ModelRoutingConfig modelRoutingConfig,
                               ControlledMessageAccessService controlledMessageAccessService,
                               FinWorkOrchestrationService orchestrationService,
                               @Qualifier("finWorkStreamExecutor") Executor streamExecutor) {
        this.conversationService = conversationService;
        this.modelRoutingService = modelRoutingService;
        this.modelRoutingConfig = modelRoutingConfig;
        this.controlledMessageAccessService = controlledMessageAccessService;
        this.orchestrationService = orchestrationService;
        this.streamExecutor = streamExecutor;
    }

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
    public Result<ConversationPageVO> getConversations(@RequestParam(required = false) String cursor,
                                                       @RequestParam(defaultValue = "false") boolean excludePinned) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        ConversationService.ConversationPage page = conversationService.listUserConversations(userId, cursor, excludePinned);
        List<ConversationVO> vos = page.items().stream()
                .map(this::toConversationVO)
                .toList();
        return Result.success(new ConversationPageVO(vos, page.nextCursor()));
    }

    @GetMapping("/conversations/pinned")
    public Result<List<ConversationVO>> getPinnedConversations() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("用户未登录");
        return Result.success(conversationService.listPinnedConversations(userId).stream()
                .map(this::toConversationVO).toList());
    }

    @PatchMapping("/conversations/{sessionId}/title")
    public Result<Boolean> renameConversation(@PathVariable String sessionId, @RequestBody RenameConversationRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("用户未登录");
        try {
            conversationService.renameConversation(sessionId, userId, request == null ? null : request.getTitle());
            return Result.success(true);
        } catch (IllegalArgumentException invalid) {
            return Result.error(invalid.getMessage());
        }
    }

    @PutMapping("/conversations/{sessionId}/pin")
    public Result<Boolean> pinConversation(@PathVariable String sessionId, @RequestBody PinConversationRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("用户未登录");
        if (request == null || request.getPinned() == null) return Result.error("置顶状态无效");
        try {
            conversationService.setPinned(sessionId, userId, request.getPinned());
            return Result.success(true);
        } catch (IllegalArgumentException | IllegalStateException invalid) {
            return Result.error(invalid.getMessage());
        }
    }

    @DeleteMapping("/conversations/{sessionId}")
    public Result<Boolean> deleteConversation(@PathVariable String sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("用户未登录");
        try {
            conversationService.deleteConversation(sessionId, userId);
            return Result.success(true);
        } catch (IllegalArgumentException invalid) {
            return Result.error(invalid.getMessage());
        }
    }

    private ConversationVO toConversationVO(ConversationService.ConversationItem item) {
        return ConversationVO.builder().sessionId(item.sessionId())
                .companyId(item.companyId()).companyName(item.companyName()).title(item.title())
                .pinned(item.pinned()).messageCount(item.messageCount())
                .lastMessageAt(item.lastMessageAt()).createdAt(item.createdAt()).build();
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
            return Result.success(processMessage(sessionId, userId, request,
                    ignored -> {}, null, ignored -> {}));
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

    @PostMapping(value = "/conversations/{sessionId}/messages/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@PathVariable String sessionId, @RequestBody ChatRequestDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        SseEmitter emitter = new SseEmitter(120_000L);
        if (userId == null) {
            emit(emitter, "error", "用户未登录", new AtomicBoolean());
            emitter.complete();
            return emitter;
        }
        AgentExecutionContext context = AgentExecutionContext.capture();
        AtomicBoolean disconnected = new AtomicBoolean();
        try {
            streamExecutor.execute(() -> {
                context.restore();
                try {
                    ChatResponseVO response = processMessage(sessionId, userId, request,
                            status -> emit(emitter, "status", status, disconnected),
                            chunk -> emit(emitter, "delta", chunk, disconnected),
                            ready -> emit(emitter, "session", ready, disconnected));
                    emit(emitter, "final", response, disconnected);
                    emit(emitter, "done", Map.of(), disconnected);
                } catch (SecurityException denied) {
                    AgentConversation conversation = conversationService.getConversation(sessionId);
                    if (conversation != null && userId.equals(conversation.getUserId())) {
                        conversationService.addMessage(conversation, "assistant",
                                QueryAuthorizationService.NO_PERMISSION, "denied");
                        emit(emitter, "final", ChatResponseVO.builder().sessionId(sessionId)
                                .content(QueryAuthorizationService.NO_PERMISSION).kind("DENIED")
                                .options(List.of()).sources(List.of()).timestamp(Instant.now()).build(), disconnected);
                        emit(emitter, "done", Map.of(), disconnected);
                    } else {
                        emit(emitter, "error", "无权访问此会话", disconnected);
                    }
                } catch (Exception failure) {
                    log.error("FinWork 流式处理失败: sessionId={}", sessionId, failure);
                    emit(emitter, "error", "处理消息失败，请稍后重试", disconnected);
                } finally {
                    emitter.complete();
                    AgentExecutionContext.clear();
                }
            });
        } catch (RejectedExecutionException rejected) {
            emit(emitter, "error", "当前请求较多，请稍后重试", disconnected);
            emitter.complete();
        }
        return emitter;
    }

    private void emit(SseEmitter emitter, String name, Object data, AtomicBoolean disconnected) {
        if (disconnected.get()) return;
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (Exception error) {
            disconnected.set(true);
        }
    }

    private ChatResponseVO processMessage(String sessionId, Long userId, ChatRequestDTO request,
                                          Consumer<String> status, Consumer<String> delta,
                                          Consumer<String> sessionReady) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank())
            throw new IllegalArgumentException("消息不能为空");
        AgentConversation conversation = conversationService.getConversation(sessionId);
        boolean firstMessage = conversation == null;
        if (!firstMessage && !conversation.getUserId().equals(userId))
            throw new SecurityException("无权访问此会话");
        String companyId = firstMessage ? TenantContext.getEffectiveCompanyId() : conversation.getCompanyId();
        if (!conversationService.isCompanyActive(companyId))
            throw new IllegalStateException("原会话公司已停用，无法继续提问");
        String previousCompany = TenantContext.getCompanyId();
        try {
            TenantContext.setCompanyId(companyId);
            if (firstMessage) {
                conversation = conversationService.createWithFirstMessage(
                        sessionId, userId, request.getMessage());
            } else {
                conversationService.addMessage(conversation, "user", request.getMessage(), null);
            }
            sessionReady.accept(sessionId);
            List<AgentMessage> recentMessages = conversationService.getRecentMessages(sessionId, 10);
            status.accept("正在分析任务");
            FinWorkOrchestrationService.Outcome outcome = orchestrationService.respond(
                    sessionId, userId, request.getMessage(), request.getDigitalEmployeeId(),
                    request.getWorkflowBindingId(), request.getSelectedOptions(), request.getSelectedAnswers(), status);
            String intent = "ANSWER".equals(outcome.kind()) ? "controlled_query"
                    : "CLARIFY".equals(outcome.kind()) ? "clarification" : "chat";
            if ("CHAT".equals(outcome.kind())) status.accept("正在生成回答");
            else if ("ANSWER".equals(outcome.kind())) status.accept("正在复核查询结果");
            String content = "CHAT".equals(outcome.kind())
                    ? callLLM(request, recentMessages, delta) : outcome.content();
            AgentMessage assistant = conversationService.addMessage(conversation, "assistant", content, intent);
            if ("ANSWER".equals(outcome.kind())) {
                controlledMessageAccessService.recordComposite(assistant, outcome.executions());
                if (!controlledMessageAccessService.canRead(userId, assistant))
                    content = QueryAuthorizationService.NO_PERMISSION;
                if (delta != null) {
                    for (int offset = 0; offset < content.length(); offset += 200) {
                        if (!QueryAuthorizationService.NO_PERMISSION.equals(content)
                                && !controlledMessageAccessService.canRead(userId, assistant))
                            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
                        delta.accept(content.substring(offset, Math.min(offset + 200, content.length())));
                    }
                }
            } else if (delta != null && !"CHAT".equals(outcome.kind())) {
                delta.accept(content);
            }
            boolean denied = "ANSWER".equals(outcome.kind())
                    && QueryAuthorizationService.NO_PERMISSION.equals(content);
            return ChatResponseVO.builder().sessionId(sessionId).content(content).intent(intent)
                    .digitalEmployeeId(denied || outcome.executions().isEmpty() ? null
                            : outcome.executions().get(0).result().getDigitalEmployeeId())
                    .kind(denied ? "DENIED" : outcome.kind()).options(outcome.options())
                    .clarifications(outcome.clarifications())
                    .sources(denied ? List.of() : outcome.executions().stream().map(item -> new SourceVO(
                            item.result().getDigitalEmployeeId(), item.bindingId(),
                            item.result().getAgentVersionId(), item.result().getQueryPermissionCode())).toList())
                    .timestamp(Instant.now()).build();
        } finally {
            TenantContext.setCompanyId(previousCompany);
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
        String previousCompany = TenantContext.getCompanyId();
        TenantContext.setCompanyId(conversation.getCompanyId());
        try {
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
        } finally {
            TenantContext.setCompanyId(previousCompany);
        }
    }

    @GetMapping("/conversations/{sessionId}/messages/page")
    public Result<MessagePageVO> getMessagePage(@PathVariable String sessionId,
                                                 @RequestParam(required = false) String cursor) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("用户未登录");
        AgentConversation conversation = conversationService.getConversation(sessionId);
        if (conversation == null) return Result.error("会话不存在");
        if (!userId.equals(conversation.getUserId())) return Result.error("无权访问此会话");
        String previousCompany = TenantContext.getCompanyId();
        TenantContext.setCompanyId(conversation.getCompanyId());
        try {
        ConversationService.MessagePage page = conversationService.getMessagePage(conversation, cursor);
        List<MessageVO> items = page.items().stream().map(message -> {
            MessageVO item = MessageVO.from(message);
            if ("assistant".equals(message.getRole()) && isProtectedIntent(message.getIntent())
                    && !controlledMessageAccessService.canRead(userId, message)) {
                item.setContent(QueryAuthorizationService.NO_PERMISSION);
            }
            return item;
        }).toList();
        if (cursor == null && !items.isEmpty()) {
            List<FinWorkOrchestrationService.Option> options = orchestrationService.pendingOptions(sessionId, userId);
            List<FinWorkParameterService.Group> groups = orchestrationService.pendingGroups(sessionId, userId);
            for (int i = items.size() - 1; i >= 0; i--) {
                MessageVO item = items.get(i);
                if ("CLARIFY".equals(item.getKind())) {
                    item.setOptions(options);
                    item.setClarifications(groups);
                    break;
                }
            }
        }
        return Result.success(new MessagePageVO(items, page.nextCursor()));
        } finally {
            TenantContext.setCompanyId(previousCompany);
        }
    }

    private boolean isProtectedIntent(String intent) {
        return "query_expense".equals(intent) || "query_finance_close".equals(intent)
                || "controlled_query".equals(intent);
    }

    /**
     * 调用大模型
     */
    private String callLLM(ChatRequestDTO chatRequest, List<AgentMessage> recentMessages,
                           Consumer<String> delta) {
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
        AtomicBoolean sentChunk = new AtomicBoolean();
        ChatModelProvider.ChatResponse response = delta == null
                ? modelRoutingService.chatWithPreferredProvider(chatRequest.getProviderCode(),
                        ModelCapability.ModelUseCase.CHAT_QUERY,
                        ModelRoutingService.DataSensitivity.LOW, request)
                : modelRoutingService.chatStreamWithPreferredProvider(chatRequest.getProviderCode(),
                        ModelCapability.ModelUseCase.CHAT_QUERY,
                        ModelRoutingService.DataSensitivity.LOW, request, chunk -> {
                            sentChunk.set(true);
                            delta.accept(chunk);
                        });

        if (response.isSuccess()) {
            return response.getContent();
        } else {
            if (sentChunk.get()) throw new IllegalStateException("模型流式响应中断");
            String unavailable = "抱歉，当前 AI 服务暂时不可用，请稍后再试。";
            if (delta != null) delta.accept(unavailable);
            return unavailable;
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
        private String companyId;
        private String companyName;
        private String title;
        private boolean pinned;
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
    public static class RenameConversationRequest {
        private String title;
    }

    @Data
    public static class PinConversationRequest {
        private Boolean pinned;
    }

    public record ConversationPageVO(List<ConversationVO> items, String nextCursor) { }
    public record MessagePageVO(List<MessageVO> items, String nextCursor) { }

    @Data
    @lombok.Builder
    public static class MessageVO {
        private Long id;
        private String role;
        private String content;
        private Instant createdAt;
        private String kind;
        private List<FinWorkOrchestrationService.Option> options;
        private List<FinWorkParameterService.Group> clarifications;

        public static MessageVO from(AgentMessage message) {
            return MessageVO.builder()
                    .id(message.getId())
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
