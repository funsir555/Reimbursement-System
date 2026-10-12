package com.finex.auth.agent.workflow;

import com.finex.auth.service.queryauth.QueryAuthorization;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * 受控工具执行请求。
 */
@Data
@Builder
public class ToolExecutionRequest {

    private Long userId;
    private String companyId;
    private String sessionId;
    private String traceId;
    private String queryTemplateCode;
    private QueryTemplateVariables variables;
    private QueryAuthorization authorization;
    private String userMessage;
    private Map<String, Object> parameters;
}
