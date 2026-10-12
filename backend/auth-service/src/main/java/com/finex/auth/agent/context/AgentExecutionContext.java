package com.finex.auth.agent.context;

import com.finex.auth.util.SecurityUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.time.Instant;

/**
 * Agent 执行上下文
 * 用于异步任务的上下文传播
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecutionContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 公司ID
     */
    private String companyId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * Agent Run ID
     */
    private Long agentRunId;

    /**
     * 触发源
     */
    private String triggerSource;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 从当前线程捕获上下文
     */
    public static AgentExecutionContext capture() {
        return AgentExecutionContext.builder()
                .tenantId(TenantContext.getTenantId())
                .companyId(TenantContext.getCompanyId())
                .userId(TenantContext.getUserId())
                .traceId(TraceContext.currentOrNull())
                .createdAt(Instant.now())
                .build();
    }

    /**
     * 恢复到当前线程
     */
    public void restore() {
        if (this.tenantId != null) {
            TenantContext.setTenantId(this.tenantId);
        }
        if (this.companyId != null) {
            TenantContext.setCompanyId(this.companyId);
        }
        if (this.userId != null) {
            TenantContext.setUserId(this.userId);
            SecurityUtils.setCurrentUserId(this.userId);
        }
        if (this.traceId != null) {
            TraceContext.setTraceId(this.traceId);
        }
    }

    /**
     * 清理当前线程上下文
     */
    public static void clear() {
        TenantContext.clear();
        SecurityUtils.clear();
        TraceContext.clear();
    }
}
