package com.finex.auth.support.archiveagent;

import java.util.Map;

public interface ModelProviderAdapter {

    String getProviderCode();

    InvocationResult invoke(InvocationContext context);

    default InvocationResult invokeStream(InvocationContext context, java.util.function.Consumer<String> onChunk) {
        InvocationResult result = invoke(context);
        if (result.content() != null && !result.content().isEmpty()) onChunk.accept(result.content());
        return result;
    }

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
