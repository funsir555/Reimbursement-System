package com.finex.auth.agent.context;

import java.util.UUID;

/**
 * 追踪上下文
 * 用于分布式追踪和日志关联
 */
public class TraceContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    /**
     * 设置追踪ID
     */
    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    /**
     * 获取当前追踪ID
     */
    public static String getCurrentTraceId() {
        String traceId = TRACE_ID.get();
        if (traceId == null) {
            traceId = generateTraceId();
            TRACE_ID.set(traceId);
        }
        return traceId;
    }

    /**
     * 生成新的追踪ID
     */
    public static String generateTraceId() {
        return "trace_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 清理追踪ID
     */
    public static void clear() {
        TRACE_ID.remove();
    }
}
