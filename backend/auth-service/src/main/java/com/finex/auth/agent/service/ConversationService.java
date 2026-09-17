package com.finex.auth.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.entity.AgentConversation;
import com.finex.auth.agent.entity.AgentMessage;
import com.finex.auth.agent.mapper.AgentConversationMapper;
import com.finex.auth.agent.mapper.AgentMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Agent 会话服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final AgentConversationMapper conversationMapper;
    private final AgentMessageMapper messageMapper;

    /**
     * 创建新会话
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentConversation createConversation(Long userId) {
        AgentConversation conversation = new AgentConversation();
        conversation.setSessionId(generateSessionId());
        conversation.setUserId(userId);
        conversation.setMessageCount(0);
        conversation.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        // 自动填充租户信息
        conversation.prePersist();

        conversationMapper.insert(conversation);

        log.info("创建新会话: sessionId={}, userId={}", conversation.getSessionId(), userId);

        return conversation;
    }

    /**
     * 根据 sessionId 获取会话
     */
    public AgentConversation getConversation(String sessionId) {
        LambdaQueryWrapper<AgentConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentConversation::getSessionId, sessionId)
               .eq(AgentConversation::getTenantId, TenantContext.getEffectiveTenantId())
               .eq(AgentConversation::getDeleted, false);

        return conversationMapper.selectOne(wrapper);
    }

    /**
     * 获取用户的最近会话列表
     */
    public List<AgentConversation> getUserConversations(Long userId, int limit) {
        LambdaQueryWrapper<AgentConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentConversation::getUserId, userId)
               .eq(AgentConversation::getTenantId, TenantContext.getEffectiveTenantId())
               .eq(AgentConversation::getDeleted, false)
               .orderByDesc(AgentConversation::getLastMessageAt)
               .last("LIMIT " + limit);

        return conversationMapper.selectList(wrapper);
    }

    /**
     * 获取会话的最近消息列表
     */
    public List<AgentMessage> getRecentMessages(String sessionId, int limit) {
        LambdaQueryWrapper<AgentMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentMessage::getSessionId, sessionId)
               .eq(AgentMessage::getTenantId, TenantContext.getEffectiveTenantId())
               .orderByDesc(AgentMessage::getCreatedAt)
               .last("LIMIT " + limit);

        List<AgentMessage> messages = messageMapper.selectList(wrapper);

        // 反转列表，使其按时间正序排列
        java.util.Collections.reverse(messages);

        return messages;
    }

    /**
     * 获取会话消息数量
     */
    public int getMessageCount(String sessionId) {
        LambdaQueryWrapper<AgentMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentMessage::getSessionId, sessionId)
               .eq(AgentMessage::getTenantId, TenantContext.getEffectiveTenantId());

        return Math.toIntExact(messageMapper.selectCount(wrapper));
    }

    /**
     * 添加消息到会话
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentMessage addMessage(String sessionId, String role, String content, String intent) {
        // 创建消息
        AgentMessage message = new AgentMessage();
        message.setTenantId(TenantContext.getEffectiveTenantId());
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setIntent(intent);
        message.setCreatedAt(Instant.now());

        messageMapper.insert(message);

        // 更新会话统计
        AgentConversation conversation = getConversation(sessionId);
        if (conversation != null) {
            conversation.setMessageCount(conversation.getMessageCount() + 1);
            conversation.setLastMessageAt(Instant.now());
            conversation.preUpdate();
            conversationMapper.updateById(conversation);
        }

        log.debug("添加消息到会话: sessionId={}, role={}, contentLength={}",
                  sessionId, role, content.length());

        return message;
    }

    /**
     * 更新会话摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSummary(String sessionId, String summary) {
        AgentConversation conversation = getConversation(sessionId);
        if (conversation != null) {
            conversation.setSummaryText(summary);
            conversation.preUpdate();
            conversationMapper.updateById(conversation);

            log.info("更新会话摘要: sessionId={}", sessionId);
        }
    }

    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return "sess_" + UUID.randomUUID().toString().replace("-", "");
    }
}
