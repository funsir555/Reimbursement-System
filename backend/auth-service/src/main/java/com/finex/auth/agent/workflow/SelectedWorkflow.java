package com.finex.auth.agent.workflow;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.List;

/**
 * Workflow Selector 的选择结果。
 */
@Data
@Builder
public class SelectedWorkflow {

    private Long agentId;
    private Long agentVersionId;
    private Long ownerUserId;
    private String agentName;
    private String agentDescription;
    private String toolCode;
    private List<String> toolCodes;
    private List<String> queryPermissionCodes;
    private String providerCode;
    private String model;
    private Map<String, Object> config;
    private boolean builtInFallback;
    private Long digitalEmployeeId;
    private String digitalEmployeeName;
    private Long responsibleUserId;
    private String queryPermissionCode;
    private Long workflowBindingId;
}
