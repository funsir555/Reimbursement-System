package com.finex.auth.agent.service;

import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.entity.AgentConversation;
import com.finex.auth.agent.entity.AgentMessage;
import com.finex.auth.agent.mapper.AgentConversationMapper;
import com.finex.auth.agent.mapper.AgentMessageMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class ConversationServiceTest {

    private final AgentConversationMapper conversationMapper = mock(AgentConversationMapper.class);
    private final AgentMessageMapper messageMapper = mock(AgentMessageMapper.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ConversationService service = new ConversationService(conversationMapper, messageMapper, jdbc,
            "test-only-session-signing-key-with-sufficient-length");

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void reservesWithoutInsertAndPersistsFirstMessageTogether() {
        TenantContext.setTenantId("default");
        TenantContext.setCompanyId("COMPANY_A");
        AgentConversation reserved = service.createConversation(7L);
        verify(conversationMapper, never()).insert(any());

        AgentConversation saved = service.createWithFirstMessage(reserved.getSessionId(), 7L, "你好");
        assertThat(saved.getCompanyId()).isEqualTo("COMPANY_A");
        assertThat(saved.getMessageCount()).isEqualTo(1);
        verify(conversationMapper).insert(any(AgentConversation.class));
        verify(messageMapper).insert(any(AgentMessage.class));
    }

    @Test
    void rejectsForgedOrCrossUserOrCrossCompanyReservation() {
        TenantContext.setTenantId("default");
        TenantContext.setCompanyId("COMPANY_A");
        String id = service.createConversation(7L).getSessionId();
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.createWithFirstMessage(id, 8L, "你好")).isInstanceOf(IllegalArgumentException.class);
        TenantContext.setCompanyId("COMPANY_B");
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.createWithFirstMessage(id, 7L, "你好")).isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.createWithFirstMessage("sess_" + "0".repeat(32), 7L, "你好"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(conversationMapper, never()).insert(any());
    }

    @Test
    void conflictingFirstSendNeverAppendsAnotherMessage() {
        TenantContext.setTenantId("default");
        TenantContext.setCompanyId("COMPANY_A");
        String id = service.createConversation(7L).getSessionId();
        when(conversationMapper.insert(any(AgentConversation.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.createWithFirstMessage(id, 7L, "你好"))
                .isInstanceOf(IllegalStateException.class);
        verify(messageMapper, never()).insert(any());
    }

    @Test
    void appendsMessageUsingAlreadyAuthorizedConversationWithoutReloadingIt() {
        TenantContext.setTenantId("default");
        TenantContext.setCompanyId("GROUP_HQ");
        TenantContext.setUserId(7L);
        AgentConversation conversation = new AgentConversation();
        conversation.setId(11L);
        conversation.setSessionId("session-1");
        conversation.setCompanyId("GROUP_HQ");
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

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void listsThirtyOwnConversationsAcrossCompaniesAndReturnsStableCursor() throws Exception {
        TenantContext.setTenantId("default");
        Instant timestamp = Instant.parse("2026-10-12T02:00:00Z");
        doAnswer(call -> {
            RowMapper mapper = call.getArgument(1);
            var rows = new ArrayList<>();
            int firstId = call.getArguments().length == 4 ? 31 : 1;
            for (int id = firstId; id >= 1; id--) {
                ResultSet rs = mock(ResultSet.class);
                when(rs.getLong("id")).thenReturn((long) id);
                when(rs.getString("session_id")).thenReturn("session-" + id);
                when(rs.getString("company_id")).thenReturn(id == 29 ? "MISSING" : id % 2 == 0 ? "A" : "B");
                when(rs.getString("title")).thenReturn("问题" + id);
                when(rs.getInt("message_count")).thenReturn(2);
                when(rs.getTimestamp("last_message_at")).thenReturn(Timestamp.from(timestamp));
                when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(timestamp));
                rows.add(mapper.mapRow(rs, rows.size()));
            }
            return rows;
        }).when(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
        doAnswer(call -> {
            Object[] companyIds = java.util.Arrays.copyOfRange(call.getArguments(), 2, call.getArguments().length);
            List<String> found = java.util.Arrays.stream(companyIds).map(String::valueOf)
                    .filter(id -> !"MISSING".equals(id)).toList();
            ResultSet rs = mock(ResultSet.class);
            AtomicInteger index = new AtomicInteger();
            when(rs.next()).thenAnswer(ignored -> index.getAndIncrement() < found.size());
            when(rs.getString("company_id")).thenAnswer(ignored -> found.get(index.get() - 1));
            when(rs.getString("company_name")).thenAnswer(ignored -> "公司" + found.get(index.get() - 1));
            ResultSetExtractor<?> extractor = call.getArgument(1);
            return extractor.extractData(rs);
        }).when(jdbc).query(anyString(), any(ResultSetExtractor.class), any(Object[].class));

        ConversationService.ConversationPage page = service.listUserConversations(7L, null);
        assertThat(page.items()).hasSize(30);
        assertThat(page.items().get(0).companyId()).isEqualTo("B");
        assertThat(page.items().get(0).companyName()).isEqualTo("公司B");
        assertThat(page.items().get(2).companyName()).isNull();
        assertThat(page.nextCursor()).isNotNull();
        assertThat(new String(Base64.getUrlDecoder().decode(page.nextCursor()), StandardCharsets.UTF_8))
                .endsWith(":2");
        ConversationService.ConversationPage older = service.listUserConversations(7L, page.nextCursor());
        assertThat(older.items()).hasSize(1);
        assertThat(older.items().get(0).sessionId()).isEqualTo("session-1");
        assertThat(older.nextCursor()).isNull();
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(2)).query(sql.capture(), any(RowMapper.class), any(Object[].class));
        assertThat(sql.getAllValues()).allSatisfy(query -> assertThat(query)
                .contains("c.tenant_id=? AND c.user_id=?")
                .contains("EXISTS (SELECT 1 FROM ea_agent_message visible")
                .contains("visible.tenant_id=c.tenant_id AND visible.company_id=c.company_id")
                .contains("COALESCE(c.custom_title")
                .doesNotContain("JOIN sys_company", "c.company_id=?"));
        ArgumentCaptor<String> companySql = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(2)).query(companySql.capture(), any(ResultSetExtractor.class), any(Object[].class));
        assertThat(companySql.getAllValues()).allSatisfy(query -> assertThat(query)
                .contains("FROM sys_company WHERE company_id IN (")
                .doesNotContain("ea_agent_conversation"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void pinnedListUsesOwnerTenantAndCurrentMessageOrdering() throws Exception {
        TenantContext.setTenantId("tenant-A");
        doAnswer(call -> {
            RowMapper mapper = call.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getLong("id")).thenReturn(9L);
            when(rs.getString("session_id")).thenReturn("pinned-9");
            when(rs.getString("company_id")).thenReturn("COMPANY_A");
            when(rs.getString("title")).thenReturn("自定义标题");
            when(rs.getBoolean("is_pinned")).thenReturn(true);
            when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.parse("2026-10-14T00:00:00Z")));
            return List.of(mapper.mapRow(rs, 0));
        }).when(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
        doAnswer(call -> {
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true, false);
            when(rs.getString("company_id")).thenReturn("COMPANY_A");
            when(rs.getString("company_name")).thenReturn("甲公司");
            return ((ResultSetExtractor<?>) call.getArgument(1)).extractData(rs);
        }).when(jdbc).query(anyString(), any(ResultSetExtractor.class), any(Object[].class));

        var pinned = service.listPinnedConversations(7L);
        assertThat(pinned).hasSize(1);
        assertThat(pinned.get(0).title()).isEqualTo("自定义标题");
        assertThat(pinned.get(0).pinned()).isTrue();
        assertThat(pinned.get(0).companyName()).isEqualTo("甲公司");
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(RowMapper.class), eq("tenant-A"), eq(7L));
        assertThat(sql.getValue()).contains("c.is_pinned=1", "c.deleted=0",
                "ORDER BY COALESCE(c.last_message_at,c.created_at) DESC,c.id DESC LIMIT 20");
    }

    @Test
    void renameValidatesLengthAndScopesUpdateToOwnerAndTenant() {
        TenantContext.setTenantId("tenant-A");
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        service.renameConversation("session-A", 7L, "  新标题  ");
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), eq("新标题"), eq("session-A"), eq("tenant-A"), eq(7L));
        assertThat(sql.getValue()).contains("custom_title=?", "deleted=0", "tenant_id=?", "user_id=?");
        assertThatThrownBy(() -> service.renameConversation("session-A", 7L, "  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.renameConversation("session-A", 7L, "字".repeat(61)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pinLimitIsCheckedAfterOwnerLockAndDoesNotUpdate() {
        TenantContext.setTenantId("tenant-A");
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of(false));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(20);
        assertThatThrownBy(() -> service.setPinned("session-A", 7L, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("20");
        verify(jdbc).query(org.mockito.ArgumentMatchers.contains("FOR UPDATE"),
                any(org.springframework.jdbc.core.RowCallbackHandler.class), eq("tenant-A"), eq(7L));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void missingOrDeletedConversationCannotBeManaged() {
        TenantContext.setTenantId("tenant-A");
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
        assertThatThrownBy(() -> service.setPinned("other-user-session", 7L, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.renameConversation("other-user-session", 7L, "新名称"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.deleteConversation("other-user-session", 7L))
                .isInstanceOf(IllegalArgumentException.class);
        verify(jdbc, never()).update(org.mockito.ArgumentMatchers.contains("is_pinned=?"),
                any(Object[].class));
    }

    @Test
    void softDeleteKeepsMessagesAndPreventsFurtherActiveConversationReads() {
        TenantContext.setTenantId("tenant-A");
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        service.deleteConversation("session-A", 7L);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), eq(7L), eq("session-A"), eq("tenant-A"), eq(7L));
        assertThat(sql.getValue()).contains("deleted=1", "deleted_at=NOW()", "deleted_by=?",
                "is_pinned=0", "tenant_id=?", "user_id=?", "deleted=0");
        verify(messageMapper, never()).delete(any());
        assertThat(service.getConversation("session-A")).isNull();
        verify(conversationMapper).selectOne(any());
    }
}
