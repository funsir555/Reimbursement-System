package com.finex.auth.agent.controller;

import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.entity.AgentConversation;
import com.finex.auth.agent.entity.AgentMessage;
import com.finex.auth.agent.model.ModelRoutingConfig;
import com.finex.auth.agent.model.ModelRoutingService;
import com.finex.auth.agent.service.ConversationService;
import com.finex.auth.agent.workflow.FinWorkOrchestrationService;
import com.finex.auth.agent.workflow.WorkflowExecutionResult;
import com.finex.auth.service.digitalemployee.ControlledMessageAccessService;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentChatControllerStreamTest {
    @AfterEach
    void clear() { TenantContext.clear(); }

    @Test
    void neverStreamsControlledDataWhenFinalAuthorizationFails() {
        ConversationService conversations = mock(ConversationService.class);
        FinWorkOrchestrationService orchestration = mock(FinWorkOrchestrationService.class);
        ControlledMessageAccessService access = mock(ControlledMessageAccessService.class);
        AgentChatController controller = new AgentChatController(conversations,
                mock(ModelRoutingService.class), mock(ModelRoutingConfig.class), access,
                orchestration, (Executor) Runnable::run);
        AgentConversation conversation = new AgentConversation();
        conversation.setSessionId("old-session");
        conversation.setUserId(7L);
        conversation.setCompanyId("COMPANY_A");
        when(conversations.getConversation("old-session")).thenReturn(conversation);
        when(conversations.isCompanyActive("COMPANY_A")).thenReturn(true);
        when(conversations.getRecentMessages("old-session", 10)).thenReturn(List.of());
        when(conversations.addMessage(eq(conversation), anyString(), anyString(), any())).thenAnswer(call -> {
            AgentMessage message = new AgentMessage();
            message.setId(11L);
            return message;
        });
        WorkflowExecutionResult result = WorkflowExecutionResult.builder()
                .digitalEmployeeId(1L).agentVersionId(2L).queryPermissionCode("expense:agent:query").build();
        when(orchestration.respond(eq("old-session"), eq(7L), eq("查询"),
                isNull(), isNull(), isNull(), isNull(), any())).thenReturn(
                new FinWorkOrchestrationService.Outcome("ANSWER", "受控业务数据", List.of(), List.of(),
                        List.of(new FinWorkOrchestrationService.Execution(result, "查询", 3L))));
        when(access.canRead(eq(7L), any(AgentMessage.class))).thenReturn(false);
        TenantContext.setCompanyId("COMPANY_CURRENT");
        AgentChatController.ChatRequestDTO request = new AgentChatController.ChatRequestDTO();
        request.setMessage("查询");
        List<String> chunks = new ArrayList<>();
        AgentChatController.ChatResponseVO response = ReflectionTestUtils.invokeMethod(controller,
                "processMessage", "old-session", 7L, request,
                (java.util.function.Consumer<String>) ignored -> {},
                (java.util.function.Consumer<String>) chunks::add,
                (java.util.function.Consumer<String>) ignored -> {});
        assertThat(chunks).containsExactly(QueryAuthorizationService.NO_PERMISSION);
        assertThat(response.getKind()).isEqualTo("DENIED");
        assertThat(response.getSources()).isEmpty();
        assertThat(TenantContext.getCompanyId()).isEqualTo("COMPANY_CURRENT");
    }

    @Test
    void confirmsFirstSessionOnlyAfterFirstUserMessageIsSaved() {
        ConversationService conversations = mock(ConversationService.class);
        FinWorkOrchestrationService orchestration = mock(FinWorkOrchestrationService.class);
        AgentChatController controller = new AgentChatController(conversations,
                mock(ModelRoutingService.class), mock(ModelRoutingConfig.class),
                mock(ControlledMessageAccessService.class), orchestration, (Executor) Runnable::run);
        String sessionId = "sess_test";
        AgentConversation saved = new AgentConversation();
        saved.setSessionId(sessionId);
        saved.setUserId(7L);
        saved.setCompanyId("COMPANY_A");
        when(conversations.isCompanyActive("COMPANY_A")).thenReturn(true);
        when(conversations.createWithFirstMessage(sessionId, 7L, "你好")).thenReturn(saved);
        when(conversations.getRecentMessages(sessionId, 10)).thenReturn(List.of());
        when(orchestration.respond(eq(sessionId), eq(7L), eq("你好"),
                isNull(), isNull(), isNull(), isNull(), any())).thenReturn(
                new FinWorkOrchestrationService.Outcome("CLARIFY", "请选择", List.of(), List.of(), List.of()));
        TenantContext.setCompanyId("COMPANY_A");
        AgentChatController.ChatRequestDTO request = new AgentChatController.ChatRequestDTO();
        request.setMessage("你好");
        List<String> events = new ArrayList<>();
        ReflectionTestUtils.invokeMethod(controller, "processMessage", sessionId, 7L, request,
                (java.util.function.Consumer<String>) events::add,
                (java.util.function.Consumer<String>) ignored -> {},
                (java.util.function.Consumer<String>) confirmed -> {
                    verify(conversations).createWithFirstMessage(sessionId, 7L, "你好");
                    events.add("session:" + confirmed);
                });
        assertThat(events.get(0)).isEqualTo("session:" + sessionId);
        verify(conversations, never()).addMessage(eq(saved), eq("user"), anyString(), any());
    }

    @Test
    void blankFirstMessageDoesNotCreateConversation() {
        ConversationService conversations = mock(ConversationService.class);
        AgentChatController controller = new AgentChatController(conversations,
                mock(ModelRoutingService.class), mock(ModelRoutingConfig.class),
                mock(ControlledMessageAccessService.class), mock(FinWorkOrchestrationService.class),
                (Executor) Runnable::run);
        AgentChatController.ChatRequestDTO request = new AgentChatController.ChatRequestDTO();
        request.setMessage(" ");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                controller, "processMessage", "sess_invalid", 7L, request,
                (java.util.function.Consumer<String>) ignored -> {},
                (java.util.function.Consumer<String>) ignored -> {},
                (java.util.function.Consumer<String>) ignored -> {}))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(conversations);
    }
}
