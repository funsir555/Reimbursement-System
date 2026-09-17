package com.finex.auth.agent.workflow;

import lombok.Builder;
import lombok.Data;

/**
 * 受控工具执行请求。
 */
@Data
@Builder
public class ToolExecutionRequest {

    private Long userId;
    private String sessionId;
    private String traceId;
    private String queryTemplateCode;
    private QueryTemplateVariables variables;
}
