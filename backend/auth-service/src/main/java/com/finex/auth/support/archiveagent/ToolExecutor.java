package com.finex.auth.support.archiveagent;

import java.util.List;
import java.util.Map;

public interface ToolExecutor {

    List<String> supportedToolCodes();

    ToolResult execute(ToolContext context);

    record ToolContext(
            Long userId,
            String toolCode,
            Map<String, Object> toolConfig,
            Map<String, Object> runtimeContext
    ) {
    }

    record ToolResult(String summary, Map<String, Object> outputPayload) {
    }
}
