package com.finex.auth.agent.workflow;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.math.BigDecimal;

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
    private Long digitalEmployeeId;
    private Long responsibleUserId;
    private Long agentVersionId;
    private String queryPermissionCode;
    private String authorizationDigest;
    private Integer resultCount;
    private BigDecimal totalAmount;
    private Integer displayedCount;
    private String queryDescriptorJson;
    private String sourceDigest;
    private List<ToolOrigin> toolOrigins;

    public record ToolOrigin(String nodeKey, String toolCode, String permissionCode,
                             String authorizationDigest, String queryDescriptorJson,
                             String sourceDigest) { }
}
