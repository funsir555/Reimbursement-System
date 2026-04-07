package com.finex.auth.support.archiveagent;

import java.util.Map;

public interface ModelProviderAdapter {

    String getProviderCode();

    InvocationResult invoke(InvocationContext context);

    record InvocationContext(
            Long userId,
            String agentName,
            Map<String, Object> modelConfig,
            Map<String, Object> promptConfig,
            Map<String, Object> nodeConfig,
            Map<String, Object> runtimeContext
    ) {
    }

    record InvocationResult(String content, Map<String, Object> outputPayload) {
    }
}
