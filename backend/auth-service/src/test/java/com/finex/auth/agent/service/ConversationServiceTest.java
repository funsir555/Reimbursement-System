package com.finex.auth.agent.service;

import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.entity.AgentConversation;
import com.finex.auth.agent.entity.AgentMessage;
import com.finex.auth.agent.mapper.AgentConversationMapper;
import com.finex.auth.agent.mapper.AgentMessageMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ConversationServiceTest {

    private final AgentConversationMapper conversationMapper = mock(AgentConversationMapper.class);
    private final AgentMessageMapper messageMapper = mock(AgentMessageMapper.class);
    private final ConversationService service = new ConversationService(conversationMapper, messageMapper);

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void appendsMessageUsingAlreadyAuthorizedConversationWithoutReloadingIt() {
        TenantContext.setTenantId("default");
        TenantContext.setCompanyId("GROUP_HQ");
        TenantContext.setUserId(7L);
        AgentConversation conversation = new AgentConversation();
        conversation.setId(11L);
        conversation.setSessionId("session-1");
        conversation.setMessageCount(2);

        service.addMessage(conversation, "user", "hello", null);

        ArgumentCaptor<AgentMessage> messageCaptor = ArgumentCaptor.forClass(AgentMessage.class);
        verify(messageMapper).insert(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getCompanyId()).isEqualTo("GROUP_HQ");
        assertThat(messageCaptor.getValue().getSessionId()).isEqualTo("session-1");
        assertThat(conversation.getMessageCount()).isEqualTo(3);
        verify(conversationMapper).updateById(conversation);
        verify(conversationMapper, never()).selectOne(org.mockito.ArgumentMatchers.any());
    }
}
