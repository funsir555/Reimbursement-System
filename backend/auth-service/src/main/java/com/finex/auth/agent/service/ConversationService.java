package com.finex.auth.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.entity.AgentConversation;
import com.finex.auth.agent.entity.AgentMessage;
import com.finex.auth.agent.mapper.AgentConversationMapper;
import com.finex.auth.agent.mapper.AgentMessageMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agent 会话服务
 */
@Slf4j
@Service
public class ConversationService {

    private static final Pattern NEW_SESSION_ID = Pattern.compile("sess_([0-9a-f]{13})_([0-9a-f]{16})_([0-9a-f]{24})");
    private static final long RESERVATION_MILLIS = 24 * 60 * 60 * 1000L;
    private static final String CONVERSATION_LIST_SELECT = """
            SELECT c.id,c.session_id,c.company_id,c.message_count,c.last_message_at,c.created_at,c.is_pinned,
                   COALESCE(c.custom_title,(SELECT LEFT(m.content,48) FROM ea_agent_message m
                    WHERE m.tenant_id=c.tenant_id AND m.company_id=c.company_id
                      AND m.session_id=c.session_id AND m.role='user'
                    ORDER BY m.created_at,m.id LIMIT 1)) AS title
            FROM ea_agent_conversation c
            WHERE c.tenant_id=? AND c.user_id=? AND c.deleted=0
              AND EXISTS (SELECT 1 FROM ea_agent_message visible
                          WHERE visible.tenant_id=c.tenant_id AND visible.company_id=c.company_id
                            AND visible.session_id=c.session_id)
            """;

    private final AgentConversationMapper conversationMapper;
    private final AgentMessageMapper messageMapper;
    private final JdbcTemplate jdbc;
    private final String sessionSecret;

    public ConversationService(AgentConversationMapper conversationMapper, AgentMessageMapper messageMapper,
                               JdbcTemplate jdbc, @Value("${finex.security.jwt-secret}") String sessionSecret) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.jdbc = jdbc;
        this.sessionSecret = sessionSecret;
    }

    public record ConversationItem(String sessionId, String companyId, String companyName, String title,
                                   boolean pinned, int messageCount, Instant lastMessageAt, Instant createdAt) { }
    public record ConversationPage(List<ConversationItem> items, String nextCursor) { }
    public record MessagePage(List<AgentMessage> items, String nextCursor) { }

    /** Reserve an id without storing an empty conversation. */
    public AgentConversation createConversation(Long userId) {
        AgentConversation conversation = new AgentConversation();
        conversation.setSessionId(generateSessionId(userId));
        conversation.setUserId(userId);
        conversation.setMessageCount(0);
        conversation.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        // 自动填充租户信息
        conversation.prePersist();

        return conversation;
    }

    /** The conversation and its first user message are committed together. */
    @Transactional(rollbackFor = Exception.class)
    public AgentConversation createWithFirstMessage(String sessionId, Long userId, String content) {
        if (userId == null || content == null || content.isBlank()
                || !validNewSessionId(sessionId, userId)) {
            throw new IllegalArgumentException("会话或消息无效");
        }
        AgentConversation conversation = new AgentConversation();
        conversation.setSessionId(sessionId);
        conversation.setUserId(userId);
        conversation.setMessageCount(0);
        conversation.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        conversation.prePersist();
        try {
            conversationMapper.insert(conversation);
        } catch (DuplicateKeyException conflict) {
            throw new IllegalStateException("会话不可用");
        }
        addMessage(conversation, "user", content, null);
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

    public boolean isCompanyActive(String companyId) {
        if (companyId == null || companyId.isBlank()) return false;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_company WHERE company_id=? AND status=1", Integer.class, companyId);
        return count != null && count > 0;
    }

    /** A cursor is bound to the ordered timestamp and database id, never to a client company. */
    public ConversationPage listUserConversations(Long userId, String cursor) {
        return listUserConversations(userId, cursor, false);
    }

    public ConversationPage listUserConversations(Long userId, String cursor, boolean excludePinned) {
        Cursor after = decodeCursor(cursor);
        String sql = CONVERSATION_LIST_SELECT
                + (excludePinned ? " AND c.is_pinned=0 " : "") + (after == null ? "" : """
                  AND (COALESCE(c.last_message_at,c.created_at) < ?
                    OR (COALESCE(c.last_message_at,c.created_at)=? AND c.id<?))
                """) + " ORDER BY COALESCE(c.last_message_at,c.created_at) DESC,c.id DESC LIMIT 31";
        List<Object> args = new ArrayList<>(List.of(TenantContext.getEffectiveTenantId(), userId));
        if (after != null) {
            java.sql.Timestamp time = java.sql.Timestamp.from(after.time());
            args.add(time);
            args.add(time);
            args.add(after.id());
        }
        List<Row> rows = jdbc.query(sql, this::readConversationRow, args.toArray());
        boolean more = rows.size() > 30;
        List<Row> page = rows.subList(0, Math.min(rows.size(), 30));
        Row last = page.isEmpty() ? null : page.get(page.size() - 1);
        List<ConversationItem> items = withCompanyNames(page);
        return new ConversationPage(items, more && last != null
                ? encodeCursor(last.item().lastMessageAt() == null ? last.item().createdAt()
                        : last.item().lastMessageAt(), last.id()) : null);
    }

    public List<ConversationItem> listPinnedConversations(Long userId) {
        String sql = CONVERSATION_LIST_SELECT + """
                AND c.is_pinned=1
                ORDER BY COALESCE(c.last_message_at,c.created_at) DESC,c.id DESC LIMIT 20
                """;
        return withCompanyNames(jdbc.query(sql, this::readConversationRow,
                TenantContext.getEffectiveTenantId(), userId));
    }

    public void renameConversation(String sessionId, Long userId, String title) {
        if (title == null) throw new IllegalArgumentException("会话名称不能为空");
        String normalized = title.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty() || normalized.codePointCount(0, normalized.length()) > 60
                || normalized.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("会话名称须为 1～60 个字符");
        int changed = jdbc.update("""
                UPDATE ea_agent_conversation SET custom_title=?,updated_at=NOW()
                WHERE session_id=? AND tenant_id=? AND user_id=? AND deleted=0
                """, normalized, sessionId, TenantContext.getEffectiveTenantId(), userId);
        if (changed == 0) throw new IllegalArgumentException("会话不存在或无权访问");
    }

    @Transactional(rollbackFor = Exception.class)
    public void setPinned(String sessionId, Long userId, boolean pinned) {
        lockUserConversations(userId);
        List<Boolean> current = jdbc.query("""
                SELECT is_pinned FROM ea_agent_conversation
                WHERE session_id=? AND tenant_id=? AND user_id=? AND deleted=0
                  AND EXISTS (SELECT 1 FROM ea_agent_message m
                              WHERE m.tenant_id=ea_agent_conversation.tenant_id
                                AND m.company_id=ea_agent_conversation.company_id
                                AND m.session_id=ea_agent_conversation.session_id)
                """, (rs, rowNum) -> rs.getBoolean(1), sessionId, TenantContext.getEffectiveTenantId(), userId);
        if (current.isEmpty()) throw new IllegalArgumentException("会话不存在或无权访问");
        if (current.get(0) == pinned) return;
        if (pinned) {
            Integer count = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ea_agent_conversation
                    WHERE tenant_id=? AND user_id=? AND deleted=0 AND is_pinned=1
                      AND EXISTS (SELECT 1 FROM ea_agent_message m
                                  WHERE m.tenant_id=ea_agent_conversation.tenant_id
                                    AND m.company_id=ea_agent_conversation.company_id
                                    AND m.session_id=ea_agent_conversation.session_id)
                    """, Integer.class, TenantContext.getEffectiveTenantId(), userId);
            if (count != null && count >= 20) throw new IllegalStateException("最多置顶 20 条会话");
        }
        jdbc.update("""
                UPDATE ea_agent_conversation SET is_pinned=?,updated_at=NOW()
                WHERE session_id=? AND tenant_id=? AND user_id=? AND deleted=0
                """, pinned, sessionId, TenantContext.getEffectiveTenantId(), userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(String sessionId, Long userId) {
        lockUserConversations(userId);
        int changed = jdbc.update("""
                UPDATE ea_agent_conversation
                SET deleted=1,deleted_at=NOW(),deleted_by=?,is_pinned=0,updated_at=NOW()
                WHERE session_id=? AND tenant_id=? AND user_id=? AND deleted=0
                """, userId, sessionId, TenantContext.getEffectiveTenantId(), userId);
        if (changed == 0) throw new IllegalArgumentException("会话不存在或无权访问");
    }

    private void lockUserConversations(Long userId) {
        jdbc.query("""
                SELECT id FROM ea_agent_conversation
                WHERE tenant_id=? AND user_id=? AND deleted=0 ORDER BY id LIMIT 1 FOR UPDATE
                """, rs -> { }, TenantContext.getEffectiveTenantId(), userId);
    }

    private Row readConversationRow(ResultSet rs, int ignored) throws SQLException {
        String title = rs.getString("title");
        return new Row(rs.getLong("id"), new ConversationItem(
                rs.getString("session_id"), rs.getString("company_id"), null,
                title == null || title.isBlank() ? "新会话" : title.replaceAll("\\s+", " ").trim(),
                rs.getBoolean("is_pinned"), rs.getInt("message_count"),
                rs.getTimestamp("last_message_at") == null ? null : rs.getTimestamp("last_message_at").toInstant(),
                rs.getTimestamp("created_at").toInstant()));
    }

    private List<ConversationItem> withCompanyNames(List<Row> rows) {
        Set<String> companyIds = new LinkedHashSet<>();
        rows.forEach(row -> companyIds.add(row.item().companyId()));
        Map<String, String> names = loadCompanyNames(companyIds);
        return rows.stream().map(row -> {
            ConversationItem item = row.item();
            return new ConversationItem(item.sessionId(), item.companyId(),
                    names.get(item.companyId()), item.title(), item.pinned(), item.messageCount(),
                    item.lastMessageAt(), item.createdAt());
        }).toList();
    }

    private Map<String, String> loadCompanyNames(Set<String> companyIds) {
        if (companyIds.isEmpty()) return Map.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(companyIds.size(), "?"));
        return jdbc.query("SELECT company_id,company_name FROM sys_company WHERE company_id IN (" + placeholders + ")",
                rs -> {
                    Map<String, String> names = new HashMap<>();
                    while (rs.next()) names.put(rs.getString("company_id"), rs.getString("company_name"));
                    return names;
                }, companyIds.toArray());
    }

    private record Row(long id, ConversationItem item) { }
    private record Cursor(Instant time, long id) { }

    private String encodeCursor(Instant time, long id) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (time.toString() + ":" + id).getBytes(StandardCharsets.UTF_8));
    }

    private Cursor decodeCursor(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            int split = decoded.lastIndexOf(':');
            if (split < 0) throw new IllegalArgumentException();
            return new Cursor(Instant.parse(decoded.substring(0, split)),
                    Long.parseLong(decoded.substring(split + 1)));
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("会话分页位置无效");
        }
    }

    /**
     * 获取会话的最近消息列表
     */
    public List<AgentMessage> getRecentMessages(String sessionId, int limit) {
        AgentConversation conversation = getConversation(sessionId);
        if (conversation == null) return List.of();
        LambdaQueryWrapper<AgentMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentMessage::getSessionId, sessionId)
               .eq(AgentMessage::getTenantId, conversation.getTenantId())
               .eq(AgentMessage::getCompanyId, conversation.getCompanyId())
               .orderByDesc(AgentMessage::getCreatedAt)
               .orderByDesc(AgentMessage::getId)
               .last("LIMIT " + limit);

        List<AgentMessage> messages = messageMapper.selectList(wrapper);

        // 反转列表，使其按时间正序排列
        java.util.Collections.reverse(messages);

        return messages;
    }

    public MessagePage getMessagePage(AgentConversation conversation, String cursor) {
        Cursor before = decodeCursor(cursor);
        LambdaQueryWrapper<AgentMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentMessage::getSessionId, conversation.getSessionId())
                .eq(AgentMessage::getTenantId, conversation.getTenantId())
                .eq(AgentMessage::getCompanyId, conversation.getCompanyId());
        if (before != null) wrapper.and(group -> group
                .lt(AgentMessage::getCreatedAt, before.time())
                .or(inner -> inner.eq(AgentMessage::getCreatedAt, before.time())
                        .lt(AgentMessage::getId, before.id())));
        wrapper.orderByDesc(AgentMessage::getCreatedAt).orderByDesc(AgentMessage::getId).last("LIMIT 51");
        List<AgentMessage> rows = messageMapper.selectList(wrapper);
        boolean more = rows.size() > 50;
        List<AgentMessage> page = new ArrayList<>(rows.subList(0, Math.min(rows.size(), 50)));
        AgentMessage oldest = page.isEmpty() ? null : page.get(page.size() - 1);
        java.util.Collections.reverse(page);
        return new MessagePage(page, more && oldest != null
                ? encodeCursor(oldest.getCreatedAt(), oldest.getId()) : null);
    }

    /**
     * 获取会话消息数量
     */
    public int getMessageCount(String sessionId) {
        LambdaQueryWrapper<AgentMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentMessage::getSessionId, sessionId)
               .eq(AgentMessage::getTenantId, TenantContext.getEffectiveTenantId())
               .eq(AgentMessage::getCompanyId, TenantContext.getEffectiveCompanyId());

        return Math.toIntExact(messageMapper.selectCount(wrapper));
    }

    /**
     * 添加消息到会话
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentMessage addMessage(AgentConversation conversation, String role, String content, String intent) {
        if (conversation == null || conversation.getSessionId() == null) {
            throw new IllegalArgumentException("会话不能为空");
        }
        String sessionId = conversation.getSessionId();
        // 创建消息
        AgentMessage message = new AgentMessage();
        message.setTenantId(TenantContext.getEffectiveTenantId());
        message.setCompanyId(conversation.getCompanyId());
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setIntent(intent);
        message.setCreatedAt(Instant.now());

        messageMapper.insert(message);

        // 更新会话统计
        conversation.setMessageCount((conversation.getMessageCount() == null ? 0 : conversation.getMessageCount()) + 1);
        conversation.setLastMessageAt(Instant.now());
        conversation.preUpdate();
        conversationMapper.updateById(conversation);

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
    private String generateSessionId(Long userId) {
        String stamp = String.format("%013x", System.currentTimeMillis());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String payload = "sess_" + stamp + "_" + random;
        return payload + "_" + signature(payload, userId);
    }

    private boolean validNewSessionId(String sessionId, Long userId) {
        if (sessionId == null) return false;
        var match = NEW_SESSION_ID.matcher(sessionId);
        if (!match.matches()) return false;
        long created;
        try { created = Long.parseLong(match.group(1), 16); }
        catch (NumberFormatException invalid) { return false; }
        long age = System.currentTimeMillis() - created;
        if (age < 0 || age > RESERVATION_MILLIS) return false;
        String payload = sessionId.substring(0, sessionId.lastIndexOf('_'));
        return MessageDigest.isEqual(signature(payload, userId).getBytes(StandardCharsets.US_ASCII),
                match.group(3).getBytes(StandardCharsets.US_ASCII));
    }

    private String signature(String payload, Long userId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(sessionSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String bound = payload + ":" + TenantContext.getEffectiveTenantId() + ":"
                    + TenantContext.getEffectiveCompanyId() + ":" + userId;
            return java.util.HexFormat.of().formatHex(mac.doFinal(bound.getBytes(StandardCharsets.UTF_8)))
                    .substring(0, 24);
        } catch (java.security.GeneralSecurityException failure) {
            throw new IllegalStateException("会话签名不可用", failure);
        }
    }
}
