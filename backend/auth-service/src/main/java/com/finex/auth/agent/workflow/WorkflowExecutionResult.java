package com.finex.auth.agent.workflow;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 工作流执行结果。
 */
@Data
@Builder
public class WorkflowExecutionResult {

    private boolean success;
    private String content;
    private String errorMessage;
    private String intent;
    private Long runId;
    private String traceId;
    private String mappingId;
    private List<String> evidenceRefs;
}
