package com.finex.auth.support.archiveagent;

import com.finex.auth.agent.context.AgentExecutionContext;
import com.finex.auth.agent.context.TraceContext;
import com.finex.auth.entity.ArchiveAgentRun;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

/**
 * Resolves the trusted identity snapshot used by an asynchronous Agent run.
 */
@Component
@RequiredArgsConstructor
public class ArchiveAgentExecutionContextResolver {

    private static final String DEFAULT_TENANT_ID = "default";

    private final UserMapper userMapper;

    public AgentExecutionContext resolve(ArchiveAgentRun run, AgentExecutionContext capturedContext) {
        if (run == null || run.getOwnerUserId() == null) {
            throw new IllegalStateException("Agent 运行缺少所有者身份");
        }

        AgentExecutionContext captured = capturedContext == null
                ? new AgentExecutionContext()
                : capturedContext;
        Long ownerUserId = run.getOwnerUserId();
        Long capturedUserId = captured.getUserId();
        if (capturedUserId != null && !Objects.equals(capturedUserId, ownerUserId)) {
            throw new SecurityException("Agent 运行用户与提交身份不一致");
        }

        User owner = userMapper.selectById(ownerUserId);
        if (owner == null || !Integer.valueOf(1).equals(owner.getStatus())) {
            throw new SecurityException("Agent 运行用户不存在或已停用");
        }
        String ownerCompanyId = normalize(owner.getCompanyId());
        if (ownerCompanyId == null) {
            throw new SecurityException("Agent 运行用户缺少可用的公司上下文");
        }
        String companyId = capturedUserId == null
                ? ownerCompanyId
                : normalize(captured.getCompanyId());
        if (companyId == null) {
            throw new SecurityException("Agent 提交身份缺少可用的公司上下文");
        }

        String tenantId = normalize(captured.getTenantId());
        String traceId = normalize(captured.getTraceId());
        return AgentExecutionContext.builder()
                .tenantId(tenantId == null ? DEFAULT_TENANT_ID : tenantId)
                .companyId(companyId)
                .userId(ownerUserId)
                .traceId(traceId == null ? TraceContext.generateTraceId() : traceId)
                .conversationId(captured.getConversationId())
                .agentRunId(run.getId())
                .triggerSource(run.getTriggerSource())
                .createdAt(captured.getCreatedAt() == null ? Instant.now() : captured.getCreatedAt())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
